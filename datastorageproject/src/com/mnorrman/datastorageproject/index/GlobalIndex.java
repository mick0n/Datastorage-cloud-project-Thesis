/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.index;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.objects.GlobalIndexedDataObject;
import com.mnorrman.datastorageproject.tools.Hash;
import java.util.*;

/**
 *
 * @author Mikael
 */
public class GlobalIndex extends Index<GlobalIndexedDataObject> {

    private HashMap<String, ArrayList<GlobalIndexedDataObject>> table;

    public GlobalIndex() {
        table = new HashMap<String, ArrayList<GlobalIndexedDataObject>>();
        IndexPersistenceGlobal ip = new IndexPersistenceGlobal();
        insertAll(ip.load());

        long delay = Long.parseLong(Main.properties.getValue("indexInterval").toString()) * 1000L;
        Main.timer.scheduleAtFixedRate(new IndexPercistanceTimerTask(this), delay, delay);
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
        String checksum = dataObject.getHash();
        if (table.containsKey(checksum)) {
            ArrayList<GlobalIndexedDataObject> temp = table.get(checksum);
            GlobalIndexedDataObject gidoTemp = null;
            temp.add(dataObject);
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
            table.put(checksum, new ArrayList<GlobalIndexedDataObject>());
            table.get(checksum).add(dataObject);
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

    /**
     * Implies to remove all versions of column a and row b
     * @param a Column
     * @param b Row
     */
    @Override
    public void remove(String a, String b) {
        table.remove(Hash.get(a, b));
    }

    /**
     * Implies to remove all versions of column and row-hash
     * @param hash column and row-hash
     */
    @Override
    public void remove(String hash) {
        table.remove(hash);
    }

    /**
     * Removes only one version of column a and row b
     * @param a Column
     * @param b Row
     * @param version Version number
     */
    @Override
    public void remove(String a, String b, int version) {
        table.get(Hash.get(a, b)).remove(version);
    }

    /**
     * Removes only one version of column and row-hash
     * @param hash Column and row-hash
     * @param version Version number
     */
    @Override
    public void remove(String hash, int version) {
        table.get(hash).remove(version);
    }

    @Override
    public int versionCount(GlobalIndexedDataObject dataObject) {
        return table.get(Hash.get(dataObject.getColname(), dataObject.getRowname())).size();
    }
}
