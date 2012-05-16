/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.objects.ServerNode;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Mikael
 */
public class InternalTrafficContext {
    
    protected String identifier;
    
    protected ServerNode node;
    
    protected SocketChannel channel;
    
    public InternalTrafficContext(SocketChannel channel){
        this.channel = channel;
    }
    
    public InternalTrafficContext(SocketChannel channel, String identifier){
        this.channel = channel;
        this.identifier = identifier;
        node = new ServerNode(channel.socket().getInetAddress(), channel.socket().getPort(), identifier);
    }
    
    public void setIdentifier(String identifier){
        this.identifier = identifier;
        node.setId(this.identifier);
    }

    public String getIdentifier() {
        return identifier;
    }

    public ServerNode getNode() {
        return node;
    }

    public SocketChannel getChannel() {
        return channel;
    }    
}
