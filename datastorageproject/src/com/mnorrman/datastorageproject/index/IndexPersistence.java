/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.index;

import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.tools.MetaDataComposer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mikael
 */
public class IndexPersistence implements Runnable{

    private Collection<ArrayList<IndexedDataObject>> data;

    public IndexPersistence(Collection<ArrayList<IndexedDataObject>> data) {
        this.data = data;
    }
    
    public IndexPersistence(){
        this.data = null;
    }
    
    public List<IndexedDataObject> load(){
        List<IndexedDataObject> l = new LinkedList<IndexedDataObject>();
        File file = new File("LocalIndex_");
        
        //If the file exist, try to load the data
        if(file.exists()){
            try{
                FileChannel input = new FileInputStream(file).getChannel();

                ByteBuffer buffer = ByteBuffer.allocate(512);
                while(input.read(buffer) != -1){
                    l.add(MetaDataComposer.compose(buffer));
                    buffer.clear();
                }
                Logger.getLogger("b-log").log(Level.FINEST, "{0} was loaded successfully", "LocalIndex");
            }catch(IOException e){
                Logger.getLogger("b-log").log(Level.SEVERE, "Error when loading LocalIndex from persistent storage", e);
            }
        }
        
        return l;
    }
    
    @Override
    public void run() {
        try{
            File file = new File("LocalIndex_");
            FileChannel output = new FileOutputStream(file).getChannel();
            FileLock lock = output.lock();
            
            for(ArrayList<IndexedDataObject> al : data){
                for(IndexedDataObject i : al){
                    output.write(MetaDataComposer.decompose(i));
                }
            }
            lock.release();
            output.close();
            Logger.getLogger("b-log").log(Level.FINEST, "{0} just saved to persistent  storage successfully", "LocalIndex");
        }catch(IOException e){
            Logger.getLogger("b-log").log(Level.SEVERE, "Error when writing LocalIndex to persistent storage", e);
        }
    }
}
