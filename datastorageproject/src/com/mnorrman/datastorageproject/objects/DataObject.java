
package com.mnorrman.datastorageproject.objects;

import com.mnorrman.datastorageproject.tools.Hash;
import java.io.Serializable;

/**
 * Definition of a dataobject.
 * @author Mikael Norrman
 */
public abstract class DataObject implements Serializable{
    
    protected String colname, rowname, owner;
    protected long length, checksum;
    
    /**
     * Create an empty instance of DataObject
     */
    public DataObject(){ }

    /**
     * Create new instance of DataObject with input variables
     * @param colname
     * @param rowname
     * @param owner
     * @param length
     * @param checksum 
     */
    public DataObject(String colname, String rowname, String owner, long length, long checksum) {
        this.colname = colname;
        this.rowname = rowname;
        this.owner = owner;
        this.length = length;
        this.checksum = checksum;
    }

    /**
     * Return the checksumvalue of this dataobject
     * @return checksumvalue
     */
    public long getChecksum() {
        return checksum;
    }

    /**
     * Return the column name of this dataobject
     * @return column name
     */
    public String getColname() {
        return colname;
    }

    /**
     * Return the number of bytes that this dataobject represent
     * @return number of bytes
     */
    public long getLength() {
        return length;
    }

    /**
     * Return owner name of this dataobject
     * @return 
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Return row name of this dataobject
     * @return 
     */
    public String getRowname() {
        return rowname;
    }

    /**
     * Return the hash value of this dataobject
     * @return 
     */
    public String getHash() {
        return Hash.get(colname, rowname);
    }
    
    /**
     * Abstract method that returns the values of this dataobject in a common
     * way.
     * @return 
     */
    public abstract String getClearText();
    
    @Override
    public abstract String toString();
    
}
