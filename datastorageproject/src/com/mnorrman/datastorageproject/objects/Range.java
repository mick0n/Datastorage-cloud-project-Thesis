/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.objects;

import com.mnorrman.datastorageproject.tools.HexConverter;

/**
 *
 * @author Mikael
 */
public class Range {
    
    public long startRange, endRange;
    
    public Range(long initialStartingRange, long initialEndingRange){
        this.startRange = initialStartingRange;
        this.endRange = initialEndingRange;
    }

    @Override
    public String toString() {
        return startRange + " to " + endRange;
    }
    
    public String toHexString(){
        return "0x" + HexConverter.toHex(startRange) + " to 0x" + HexConverter.toHex(endRange);
    }
}
