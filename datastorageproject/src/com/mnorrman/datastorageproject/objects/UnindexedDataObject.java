/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.objects;

import java.io.File;

/**
 *
 * @author Mikael
 */
public class UnindexedDataObject extends DataObject{

    private File tempFile;

    public UnindexedDataObject(File file, String colname, String rowname, String owner, long length) {
        this.tempFile = file;
        this.colname = colname;
        this.rowname = rowname;
        this.owner = owner;
        this.length = length;
    }

    public File getTempFile(){
        return tempFile;
    }
    
    public void removeTempFile(){
        tempFile.delete();
        tempFile = null;
    }

    public void setChecksum(long checksum) {
        this.checksum = checksum;
    }

    @Override
    public String getClearText() {
        return "colname=" + colname + ",rowname=" + rowname + ",owner=" + owner + ",length=" + length + ",checksum=" + checksum;
    }
    
    @Override
    public String toString() {
        return "UnindexedDataObject: colname=" + colname + ", rowname=" + rowname + ", owner=" + owner + ", length=" + length + ", checksum=" + checksum;
    }    
}
