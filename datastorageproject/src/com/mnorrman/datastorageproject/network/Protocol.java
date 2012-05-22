
package com.mnorrman.datastorageproject.network;
/**
 * This class should be pretty self explanatory.
 * @author Mikael Norrman
 */
public enum Protocol {
    
    NULL (0x00),
    CONNECT (0x01),
    GET (0x04),
    PUT (0x05),
    SYNC_LOCAL_INDEX (0x11),
    SYNC_STATE (0x12),
    DISCONNECT (0x7E),
    PING (0x7F);

    private final byte value;
    
    private Protocol(int byteValue) {
        this.value = (byte)byteValue;
    }
    
    public byte getValue(){
        return value;
    }
    
    public static Protocol getCommand(byte value){
        Protocol[] val = values();
        for(int b = 0; b < val.length; b++){
            if(val[b].getValue() == value)
                return val[b];
        }
        return null;
    }
    
}
