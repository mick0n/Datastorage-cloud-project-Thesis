
package com.mnorrman.datastorageproject.index;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.objects.GlobalIndexedDataObject;
import com.mnorrman.datastorageproject.tools.Hash;
import java.util.*;

/**
 *
 * @author Mikael Norrman
 */
public class GlobalIndex extends Index<GlobalIndexedDataObject> {

    private HashMap<String, ArrayList<GlobalIndexedDataObject>> table;

    public GlobalIndex() {
        table = new HashMap<String, ArrayList<GlobalIndexedDataObject>>();
        
        //Load indexdata from file. IndexPersistance may return an empty file.
        IndexPersistenceGlobal ip = new IndexPersistenceGlobal();
        insertAll(ip.load());

        //Add a scheduled task for automatically saving this index
        long delay = Long.parseLong(Main.properties.getValue("indexInterval").toString()) * 1000L;
        Main.timer.scheduleAtFixedRate(new IndexPersistenceTimerTask(this), delay, delay);
    }

    public void clear() {
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
    public GlobalIndexedDataObject get(String a, String b) {
        return table.get(Hash.get(a, b)).get(0);
    }

    @Override
    public GlobalIndexedDataObject get(String a, String b, int version) {
        if (table.get(Hash.get(a, b)).size() > version && version >= 0) {
            return table.get(Hash.get(a, b)).get(version);
        } else {
            return null;
        }
    }

    @Override
    public GlobalIndexedDataObject get(String hash) {
        return table.get(hash).get(0);
    }

    @Override
    public GlobalIndexedDataObject get(String hash, int version) {
        if (table.get(hash).size() > version && version >= 0) {
            return table.get(hash).get(version);
        } else {
            return null;
        }
    }

    @Override
    public Collection<ArrayList<GlobalIndexedDataObject>> getData() {
        return table.values();
    }

    @Override
    public void insert(GlobalIndexedDataObject dataObject) {
        String hash = dataObject.getHash();
        if (table.containsKey(hash)) {
            ArrayList<GlobalIndexedDataObject> temp = table.get(hash);
            GlobalIndexedDataObject gidoTemp = null;
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
            table.put(hash, new ArrayList<GlobalIndexedDataObject>());
            table.get(hash).add(dataObject);
        }
    }

    @Override
    public void insertAll(List<GlobalIndexedDataObject> list) {
        Iterator<GlobalIndexedDataObject> iterator = list.iterator();
        while (iterator.hasNext()) {
            GlobalIndexedDataObject temp = iterator.next();
            insert(temp);
        }
    }

    @Override
    public void remove(String a, String b) {
        table.remove(Hash.get(a, b));
    }

    @Override
    public void remove(String hash) {
        table.remove(hash);
    }

    @Override
    public void remove(String a, String b, int version) {
        table.get(Hash.get(a, b)).remove(version);
    }

    @Override
    public void remove(String hash, int version) {
        table.get(hash).remove(version);
    }

    @Override
    public int versionCount(GlobalIndexedDataObject dataObject) {
        return table.get(Hash.get(dataObject.getColname(), dataObject.getRowname())).size();
    }
    
    @Override
    public int versionCount(String a, String b) {
        return table.get(Hash.get(a, b)).size();
    }

    @Override
    public int versionCount(String hash) {
        return table.get(hash).size();
    }
}
