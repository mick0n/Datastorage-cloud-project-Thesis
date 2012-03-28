/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.tools.Checksum;
import java.util.HashMap;

/**
 *
 * @author Mikael
 */
public class Index extends HashMap<String, IndexedDataObject> {
        
    public Index(){
        super();
    }
    
    public void insertNew(IndexedDataObject ido){
        put(Checksum.getFor(ido.getColname(), ido.getRowname()), ido);
    }
    
    public IndexedDataObject getIndexedObject(String hash){
        return get(hash);
    }
    
    public boolean doIndexedObjectExist(String hash){
        return containsKey(hash);
    }
}
