/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.network.ClusterMessageVariables;
import com.mnorrman.datastorageproject.network.MasterNode;
import com.mnorrman.datastorageproject.network.Protocol;
import com.mnorrman.datastorageproject.tools.HexConverter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Mikael
 */
public class RouteJob extends AbstractJob{
    
    private MasterNode mn;
    private String destinationNode, client;
    private String remoteJobID;
    private ByteBuffer miniBuffer;
    private Protocol command;
    
    public RouteJob(String destinationNode, String client, MasterNode mn, Protocol command){
        super();
        this.destinationNode = destinationNode;
        this.client = client;
        this.mn = mn;
        this.command = command;
        this.miniBuffer = ByteBuffer.allocate(24);
        this.remoteJobID = "00000000";
    }

    public String getClient() {
        return client;
    }

    public String getDestinationNode() {
        return destinationNode;
    }

    public Protocol getCommand(){
        return command;
    }
    
    public void setRemoteJobID(String remoteJobID) {
        this.remoteJobID = remoteJobID;
    }

    public String getRemoteJobID() {
        return remoteJobID;
    }
    
    @Override
    public boolean readOperation(ByteBuffer buffer) throws IOException {
        SocketChannel sc = null;
        if(buffer.position() <= 10){ //means client
            System.out.println("Routejob goes client");
            sc = mn.getConnections().get(destinationNode).getSocketChannel();
            
            miniBuffer.put(HexConverter.toByte(client));
            miniBuffer.putInt(buffer.limit());
            miniBuffer.put(HexConverter.toByte(remoteJobID));
            
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
        }else{ //Means slaveNode
            if(!mn.getConnections().containsKey(client)){
                setFinished(true);
                buffer.clear();
                return false;
            }
            sc = mn.getConnections().get(client).getSocketChannel();
            buffer.rewind();
            ClusterMessageVariables cmv = new ClusterMessageVariables(buffer);
            if(remoteJobID.equals("00000000")){
                remoteJobID = cmv.getJobID();
            }
            while(buffer.hasRemaining())
                sc.write(buffer);
            buffer.clear();
        }
        
        buffer.clear();
        return false;
    }

    @Override
    public boolean writeOperation(SocketChannel s, ByteBuffer buffer) {
        setFinished(true);
        return true;
    }
}
