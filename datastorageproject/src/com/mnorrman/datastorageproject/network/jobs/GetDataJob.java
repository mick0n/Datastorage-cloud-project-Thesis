
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.storage.BackStorage;
import com.mnorrman.datastorageproject.storage.DataProcessor;
import java.nio.ByteBuffer;

/**
 *
 * @author Mikael Norrman
 */
public class GetDataJob extends AbstractJob{
    
    private long crntPos;
    private DataProcessor dataProcessor;
    private IndexedDataObject ido;
    private ByteBuffer largebuf;
    
    public GetDataJob(IndexedDataObject ido, DataProcessor dp){
        this.ido = ido;
        this.dataProcessor = dp;
        crntPos = 0L;
        largebuf = ByteBuffer.allocate(BackStorage.BlOCK_SIZE);
    }
    
    public DataProcessor getDataProcessor(){
        return dataProcessor;
    }
    
    public IndexedDataObject getIndexedDataObject(){
        return ido;
    }
    
    public long getCurrentPosition(){
        return crntPos;
    }
    
    public byte[] getData(){
        if(isFinished())
            return null;
        largebuf.clear();
        int readBytes = dataProcessor.retrieveData(largebuf, crntPos, ido);
        update(crntPos + readBytes);
        largebuf.flip();
        return largebuf.array();
    }
    
    public int getBufferLimit(){
        return largebuf.limit();
    }
    
    public void update(long pos){
        this.crntPos = pos;
        if(crntPos == ido.getLength()){
            setFinished(true);
        }
    }
}
