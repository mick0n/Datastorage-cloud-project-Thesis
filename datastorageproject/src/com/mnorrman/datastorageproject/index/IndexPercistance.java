/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.index;

import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author Mikael
 */
public class IndexPercistance implements Runnable{

    private String className;
    private Collection<ArrayList<IndexedDataObject>> data;

    public IndexPercistance(String className, Collection<ArrayList<IndexedDataObject>> data) {
        this.className = className;
        this.data = data;
    }
    
    @Override
    public void run() {
        try{
            File file = new File(className + "_");
            FileChannel output = new FileOutputStream(file).getChannel();
            FileLock lock = output.lock();
            
            ByteBuffer buffer = ByteBuffer.allocateDirect(512); //416 + 96 (Void)
            for(ArrayList<IndexedDataObject> al : data){
                for(IndexedDataObject i : al){
                    byte[] colnameBytes = new byte[128];
                    byte[] rownameBytes = new byte[128];
                    byte[] ownerBytes = new byte[128];
                    System.arraycopy(i.getColname().getBytes(), 0, colnameBytes, 0, i.getColname().getBytes().length);
                    System.arraycopy(i.getRowname().getBytes(), 0, rownameBytes, 0, i.getRowname().getBytes().length);
                    System.arraycopy(i.getOwner().getBytes(), 0, ownerBytes, 0, i.getOwner().getBytes().length);

                    buffer.put(colnameBytes);
                    buffer.put(rownameBytes);
                    buffer.putLong(i.getVersion());
                    buffer.putLong(i.getLength());
                    buffer.putLong(i.getChecksum());
                    buffer.put(ownerBytes);

                    byte[] voidData = new byte[104];
                    buffer.put(voidData);
                    buffer.flip();
                    
                    output.write(buffer);
                    buffer.clear();
                }
            }
            lock.release();
            output.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
}
