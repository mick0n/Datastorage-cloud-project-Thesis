package com.mnorrman.datastorageproject.objects;

import java.io.File;

/**
 *
 * @author Mikael Norrman
 */
public class UnindexedDataObject extends DataObject {

    private File tempFile;

    public UnindexedDataObject(File file, String colname, String rowname, String owner, long length) {
        this.tempFile = file;
        this.colname = colname;
        this.rowname = rowname;
        this.owner = owner;
        this.length = length;
    }

    /**
     * Return the temporary file-object
     *
     * @return
     */
    public File getTempFile() {
        return tempFile;
    }

    /**
     * Removes the "physical" file that is connected to this object
     */
    public void removeTempFile() {
        tempFile.delete();
        tempFile = null;
    }

    /**
     * Set the checksum
     *
     * @param checksum
     */
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
