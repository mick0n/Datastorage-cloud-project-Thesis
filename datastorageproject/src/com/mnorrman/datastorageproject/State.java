/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

/**
 *
 * @author Mikael
 */
public enum State {
    
    NOTRUNNING (0x30),
    CONNECTING (0x31),
    CHKINTEG (0x32),
    CLEANING (0x33),
    INDEXING (0x34),
    RUNNING (0x35),
    IDLE (0x36),
    CLOSING (0x3F);
    
    private final byte value;
    
    private State(int byteValue) {
        this.value = (byte)byteValue;
    }
    
    protected byte getValue(){
        return value;
    }
    
    protected static State getState(byte value){
        State[] val = values();
        for(int b = 0; b < val.length; b++){
            if(val[b].getValue() == value)
                return val[b];
        }
        return null;
    }
}
