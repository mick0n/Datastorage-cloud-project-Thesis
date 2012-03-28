/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

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
    
    
    public ByteBuffer getHead(String col, String row){
        
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
    
    public boolean insertData(String colname, String rowname, long version, long length, String owner, ByteBuffer data){
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
            
            dataChannel.position(dataChannel.size());
            dataChannel.write(buffer);
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
        buf.put(new byte[]{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        p.insertData("Testvalue a", "Testvalue b", System.currentTimeMillis(), 10L, "Ownervalue", buf);
        
        p.testReadData();
    }
    
}
