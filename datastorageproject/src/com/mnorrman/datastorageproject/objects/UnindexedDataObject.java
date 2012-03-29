/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.objects;

import com.mnorrman.datastorageproject.tools.Checksum;
import com.mnorrman.datastorageproject.tools.HexConverter;
import java.nio.ByteBuffer;

/**
 *
 * @author Mikael
 */
public class UnindexedDataObject extends DataObject{

    private ByteBuffer data;

    public UnindexedDataObject(ByteBuffer data, String colname, String rowname, String owner) {
        super(colname, rowname, owner, data.capacity(), Checksum.getFor(data));
        this.data = data;
    }
    
    public UnindexedDataObject(ByteBuffer data){
        this.data = data;
    }
    
    public ByteBuffer getData(){
        return data;
    }
    
    @Override
    public String toString() {
        return "UnindexedDataObject: colname=" + colname + ", rowname=" + rowname + ", owner=" + owner + ", length=" + length + ", checksum=" + HexConverter.toHex(checksum);
    }    
}
