/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.network.ClusterMessageVariables;
import com.mnorrman.datastorageproject.network.InternalTrafficContext;
import com.mnorrman.datastorageproject.network.InternalTrafficHandler;
import com.mnorrman.datastorageproject.network.Protocol;
import com.mnorrman.datastorageproject.objects.ServerNode;
import com.mnorrman.datastorageproject.tools.HexConverter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Random;

/**
 *
 * @author Mikael
 */
public class ReceiveConnectJob extends InternalJob{

    private InternalTrafficHandler ith;
    
    public ReceiveConnectJob(InternalTrafficContext context, String jobID, InternalTrafficHandler ith){
        super(context, jobID);
        this.ith = ith;
    }
    
    @Override
    public boolean readOperation(ByteBuffer buffer) throws IOException {
        buffer.rewind();
        ClusterMessageVariables cmv = new ClusterMessageVariables(buffer);
        buffer.get();
        int internalport = buffer.getInt();
        int externalport = buffer.getInt();
        
        System.out.println("Got connect from 0x" + cmv.getFrom());
        
        if(ith.isMaster()){
            System.out.println("SlaveiD: " + cmv.getFrom());
            if(cmv.getFrom().equals("FFFFFFFF")){
                cmv.setFrom(HexConverter.toHex(new Random().nextInt()));
                getContext().getNode().setId(cmv.getFrom());
            }
        }
        
        System.out.println("Switching connection from 0x" + getContext().getIdentifier() + " to 0x" + cmv.getFrom());
        ith.switchConnectionID(getContext().getIdentifier(), cmv.getFrom());
        
        getContext().getNode().setInternalport(internalport);
        getContext().getNode().setExternalport(externalport);
        
        if(ith.isMaster()){
            //Add new node. If it exists in the tree the new ip and port will be updated.
            ith.getMasterProperties().getTree().addNewNode(getContext().getNode());

            //Um, basically if the parent of this node is root, we should add it to this ones children.
            //If not we should redirect.
            if(ith.getMasterProperties().getTree().getNode(getContext().getNode().getId()).getParentNode().getServerNode().getId().equals(ith.getMasterProperties().getTree().getRoot().getServerNode().getId())){
                ith.getMasterProperties().addChild(getContext().getNode());
            }
        }else{
            ith.getChildProperties().addChild(getContext().getNode());
        }
        
        
        
        



//        if(!ith.getMasterProperties().getTree().containsID(childID)){
//            getContext().setNode(ith.getMasterProperties().getTree().addNode(getContext().getNode()));

//            System.out.println("Switching connection from 0x" + getContext().getIdentifier() + " to 0x" + cmv.getFrom());
//            ith.switchConnectionID(getContext().getIdentifier(), cmv.getFrom());
//            
//            if(!ith.getChildProperties().containsID(cmv.getFrom())){
//                getContext().setNode(ith.getChildProperties().addChild(getContext().getNode()));
//            }
        
//        
//        
//        System.out.println("Switching connection from 0x" + getContext().getIdentifier() + " to 0x" + childID);
//
//        ith.switchConnectionID(getContext().getIdentifier(), childID);
//
//        getContext().getNode().setStorageLimit(storageLimit);
//        if(!ith.getMasterProperties().getTree().containsID(childID)){
//            ith.getMasterProperties().getTree().addNode(getContext().getNode());
//        }
//        
        buffer.clear();
        return true;
    }

    @Override
    public boolean writeOperation(SocketChannel s, ByteBuffer buffer) throws IOException {
        buffer.put(HexConverter.toByte(Main.ID));
        if(ith.isMaster()){
            if(ith.getMasterProperties().getChildren().containsKey(getContext().getNode().getId())){
                ServerNode node = ith.getMasterProperties().getChildren().get(getContext().getNode().getId());
                buffer.putInt(25);
                buffer.put(HexConverter.toByte(getJobID()));
                buffer.put(Protocol.CONNECT.getValue());
                buffer.put(HexConverter.toByte(getContext().getIdentifier()));
                buffer.putLong(node.getRange().startRange);
                buffer.putLong(node.getRange().endRange);
            }else{
                ServerNode parentNode = ith.getMasterProperties().getTree().getNode(getContext().getNode().getId()).getParentNode().getServerNode();
                buffer.putInt(21);
                buffer.put(HexConverter.toByte(getJobID()));
                buffer.put(Protocol.REDIRECT.getValue());
                buffer.put(HexConverter.toByte(getContext().getIdentifier()));
                buffer.put(parentNode.getIpaddress().getAddress());
                buffer.putInt(parentNode.getInternalport());
                buffer.put(HexConverter.toByte(parentNode.getId()));
            }
        }else{
            ServerNode child = ith.getChildProperties().getChild(getContext().getNode().getId());
            buffer.putInt(25);
            buffer.put(HexConverter.toByte(getJobID()));
            buffer.put(Protocol.CONNECT.getValue());
            buffer.put(HexConverter.toByte(getContext().getIdentifier()));
            buffer.putLong(child.getRange().startRange);
            buffer.putLong(child.getRange().endRange);
        }
            
            
//            TreeNode node = ith.getMasterProperties().getTree().getNode(getContext().getIdentifier());
//            System.out.println("Node.getsernode is null? " + node == null);
//            if(!node.getParentNode().getServerNode().getId().equals(ith.getMasterProperties().getRootNode().getId())){
//                buffer.putInt(21);
//                buffer.put(HexConverter.toByte(getJobID()));
//                buffer.put(Protocol.REDIRECT.getValue());
//                buffer.put(HexConverter.toByte(getContext().getIdentifier()));
//                buffer.put(node.getParentNode().getServerNode().getIpaddress().getAddress());
//                buffer.putInt(node.getParentNode().getServerNode().getPort());
//                buffer.put(HexConverter.toByte(node.getParentNode().getServerNode().getId()));
//            }else{
//                buffer.putInt(25);
//                buffer.put(HexConverter.toByte(getJobID()));
//                buffer.put(Protocol.CONNECT.getValue());
//                buffer.put(HexConverter.toByte(getContext().getIdentifier()));
//                buffer.putLong(node.getServerNode().getRange().startRange);
//                buffer.putLong(node.getServerNode().getRange().endRange);
//            }
//        }else{
//            ServerNode child = ith.getChildProperties().getChild(getContext().getIdentifier());
//            buffer.putInt(25);
//            buffer.put(HexConverter.toByte(getJobID()));
//            buffer.put(Protocol.CONNECT.getValue());
//            buffer.put(HexConverter.toByte(getContext().getIdentifier()));
//            buffer.putLong(child.getRange().startRange);
//            buffer.putLong(child.getRange().endRange);
//        }
        
        buffer.flip();
        while(buffer.hasRemaining())
            s.write(buffer);
        
        buffer.clear(); //Always clear buffer
        setFinished(true);
        return true;
    }
}
