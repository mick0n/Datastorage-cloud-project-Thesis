/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.objects.UnindexedDataObject;
import com.mnorrman.datastorageproject.tools.Checksum;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mikael
 */
public class DataProcessor {
    
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
            buffer.put(Checksum.getFor(udo.getData()));
            buffer.put(ownerBytes);
            
            byte[] voidData = new byte[96];
            buffer.put(voidData);
            buffer.flip();
            udo.getData().flip();
            
            long newOffset = dataChannel.size();
            
            dataChannel.position(dataChannel.size());
            dataChannel.write(buffer);
            dataChannel.write(udo.getData());
            return new IndexedDataObject(udo, newOffset, newVersion);
            
        }catch(IOException e){
            Logger.getGlobal().log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    public static void main(String[] args) {
        BackStorage b = new BackStorage();
        try{
            b.initialize();
        }catch(IOException e){ e.printStackTrace(); }
        
        DataProcessor p = new DataProcessor(b.getChannel());
        ByteBuffer buf = ByteBuffer.allocate(10);
        buf.put(new byte[]{ 110, 111, 112, 113, 114, 115, 116, 117, 118, 119 });
        
        UnindexedDataObject udo = new UnindexedDataObject(buf, "This is the column", "This is the row", "Mikael Norrman");
        IndexedDataObject ido = p.storeData(udo);
        
        System.out.println(ido);
    }
    
}
