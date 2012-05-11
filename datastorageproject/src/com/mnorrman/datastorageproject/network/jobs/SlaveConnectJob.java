/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.ServerState;
import com.mnorrman.datastorageproject.network.MasterNode;
import com.mnorrman.datastorageproject.network.Protocol;
import com.mnorrman.datastorageproject.network.SlaveNode;
import com.mnorrman.datastorageproject.tools.HexConverter;
import com.mnorrman.datastorageproject.tools.IntConverter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Mikael
 */
public class SlaveConnectJob extends AbstractJob{
    
    private SlaveNode sn;

    public SlaveConnectJob(SlaveNode sn){
        super();
        this.sn = sn;
    }
    
    @Override
    public boolean readOperation(ByteBuffer buffer) throws IOException {
        if(Main.ID.equals("FFFFFFFF")){
            System.out.println("Fixing new id");
            byte[] newID = new byte[4];
            buffer.get(newID);
            Main.ID = HexConverter.toHex(newID);
            Main.properties.setProperty("serverID", Main.ID);
            Main.properties.saveProperties();
        }
        System.out.println("ID: 0x" + Main.ID);
        Main.state = ServerState.IDLE;

        //Initiate full localIndex-sync
        SyncLocalIndexJob slij = new SyncLocalIndexJob();
        sn.createJob(slij.getJobID(), slij);
        setFinished(true);
        buffer.clear();
        return false;
    }

    @Override
    public boolean writeOperation(SocketChannel s, ByteBuffer buffer) throws IOException {
        buffer.put(HexConverter.toByte(Main.ID));
        buffer.putInt(1);
        buffer.put(HexConverter.toByte(getJobID()));
        buffer.put(Protocol.CONNECT.getValue());
        buffer.rewind();
        int writtenBytes = 0;
        while(writtenBytes < MasterNode.NETWORK_BLOCK_SIZE){
            writtenBytes += s.write(buffer);
        }
        buffer.clear(); //Always clear buffer
        return true;
    }
    
}
