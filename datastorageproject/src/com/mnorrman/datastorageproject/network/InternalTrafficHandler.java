/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.ServerState;
import com.mnorrman.datastorageproject.network.jobs.*;
import com.mnorrman.datastorageproject.objects.Pair;
import com.mnorrman.datastorageproject.objects.ServerNode;
import com.mnorrman.datastorageproject.objects.TreeNode;
import com.mnorrman.datastorageproject.tools.HexConverter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Mikael
 */
public class InternalTrafficHandler extends Thread{
    public static final int MAXIMUM_NETWORK_BLOCK_SIZE = 8192;
    private final boolean master;
    private Main main;
    private Selector selector;
    private TreeNode root;
    private ConcurrentLinkedQueue<SocketChannel> channelQueue;
    private HashMap<String, InternalTrafficContext> connections;
    private ByteBuffer buffer;
    
    private InternalTrafficHandlerChildProperties childStuff;
    private InternalTrafficHandlerMasterProperties masterStuff;
    
    private HashMap<String, InternalJob> jobs;
    private Queue<InternalJob> jobQueue;
    
    private int readBytes;
    private boolean keepWorking = true;

    public InternalTrafficHandler(Main main) {

        if (Main.properties.getValue("master").toString().equals("127.0.0.1")) {
            master = true;
        } else {
            master = false;
            childStuff = new InternalTrafficHandlerChildProperties();
        }

        this.main = main; //used for getting DataProcesses from BackStorage
//        this.root = new TreeNode();

        try {
            selector = Selector.open();
            channelQueue = new ConcurrentLinkedQueue<SocketChannel>();
            connections = new HashMap<String, InternalTrafficContext>();
            jobs = new HashMap<String, InternalJob>(1033);
            jobQueue = new LinkedList<InternalJob>();
        } catch (IOException e) {
            LogTool.log(e, LogTool.CRITICAL);
        }
    }
    
    /**
     * Initiate threads related to this object
     */
    public void startup() {
        this.start();
        if (!master) {
//            Main.timer.scheduleAtFixedRate(new SyncStateTimerTask(this, main), 1500, 1500);
        }
    }
    
    @Override
    public void run() {
        
        //The bytebuffer here is used in multiple ways. Even though it is
        //quite big, it can be used for small amounts of data by setting
        //the limit to appropriate sizes.
        buffer = ByteBuffer.allocateDirect(MAXIMUM_NETWORK_BLOCK_SIZE);
        int readyChannels = 0;

        while (keepWorking) {
            if (!master) {
                try {
                    if(childStuff.masterContext == null){
                        childStuff.masterContext = new InternalTrafficContext(SocketChannel.open());
                        childStuff.masterContext.channel.configureBlocking(false);
                    }
                    if (!childStuff.masterContext.channel.isConnected()) {
                        Main.state = ServerState.CONNECTING;
                        //childStuff.channelToMaster.connect(new InetSocketAddress(InetAddress.getByName(Main.properties.getValue("master").toString()), Integer.parseInt(Main.properties.getValue("internalport").toString())));
                        childStuff.masterContext.channel.connect(new InetSocketAddress(InetAddress.getByName(Main.properties.getValue("master").toString()), 8989));
                        while (!childStuff.masterContext.channel.finishConnect()) {
                            LogTool.log("Could not connect to master, trying again in 2 seconds", LogTool.WARNING);
                            try {
                                sleep(2000);
                            } catch (InterruptedException e) {
                                LogTool.log(e, LogTool.CRITICAL);
                            }
                        }
                        childStuff.masterContext.channel.register(selector, SelectionKey.OP_READ, childStuff.masterContext);
                        ConnectToMasterJob ctmj = new ConnectToMasterJob(childStuff.masterContext, this);
                        jobs.put(ctmj.getJobID(), ctmj);
                        jobQueue.add(ctmj);
                    }
                } catch (IOException e) {
                    LogTool.log(e, LogTool.CRITICAL);
                }
            }

            try {
                if (!jobQueue.isEmpty()) {
                    readyChannels = selector.selectNow();
                } else {
                    readyChannels = selector.select();
                }
            } catch (IOException e) {
                LogTool.log(e, LogTool.CRITICAL);
            }

            //Since readyChannels may be zero, we check this first
            if (readyChannels > 0) {

                Iterator it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = (SelectionKey) it.next();

                    //Remove key, otherwise it will stay in the list forever.
                    it.remove();

                    try {
                        if (key.isValid() && key.isReadable()) {    
                            InternalTrafficContext itc = (InternalTrafficContext) key.attachment();

                            ClusterMessageVariables cmv = new ClusterMessageVariables();

                            buffer.limit(8);
                            if(buffer.position() != 0)
                                buffer.rewind();

                            while (buffer.hasRemaining()) {
                                try {
                                    if ((readBytes = itc.channel.read(buffer)) == -1) {
                                        connections.remove(itc.identifier);
                                        key.cancel();
                                        LogTool.log("Connection from " + itc.channel.getRemoteAddress() + " was closed", LogTool.INFO);
                                        return;
                                    }
                                    if (buffer.limit() == 8 && buffer.position() == buffer.limit()) {
                                        System.out.println("Read first 8 bytes");
                                        buffer.rewind();
                                        cmv.setFrom(HexConverter.toHex(buffer.getInt()));
                                        cmv.setLength(buffer.getInt());
                                        System.out.println("Length variable: " + cmv.getLength());
                                        if (cmv.getLength() > 0) {
                                            buffer.limit(8 + cmv.getLength());
                                        }
                                    }

                                } catch (IOException e) {
                                    connections.remove(itc.identifier);
                                        key.cancel();
                                        LogTool.log("Connection from " + itc.channel.getRemoteAddress() + " was closed", LogTool.INFO);
                                    return;
                                }
                            }
                            buffer.flip();
                            cmv = new ClusterMessageVariables(buffer);

                            if (cmv.getJobID().equals("00000000") || !jobs.containsKey(cmv.getJobID())) {
                                Protocol command = Protocol.getCommand(buffer.get());

                                switch (command) {
                                    case CONNECT:
                                        ReceiveConnectJob rcj = new ReceiveConnectJob(itc, cmv.getJobID(), this);
                                        jobs.put(rcj.getJobID(), rcj);
                                        cmv.setJobID(rcj.getJobID());
                                        //jobQueue.add(mcj);
                                        break;
                                    case SYNC_STATE:
//                                        SyncStateJob ssj = new SyncStateJob(cmv.getJobID(), cmv.getFrom());
//                                        jobs.put(ssj.getJobID(), ssj);
//                                        cmv.setJobID(ssj.getJobID());
//                                        break;
                                }
                            }

                            //Perform the job
                            InternalJob job = jobs.get(cmv.getJobID());
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
                            buffer.clear();
                        }
                    } catch (IOException e) {
                        //Remove the channel from this selector
                        key.cancel();
                        connections.remove(((InternalTrafficContext)key.attachment()).identifier);
                        LogTool.log(e, LogTool.WARNING);
                    }
                }
            }

            //Perform a write operation, if any
            try {
                if (!jobQueue.isEmpty()) {
                    InternalJob queuedJob = jobQueue.poll();
                    if(queuedJob.getContext().channel != null){
                        if (!queuedJob.writeOperation(queuedJob.getContext().channel, buffer)) {
                                jobQueue.offer(queuedJob);
                            } else {
                                if (queuedJob.isFinished()) {
                                    jobs.remove(queuedJob.getJobID());
                                }
                            }
                        }
                }
            } catch (IOException e) {
                LogTool.log(e, LogTool.CRITICAL);
            }
//
//            if (jobs.isEmpty() && jobQueue.isEmpty() && Main.state.getValue() > ServerState.INDEXING.getValue()) {
//                Main.state = ServerState.IDLE;
//            } else if (Main.state.getValue() > ServerState.INDEXING.getValue()) {
//                Main.state = ServerState.RUNNING;
//            }

            //After checking all keys we check if there are any channels waiting
            //to be registered with this selector. Creates a new ID for each
            //connection and stores the id as an attachment while a 
            //connectionContext is stored in a hashmap.
            if (!channelQueue.isEmpty()) {
                
                SocketChannel sc = channelQueue.poll();
                
                try {
                    String temporaryID = HexConverter.toHex((short) new Random().nextInt());
                    InternalTrafficContext itc = new InternalTrafficContext(sc, temporaryID);
                    sc.register(selector, SelectionKey.OP_READ, itc);
                    connections.put(temporaryID, itc);
                    LogTool.log("Connection from " + sc.getRemoteAddress() + " was added to selector", LogTool.INFO);
                } catch (NullPointerException e) {
                    LogTool.log(e, LogTool.CRITICAL);
                } catch (ClosedChannelException e) {
                    LogTool.log(e, LogTool.CRITICAL);
                } catch (IOException e) {
                    LogTool.log(e, LogTool.CRITICAL);
                }
            }


//            //Reconfigure maximum size of buffer
//            int a = (int) Math.floor(Math.log(childConn.size()) / Math.log(2));
//            int b = (int) Math.floor(Math.log(jobQueue.size()) / Math.log(2));
//            int c = (int) Math.floor(Math.log(jobs.size()) / Math.log(2));
//            int medium = (int) Math.floor((a + b + c) / 3);
//            if (medium > 0) {
//                buffer.limit(MAXIMUM_NETWORK_BLOCK_SIZE / medium);
//            } else {
//                buffer.limit(MAXIMUM_NETWORK_BLOCK_SIZE);
//            }
//            System.out.println("New buffer limit = " + buffer.limit());
        }
    }
    
    public void createJob(String jobID, InternalJob job) {
        if (jobID != null && job != null) {
            jobs.put(jobID, job);
            jobQueue.add(job);
            selector.wakeup();
        }
    }

    /**
     * Adds a SocketChannel to a queue which eventually will be registered to a
     * selector.
     *
     * @param channel
     */
    public synchronized void addSocketChannel(SocketChannel channel) {
        this.channelQueue.add(channel);
        selector.wakeup();
    }

    public void switchConnectionID(String oldID, String newID) {
        InternalTrafficContext itc = connections.get(oldID);
        itc.setIdentifier(newID);
        connections.remove(oldID);
        connections.put(itc.identifier, itc);
    }

    public void close() throws IOException {
        keepWorking = false;
        selector.wakeup();
    }

    public HashMap<String, InternalTrafficContext> getConnections() {
        return connections;
    }

    public HashMap<String, InternalJob> getJobs() {
        return jobs;
    }
}
