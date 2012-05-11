/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Mikael
 */
public class RouteJob extends AbstractJob{
    
    byte[] destinationNode, client;
    
    public RouteJob(byte[] destinationNode, byte[] client){
        super();
        this.destinationNode = destinationNode;
        this.client = client;
    }

    public byte[] getClient() {
        return client;
    }

    public byte[] getDestinationNode() {
        return destinationNode;
    }

    @Override
    public boolean readOperation(ByteBuffer buffer) throws IOException {
        return false;
    }

    @Override
    public boolean writeOperation(SocketChannel s, ByteBuffer buffer) {
        setFinished(true);
        return true;
    }
}
