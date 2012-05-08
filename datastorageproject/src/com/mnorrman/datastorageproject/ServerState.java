/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

/**
 *
 * @author Mikael
 */
public enum ServerState {
    
    NOTRUNNING (0x30),
    CONNECTING (0x31),
    CHKINTEG (0x32),
    CLEANING (0x33),
    SYNCHRONIZING (0x34),
    INDEXING (0x35),
    RUNNING (0x36),
    IDLE (0x37),
    CLOSING (0x3F);
    
    private final byte value;
    
    private ServerState(int byteValue) {
        this.value = (byte)byteValue;
    }
    
    public byte getValue(){
        return value;
    }
    
    public static ServerState getState(byte value){
        ServerState[] val = values();
        for(int b = 0; b < val.length; b++){
            if(val[b].getValue() == value)
                return val[b];
        }
        return null;
    }
}
