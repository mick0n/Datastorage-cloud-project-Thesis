
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

    
//    public synchronized static String get(String a, String b){
//        try{
//            md = MessageDigest.getInstance("MD5");
//            return new String(md.digest((a+b).getBytes()));
//        }catch(NoSuchAlgorithmException e){
//            Logger.getLogger("b-log").log(Level.SEVERE, "An error occured when retrieving hash value!", e);
//        }
//        return null;
//    }
    
    /**
     * Get hash value from string a and string b
     * @param a
     * @param b
     * @return a combined 16 character hexdecimal hashvalue based on the hashcode
     * from each of the two input parameters.
     */
    public synchronized static String get(String column, String row){
        return HexConverter.toHex(column.hashCode()) + HexConverter.toHex(row.hashCode());
    }
}
