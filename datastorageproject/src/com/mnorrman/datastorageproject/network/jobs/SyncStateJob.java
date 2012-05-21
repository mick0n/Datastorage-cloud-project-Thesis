/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.ServerState;
import com.mnorrman.datastorageproject.network.*;
import com.mnorrman.datastorageproject.objects.Range;
import com.mnorrman.datastorageproject.tools.HexConverter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Mikael
 */
public class SyncStateJob extends InternalJob {

    private Main main;
    private InternalTrafficHandler ith;

    public SyncStateJob(InternalTrafficContext context, Main main, InternalTrafficHandler ith) {
        super(context);
        this.main = main;
        this.ith = ith;
    }

    public SyncStateJob(InternalTrafficContext context, String jobID, String owner) {
        super(context, jobID);
    }

    @Override
    public boolean readOperation(ByteBuffer buffer) throws IOException {
        if (!Main.properties.getValue("master").toString().equals("127.0.0.1")) {
            buffer.rewind();
            while (buffer.hasRemaining()) {
                ith.getMasterContext().getChannel().write(buffer);
            }
            buffer.clear();
        } else {
            buffer.rewind();
            ClusterMessageVariables cmv = new ClusterMessageVariables(buffer);
            buffer.get();
            if(ith.getMasterProperties().getTree().containsID(cmv.getFrom())){
                ith.getMasterProperties().getTree().getNode(cmv.getFrom()).getServerNode().setState(ServerState.getState(buffer.get()));
                ith.getMasterProperties().getTree().getNode(cmv.getFrom()).getServerNode().setStorageLimit(buffer.getLong());
                ith.getMasterProperties().getTree().getNode(cmv.getFrom()).getServerNode().setDataSize(buffer.getLong());
                ith.getMasterProperties().getTree().getNode(cmv.getFrom()).getServerNode().setRange(new Range(buffer.getLong(), buffer.getLong()));
            }
//            System.out.println("Received state change from 0x" + getContext().getIdentifier() + ": " + ServerState.getState(buffer.get()).toString());
//            System.out.println("It has current datasize: " + buffer.getLong());
        }
        //System.out.println("Received state change from 0x" + getFromConnection() + ": " + ServerState.getState(buffer.get()).toString());
//        Main.slaveList.get(getFromConnection()).setState(ServerState.getState(buffer.get()));
//        Main.slaveList.get(getFromConnection()).setDataSize(buffer.getLong());
        setFinished(true);
        buffer.clear();
//        context.getNode().setState(ServerState.getState(newState));
//        System.out.println("Check: " + context.getNode().getState().toString());
//        context.setTask(null);
//        context.setCommand(Protocol.NULL);
        return false;
    }

    @Override
    public boolean writeOperation(SocketChannel s, ByteBuffer buffer) throws IOException {
        if(Main.state.getValue() > ServerState.INDEXING.getValue()){
            buffer.put(HexConverter.toByte(Main.ID));
            buffer.putInt(38);
            buffer.put(HexConverter.toByte(getJobID()));
            buffer.put(Protocol.SYNC_STATE.getValue());
            buffer.put(Main.state.getValue());
            buffer.putLong(Long.parseLong(Main.properties.getValue("storagelimit").toString()) * 1000);
            buffer.putLong(main.getCurrentDataSize());
            buffer.putLong(ith.getChildProperties().getThisNode().getRange().startRange);
            buffer.putLong(ith.getChildProperties().getThisNode().getRange().endRange);
            buffer.flip();
            while (buffer.hasRemaining()) {
                s.write(buffer);
            }
        }
        setFinished(true);
        buffer.clear(); //Always clear buffer
        return true;
    }
}
