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
public final class IndexedDataObject implements Serializable{
    
    private String colname, rowname, owner;
    private long offset, length, version;
    private byte[] checksum; //always 16 byte

    public IndexedDataObject(String colname, String rowname, String owner, long offset, long length, long version, byte[] checksum) {
        this.colname = colname;
        this.rowname = rowname;
        this.owner = owner;
        this.offset = offset;
        this.length = length;
        this.version = version;
        this.checksum = checksum;
    }
    
    public IndexedDataObject(){
        //Should probably be removed later on.
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

    public long getOffset() {
        return offset;
    }

    public String getOwner() {
        return owner;
    }

    public String getRowname() {
        return rowname;
    }

    public long getVersion() {
        return version;
    }
}
