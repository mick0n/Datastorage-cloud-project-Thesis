/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.tools;

import java.nio.ByteBuffer;

/**
 *
 * @author Mikael
 */
public class IntConverter {
    
    private IntConverter(){}
    
    public static final byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }
    
    public static final int byteToInt(byte[] value){
        if(value.length == 4)
            return ByteBuffer.wrap(value).getInt();
        else
            return -1;
    }
}
