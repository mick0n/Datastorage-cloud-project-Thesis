/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.network.ConnectionContext;
import com.mnorrman.datastorageproject.network.MasterNode;
import com.mnorrman.datastorageproject.network.Protocol;
import com.mnorrman.datastorageproject.tools.HexConverter;
import com.mnorrman.datastorageproject.tools.IntConverter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Random;

/**
 *
 * @author Mikael
 */
public class MasterConnectJob extends AbstractJob{

    private MasterNode mn;
    private String from;
    
    public MasterConnectJob(String jobID, String from, String owner, MasterNode mn){
        super(jobID);
        this.from = from;
        setOwner(owner);
        this.mn = mn;
    }
    
    @Override
    public boolean readOperation(ByteBuffer buffer) throws IOException {
        String slaveID = from;
        System.out.println("Slaveif: " + slaveID);
        if(slaveID.equals("FFFFFFFF")){
            //Create new SlaveID
            slaveID = HexConverter.toHex(IntConverter.intToByteArray(new Random().nextInt(Integer.MAX_VALUE/2) + Integer.MAX_VALUE/2));
        }
        
        //Get the connectionContext, remove the old key mapping and add a new one.
        ConnectionContext cc = mn.getConnections().get(getOwner());
        mn.getConnections().remove(getOwner());
        cc.getNode().setId(slaveID);
        mn.getConnections().put(slaveID, cc);
        setOwner(slaveID);
        Main.slaveList.put(cc.getNode());
        
        buffer.clear();
        return true;
    }

    @Override
    public boolean writeOperation(SocketChannel s, ByteBuffer buffer) throws IOException {
        buffer.put(HexConverter.toByte(Main.ID)); 
        buffer.putInt(5);
        buffer.put(HexConverter.toByte(getJobID()));
        buffer.put(Protocol.CONNECT.getValue());
        buffer.put(HexConverter.toByte(getOwner()));
        buffer.rewind();
        int writtenBytes = 0;
        while(writtenBytes < MasterNode.NETWORK_BLOCK_SIZE){
            writtenBytes += s.write(buffer);
        }
        
        buffer.clear(); //Always clear buffer
        setFinished(true);
        return true;
    }
}
