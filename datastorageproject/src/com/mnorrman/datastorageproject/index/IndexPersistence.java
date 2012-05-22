
package com.mnorrman.datastorageproject.index;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
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

/**
 *
 * @author Mikael Norrman
 */
public class IndexPersistence implements Runnable{

    private Collection<ArrayList<IndexedDataObject>> data;

    /**
     * Create new instance of IndexPersistence
     * @param data The data that should be saved
     */
    public IndexPersistence(Collection<ArrayList<IndexedDataObject>> data) {
        this.data = data;
    }
    
    /**
     * Create new instance of IndexPersistence. This constructor is mainly used
     * when reading data.
     */
    public IndexPersistence(){
        this.data = null;
    }
    
    /**
     * Read indexdata from localIndex_ file. If the file doesn't exist an empty
     * list will be returned.
     * @return List containing all indexdata.
     */
    public List<IndexedDataObject> load(){
        List<IndexedDataObject> list = new LinkedList<IndexedDataObject>();
        File file = new File(Main.properties.getValue("dataPath") + File.separator + "LocalIndex_");
        
        //If the file exist, try to load the data
        if(file.exists()){
            try{
                FileChannel input = new FileInputStream(file).getChannel();

                ByteBuffer buffer = ByteBuffer.allocate(512);
                while(input.read(buffer) != -1){
                    list.add(MetaDataComposer.compose(buffer));
                    buffer.clear();
                }
                LogTool.log("Local index was loaded successfully", LogTool.INFO);
            }catch(IOException e){
                LogTool.log(e, LogTool.CRITICAL);
            }
        }
        
        return list;
    }
    
    @Override
    public void run() {
        try{
            File file = new File(Main.properties.getValue("dataPath") + File.separator + "LocalIndex_");
            FileChannel output = new FileOutputStream(file).getChannel();
            FileLock lock = output.lock();
            
            for(ArrayList<IndexedDataObject> al : data){
                for(IndexedDataObject i : al){
                    output.write(MetaDataComposer.decompose(i));
                }
            }
            lock.release();
            output.close();
            LogTool.log("LocalIndex was saved to persistent storage successfully", LogTool.INFO);
        }catch(IOException e){
            LogTool.log(e, LogTool.CRITICAL);
        }
    }
}
