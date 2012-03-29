/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.tools.Checksum;
import com.mnorrman.datastorageproject.tools.HexConverter;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.LinkedList;

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
    
    public LinkedList<IndexedDataObject> reindexData(){
        LinkedList<IndexedDataObject> list = new LinkedList<IndexedDataObject>();
        long position = 0;
        ByteBuffer data = ByteBuffer.allocateDirect(512);
        try{
            FileChannel channel = fileConnection.getChannel();
            channel.position(position);
            do{
                data.clear(); //Clear it first
                channel.read(data);
                data.rewind();

                byte[] temp;
                byte[] checksum = new byte[16];
                String colname, rowname, owner;
                long version, len;

                temp = new byte[128];
                data.get(temp);
                colname = new String(temp).trim();

                temp = new byte[128];
                data.get(temp);
                rowname = new String(temp).trim();

                version = data.getLong();

                len = data.getLong();

                data.get(checksum);

                temp = new byte[128];
                data.get(temp);
                owner = new String(temp).trim();

                list.add(new IndexedDataObject(colname, rowname, owner, 0L, len, version, checksum));
                position += (512+len);
                channel.position(position);
            }while(channel.size() - channel.position() > 0);
        }catch(IOException e){
            e.printStackTrace();
        }
        return list;
    }
    
    public boolean performIntegrityCheck(){
        long position = 0;
        ByteBuffer length = ByteBuffer.allocate(8);
        ByteBuffer checksum = ByteBuffer.allocate(16);
        ByteBuffer data;
        try{
            FileChannel channel = fileConnection.getChannel();
            channel.position(position);
            do{
                length.clear();
                checksum.clear(); //Clear it first
                channel.position(position + 264); //Jump to length and checksum
                channel.read(length);
                channel.read(checksum);
                length.rewind();
                checksum.rewind();

                //Set variables
                long len;
                byte[] checksumDataFromIndex = new byte[16];
                
                //Read from bytebuffers
                len = length.getLong();
                checksum.get(checksumDataFromIndex);
                
                data = ByteBuffer.allocate((int)len);
                channel.position(position + 512);
                channel.read(data);
                
                data.flip();
                
                byte[] checksumDataFromData = Checksum.getFor(data);
                
                if(!HexConverter.toHex(checksumDataFromIndex).equals(HexConverter.toHex(checksumDataFromData))){
                    return false;
                }
                
                position += (512+len);
                channel.position(position);
            }while(channel.size() - channel.position() > 0);
        }catch(IOException e){
            e.printStackTrace();
        }
        return true;
    }
    
    public FileChannel getChannel(){
        return fileConnection.getChannel();
    }
    
    public static void main(String[] args) {
        BackStorage t = new BackStorage();
        try{
            LinkedList<IndexedDataObject> list = t.initialize().reindexData();
            System.out.println("List = " + list.size());
            Iterator<IndexedDataObject> it = list.iterator();
            while(it.hasNext()){
                System.out.println(it.next().toString());
            }
            
            System.out.println("Integrity check is: " + t.performIntegrityCheck());
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
