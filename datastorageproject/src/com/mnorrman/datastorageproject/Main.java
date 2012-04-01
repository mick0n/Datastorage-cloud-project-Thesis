/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.objects.UnindexedDataObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
            
            File file = new File("largefile2");
            //ByteBuffer dataBuffer = ByteBuffer.allocate((int)file.length());
            //FileChannel fc = new FileInputStream(file).getChannel();
            //fc.read(dataBuffer);
            //fc.close();
            UnindexedDataObject udo = new UnindexedDataObject(new FileInputStream(file), "Example2", "Example2-2", "Mikael Norrman", file.length());
            
            localIndex.insertIndex(dp.storeData(udo));
            
            IndexedDataObject ido = localIndex.getWithHash(udo.getHash());
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
