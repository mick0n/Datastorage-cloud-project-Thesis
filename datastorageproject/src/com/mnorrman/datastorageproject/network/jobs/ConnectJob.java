/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.network.MasterNode;
import com.mnorrman.datastorageproject.network.Protocol;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Mikael
 */
public class ConnectJob extends AbstractJob{

    private boolean serverSide;
    private byte newID;
    
    public ConnectJob(String owner, boolean serverSide) {
        super(owner);
        this.serverSide = serverSide;
    }
    
    public void setNewId(byte newID){
        this.newID = newID;
    }

    @Override
    public boolean update(SocketChannel s, ByteBuffer buffer) throws IOException{
        buffer.put(Main.ID);
        if(serverSide){    
            buffer.putInt(5);
            buffer.put(Protocol.CONNECT.getValue());
            buffer.put(newID);
        }else{
            buffer.putInt(1);
            buffer.put(Protocol.CONNECT.getValue());
        }
        buffer.rewind();
        int writtenBytes = 0;
        while(writtenBytes < MasterNode.NETWORK_BLOCK_SIZE){
            writtenBytes += s.write(buffer);
            System.out.println("writtenbytes: " + writtenBytes);
        }
        
        buffer.clear(); //Always clear buffer
        return true;
    }
}
