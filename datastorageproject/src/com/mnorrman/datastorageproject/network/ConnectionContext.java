
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.network.jobs.AbstractJob;
import com.mnorrman.datastorageproject.objects.ServerNode;
import com.mnorrman.datastorageproject.tools.HexConverter;
import com.mnorrman.datastorageproject.tools.IntConverter;
import java.nio.channels.SocketChannel;

/**
 * An object used by selector along with a SocketChannel to determine current
 * actions.
 * @author Mikael Norrman
 */
public class ConnectionContext {
    
    protected ServerNode node;
    
    protected SocketChannel channel;
    
    protected AbstractJob tempJob;
    
    protected String jobID;
    
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
    
    public SocketChannel getSocketChannel(){
        return channel;
    }
    
    public void setTempJob(AbstractJob job){
        this.tempJob = job;
    }
    
    public void setJobID(String jobID){
        this.jobID = jobID;
    }
    
    public boolean isClient(){
        return IntConverter.byteToInt(HexConverter.toByte(node.getId())) <= Integer.MAX_VALUE/2;
    }

    @Override
    public String toString() {
        return node.toString() + ", isClient? " + isClient();
    }
}
