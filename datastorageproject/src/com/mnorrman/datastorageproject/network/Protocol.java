
package com.mnorrman.datastorageproject.network;
/**
 * This class should be pretty self explanatory.
 * @author Mikael Norrman
 */
public enum Protocol {
    
    NULL (0x00),
    GET (0x04),
    PING (0x7F);

    private final byte value;
    
    private Protocol(int byteValue) {
        this.value = (byte)byteValue;
    }
    
    protected byte getValue(){
        return value;
    }
    
}
