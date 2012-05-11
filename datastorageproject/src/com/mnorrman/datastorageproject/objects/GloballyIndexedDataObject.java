
package com.mnorrman.datastorageproject.objects;

import com.mnorrman.datastorageproject.tools.HexConverter;

/**
 *
 * @author Mikael Norrman
 */
public class GloballyIndexedDataObject extends DataObject{

    private ServerNode indexOwner;
    private long version;

    public GloballyIndexedDataObject(ServerNode indexOwner, String colname, String rowname, long version, String owner, long length, long checksum) {
        super(colname, rowname, owner, length, checksum);
        this.indexOwner = indexOwner;
        this.version = version;
    }
    
    public GloballyIndexedDataObject(IndexedDataObject ido, ServerNode sn){
        super(ido.getColname(), ido.getRowname(), ido.getOwner(), ido.getLength(), ido.getChecksum());
        this.indexOwner = sn;
        this.version = ido.getVersion();   
    }

    @Override
    public String getClearText() {
        return "Whatevaaa";
    }
    
    @Override
    public String toString() {
        return indexOwner.getIpaddress().toString() + ":" + indexOwner.getPort() + "(" + indexOwner.getId() + ") >>> Colname: " + colname + ", Rowname: " + rowname + ", Owner: " + owner + ", Version: " + version + ", Length: " + length + ", Checksum: " + checksum;
    }
    
    public ServerNode getServerNode(){
        return indexOwner;
    }
    
    public long getVersion(){
        return version;
    }
}
