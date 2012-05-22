
package com.mnorrman.datastorageproject.tools;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for generating hash values.
 * @author Mikael Norrman
 */
public class Hash {
    
    private Hash(){ }
    
    private static MessageDigest md;

    /**
     * Get hash value from string a and string b
     * @param a
     * @param b
     * @return new MD5-hash based on a+b
     */
    public synchronized static String get(String a, String b){
        try{
            md = MessageDigest.getInstance("MD5");
            return new String(md.digest((a+b).getBytes()));
        }catch(NoSuchAlgorithmException e){
            Logger.getLogger("b-log").log(Level.SEVERE, "An error occured when retrieving hash value!", e);
        }
        return null;
    }
}
