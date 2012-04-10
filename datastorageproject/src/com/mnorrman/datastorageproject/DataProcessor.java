/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

import com.mnorrman.datastorageproject.index.LocalIndex;
import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.objects.UnindexedDataObject;
import java.io.*;
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
    
    
    /**
     * Constructor. Requires a new FileChannel-object from the backstorage.
     * @param channel FileChannel from the backstorage.
     */
    public DataProcessor(FileChannel channel){
        this.dataChannel = channel;
    }
    
    /**
     * A method for retrieving data from our backStorage. It does not return any
     * data, it simply returns a boolean value telling if the operation was 
     * successful or not. 
     * @param os An outputstream to which the data will be written.
     * @param ido The indexedDataObject that contains the metadata for the data.
     * @return True if everything went as expected, otherwise false.
     */
    public boolean retrieveData(OutputStream os, IndexedDataObject ido){
        try{
            ByteBuffer buffer = ByteBuffer.allocate(BlOCK_SIZE);
            dataChannel.position(ido.getOffset() + 512);
            
            int readBytes = 0;
            long totalBytes = ido.getLength();

            while(totalBytes > 0){
                buffer.clear();
                readBytes = dataChannel.read(buffer);
                buffer.flip();
                
                if(readBytes >= totalBytes){
                    os.write(buffer.array(), 0, (int)(totalBytes));
                }else{
                    os.write(buffer.array(), 0, readBytes);
                }
                
                totalBytes -= readBytes;
                if(totalBytes <= 0)
                    break;
            }
            os.flush();
            return true;
        }catch(IOException e){
            Logger.getLogger("b-log").log(Level.SEVERE, "An error occured when retrieving data!", e);
        }
        return false;
    }

    public IndexedDataObject storeData(UnindexedDataObject udo){
        
        CRC32 crc = new CRC32();
        String fileName = udo.getColname() + "-" + udo.getRowname() + Math.random();
        File tempFile = new File(fileName);
        
        int readBytes = 0;
        long totalBytesRead = 0;
        byte[] bytes = null;
        
        try {
            FileOutputStream fos = new FileOutputStream(tempFile);
            while(totalBytesRead < udo.getLength()){
                bytes = new byte[BlOCK_SIZE];
                readBytes = udo.getStream().read(bytes);
                crc.update(bytes, 0, readBytes);
                fos.write(bytes, 0, readBytes);
                totalBytesRead += readBytes;
                if(totalBytesRead >= udo.getLength())
                    break;
            }
            fos.flush();
            fos.close();
        }catch(IOException e){
            Main.logger.log(e, LogTool.CRITICAL);
        }

        udo.setChecksum(crc.getValue());
        
        long filesizeBeforeOperation = -1; 
        
        
        
        try{
            ByteBuffer bbb = MetaDataComposer.decompose(udo);
            bbb.position(264);
            long newVersion = bbb.getLong();
            bbb.position(0);
            
            FileLock fl = dataChannel.lock();

            long newOffset = dataChannel.size();
                        
            filesizeBeforeOperation = newOffset;
            
            dataChannel.position(newOffset);
            dataChannel.write(bbb);
            
            //-This part makes sure that the full amount of bytes are pre-
            //allocated, thus making it easier to rollback the changes.
            //(Since we still know how much data to remove)
            long tempPos = dataChannel.position();
            ByteBuffer voidbuf = ByteBuffer.allocate(1);
            voidbuf.put((byte)0);
            voidbuf.flip();
            if(tempPos+(udo.getLength()-1) < 0)
                dataChannel.position(0);
            else
                dataChannel.position(tempPos+(udo.getLength()-1));
            dataChannel.write(voidbuf);
            dataChannel.position(tempPos);
            
            FileInputStream fis = new FileInputStream(tempFile);
            FileChannel fc = fis.getChannel();
            
            //Transfer all data from the temporary file into the backstorage.
            dataChannel.transferFrom(fc, dataChannel.position(), tempFile.length());
            fc.close();
            
            //Remove the temporary file.
            tempFile.delete();
            
            fl.release();
            return new IndexedDataObject(udo, newOffset, newVersion);
            
        }catch(IOException e){
            Main.logger.log(e, LogTool.CRITICAL);
            try{
                dataChannel.truncate(filesizeBeforeOperation);
            }catch(IOException e2){
                Main.logger.log(e2, LogTool.CRITICAL);
            }
        }
        return null;
    }
    
    public static void main(String[] args) {
        BackStorage b = null;
        try{
            b = new BackStorage().initialize();
        }catch(Exception e){
            Logger.getLogger("b-log").log(Level.SEVERE, "Error in main", e);
        }
        DataProcessor p = new DataProcessor(b.getChannel());
        LocalIndex li = new LocalIndex();
        li.insertAll(b.reindexData());
        System.out.println("Successful? " + p.retrieveData(System.out, li.get("a", "a")));
        System.out.println("Done");
    }
}
