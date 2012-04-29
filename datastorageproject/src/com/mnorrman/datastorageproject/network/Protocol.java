
package com.mnorrman.datastorageproject.network;
/**
 * This class should be pretty self explanatory.
 * @author Mikael Norrman
 */
public enum Protocol {
    
    NULL (0x00),
    CONNECT (0x01),
    GET (0x04),
    PING (0x7F);

    private final byte value;
    
    private Protocol(int byteValue) {
        this.value = (byte)byteValue;
    }
    
    protected byte getValue(){
        return value;
    }
    
    protected static Protocol getCommand(byte value){
        switch(value){
            case 0x00:
                return NULL;
            case 0x01:
                return CONNECT;
            case 0x04:
                return GET; 
            case 0x7F:
                return PING;
            default:
                return NULL;
        }
    }
    
}
