
package com.mnorrman.datastorageproject.index;

import com.mnorrman.datastorageproject.objects.DataObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Mikael Norrman
 */
public abstract class Index<T extends DataObject> {
    
    /**
     * Creates new instance of Index
     */
    public Index(){
    }
    
    /**
     * Get all data from this index (Columns, rows and timestamps)
     * @return Collection<Arraylist<T>>
     */
    public abstract Collection<ArrayList<T>> getData();
    
    /**
     * Check to see if this index contains the specified key value
     * @param hash The hashvalue to search for.
     * @return true if it exists, otherwise false.
     */
    public abstract boolean contains(String hash);
    
    /**
     * Check to see if this index contains the specified key value
     * @param a Columnvalue
     * @param b Rowvalue
     * @return true if it exists, otherwise false.
     */
    public abstract boolean contains(String a, String b);
    
    /**
     * Removes all indexes for this cell. 
     * @param a Columnvalue
     * @param b Rowvalue
     */
    public abstract void remove(String a, String b);
    
    /**
     * Removes only a single index in this cell
     * @param a Columnvalue
     * @param b Rowvalue
     * @param version Versionvalue (as index, not timestamp)
     */
    public abstract void remove(String a, String b, int version);
    
    /**
     * Removes all indexes for this cell. 
     * @param hash Hashvalue
     */
    public abstract void remove(String hash);
    
    /**
     * Removes only a single index in this cell
     * @param hash Hashvalue
     * @param version Versionvalue (as index, not timestamp)
     */
    public abstract void remove(String hash, int version);
            
    /**
     * Get the head version of this cell (versionindex 0)
     * @param a Columnvalue
     * @param b Rowvalue
     * @return T if value is found, otherwise null.
     */
    public abstract T get(String a, String b);
    
    /**
     * Get a specified version of this cell
     * @param a Columnvalue
     * @param b Rowvalue
     * @param version Versionindex
     * @return T if value is found, otherwise null.
     */
    public abstract T get(String a, String b, int version);
    
    /**
     * Get the head version of this cell (versionindex 0)
     * @param hash Hashvalue
     * @return T if value is found, otherwise null.
     */
    public abstract T get(String hash);
    
    /**
     * Get a specified version of this cell
     * @param hash  Hashvalue
     * @param version Versionindex
     * @return T if value is found, otherwise null.
     */
    public abstract T get(String hash, int version);
    
    /**
     * Insert a single dataObject into this index
     * @param dataObject 
     */
    public abstract void insert(T dataObject);
    
    /**
     * Insert a list of dataObjects into this index
     * @param list 
     */
    public abstract void insertAll(List<T> list);
    
    /**
     * Count the number of versions for a specified dataObject.
     * @param dataObject Dataobject to be counted for
     * @return Number of versions.
     */
    public abstract int versionCount(T dataObject);
    
    /**
     * Count the number of versions for the specified column and row.
     * @param a Columnvalue
     * @param b Rowvalue
     * @return Number of versions.
     */
    public abstract int versionCount(String a, String b);
    
    /**
     * Count the number of versions for the specified hashvalue.
     * @param hash Hashvalue
     * @return Number of versions.
     */
    public abstract int versionCount(String hash);
}
