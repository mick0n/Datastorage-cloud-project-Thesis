
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
    
    protected Protocol command;
    
    protected AbstractJob task;
    
    protected ServerNode node;
    
    protected SocketChannel channel;
    
    /**
     * Creates new instance of ConnectionContext.
     * @param command Starting command. Should always be Protocol.NULL.
     */
    public ConnectionContext(SocketChannel channel, Protocol command){
        this.channel = channel;
        this.command = command;
    }
    
    /**
     * Creates new instance of ConnectionContext.
     * @param command Starting command. Should always be Protocol.NULL.
     */
    public ConnectionContext(SocketChannel channel, Protocol command, byte[] id){
        this.channel = channel;
        this.command = command;
        this.node = new ServerNode(channel.socket().getInetAddress(), channel.socket().getPort(), id);
    }
    
    /**
     * Set a new command.
     * @param command 
     */
    public void setCommand(Protocol command){
        this.command = command;
    }
    
    /**
     * Set current task.
     * @param aj 
     */
    public void setTask(AbstractJob aj){
        this.task = aj;
    }
    
    /**
     * Set the serverNode of this context object
     */
    public void setServerNode(ServerNode node){
        this.node = node;
    }

    public ServerNode getNode() {
        return node;
    }
}
