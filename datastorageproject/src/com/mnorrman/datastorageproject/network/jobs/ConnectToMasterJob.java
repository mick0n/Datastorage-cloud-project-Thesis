/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.ServerState;
import com.mnorrman.datastorageproject.network.ClusterNode;
import com.mnorrman.datastorageproject.network.InternalTrafficContext;
import com.mnorrman.datastorageproject.network.InternalTrafficHandler;
import com.mnorrman.datastorageproject.network.Protocol;
import com.mnorrman.datastorageproject.tools.HexConverter;
import java.io.IOException;
import java.net.InetSocketAddress;
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
        if(Main.ID.equals("FFFFFFFF")){
            System.out.println("Fixing new id");
            Main.ID = HexConverter.toHex(buffer.getInt());
            Main.properties.setProperty("serverID", Main.ID);
            Main.properties.saveProperties();
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
        buffer.putInt(13);
        buffer.put(HexConverter.toByte(getJobID()));
        buffer.put(Protocol.CONNECT.getValue());
        buffer.putLong(Long.parseLong(Main.properties.getValue("storagelimit").toString()) * 1000);
        buffer.flip();
        while(buffer.hasRemaining())
            s.write(buffer);
        buffer.clear(); //Always clear buffer
        return true;
    }
    
    
    
}
