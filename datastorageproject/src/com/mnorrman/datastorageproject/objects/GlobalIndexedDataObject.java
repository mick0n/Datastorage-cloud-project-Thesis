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
public class GlobalIndexedDataObject extends DataObject implements Serializable{

    private ServerNode indexOwner;
    private long version;

    public GlobalIndexedDataObject(ServerNode indexOwner, String colname, String rowname, long version, String owner, long length, long checksum) {
        super(colname, rowname, owner, length, checksum);
        this.indexOwner = indexOwner;
        this.version = version;
    }
    
    public GlobalIndexedDataObject(IndexedDataObject ido, ServerNode sn){
        this.indexOwner = sn;
        this.colname = ido.getColname();
        this.rowname = ido.getRowname();
        this.owner = ido.getOwner();
        this.length = ido.getLength();
        this.version = ido.getVersion();
        this.checksum = ido.getChecksum();        
    }
    
    @Override
    public String toString() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public ServerNode getServerNode(){
        return indexOwner;
    }
    
    public long getVersion(){
        return version;
    }
}
