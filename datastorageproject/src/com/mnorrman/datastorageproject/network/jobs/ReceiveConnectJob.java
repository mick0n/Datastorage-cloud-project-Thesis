/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.network.ClusterNode;
import com.mnorrman.datastorageproject.network.ClusterMessageVariables;
import com.mnorrman.datastorageproject.network.ConnectionContext;
import com.mnorrman.datastorageproject.network.InternalTrafficContext;
import com.mnorrman.datastorageproject.network.InternalTrafficHandler;
import com.mnorrman.datastorageproject.network.MasterNode;
import com.mnorrman.datastorageproject.objects.Pair;
import com.mnorrman.datastorageproject.tools.HexConverter;
import com.mnorrman.datastorageproject.tools.IntConverter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Random;

/**
 *
 * @author Mikael
 */
public class ReceiveConnectJob extends InternalJob{

    private InternalTrafficHandler ith;
    private SelectionKey key;
    
    public ReceiveConnectJob(InternalTrafficContext context, String jobID, InternalTrafficHandler ith){
        super(context, jobID);
        this.ith = ith;
    }
    
    @Override
    public boolean readOperation(ByteBuffer buffer) throws IOException {
        buffer.rewind();
        ClusterMessageVariables cmv = new ClusterMessageVariables(buffer);
        buffer.get();
        long storageLimit = buffer.getLong();
        
        String childID = cmv.getFrom();
        System.out.println("SlaveiD: " + childID);
        if(childID.equals("FFFFFFFF")){
            childID = HexConverter.toHex(new Random().nextInt());
        }
        
        System.out.println("Switching connection from 0x" + getContext().getIdentifier() + " to 0x" + childID);

        ith.switchConnectionID(getContext().getIdentifier(), childID);

        
        getContext().getNode().setStorageLimit(storageLimit);
        
        buffer.clear();
        return true;
    }

    @Override
    public boolean writeOperation(SocketChannel s, ByteBuffer buffer) throws IOException {
        buffer.put(HexConverter.toByte(Main.ID)); 
        buffer.putInt(8);
        buffer.put(HexConverter.toByte(getJobID()));
        buffer.put(HexConverter.toByte(getContext().getIdentifier()));
        buffer.flip();
        while(buffer.hasRemaining())
            s.write(buffer);
        
        buffer.clear(); //Always clear buffer
        setFinished(true);
        return true;
    }
}
