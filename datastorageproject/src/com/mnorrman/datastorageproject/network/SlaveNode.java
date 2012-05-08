/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.ServerState;
import com.mnorrman.datastorageproject.network.jobs.AbstractJob;
import com.mnorrman.datastorageproject.network.jobs.ConnectJob;
import com.mnorrman.datastorageproject.network.jobs.SyncLocalIndexJob;
import com.mnorrman.datastorageproject.tools.HexConverter;
import com.mnorrman.datastorageproject.tools.IntConverter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 *
 * @author Mikael
 */
public class SlaveNode extends Thread {

    private Main main;
    private ConnectionContext context;
    private Selector selector;
    private ByteBuffer buffer;
    private boolean keepWorking = true;
    private HashMap<String, AbstractJob> jobs;
    private PriorityQueue<AbstractJob> jobQueue;

    public SlaveNode(Main main) {
        this.main = main;
        try {
            selector = Selector.open();
            context = new ConnectionContext(SocketChannel.open(), Protocol.NULL);
            context.channel.configureBlocking(false);
            jobs = new HashMap<String, AbstractJob>();
            jobQueue = new PriorityQueue<AbstractJob>();

            Main.timer.scheduleAtFixedRate(new SyncStateTimerTask(this), 5000, 5000);
        } catch (IOException e) {
            LogTool.log(e, LogTool.CRITICAL);
        }
    }

    public void startSlaveServer() {
        this.start();
    }

    @Override
    public void run() {

        //The bytebuffer here is used in multiple ways. Even though it is
        //quite big, it can be used for small amounts of data by setting
        //the limit to appropriate sizes.
        buffer = ByteBuffer.allocateDirect(MasterNode.NETWORK_BLOCK_SIZE);

        while (keepWorking) {
            try {
                while (keepWorking && !context.channel.isConnected()) {
                    context.channel.connect(new InetSocketAddress(InetAddress.getByName(Main.properties.getValue("master").toString()), Integer.parseInt(Main.properties.getValue("port").toString())));
                    if (!context.channel.finishConnect()) {
                        LogTool.log("Could not connect to master node, trying again in 10 seconds", LogTool.WARNING);
                        try {
                            sleep(10000);
                        } catch (InterruptedException e) {
                            LogTool.log(e, LogTool.CRITICAL);
                        }
                    } else {
                        context.channel.register(selector, SelectionKey.OP_READ);
                        ConnectJob conJob = new ConnectJob("00000000");
                        jobs.put("00000000", conJob);
                        jobQueue.add(conJob);
                    }
                }
            } catch (IOException e) {
                LogTool.log(e, LogTool.CRITICAL);
            }
            
            
            int readyKeys = 0;
            
            try {
                if(!jobQueue.isEmpty()){
                    readyKeys = selector.selectNow();
                }else{
                    readyKeys = selector.select();
                }
            } catch (IOException e) {
                LogTool.log(e, LogTool.CRITICAL);
            }
            
            if(readyKeys > 0){
                Iterator it = selector.selectedKeys().iterator();

                while (it.hasNext()) {
                    SelectionKey key = (SelectionKey) it.next();
                    it.remove();

                    if (key.isValid()) {
                        if (key.isReadable()) {
                            while (buffer.position() != buffer.capacity()) {
                                try {
                                    if (context.channel.read(buffer) == -1) {
                                        //Connection has been terminated, cleanup.                                   
                                        key.cancel();
                                        LogTool.log("Connection to master was closed", LogTool.INFO);
                                        buffer.clear();
                                        return;
                                    }
                                } catch (IOException e) {
                                    key.cancel();
                                    LogTool.log("Connection to master was closed", LogTool.INFO);
                                    buffer.clear();
                                    return;
                                }
                            }
                            buffer.flip();
                            byte[] from = new byte[4];
                            buffer.get(from);
                            int length = buffer.getInt();

                            System.out.println("From: 0x" + HexConverter.toHex(from) + " with len: " + length);

                            if (jobs.containsKey(HexConverter.toHex(from))) {
                                System.out.println("Contains!");
                                AbstractJob job = jobs.get(HexConverter.toHex(from));
                                if (job instanceof ConnectJob) {
                                    Main.ID = IntConverter.intToByteArray(buffer.getInt());
                                    System.out.println("My ID: 0x" + HexConverter.toHex(Main.ID));
                                    jobs.remove(HexConverter.toHex(from));
                                    Main.state = com.mnorrman.datastorageproject.ServerState.IDLE;
                                }
                            }else{
                                Protocol command = Protocol.getCommand(buffer.get());

                                switch(command){
                                    case GET:

                                        break;
                                    case PING:

                                        break;
                                }
                            }
                        }
                    }
                    selector.selectedKeys().clear();
                }
            }
            try {
                if (!jobQueue.isEmpty()) {
                    AbstractJob queuedJob = jobQueue.poll();
                    if (!queuedJob.update(context.channel, buffer)) {
                        jobQueue.offer(queuedJob);
                    } else {
                        if (queuedJob.isFinished()) {
                            jobs.remove(queuedJob.getOwner());
                        }
                    }
                }
            } catch (IOException e) {
                LogTool.log(e, LogTool.CRITICAL);
            }
            
            if(jobs.isEmpty() && jobQueue.isEmpty() && Main.state.getValue() > ServerState.INDEXING.getValue()){
                Main.state = ServerState.IDLE;
            }else{
                Main.state = ServerState.RUNNING;
            }
        }
    }

    public void createJob(String jobOwner, AbstractJob job) {
        if(Main.state == ServerState.IDLE || Main.state == ServerState.RUNNING){
            if (jobOwner != null && job != null) {
                jobs.put(jobOwner, job);
                jobQueue.add(job);
                selector.wakeup();
            }
        }
    }

    public void close() throws IOException {
        buffer.clear();
        buffer.put(Main.ID);
        buffer.putInt(1);
        buffer.put(Protocol.DISCONNECT.getValue());
        buffer.rewind();
        context.channel.write(buffer);
        context.channel.close();
        selector.close();
        keepWorking = false;
    }
}
