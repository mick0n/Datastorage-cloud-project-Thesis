/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

import com.mnorrman.datastorageproject.objects.DataObject;
import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.objects.UnindexedDataObject;
import java.nio.ByteBuffer;

/**
 *
 * @author Mikael
 */
public class MetaDataComposer {
    
    private static final byte[] VOID_BYTES = new byte[104];
    
    private MetaDataComposer(){ }
    
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
        bb.put(VOID_BYTES);
        bb.flip();
        
        return bb;
    }
    
}
