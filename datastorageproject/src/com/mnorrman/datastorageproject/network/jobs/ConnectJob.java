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
public class ConnectJob extends AbstractJob{

    public ConnectJob(String owner) {
        super(owner);
    }

    @Override
    public boolean update(SocketChannel s, ByteBuffer buffer) throws IOException{
        buffer.put(Main.ID); //Unknown so far
        buffer.putInt(1);
        buffer.put(Protocol.CONNECT.getValue());
        buffer.rewind();
        s.write(buffer);
        buffer.clear(); //Always clear buffer
        return true;
    }
}
