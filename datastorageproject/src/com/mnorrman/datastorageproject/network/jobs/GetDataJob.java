/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.storage.DataProcessor;

/**
 *
 * @author Mikael
 */
public class GetDataJob extends AbstractJob{
    
    private long crntPos;
    private DataProcessor dataProcessor;
    private IndexedDataObject ido;
    
    public GetDataJob(IndexedDataObject ido, DataProcessor dp){
        this.ido = ido;
        this.dataProcessor = dp;
        crntPos = 0L;
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
    
    public void update(long pos){
        this.crntPos = pos;
        if(crntPos == ido.getLength()){
            setFinished(true);
        }
    }
}
