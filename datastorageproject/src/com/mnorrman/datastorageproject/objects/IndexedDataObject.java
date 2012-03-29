/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.objects;

import com.mnorrman.datastorageproject.tools.HexConverter;
import java.io.Serializable;

/**
 *
 * @author Mikael
 */
public final class IndexedDataObject extends DataObject implements Serializable{
    
    private long offset, version;

    public IndexedDataObject(String colname, String rowname, String owner, long offset, long length, long version, byte[] checksum) {
        this.colname = colname;
        this.rowname = rowname;
        this.owner = owner;
        this.offset = offset;
        this.length = length;
        this.version = version;
        this.checksum = checksum;
    }
    
    public IndexedDataObject(UnindexedDataObject udo, long offset, long version){
        this.colname = udo.getColname();
        this.rowname = udo.getRowname();
        this.owner = udo.getOwner();
        this.offset = offset;
        this.length = udo.getLength();
        this.version = version;
        this.checksum = udo.getChecksum();
    }
    
    public IndexedDataObject(){
        //Should probably be removed later on.
    }

    public long getOffset() {
        return offset;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "IndexedDataObject: colname=" + colname + ", rowname=" + rowname + ", owner=" + owner + ", offset=" + offset + ", length=" + length + ", version=" + version + ", checksum=" + HexConverter.toHex(checksum);
    }
}
