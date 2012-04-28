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
    CHKINTEG (0x31),
    CLEANING (0x32),
    INDEXING (0x33),
    RUNNING (0x34),
    IDLE (0x35),
    CLOSING (0x3F);
    
    private final byte value;
    
    private State(int byteValue) {
        this.value = (byte)byteValue;
    }
    
    protected byte getValue(){
        return value;
    }
}
