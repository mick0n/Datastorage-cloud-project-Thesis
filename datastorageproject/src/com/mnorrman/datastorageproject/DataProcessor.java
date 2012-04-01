/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.objects.UnindexedDataObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

/**
 *
 * @author Mikael
 */
public class DataProcessor {
    
    public static final int BlOCK_SIZE = 131072;
    
    private FileChannel dataChannel;
    
    public DataProcessor(FileChannel channel){
        this.dataChannel = channel;
    }
    
    public ByteBuffer retrieveData(IndexedDataObject ido){
        try{
            ByteBuffer data = ByteBuffer.allocateDirect((int)ido.getLength());
            dataChannel.position(ido.getOffset() + 512);
            dataChannel.read(data);
            data.flip();
            return data;
        }catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }

    public IndexedDataObject storeData(UnindexedDataObject udo){
        
        CRC32 crc = new CRC32();
        String fileName = udo.getColname() + "-" + udo.getRowname() + Math.random();
        File file = new File(fileName);
        
        int readBytes = 0;
        long totalBytesRead = 0;
        byte[] bytes = null;
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            do{
                bytes = new byte[BlOCK_SIZE];
                readBytes = udo.getStream().read(bytes);
                crc.update(bytes, 0, readBytes);
                fos.write(bytes, 0, readBytes);
                totalBytesRead += readBytes;
            }while(totalBytesRead < udo.getLength());
            fos.flush();
        }catch(IOException e){
            e.printStackTrace();
        }

        udo.setChecksum(crc.getValue());
        
        long filesizeBeforeOperation = -1; 
        
        try{
            ByteBuffer buffer = ByteBuffer.allocateDirect(512); //416 + 96 (Void)
            byte[] colnameBytes = new byte[128];
            byte[] rownameBytes = new byte[128];
            byte[] ownerBytes = new byte[128];
            System.arraycopy(udo.getColname().getBytes(), 0, colnameBytes, 0, udo.getColname().getBytes().length);
            System.arraycopy(udo.getRowname().getBytes(), 0, rownameBytes, 0, udo.getRowname().getBytes().length);
            System.arraycopy(udo.getOwner().getBytes(), 0, ownerBytes, 0, udo.getOwner().getBytes().length);
            
            long newVersion = System.currentTimeMillis();
            
            buffer.put(colnameBytes);
            buffer.put(rownameBytes);
            buffer.putLong(newVersion);
            buffer.putLong(udo.getLength());
            buffer.putLong(udo.getChecksum());
            buffer.put(ownerBytes);
            
            byte[] voidData = new byte[104];
            buffer.put(voidData);
            buffer.flip();
            FileLock fl = dataChannel.lock();
            long newOffset = dataChannel.size();
            filesizeBeforeOperation = newOffset;
            
            dataChannel.position(newOffset);
            dataChannel.write(buffer);
            
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            fc.transferTo(0, file.length(), dataChannel);
            fc.close();
            file.delete();
            
            fl.release();
            return new IndexedDataObject(udo, newOffset, newVersion);
            
        }catch(IOException e){
            Logger.getGlobal().log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
            try{
                dataChannel.truncate(filesizeBeforeOperation);
            }catch(IOException e2){
                e2.printStackTrace();
            }
        }
        return null;
    }
}
