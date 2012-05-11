
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.network.jobs.AbstractJob;
import com.mnorrman.datastorageproject.objects.ServerNode;
import java.nio.channels.SocketChannel;

/**
 * An object used by selector along with a SocketChannel to determine current
 * actions.
 * @author Mikael Norrman
 */
public class ConnectionContext {
    
    protected ServerNode node;
    
    protected SocketChannel channel;
    
    /**
     * Creates new instance of ConnectionContext.
     * 
     */
    public ConnectionContext(SocketChannel channel){
        this.channel = channel;
    }
    
    /**
     * Creates new instance of ConnectionContext.
     * 
     */
    public ConnectionContext(SocketChannel channel, String id){
        this.channel = channel;
        this.node = new ServerNode(channel.socket().getInetAddress(), channel.socket().getPort(), id);
    }

    /**
     * Set the serverNode of this context object
     */
    public void setServerNode(ServerNode node){
        this.node = node;
    }

    /**
     * Get the node that represents this connection.
     * @return Servernode
     */
    public ServerNode getNode() {
        return node;
    }
}
