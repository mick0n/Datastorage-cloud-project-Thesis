/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.objects.UnindexedDataObject;
import com.mnorrman.datastorageproject.tools.Checksum;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 *
 * @author Mikael
 */
public class Main {

    public static Index localIndex;
    public BackStorage storage;
    
    public Main(){
        try{
            storage = new BackStorage().initialize();
            DataProcessor dp = new DataProcessor(storage.getChannel());
            
            File file = new File("testfile.exe");
            ByteBuffer dataBuffer = ByteBuffer.allocate((int)file.length());
            FileChannel fc = new FileInputStream(file).getChannel();
            fc.read(dataBuffer);
            fc.close();
            UnindexedDataObject udo = new UnindexedDataObject(dataBuffer, "Example1", "Example1-2", "Mikael Norrman");
            
            localIndex.insertIndex(dp.storeData(udo));
            
            IndexedDataObject ido = localIndex.get("Example1", "Example1-2");
            System.out.println(ido);
            
        }catch(IOException e){
            e.printStackTrace();
        }
        /*try{
            storage = new BackStorage().initialize();
            localIndex.insertAll(storage.reindexData());
            
            System.out.println("Local index has: " + localIndex.size());
            System.out.println("Number of versions for first: " + localIndex.getNumberOfVersions(localIndex.get("Example1", "Example1-2")));
        }catch(IOException e){
            e.printStackTrace();
        }*/
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        localIndex = new Index();
        Main m = new Main();
    }
}
