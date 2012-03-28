/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

/**
 *
 * @author Mikael
 */
public class BackStorage {

    public static String dataLocation = "";
    
    private RandomAccessFile fileConnection;
    
    public BackStorage(){
        
    }
    
    public BackStorage initialize() throws IOException{
        //File file = new File(dataLocation + File.separator + "datafile_");
        File file = new File("datafile_");
        if(!file.exists()){
            file.createNewFile();
        }
        
        fileConnection = new RandomAccessFile(file, "rwd");
        return this;
    }
    
    public boolean performIntegrityCheck(){
        
        return false;
    }
    
    public FileChannel getChannel(){
        return fileConnection.getChannel();
    }
    
    public static void main(String[] args) {
        BackStorage t = new BackStorage();
        try{
            t.initialize();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
