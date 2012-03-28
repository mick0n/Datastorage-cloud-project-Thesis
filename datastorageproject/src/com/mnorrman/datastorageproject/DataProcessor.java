/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

import com.mnorrman.datastorageproject.objects.IndexedDataObject;
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
    
    public IndexedDataObject retrieveMetaData(long offset, long length){
        try{
            ByteBuffer data = ByteBuffer.allocateDirect(512 + (int)length);
            dataChannel.position(offset);
            dataChannel.read(data);
            data.rewind();
            
            byte[] temp;
            byte[] checksum = new byte[16];
            String colname, rowname, owner;
            long version, len;
            
            temp = new byte[128];
            data.get(temp);
            colname = new String(temp).trim();
            
            temp = new byte[128];
            data.get(temp);
            rowname = new String(temp).trim();
            
            version = data.getLong();
            
            len = data.getLong();
            
            data.get(checksum);
            
            temp = new byte[128];
            data.get(temp);
            owner = new String(temp).trim();
            
            //return new IndexedDataObject(colname, rowname, owner, offset, length, version, checksum)
        }catch(IOException e){
            Logger.getGlobal().log(Level.SEVERE, e.getMessage());
        }
        return null;
    }
    
    public void testReadData(){
        try{
            ByteBuffer buff = ByteBuffer.allocateDirect(512);
            byte[] temp;
            dataChannel.position(0);
            dataChannel.read(buff);
            buff.flip();

            temp = new byte[128];
            buff.get(temp);
            System.out.println("Value 1: " + new String(temp).trim());

            temp = new byte[128];
            buff.get(temp);
            System.out.println("Value 2: " + new String(temp).trim());

            System.out.println("Value 3: " + buff.getLong());
            System.out.println("Value 4: " + buff.getLong());

            temp = new byte[16];
            buff.get(temp);
            System.out.println("Value 5: " + new String(temp));

            temp = new byte[128];
            buff.get(temp);
            System.out.println("Value 6: " + new String(temp).trim());
            
            buff.clear();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
    public boolean storeData(String colname, String rowname, long version, long length, String owner, ByteBuffer data){
        try{
            ByteBuffer buffer = ByteBuffer.allocateDirect(512); //416 + 96 (Void)
            byte[] colnameBytes = new byte[128];
            byte[] rownameBytes = new byte[128];
            byte[] ownerBytes = new byte[128];
            System.arraycopy(colname.getBytes(), 0, colnameBytes, 0, colname.getBytes().length);
            System.arraycopy(rowname.getBytes(), 0, rownameBytes, 0, rowname.getBytes().length);
            System.arraycopy(owner.getBytes(), 0, ownerBytes, 0, owner.getBytes().length);
            
            buffer.put(colnameBytes);
            buffer.put(rownameBytes);
            buffer.putLong(version);
            buffer.putLong(length);
            buffer.put(Checksum.getFor(data));
            buffer.put(ownerBytes);
            
            byte[] voidData = new byte[96];
            buffer.put(voidData);
            buffer.flip();
            data.flip();
            dataChannel.position(dataChannel.size());
            dataChannel.write(buffer);
            dataChannel.write(data);
            return true;
            
        }catch(IOException e){
            Logger.getGlobal().log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    public static void main(String[] args) {
        BackStorage b = new BackStorage();
        try{
            b.initialize();
        }catch(IOException e){ e.printStackTrace(); }
        DataProcessor p = new DataProcessor(b.getChannel());
        ByteBuffer buf = ByteBuffer.allocate(10);
        buf.put(new byte[]{ 110, 111, 112, 113, 114, 115, 116, 117, 118, 119 });
        p.storeData("Testvalue a", "Testvalue b", System.currentTimeMillis(), 10L, "Ownervalue", buf);
        
        p.testReadData();
        
        System.out.println("Testing to retrieve teh data!!!!");
        IndexedDataObject o = new IndexedDataObject("Testvalue a", "Testvalue b", "ownervalue", 0, 10, 1L, Checksum.getFor(buf));
        
        byte[] stringData = new byte[(int)o.getLength()];
        p.retrieveData(o).get(stringData);
        System.out.println(new String(stringData).trim());
    }
    
}
