/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.objects;

import java.io.Serializable;

/**
 *
 * @author Mikael
 */
public abstract class DataObject implements Serializable{
    
    protected String colname, rowname, owner;
    protected long length;
    protected byte[] checksum; //always 16 byte
    
    public DataObject(){ }

    public DataObject(String colname, String rowname, String owner, long length, byte[] checksum) {
        this.colname = colname;
        this.rowname = rowname;
        this.owner = owner;
        this.length = length;
        this.checksum = checksum;
    }

    public byte[] getChecksum() {
        return checksum;
    }

    public String getColname() {
        return colname;
    }

    public long getLength() {
        return length;
    }

    public String getOwner() {
        return owner;
    }

    public String getRowname() {
        return rowname;
    }
    
    @Override
    public abstract String toString();
    
}
