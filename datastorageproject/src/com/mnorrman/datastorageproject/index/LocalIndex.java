
package com.mnorrman.datastorageproject.index;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.tools.Hash;
import java.util.*;

/**
 *
 * @author Mikael Norrman
 */
public final class LocalIndex extends Index<IndexedDataObject> {
    
    private HashMap<String, ArrayList<IndexedDataObject>> table;
    
    /**
     * Creates new instance of LocalIndex
     */
    public LocalIndex(){
        table = new HashMap<String, ArrayList<IndexedDataObject>>();
        
        //Load indexdata from file. IndexPersistance may return an empty file.
        IndexPersistence ip = new IndexPersistence();
        insertAll(ip.load());
        
        //Add a scheduled task for automatically saving this index
        long delay = Long.parseLong(Main.properties.getValue("indexInterval").toString()) * 1000L;
        Main.timer.scheduleAtFixedRate(new IndexPersistenceTimerTask(this), delay, delay);
    }

    /**
     * Clear the index
     */
    public synchronized LocalIndex clear(){
        table.clear();
        return this;
    }
    
    @Override
    public synchronized boolean contains(String hash) {
        return table.containsKey(hash);
    }

    @Override
    public synchronized boolean contains(String a, String b) {
        return table.containsKey(Hash.get(a, b));
    }

    @Override
    public synchronized IndexedDataObject get(String a, String b) {
        return table.get(Hash.get(a, b)).get(0);
    }

    @Override
    public synchronized IndexedDataObject get(String a, String b, int version) {
        if(table.get(Hash.get(a, b)).size() > version && version >= 0)
            return table.get(Hash.get(a, b)).get(version);
        else
            return null;
    }

    @Override
    public synchronized IndexedDataObject get(String hash) {
        return table.get(hash).get(0);
    }

    @Override
    public synchronized IndexedDataObject get(String hash, int version) {
        if(table.get(hash).size() > version && version >= 0)
            return table.get(hash).get(version);
        else
            return null;
    }

    @Override
    public synchronized void insert(IndexedDataObject dataObject) {
        String hash = dataObject.getHash();
        if(table.containsKey(hash)){
            ArrayList<IndexedDataObject> temp = table.get(hash);
            IndexedDataObject idoTemp = null;
            temp.add(dataObject);
            
            //Sort list so the newest version is at index 0
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
            //Create new arraylist for this index
            table.put(hash, new ArrayList<IndexedDataObject>());
            table.get(hash).add(dataObject);
        }
    }

    @Override
    public synchronized void insertAll(List<IndexedDataObject> list) {
        Iterator<IndexedDataObject> iterator = list.iterator();
        while(iterator.hasNext()){
            insert(iterator.next());
        }
    }

    @Override
    public synchronized void remove(String a, String b) {
        table.remove(Hash.get(a, b));
    }

    @Override
    public synchronized void remove(String hash) {
        table.remove(hash);
    }

    @Override
    public synchronized void remove(String a, String b, int version) {
        table.get(Hash.get(a, b)).remove(version);
    }

    @Override
    public synchronized void remove(String hash, int version) {
        table.get(hash).remove(version);
    }

    @Override
    public synchronized int versionCount(IndexedDataObject dataObject) {
        return table.get(Hash.get(dataObject.getColname(), dataObject.getRowname())).size();
    }

    @Override
    public synchronized int versionCount(String a, String b) {
        return table.get(Hash.get(a, b)).size();
    }

    @Override
    public synchronized int versionCount(String hash) {
        return table.get(hash).size();
    }
    
    @Override
    public synchronized Collection<ArrayList<IndexedDataObject>> getData(){
        return table.values();
    }
    
    /**
     * Populates a hashtable that uses only column as key, not hash(column + row)
     * @return 
     */
    public synchronized HashMap<String, ArrayList<IndexedDataObject>> getDistinctData(){
        HashMap<String, ArrayList<IndexedDataObject>> list = new HashMap<String, ArrayList<IndexedDataObject>>();
        
        for(ArrayList<IndexedDataObject> al : table.values()){
            for(IndexedDataObject odi : al){
                if(!list.containsKey(odi.getColname())){
                    list.put(odi.getColname(), new ArrayList<IndexedDataObject>());
                }
                list.get(odi.getColname()).add(odi);
            }
        }
        
        return list;
    }
    
    public synchronized LinkedList<ArrayList<IndexedDataObject>> cloneData(){
        LinkedList<ArrayList<IndexedDataObject>> list = new LinkedList<ArrayList<IndexedDataObject>>();
        
        Iterator<ArrayList<IndexedDataObject>> it1 = table.values().iterator();
        while(it1.hasNext()){
            Iterator<IndexedDataObject> it2 = it1.next().iterator();
            list.addLast(new ArrayList<IndexedDataObject>());
            while(it2.hasNext()){
                list.getLast().add(it2.next().cloneMe());
            }
        }
        
        return list;
    }
}
