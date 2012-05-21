/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import java.nio.channels.SocketChannel;

/**
 *
 * @author Mikael
 */
public class ExternalTrafficContext {
    
    protected int identifier;
    protected SocketChannel channel;
    
    public ExternalTrafficContext(int identifier, SocketChannel channel){
        this.identifier = identifier;
        this.channel = channel;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }

    public int getIdentifier() {
        return identifier;
    }
}
