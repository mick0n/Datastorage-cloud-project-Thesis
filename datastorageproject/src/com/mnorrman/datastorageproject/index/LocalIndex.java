/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.index;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.tools.Hash;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Mikael
 */
public class LocalIndex extends HashMap<String, ArrayList<IndexedDataObject>> {
        
    public LocalIndex(){
        super();
    }
    
    public void insertIndex(IndexedDataObject ido){
        String checksum = ido.getHash();
        if(containsKey(checksum)){
            ArrayList<IndexedDataObject> temp = get(checksum);
            IndexedDataObject idoTemp = null;
            temp.add(ido);
            for(int a = temp.size() - 1; a > 0; a--){
                if(temp.get(a).getVersion() > temp.get(a-1).getVersion()){
                    idoTemp = temp.get(a-1);
                    temp.set(a-1, temp.get(a));
                    temp.set(a, idoTemp);
                }else{
                    break;
                }
            }
        }else{
            put(checksum, new ArrayList<IndexedDataObject>());
            get(checksum).add(ido);
        }
        
    }
        
    public IndexedDataObject get(String a, String b){
        return get(Hash.get(a, b)).get(0); //Zero is head
    }
    
    public IndexedDataObject get(String a, String b, int version){
        if(get(Hash.get(a, b)).size() > version)
            return get(Hash.get(a, b)).get(version);
        else
            return null;
    }
    
    public IndexedDataObject getWithHash(String hash){
        return get(hash).get(0); //Zero is head
    }
    
    public int getNumberOfVersions(IndexedDataObject ido){
        return get(Hash.get(ido.getColname(), ido.getRowname())).size();
    }
    
    public boolean contains(String hash){
        return containsKey(hash);
    }
    
    public boolean contains(String a, String b){
        return containsKey(Hash.get(a, b));
    }
    
    public void insertAll(List<IndexedDataObject> list){
        Iterator<IndexedDataObject> iterator = list.iterator();
        while(iterator.hasNext()){
            IndexedDataObject temp = iterator.next();
            insertIndex(temp);
        }
        Main.pool.submit(new IndexPercistance(getClass().getSimpleName(), values()));
    }
}
