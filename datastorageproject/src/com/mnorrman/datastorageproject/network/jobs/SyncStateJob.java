/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.network.Protocol;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Mikael
 */
public class SyncStateJob extends AbstractJob{

    public SyncStateJob(String owner) {
        super(owner);
    }
    
    @Override
    public boolean update(SocketChannel s, ByteBuffer buffer) throws IOException {
        buffer.put(Main.ID);
        buffer.putInt(1);
        buffer.put(Protocol.SYNC_STATE.getValue());
        buffer.put(Main.state.getValue());
        buffer.rewind();
        s.write(buffer);
        buffer.clear(); //Always clear buffer
        setFinished(true);
        return true;
    }
    
}
