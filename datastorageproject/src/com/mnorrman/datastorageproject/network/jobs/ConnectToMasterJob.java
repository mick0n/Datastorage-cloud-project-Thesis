/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.ServerState;
import com.mnorrman.datastorageproject.network.InternalTrafficContext;
import com.mnorrman.datastorageproject.network.InternalTrafficHandler;
import com.mnorrman.datastorageproject.network.Protocol;
import com.mnorrman.datastorageproject.objects.Range;
import com.mnorrman.datastorageproject.objects.ServerNode;
import com.mnorrman.datastorageproject.tools.HexConverter;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Mikael
 */
public class ConnectToMasterJob extends InternalJob{

    private InternalTrafficHandler ith;
    
    public ConnectToMasterJob(InternalTrafficContext context, InternalTrafficHandler ith){
        super(context);
        this.ith = ith;
    }

    @Override
    public boolean readOperation(ByteBuffer buffer) throws IOException {
        Protocol command = Protocol.getCommand(buffer.get());
        if(Main.ID.equals("FFFFFFFF")){
            System.out.println("Fixing new id");
            Main.ID = HexConverter.toHex(buffer.getInt());
            ith.getChildProperties().getThisNode().setId(Main.ID);
            Main.properties.setProperty("serverID", Main.ID);
            Main.properties.saveProperties();
        }else{ buffer.getInt(); } //Just ignore the ID
        
        if(command == Protocol.REDIRECT){
            byte[] IPBytes = new byte[4];
            buffer.get(IPBytes);
            InetAddress IPAddress = InetAddress.getByAddress(IPBytes);
            int port = buffer.getInt();
            String newMasterID = HexConverter.toHex(buffer.getInt());
//            ith.getMasterContext().getChannel().close();
            ith.getMasterContext().setChannel(SocketChannel.open());
            ith.getMasterContext().getChannel().configureBlocking(false);
            ith.getMasterContext().setNode(new ServerNode(IPAddress, port, newMasterID));
            ith.getMasterContext().setIdentifier(newMasterID);
            
            System.out.println("Redirection in progress. Redirecting to master: 0x" + newMasterID);
            System.out.println("IP: " + IPAddress.getHostAddress());
            System.out.println("port: " + port);
        }else{
            long startRange = buffer.getLong();
            long endRange = buffer.getLong();
            ith.getChildProperties().getThisNode().setRange(new Range(startRange, endRange));
        }
        
        System.out.println("ID: 0x" + Main.ID);
        Main.state = ServerState.IDLE;

        setFinished(true);
        buffer.clear();
        return false;
    }

    @Override
    public boolean writeOperation(SocketChannel s, ByteBuffer buffer) throws IOException {
        buffer.put(HexConverter.toByte(Main.ID));
        buffer.putInt(9);
        buffer.put(HexConverter.toByte(getJobID()));
        buffer.put(Protocol.CONNECT.getValue());
        buffer.putInt(Integer.parseInt(Main.properties.getValue("internalport").toString()));
        buffer.flip();
        while(buffer.hasRemaining())
            s.write(buffer);
        buffer.clear(); //Always clear buffer
        return true;
    }
    
    
    
}
