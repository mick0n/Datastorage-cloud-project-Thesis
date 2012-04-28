
package com.mnorrman.datastorageproject.objects;

import java.io.Serializable;

/**
 *
 * @author Mikael Norrman
 */
public final class IndexedDataObject extends DataObject implements Serializable{
    
    private long offset, version;
    private boolean cleanupFlag;

    public IndexedDataObject(String colname, String rowname, String owner, long offset, long length, long version, long checksum, boolean cleanupFlag) {
        this.colname = colname;
        this.rowname = rowname;
        this.owner = owner;
        this.offset = offset;
        this.length = length;
        this.version = version;
        this.checksum = checksum;
        this.cleanupFlag = cleanupFlag;
    }
    
    public IndexedDataObject(UnindexedDataObject udo, long offset, long version){
        this.colname = udo.getColname();
        this.rowname = udo.getRowname();
        this.owner = udo.getOwner();
        this.offset = offset;
        this.length = udo.getLength();
        this.version = version;
        this.checksum = udo.getChecksum();
        this.cleanupFlag = false;
    }
    
    public IndexedDataObject(){
        //Should probably be removed later on.
    }

    /**
     * Returns the offset in the backStorage.
     * @return 
     */
    public long getOffset() {
        return offset;
    }

    /**
     * Returns the version timestamp of this object
     * @return 
     */
    public long getVersion() {
        return version;
    }
    
    /**
     * Check to see whether this indexedDataObject has been flagged for cleanup.
     * @return 
     */
    public boolean isCleanupFlagged(){
        return cleanupFlag;
    }
    
    /**
     * Mark this indexedDataObject for cleanup.
     */
    public void markForCleanup(){
        cleanupFlag = true;
    }

    @Override
    public String getClearText() {
        return "colname=" + colname + ",rowname=" + rowname + ",owner=" + owner + ",offset=" + offset + ",length=" + length + ",version=" +version + ",checksum=" + checksum;
    }
    
    @Override
    public String toString() {
        return "IndexedDataObject: colname=" + colname + ", rowname=" + rowname + ", owner=" + owner + ", offset=" + offset + ", length=" + length + ", version=" + version + ", checksum=" + checksum + ", cleanup=" + cleanupFlag;
    }
}
