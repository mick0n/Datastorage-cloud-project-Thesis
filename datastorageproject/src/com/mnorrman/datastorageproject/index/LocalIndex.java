/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.index;

import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.tools.Hash;
import java.util.*;

/**
 *
 * @author Mikael
 */
public class LocalIndex extends Index<IndexedDataObject> {
    
    private HashMap<String, ArrayList<IndexedDataObject>> table;
    
    public LocalIndex(){
        table = new HashMap<>();
    }

    public void clear(){
        table.clear();
    }
    
    @Override
    public boolean contains(String hash) {
        return table.containsKey(hash);
    }

    @Override
    public boolean contains(String a, String b) {
        return table.containsKey(Hash.get(a, b));
    }

    @Override
    public IndexedDataObject get(String a, String b) {
        return table.get(Hash.get(a, b)).get(0);
    }

    @Override
    public IndexedDataObject get(String a, String b, int version) {
        if(table.get(Hash.get(a, b)).size() > version && version >= 0)
            return table.get(Hash.get(a, b)).get(version);
        else
            return null;
    }

    @Override
    public IndexedDataObject get(String hash) {
        return table.get(hash).get(0);
    }

    @Override
    public IndexedDataObject get(String hash, int version) {
        if(table.get(hash).size() > version && version >= 0)
            return table.get(hash).get(version);
        else
            return null;
    }

    @Override
    public void insert(IndexedDataObject dataObject) {
        String checksum = dataObject.getHash();
        if(table.containsKey(checksum)){
            ArrayList<IndexedDataObject> temp = table.get(checksum);
            IndexedDataObject idoTemp = null;
            temp.add(dataObject);
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
            table.put(checksum, new ArrayList<IndexedDataObject>());
            table.get(checksum).add(dataObject);
        }
    }

    @Override
    public void insertAll(List<IndexedDataObject> list) {
        Iterator<IndexedDataObject> iterator = list.iterator();
        while(iterator.hasNext()){
            IndexedDataObject temp = iterator.next();
            insert(temp);
        }
    }

    @Override
    public void remove(String a, String b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove(String hash) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int versionCount(IndexedDataObject dataObject) {
        return table.get(Hash.get(dataObject.getColname(), dataObject.getRowname())).size();
    }
    
    @Override
    public Collection<ArrayList<IndexedDataObject>> getData(){
        return table.values();
    }
}
