/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.network.MasterNode;
import com.mnorrman.datastorageproject.tools.HexConverter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Mikael
 */
public class RoutePutJob extends AbstractJob{

    private MasterNode mn;
    private String destinationNode, client;
    private ByteBuffer miniBuffer;
    
    public RoutePutJob(String destinationNode, String client, MasterNode mn){
        super();
        this.destinationNode = destinationNode;
        this.client = client;
        this.mn = mn;
        this.miniBuffer = ByteBuffer.allocate(12);
    }
    
    public String getClient() {
        return client;
    }

    public String getDestinationNode() {
        return destinationNode;
    }

    @Override
    public boolean readOperation(ByteBuffer buffer) throws IOException {
        SocketChannel sc = mn.getConnections().get(destinationNode).getSocketChannel();

        miniBuffer.put(HexConverter.toByte(client));
        miniBuffer.putInt(buffer.limit());
        miniBuffer.put(HexConverter.toByte(getJobID()));

        miniBuffer.flip();
        while(miniBuffer.hasRemaining())
            sc.write(miniBuffer);
        miniBuffer.clear();

        while(buffer.hasRemaining())
            sc.write(buffer);
        buffer.clear();
        
        if(!mn.getConnections().containsKey(client)){
            setFinished(true);
            buffer.clear();
            return false;
        }
        
        buffer.clear();
        return false;
    }

    @Override
    public boolean writeOperation(SocketChannel s, ByteBuffer buffer) throws IOException {
        setFinished(true);
        return false;
    }
    
}
