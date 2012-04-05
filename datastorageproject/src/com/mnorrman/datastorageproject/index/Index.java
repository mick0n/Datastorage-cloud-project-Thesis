/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.index;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.objects.DataObject;
import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.tools.Hash;
import java.util.*;

/**
 *
 * @author Mikael
 */
public abstract class Index<T extends DataObject> {
    
    public Index(){
        super();
    }
    
    public abstract Collection<ArrayList<T>> getData();
    
    public abstract boolean contains(String hash);
    
    public abstract boolean contains(String a, String b);
    
    public abstract void remove(String a, String b);
    
    public abstract void remove(String hash);
            
    public abstract T get(String a, String b);
    
    public abstract T get(String a, String b, int version);
    
    public abstract T get(String hash);
    
    public abstract T get(String hash, int version);
    
    public abstract void insert(T dataObject);
    
    public abstract void insertAll(List<T> list);
    
    public abstract int versionCount(T dataObject);
}
