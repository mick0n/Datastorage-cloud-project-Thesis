/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.ServerState;
import com.mnorrman.datastorageproject.network.jobs.ConnectToMasterJob;
import com.mnorrman.datastorageproject.network.jobs.InternalJob;
import com.mnorrman.datastorageproject.network.jobs.ReceiveConnectJob;
import com.mnorrman.datastorageproject.network.jobs.SyncStateJob;
import com.mnorrman.datastorageproject.objects.ServerNode;
import com.mnorrman.datastorageproject.objects.Tree;
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
public class InternalTrafficHandler extends Thread {

    public static final int MAXIMUM_NETWORK_BLOCK_SIZE = 8192;
    private final boolean master;
    private Main main;
    private Selector selector;
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
        }

        this.main = main; //used for getting DataProcesses from BackStorage
//        this.root = new Tree();

        try {
            selector = Selector.open();
            channelQueue = new ConcurrentLinkedQueue<SocketChannel>();
            connections = new HashMap<String, InternalTrafficContext>();
            jobs = new HashMap<String, InternalJob>(1033);
            jobQueue = new LinkedList<InternalJob>();

            if (master) {
                masterStuff = new InternalTrafficHandlerMasterProperties();
            } else {
                childStuff = new InternalTrafficHandlerChildProperties();
                childStuff.thisNode = new ServerNode(InetAddress.getByName("127.0.0.1"), Integer.parseInt(Main.properties.getValue("internalport").toString()), Main.ID);
                childStuff.masterContext = new InternalTrafficContext(SocketChannel.open());
                childStuff.masterContext.channel.configureBlocking(false);
                childStuff.masterContext.identifier = "00000000";
                childStuff.masterContext.node = new ServerNode(InetAddress.getByName(Main.properties.getValue("master").toString()), 8989, childStuff.masterContext.identifier);
            }
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
            Main.timer.scheduleAtFixedRate(new SyncStateTimerTask(this, main), 1500, 1500);
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
                    if (!childStuff.masterContext.channel.isConnected()) {
                        Main.state = ServerState.CONNECTING;
                        //childStuff.channelToMaster.connect(new InetSocketAddress(InetAddress.getByName(Main.properties.getValue("master").toString()), Integer.parseInt(Main.properties.getValue("internalport").toString())));
                        childStuff.masterContext.channel.connect(new InetSocketAddress(childStuff.masterContext.node.getIpaddress(), childStuff.masterContext.node.getInternalport()));
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
                            if (buffer.position() != 0) {
                                buffer.rewind();
                            }

                            while (buffer.hasRemaining()) {
                                try {
                                    if ((readBytes = itc.channel.read(buffer)) == -1) {
                                        if(master){ masterStuff.tree.getNode(itc.identifier).getServerNode().setState(ServerState.NOTRUNNING); }
                                        connections.remove(itc.identifier);
                                        key.cancel();
                                        LogTool.log("Connection from " + itc.channel.getRemoteAddress() + " was closed", LogTool.INFO);
                                        break;
                                    }
                                    if (buffer.limit() == 8 && buffer.position() == buffer.limit()) {
                                        buffer.rewind();
                                        cmv.setFrom(HexConverter.toHex(buffer.getInt()));
                                        cmv.setLength(buffer.getInt());
                                        if (cmv.getLength() > 0) {
                                            buffer.limit(8 + cmv.getLength());
                                        }
                                    }

                                } catch (IOException e) {
                                    if(master){ masterStuff.tree.getNode(itc.identifier).getServerNode().setState(ServerState.NOTRUNNING); }
                                    connections.remove(itc.identifier);
                                    key.cancel();
                                    LogTool.log("Connection from " + itc.channel.socket().getInetAddress().getHostAddress() + ":" + itc.channel.socket().getPort() + " was closed", LogTool.INFO);
                                    break;
                                }
                            }

                            if (buffer.position() < 12) {
                                break;
                            }

                            buffer.flip();
                            cmv = new ClusterMessageVariables(buffer);

                            if (cmv.getJobID().equals("00000000") || !jobs.containsKey(cmv.getJobID())) {
                                Protocol command = Protocol.getCommand(buffer.get());

                                switch (command) {
                                    case CONNECT:
                                        if (!master && cmv.getFrom().equals("00000000")) {
                                            if(master){ masterStuff.tree.getNode(itc.identifier).getServerNode().setState(ServerState.NOTRUNNING); }
                                            key.cancel();
                                            connections.remove(itc.identifier);
                                            cmv = null;
                                            LogTool.log("Connection from " + itc.channel.getRemoteAddress() + " was denied", LogTool.INFO);
                                            return;
                                        }
                                        ReceiveConnectJob rcj = new ReceiveConnectJob(itc, cmv.getJobID(), this);
                                        jobs.put(rcj.getJobID(), rcj);
                                        cmv.setJobID(rcj.getJobID());
                                        //jobQueue.add(mcj);
                                        break;
                                    case SYNC_STATE:
                                        SyncStateJob ssj = new SyncStateJob(itc, main, this);
                                        jobs.put(ssj.getJobID(), ssj);
                                        cmv.setJobID(ssj.getJobID());
                                        break;
                                }
                            }

                            if (cmv != null) {
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
                            }
                            buffer.clear();
                        }
                    } catch (IOException e) {
                        //Remove the channel from this selector
                        key.cancel();
                        connections.remove(((InternalTrafficContext) key.attachment()).identifier);
                        LogTool.log(e, LogTool.WARNING);
                    }
                }
            }

            //Perform a write operation, if any
            try {
                if (!jobQueue.isEmpty()) {
                    InternalJob queuedJob = jobQueue.poll();
                    if (queuedJob.getContext().channel != null) {
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
                    LogTool.log("Connection from " + sc.socket().getInetAddress().getHostAddress() + ":" + sc.socket().getPort() + " was added to selector", LogTool.INFO);
                } catch (NullPointerException e) {
                    LogTool.log(e, LogTool.CRITICAL);
                } catch (ClosedChannelException e) {
                    LogTool.log(e, LogTool.CRITICAL);
                } catch (IOException e) {
                    LogTool.log(e, LogTool.CRITICAL);
                }
            }
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
        if (masterStuff != null) {
            masterStuff.breadthfirstSaveTree();
        }
        if (childStuff != null) {
            childStuff.saveChildren();
        }
        selector.wakeup();
    }

    public HashMap<String, InternalTrafficContext> getConnections() {
        return connections;
    }

    public HashMap<String, InternalJob> getJobs() {
        return jobs;
    }

    public InternalTrafficContext getMasterContext() {
        return childStuff.masterContext;
    }

    public boolean isMaster() {
        return master;
    }

    public InternalTrafficHandlerChildProperties getChildProperties() {
        return childStuff;
    }

    public InternalTrafficHandlerMasterProperties getMasterProperties() {
        return masterStuff;
    }
}
