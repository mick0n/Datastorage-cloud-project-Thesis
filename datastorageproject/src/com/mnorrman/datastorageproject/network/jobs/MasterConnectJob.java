/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.network.ClusterMessageVariables;
import com.mnorrman.datastorageproject.network.MasterNode;
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
public class MasterConnectJob extends AbstractJob{

    private MasterNode mn;
    private String fromSlaveNode;
    private SelectionKey connectionKey;
    
    public MasterConnectJob(String fromConnection, MasterNode mn, SelectionKey key){
        super();
        setFromConnection(fromConnection);
        this.mn = mn;
        this.connectionKey = key;
    }
    
    public MasterConnectJob(String jobID, String fromSlaveNode, String fromConnection, MasterNode mn){
        super(jobID);
        this.fromSlaveNode = fromSlaveNode;
        setFromConnection(fromConnection);
        this.mn = mn;
    }
    
    @Override
    public boolean readOperation(ByteBuffer buffer) throws IOException {
        buffer.rewind();
        ClusterMessageVariables cmv = new ClusterMessageVariables(buffer);
        buffer.get();
        long storageLimit = buffer.getLong();
        
        String slaveID = cmv.getFrom();
        System.out.println("SlaveiD: " + slaveID);
        if(slaveID.equals("FFFFFFFF")){
            //Create new SlaveID
            slaveID = HexConverter.toHex(IntConverter.intToByteArray(new Random().nextInt(Integer.MAX_VALUE/2) + Integer.MAX_VALUE/2));
        }
        
        System.out.println("Switching connection from 0x" + getFromConnection() + " to 0x" + slaveID);
        
        //Get the connectionContext, remove the old key mapping and add a new one.
        mn.switchConnectionID(getFromConnection(), slaveID);
        setFromConnection(slaveID);
        connectionKey.attach(slaveID);
        mn.getConnections().get(getFromConnection()).getNode().setStorageLimit(storageLimit);
        Main.slaveList.put(mn.getConnections().get(getFromConnection()).getNode());
        
        
        MasterConnectJob mcj = new MasterConnectJob(cmv.getJobID(), cmv.getFrom(), getFromConnection(), mn);
        mn.createJob(getFromConnection(), mcj);
        
        
        
//        if(fromSlaveNode == null){
//            buffer.rewind();
//            ClusterMessageVariables cmv = new ClusterMessageVariables(buffer);
//            System.err.println("Current job id: " + getJobID());
//            System.err.println("" + cmv.toString());
//            this.fromSlaveNode = cmv.getFrom();
//            mn.getJobs().remove(getJobID());
//            mn.getJobs().put(cmv.getJobID(), this);
//            setJobID(cmv.getJobID());
//            System.err.println("Does jobs contain the new id? " + mn.getJobs().containsKey(cmv.getJobID()));
//        }
        
        setFinished(true);
        buffer.clear();
        return false;
    }

    @Override
    public boolean writeOperation(SocketChannel s, ByteBuffer buffer) throws IOException {
        buffer.put(HexConverter.toByte(Main.ID)); 
        buffer.putInt(5);
        buffer.put(HexConverter.toByte(getJobID()));
        buffer.put(HexConverter.toByte(getFromConnection()));
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
