/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.objects;

import java.io.InputStream;

/**
 *
 * @author Mikael
 */
public class UnindexedDataObject extends DataObject{

    private InputStream stream;

    public UnindexedDataObject(InputStream stream, String colname, String rowname, String owner, long length) {
        this.stream = stream;
        this.colname = colname;
        this.rowname = rowname;
        this.owner = owner;
        this.length = length;
    }

    public InputStream getStream(){
        return stream;
    }

    public void setChecksum(long checksum) {
        this.checksum = checksum;
    }
    
    @Override
    public String toString() {
        return "UnindexedDataObject: colname=" + colname + ", rowname=" + rowname + ", owner=" + owner + ", length=" + length + ", checksum=" + checksum;
    }    
}
