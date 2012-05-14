/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.ServerState;
import com.mnorrman.datastorageproject.network.MasterNode;
import com.mnorrman.datastorageproject.network.Protocol;
import com.mnorrman.datastorageproject.tools.HexConverter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Mikael
 */
public class SyncStateJob extends AbstractJob{

    private Main main;
    
    public SyncStateJob(Main main) {
        super();
        this.main = main;
    }
    
    public SyncStateJob(String jobID, String owner){
        super(jobID);
        setFromConnection(owner);
    }

    @Override
    public boolean readOperation(ByteBuffer buffer) throws IOException {
        //System.out.println("Received state change from 0x" + getFromConnection() + ": " + ServerState.getState(buffer.get()).toString());
        Main.slaveList.get(getFromConnection()).setState(ServerState.getState(buffer.get()));
        Main.slaveList.get(getFromConnection()).setDataSize(buffer.getLong());
        setFinished(true);
        buffer.clear();
//        context.getNode().setState(ServerState.getState(newState));
//        System.out.println("Check: " + context.getNode().getState().toString());
//        context.setTask(null);
//        context.setCommand(Protocol.NULL);
        return false;
    }
    
    @Override
    public boolean writeOperation(SocketChannel s, ByteBuffer buffer) throws IOException {
        buffer.put(HexConverter.toByte(Main.ID));
        buffer.putInt(1);
        buffer.put(HexConverter.toByte(getJobID()));
        buffer.put(Protocol.SYNC_STATE.getValue());
        buffer.put(Main.state.getValue());
        buffer.putLong(main.getCurrentDataSize());
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
