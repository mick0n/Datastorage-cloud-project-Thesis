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
public class RawMetaDataPrinter {
    
    private RawMetaDataPrinter(){}
    
    public static void print(ByteBuffer bb){
        System.out.print("1-128:\t\tColname:\t0x");
        for(int a = 0; a < 128; a++){
            System.out.print("" + HexConverter.toHex(new byte[]{ bb.get() }));
        }
        System.out.println("");
        
        System.out.print("129-256:\tRowname:\t0x");
        for(int b = 0; b < 128; b++){
            System.out.print("" + HexConverter.toHex(new byte[]{ bb.get() }));
        }
        System.out.println("");
        
        System.out.print("257-264:\tVersion:\t0x");
        for(int c = 0; c < 8; c++){
            System.out.print("" + HexConverter.toHex(new byte[]{ bb.get() }));
        }
        System.out.println("");
        
        System.out.print("265-272:\tLength:\t\t0x");
        for(int d = 0; d < 8; d++){
            System.out.print("" + HexConverter.toHex(new byte[]{ bb.get() }));
        }
        System.out.println("");
        
        System.out.print("273-280:\tChecksum:\t0x");
        for(int e = 0; e < 8; e++){
            System.out.print("" + HexConverter.toHex(new byte[]{ bb.get() }));
        }
        System.out.println("");
        
        System.out.print("281-408:\tOwner:\t\t0x");
        for(int f = 0; f < 128; f++){
            System.out.print("" + HexConverter.toHex(new byte[]{ bb.get() }));
        }
        System.out.println("");
        
        System.out.print("409-512:\tVoid:\t\t0x");
        for(int g = 0; g < 104; g++){
            System.out.print("" + HexConverter.toHex(new byte[]{ bb.get() }));
        }
        System.out.println("");
    }
    
}
