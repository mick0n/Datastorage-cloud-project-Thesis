
package com.mnorrman.datastorageproject.tools;

import com.mnorrman.datastorageproject.objects.DataObject;
import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.objects.UnindexedDataObject;
import java.nio.ByteBuffer;

/**
 * Utility class used to put together and take apart dataObjects.
 * @author Mikael Norrman
 */
public class MetaDataComposer {
    
    /**
     * Used in most cases to fill the void.
     */
    private static final byte[] VOID_BYTES = new byte[104];
    
    /**
     * Used in special cases when we also need an offset value stored in
     * binary format.
     */
    private static final byte[] VOID_BYTES_WITHOUT_OFFSET = new byte[96];
    
    private MetaDataComposer(){ } //Prohibit instantiation
    
    /**
     * Compose an indexDataObject from specified ByteBuffer and specify offset
     * maunally.
     * @param bb Bytebuffer that contains the data
     * @param offset Maunally set offset
     * @return new IndexedDataObject representing the data from the byte buffer.
     */
    public static IndexedDataObject compose(ByteBuffer bb, long offset){
        if(bb == null || bb.capacity() <= 0)
            return null;
        if(bb.position() != 0)
            bb.rewind();
        
        byte[] temp;
        String colname, rowname, owner;
        long version, len, checksum;

        temp = new byte[128];
        bb.get(temp);
        colname = new String(temp).trim();

        temp = new byte[128];
        bb.get(temp);
        rowname = new String(temp).trim();
        
        version = bb.getLong();

        len = bb.getLong();

        checksum = bb.getLong();

        temp = new byte[128];
        bb.get(temp);
        owner = new String(temp).trim();

        return new IndexedDataObject(colname, rowname, owner, offset, len, version, checksum);
    }
    
    /**
     * Compose an indexDataObject from specified ByteBuffer and use special
     * void properties to also read offset from binary data.
     * @param bb Bytebuffer that contains the data
     * @return new IndexedDataObject representing the data from the byte buffer.
     */
    public static IndexedDataObject compose(ByteBuffer bb){
        if(bb == null || bb.capacity() <= 0)
            return null;
        if(bb.position() != 0)
            bb.rewind();
        
        byte[] temp;
        String colname, rowname, owner;
        long version, len, checksum, offset;

        temp = new byte[128];
        bb.get(temp);
        colname = new String(temp).trim();

        temp = new byte[128];
        bb.get(temp);
        rowname = new String(temp).trim();
        
        version = bb.getLong();

        len = bb.getLong();

        checksum = bb.getLong();

        temp = new byte[128];
        bb.get(temp);
        owner = new String(temp).trim();
        
        offset = bb.getLong();

        return new IndexedDataObject(colname, rowname, owner, offset, len, version, checksum);
    }
    
    /**
     * Returns a byteBuffer representing the specified dataObject. Dataobject
     * may in this case be either an indexed or unindexed DataObject.
     * @param edo DataObject to decompose
     * @return New bytebuffer containing the raw data
     */
    public static ByteBuffer decompose(DataObject edo){
        ByteBuffer bb = ByteBuffer.allocate(512);
        byte[] colnameBytes = new byte[128];
        byte[] rownameBytes = new byte[128];
        byte[] ownerBytes = new byte[128];
        System.arraycopy(edo.getColname().getBytes(), 0, colnameBytes, 0, edo.getColname().getBytes().length);
        System.arraycopy(edo.getRowname().getBytes(), 0, rownameBytes, 0, edo.getRowname().getBytes().length);
        System.arraycopy(edo.getOwner().getBytes(), 0, ownerBytes, 0, edo.getOwner().getBytes().length);

        long version = 0L;
        if(edo instanceof UnindexedDataObject)
            version = System.currentTimeMillis();
        else if(edo instanceof IndexedDataObject)
            version = ((IndexedDataObject)edo).getVersion();

        bb.put(colnameBytes);
        bb.put(rownameBytes);
        bb.putLong(version);
        bb.putLong(edo.getLength());
        bb.putLong(edo.getChecksum());
        bb.put(ownerBytes);
        
        //When saving an index we also need to store the offset from the metadata.
        //That's why this part is putting data in the otherwise unused data bytes
        //at the end of an index. 
        if(edo instanceof IndexedDataObject){
            bb.putLong(((IndexedDataObject)edo).getOffset());
            bb.put(VOID_BYTES_WITHOUT_OFFSET);
        }else{
            bb.put(VOID_BYTES);
        }
        bb.flip();
        
        return bb;
    }
    
}
