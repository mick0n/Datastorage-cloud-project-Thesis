/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.ServerState;
import com.mnorrman.datastorageproject.network.Protocol;
import com.mnorrman.datastorageproject.network.SlaveNode;
import com.mnorrman.datastorageproject.tools.HexConverter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Mikael
 */
public class ChildConnectJob extends AbstractJob{

    public ChildConnectJob(){
        super();
    }
    
    @Override
    public boolean readOperation(ByteBuffer buffer) throws IOException {
        if(Main.ID.equals("FFFFFFFF")){
            System.out.println("Fixing new id");
            byte[] newID = new byte[4];
            buffer.get(newID);
            Main.ID = HexConverter.toHex(newID);
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
        //First send our connect command. At this point we are considered
        //to be a client at the master.
        //Directly after we send our ID.
        buffer.limit(5);
        
        buffer.put(Protocol.CONNECT.getValue());
        buffer.put(HexConverter.toByte(Main.ID));
        buffer.flip();
        while(buffer.hasRemaining())
            s.write(buffer);
        buffer.clear(); //Always clear buffer
        System.out.println("First write");
        
        //The put the ordinary connect data
        buffer.put(HexConverter.toByte(Main.ID));
        buffer.putInt(1);
        buffer.put(HexConverter.toByte(getJobID()));
        buffer.put(Protocol.CONNECT.getValue());
        buffer.putLong(Long.parseLong(Main.properties.getValue("storagelimit").toString()) * 1000);
        buffer.rewind();
        while(buffer.hasRemaining())
            s.write(buffer);
        buffer.clear(); //Always clear buffer
        return true;
    }
    
}
