/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.objects.Pair;
import com.mnorrman.datastorageproject.objects.UnindexedDataObject;
import com.mnorrman.datastorageproject.storage.DataProcessor;
import com.mnorrman.datastorageproject.tools.HexConverter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.zip.CRC32;

/**
 *
 * @author Mikael
 */
public class PutJob extends AbstractJob{

    private FileChannel output;
    private UnindexedDataObject udo;
    private CRC32 crc;
    
    private DataProcessor dataProcessor;
    
    public PutJob(String fromConnection, DataProcessor dp){
        super();
//        setFromConnection(fromConnection);
        
        this.dataProcessor = dp;
        crc = new CRC32();
    }
    
    public PutJob(String jobID, String fromConnection, DataProcessor dp){
        super(jobID);
//        setFromConnection(fromConnection);
        
        this.dataProcessor = dp;
        crc = new CRC32();
    }
    
    @Override
    public boolean readOperation(ByteBuffer buffer) throws IOException {
        if(udo == null){
            byte[] stringbytes = new byte[128];
            buffer.get(stringbytes);
            String column = new String(stringbytes, "UTF-8").trim();
            
            stringbytes = new byte[128];
            buffer.get(stringbytes);
            String row = new String(stringbytes, "UTF-8").trim();
            
            stringbytes = new byte[128];
            buffer.get(stringbytes);
            String owner = new String(stringbytes, "UTF-8").trim();
            
            long length = buffer.getLong();
            
            udo = new UnindexedDataObject(new File("putjob" + Math.random()), column, row, owner, length);
            System.out.println("udo: " + udo.toString());
            output = new FileOutputStream(udo.getTempFile()).getChannel();
        }
        
        int currPos = buffer.position();
        
        if(buffer.hasRemaining()){
            if(buffer.remaining() > (int)(udo.getLength()-output.size())){
                buffer.limit(buffer.position() + (int)(udo.getLength()-output.size()));
            }
            output.write(buffer);
            buffer.position(currPos);
            byte[] crcBytes = new byte[buffer.limit()-currPos];
            buffer.get(crcBytes);
            crc.update(crcBytes);
        }
        
        if(output.size() == udo.getLength()){
            output.close();
            udo.setChecksum(crc.getValue());
            Main.localIndex.insert(dataProcessor.storeData(udo));
            setFinished(true);           
        }else if (output.size() > udo.getLength()){
            System.out.println("Ärror");
            buffer.clear();
            setFinished(true);
        }
        
        buffer.clear();
        return false;
    }

    @Override
    public boolean writeOperation(SocketChannel s, ByteBuffer buffer) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
