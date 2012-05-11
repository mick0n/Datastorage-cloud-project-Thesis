
package com.mnorrman.datastorageproject.index;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.objects.GloballyIndexedDataObject;
import com.mnorrman.datastorageproject.tools.Hash;
import java.util.*;

/**
 *
 * @author Mikael Norrman
 */
public class GlobalIndex extends Index<GloballyIndexedDataObject> {

    private LinkedHashMap<String, ArrayList<GloballyIndexedDataObject>> indexTable;

    public GlobalIndex() {
        indexTable = new LinkedHashMap<String, ArrayList<GloballyIndexedDataObject>>();
        
        //Load indexdata from file. IndexPersistance may return an empty file.
        IndexPersistenceGlobal ip = new IndexPersistenceGlobal();
        insertAll(ip.load());

        //Add a scheduled task for automatically saving this index
//        long delay = Long.parseLong(Main.properties.getValue("indexInterval").toString()) * 1000L;
//        Main.timer.scheduleAtFixedRate(new IndexPersistenceGlobalTimerTask(this), delay, delay);
    }

    public void clear() {
        indexTable.clear();
    }

    @Override
    public boolean contains(String hash) {
        return indexTable.containsKey(hash);
    }

    @Override
    public boolean contains(String a, String b) {
        return indexTable.containsKey(Hash.get(a, b));
    }

    @Override
    public GloballyIndexedDataObject get(String a, String b) {
        return indexTable.get(Hash.get(a, b)).get(0);
    }

    @Override
    public GloballyIndexedDataObject get(String a, String b, int version) {
        if (indexTable.get(Hash.get(a, b)).size() > version && version >= 0) {
            return indexTable.get(Hash.get(a, b)).get(version);
        } else {
            return null;
        }
    }

    @Override
    public GloballyIndexedDataObject get(String hash) {
        return indexTable.get(hash).get(0);
    }

    @Override
    public GloballyIndexedDataObject get(String hash, int version) {
        if (indexTable.get(hash).size() > version && version >= 0) {
            return indexTable.get(hash).get(version);
        } else {
            return null;
        }
    }

    @Override
    public Collection<ArrayList<GloballyIndexedDataObject>> getData() {
        return indexTable.values();
    }

    @Override
    public void insert(GloballyIndexedDataObject dataObject) {
        String hash = dataObject.getHash();
        if (indexTable.containsKey(hash)) {
            ArrayList<GloballyIndexedDataObject> temp = indexTable.get(hash);
            GloballyIndexedDataObject gidoTemp = null;
            temp.add(dataObject);
            
            //Sort so that the newest index is always first (0)
            for (int a = temp.size() - 1; a > 0; a--) {
                if (temp.get(a).getVersion() > temp.get(a - 1).getVersion()) {
                    gidoTemp = temp.get(a - 1);
                    temp.set(a - 1, temp.get(a));
                    temp.set(a, gidoTemp);
                } else {
                    break;
                }
            }
        } else {
            indexTable.put(hash, new ArrayList<GloballyIndexedDataObject>());
            indexTable.get(hash).add(dataObject);
        }
    }

    @Override
    public void insertAll(List<GloballyIndexedDataObject> list) {
        Iterator<GloballyIndexedDataObject> iterator = list.iterator();
        while (iterator.hasNext()) {
            GloballyIndexedDataObject temp = iterator.next();
            insert(temp);
        }
    }

    @Override
    public void remove(String a, String b) {
        indexTable.remove(Hash.get(a, b));
    }

    @Override
    public void remove(String hash) {
        indexTable.remove(hash);
    }

    @Override
    public void remove(String a, String b, int version) {
        indexTable.get(Hash.get(a, b)).remove(version);
    }

    @Override
    public void remove(String hash, int version) {
        indexTable.get(hash).remove(version);
    }

    @Override
    public int versionCount(GloballyIndexedDataObject dataObject) {
        return indexTable.get(Hash.get(dataObject.getColname(), dataObject.getRowname())).size();
    }
    
    @Override
    public int versionCount(String a, String b) {
        return indexTable.get(Hash.get(a, b)).size();
    }

    @Override
    public int versionCount(String hash) {
        return indexTable.get(hash).size();
    }
}
