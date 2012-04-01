/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.tools.Hash;
import com.mnorrman.datastorageproject.tools.HexConverter;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.zip.CRC32;

/**
 *
 * @author Mikael
 */
public class BackStorage {

    public static final int BlOCK_SIZE = 131072;
    
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
                String colname, rowname, owner;
                long version, len, checksum;

                temp = new byte[128];
                data.get(temp);
                colname = new String(temp).trim();

                temp = new byte[128];
                data.get(temp);
                rowname = new String(temp).trim();

                version = data.getLong();

                len = data.getLong();

                checksum = data.getLong();

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
        ByteBuffer meta = ByteBuffer.allocate(16);
        try{
            FileChannel channel = fileConnection.getChannel();
            if(channel.size() <= 0)
                return true;
            channel.position(position);
            do{
                meta.clear();
                channel.position(position + 264); //Jump to length and checksum
                channel.read(meta);
                meta.flip();

                //Set variables
                long len;
                long checksumDataFromIndex;
                
                //Read from bytebuffers
                len = meta.getLong();
                checksumDataFromIndex = meta.getLong();
                
                //Jump past metadata and create new CRC32-object
                channel.position(position + 512);
                CRC32 crc = new CRC32();

                int readBytes = 0;
                long totalBytesRead = 0;
                ByteBuffer buffer = ByteBuffer.allocate(BlOCK_SIZE);
                
                System.out.println("What are we working with? " + len + ", " + checksumDataFromIndex);
                do{
                    readBytes = channel.read(buffer);
                    buffer.flip();
                    
                    //Check if we read more bytes that necessary
                    if(readBytes + totalBytesRead > len){
                        readBytes = (int)(len - totalBytesRead);
                    }
                    crc.update(buffer.array(), 0, readBytes);
                    totalBytesRead += readBytes;
                }while(totalBytesRead < len);
                long checksumDataFromData = crc.getValue();
                
                System.out.println("Checksum 1: " + checksumDataFromIndex + " 2: " + checksumDataFromData);
                if(checksumDataFromIndex != checksumDataFromData){
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
        try{
            BackStorage t = new BackStorage().initialize();
//            LinkedList<IndexedDataObject> list = t.initialize().reindexData();
//            System.out.println("List = " + list.size());
//            Iterator<IndexedDataObject> it = list.iterator();
//            while(it.hasNext()){
//                System.out.println(it.next().toString());
//            }
//            
            System.out.println("Integrity check is: " + t.performIntegrityCheck());
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
