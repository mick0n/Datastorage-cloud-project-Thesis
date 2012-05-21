/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.ServerState;
import com.mnorrman.datastorageproject.network.jobs.AbstractJob;
import com.mnorrman.datastorageproject.network.jobs.ChildConnectJob;
import com.mnorrman.datastorageproject.network.jobs.PutJob;
import com.mnorrman.datastorageproject.tools.HexConverter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author Mikael
 */
public class SlaveNode extends Thread {

    private Main main;
    private ConnectionContext context;
    private Selector selector;
    private int readyKeys;
    private ByteBuffer buffer;
    private boolean keepWorking = true;
    private HashMap<String, AbstractJob> jobs;
    private Queue<AbstractJob> jobQueue;

    public SlaveNode(Main main) {
        this.main = main;
        try {
            selector = Selector.open();
            context = new ConnectionContext(SocketChannel.open());
            context.channel.configureBlocking(false);
            jobs = new HashMap<String, AbstractJob>();
            jobQueue = new LinkedList<AbstractJob>();
        } catch (IOException e) {
            LogTool.log(e, LogTool.CRITICAL);
        }
    }

    public void startSlaveServer() {
        this.start();
//        Main.timer.scheduleAtFixedRate(new SyncStateTimerTask(this, main), 1500, 1500);
    }

    @Override
    public void run() {

        buffer = ByteBuffer.allocateDirect(MasterNode.NETWORK_BLOCK_SIZE);

        while (keepWorking) {
            try {
                if (keepWorking && !context.channel.isConnected()) {
                    context.channel.connect(new InetSocketAddress(InetAddress.getByName(Main.properties.getValue("master").toString()), Integer.parseInt(Main.properties.getValue("port").toString())));
                    while (!context.channel.finishConnect()) {
                        LogTool.log("Could not connect to master node, trying again in 2 seconds", LogTool.WARNING);
                        try {
                            sleep(2000);
                        } catch (InterruptedException e) {
                            LogTool.log(e, LogTool.CRITICAL);
                        }
                    }
                    context.channel.register(selector, SelectionKey.OP_READ);
                    ChildConnectJob scj = new ChildConnectJob();
                    jobs.put(scj.getJobID(), scj);
                    jobQueue.add(scj);
                }
            } catch (IOException e) {
                LogTool.log(e, LogTool.CRITICAL);
            }


            readyKeys = 0;

            try {
                if (!jobQueue.isEmpty()) {
                    readyKeys = selector.selectNow();
                } else {
                    readyKeys = selector.select();
                }
            } catch (IOException e) {
                LogTool.log(e, LogTool.CRITICAL);
            }

            if (readyKeys > 0) {
                Iterator it = selector.selectedKeys().iterator();

                while (it.hasNext()) {
                    SelectionKey key = (SelectionKey) it.next();
                    it.remove();

                    if (key.isValid() && key.isReadable()) {
                        while (buffer.position() != buffer.capacity()) {
                            try {
                                if (context.channel.read(buffer) == -1) {
                                    throw new IOException();
                                }
                            } catch (IOException e) {
                                Main.state = ServerState.NOTRUNNING;
                                key.cancel();
                                LogTool.log("Connection to master was closed", LogTool.WARNING);
                                if (buffer.position() == 0){ //If there was no data
                                    buffer.clear();
                                    return;
                                }else{
                                    break;
                                }
                            }
                        }
                        buffer.flip();

                        ClusterMessageVariables cmv = new ClusterMessageVariables(buffer);
                        
                        //If the job exists
                        if (cmv.getJobID().equals("00000000") || !jobs.containsKey(cmv.getJobID())) {
                            //If no such job exist
                            Protocol command = Protocol.getCommand(buffer.get());

                            switch (command) {
                                case PUT:
//                                    PutJob pj = new PutJob(cmv.getJobID(), Main.ID, main.getNewDataProcessor());
//                                    jobs.put(pj.getJobID(), pj);
                                    //context.setJobID(pj.getJobID());
                                    break;
                                case GET:

                                    break;
                                case PING:

                                    break;
                            }
                        }
                        
                        AbstractJob job = jobs.get(cmv.getJobID());
                        try {
                            if (job.readOperation(buffer)) {
                                //If the readOperation returns true it
                                //means it has something to write.
                                jobQueue.offer(job);
                            }
                        } catch (IOException e) {
                            LogTool.log(e, LogTool.CRITICAL);
                        }
                        if (job.isFinished()) {
                            jobs.remove(job.getJobID());
                        }
                    }
                    selector.selectedKeys().clear();
                }
            }
            try {
                if (!jobQueue.isEmpty()) {
                    AbstractJob queuedJob = jobQueue.poll();
                    if (!queuedJob.writeOperation(context.channel, buffer)) {
                        jobQueue.offer(queuedJob);
                    } else {
                        if (queuedJob.isFinished()) {
                            jobs.remove(queuedJob.getJobID());
                        }
                    }
                }
            } catch (IOException e) {
                LogTool.log(e, LogTool.CRITICAL);
            }

            if (jobs.isEmpty() && jobQueue.isEmpty() && Main.state.getValue() > ServerState.INDEXING.getValue()) {
                Main.state = ServerState.IDLE;
            } else if (Main.state.getValue() > ServerState.INDEXING.getValue()) {
                Main.state = ServerState.RUNNING;
            }
        }
    }

    public void createJob(String jobOwner, AbstractJob job) {
        if (Main.state == ServerState.IDLE || Main.state == ServerState.RUNNING) {
            if (jobOwner != null && job != null) {
                jobs.put(jobOwner, job);
                jobQueue.add(job);
                selector.wakeup();
            }
        }
    }

    public void close() throws IOException {
        buffer.clear();
        buffer.put(HexConverter.toByte(Main.ID));
        buffer.putInt(1);
        buffer.put(Protocol.DISCONNECT.getValue());
        buffer.rewind();
        context.channel.write(buffer);
        context.channel.close();
        selector.close();
        keepWorking = false;
    }
}
