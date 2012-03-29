/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.tools;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Mikael
 */
public class Checksum {
    
    private Checksum(){ }
    
    public synchronized static byte[] getFor(ByteBuffer data){
        try{
            int initPos = data.position();
            data.position(0);
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data);
            data.position(initPos);
            return md.digest();
        }catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        return null;
    }
    
    public synchronized static String getFor(String a, String b){
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update((a+b).getBytes());
            return new String(md.digest());
        }catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        return null;
    }
}
