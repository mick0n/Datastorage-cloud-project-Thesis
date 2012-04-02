/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.tools;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Mikael
 */
public class Hash {
    
    private Hash(){ }
    
    private static MessageDigest md;

    public synchronized static String get(String a, String b){
        try{
            md = MessageDigest.getInstance("MD5");
            return new String(md.digest((a+b).getBytes()));
        }catch(NoSuchAlgorithmException e){
            Main.logger.log(e, LogTool.CRITICAL);
        }
        return null;
    }
}
