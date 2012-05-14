package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.ServerState;
import com.mnorrman.datastorageproject.network.jobs.*;
import com.mnorrman.datastorageproject.tools.HexConverter;
import com.mnorrman.datastorageproject.tools.IntConverter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Mikael Norrman
 */
public class MasterNode extends Thread {

    public static final int NETWORK_BLOCK_SIZE = 8192;
    private Main main;
    private MasterNodeListener listener;
    private Selector selector;
    private ConcurrentLinkedQueue<SocketChannel> channelQueue;
    private HashMap<String, ConnectionContext> connections;
    private ByteBuffer buffer;
    private int readBytes;
    private HashMap<String, AbstractJob> jobs;
    private Queue<AbstractJob> jobQueue;
    private int connectionCounter = 0;
    private boolean keepWorking = true;

    /**
     * Creates new instance of MasterNode class.
     *
     * TODO: Alot of work left on this one!
     *
     * @param main
     */
    public MasterNode(Main main) {

        this.main = main; //used for getting DataProcesses from BackStorage

        try {
            selector = Selector.open();
            listener = new MasterNodeListener(this);
            channelQueue = new ConcurrentLinkedQueue<SocketChannel>();
            connections = new HashMap<String, ConnectionContext>(1024);
            jobs = new HashMap<String, AbstractJob>(1024);
            jobQueue = new LinkedList<AbstractJob>();
        } catch (IOException e) {
            LogTool.log(e, LogTool.CRITICAL);
        }
    }

    /**
     * Initiate threads related to this object
     */
    public void startMasterServer() {
        listener.start();
        this.start();
    }

    @Override
    public void run() {

        //The bytebuffer here is used in multiple ways. Even though it is
        //quite big, it can be used for small amounts of data by setting
        //the limit to appropriate sizes.
        buffer = ByteBuffer.allocateDirect(NETWORK_BLOCK_SIZE);
        int readyChannels = 0;

        while (keepWorking) {
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
                    SelectionKey selKey = (SelectionKey) it.next();

                    //Remove key, otherwise it will stay in the list forever.
                    it.remove();

                    try {
                        processSelectionKey(selKey);
                    } catch (IOException e) {
                        //Remove the channel from this selector
                        selKey.cancel();
                        connections.remove(selKey.attachment().toString());
                        LogTool.log(e, LogTool.WARNING);
                    }
                }
            }

            try {
                if (!jobQueue.isEmpty()) {
                    AbstractJob queuedJob = jobQueue.poll();
                    if (!queuedJob.writeOperation(connections.get(queuedJob.getFromConnection()).channel, buffer)) {
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

            //After checking all keys we check if there are any channels waiting
            //to be registered with this selector. Creates a new ID for each
            //connection and stores the id as an attachment while a 
            //connectionContext is stored in a hashmap.
            if (!channelQueue.isEmpty()) {
                try {
                    SocketChannel temp = channelQueue.poll();

                    //Get a new ID for this connection
                    String newID = HexConverter.toHex(IntConverter.intToByteArray(getConnectionCounterValue()));

                    //Register this socketChannel, use ID as attachment
                    temp.register(selector, SelectionKey.OP_READ, newID);

                    //Add new client to our map
                    connections.put(newID, new ConnectionContext(temp, newID));

                    LogTool.log("Connection from " + temp.getRemoteAddress() + " was added to selector", LogTool.INFO);
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

    /**
     * Get action for key and do required operation.
     *
     * @param key
     * @throws IOException
     */
    public void processSelectionKey(SelectionKey key) throws IOException {
        if (key.isValid() && key.isReadable()) {
            SocketChannel sChannel = (SocketChannel) key.channel();
            ConnectionContext context = connections.get(key.attachment().toString());

            if(context == null){
                System.out.println("No attachment @ 0x" + key.attachment().toString());
            }
            
            if (context.isClient()) {
                if(context.jobID == null)
                    buffer.limit(1);
                else
                    buffer.limit(8180); //8192 - 12
            }

//            while (buffer.hasRemaining()) {
//                try {
//                    if ((readBytes = sChannel.read(buffer)) == -1) {
//                        throw new IOException();
//                    }
//                } catch (IOException e) {
//                    connections.remove(key.attachment().toString());
//                    key.cancel();
//                    LogTool.log("Connection from " + sChannel.getRemoteAddress() + " was closed", LogTool.INFO);
//                    if (buffer.position() == 0) { //If there was no data
//                        buffer.clear();
//                        return;
//                    } else {
//                        break;
//                    }
//                }
//            }
            boolean connectionStillOpen = readToBuffer(buffer, sChannel, key);
            if(!connectionStillOpen)
                return;
            buffer.rewind();
            
            ClusterMessageVariables cmv = null;

            if (context.isClient()) {
                cmv = new ClusterMessageVariables();
                //cmv.setFrom(key.attachment().toString());
                cmv.setFrom(context.node.getId());
                
                if(context.jobID == null){
                    Protocol command = Protocol.getCommand(buffer.get());
                    switch(command){
                        case CONNECT:
                            MasterConnectJob mcj = new MasterConnectJob(cmv.getFrom(), this, key);
                            jobs.put(mcj.getJobID(), mcj);
                            context.setJobID(mcj.getJobID());
                            buffer.clear();
                            return;
                        case PUT:
                            if(Main.slaveList.hasActiveSlaves()){
                                System.out.println("Active slave is: " + Main.slaveList.getActiveSlaveID());
                                
                                //Since we know it is put, we are expecting more data, so fill
                                //up the buffer to full. If no data is received, return.
                                buffer.limit(8180);
                                if(!readToBuffer(buffer, sChannel, key)){
                                    return;
                                }
                                
                                RoutePutJob rpj = new RoutePutJob(Main.slaveList.getActiveSlaveID(), key.attachment().toString(), this);
//                                RouteJob rj = new RouteJob(Main.slaveList.getActiveSlaveID(), key.attachment().toString(), this, command);
//                                rj.setRemoteJobID(rj.getJobID());
                                context.setJobID(rpj.getJobID());
                                jobs.put(rpj.getJobID(), rpj);
                                cmv.setJobID(rpj.getJobID());
                                
                            }else{
                                System.out.println("No active slave");
                                
                                //Since we know it is put, we are expecting more data, so fill
                                //up the buffer to full. If no data is received, return.
                                buffer.limit(8180);
                                if(!readToBuffer(buffer, sChannel, key)){
                                    return;
                                }
                                
                                PutJob pj = new PutJob(key.attachment().toString(), main.getNewDataProcessor());
                                jobs.put(pj.getJobID(), pj);
                                context.setJobID(pj.getJobID());
                                cmv.setJobID(pj.getJobID());
                            }
                            buffer.rewind();
                            break;
                        default:
                            //Y U NO Specify command?
                            return;
                    }
                }else{
                    cmv.setLength(buffer.limit());
                    cmv.setJobID(context.jobID);
                }
            } else {

                cmv = new ClusterMessageVariables(buffer);
                
                //Update key.attachment() if the ID have changed. (Should happen
                //if a new slave has connected).
                if(!key.attachment().toString().equals(cmv.getFrom())){
                    key.attach(cmv.getFrom());
                }

                if (!jobs.containsKey(cmv.getJobID()) || cmv.getJobID().equals("00000000")) {
                    Protocol command = Protocol.getCommand(buffer.get());

                    switch (command) {
                        case CONNECT:
                            MasterConnectJob mcj = new MasterConnectJob(cmv.getJobID(), cmv.getFrom(), key.attachment().toString(), this);
                            jobs.put(mcj.getJobID(), mcj);
                            //                        jobQueue.add(mcj);
                            break;
                        case SYNC_STATE:
                            SyncStateJob ssj = new SyncStateJob(cmv.getJobID(), cmv.getFrom());
                            jobs.put(ssj.getJobID(), ssj);
                            break;
                        case SYNC_LOCAL_INDEX:
                            System.out.println("Sync local index acknowledged");
                            SyncLocalIndexJob slij = new SyncLocalIndexJob(cmv.getJobID(), context.node);
                            jobs.put(slij.getJobID(), slij);
                            break;
                        case PUT:
                            //Put file
                            break;
                        case GET:

                            break;
                        case PING:

                            break;
                    }
                }
            }
            
            //Perform the job
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
                context.setJobID(null);
            }
            buffer.clear();
        }
    }
    
    private boolean readToBuffer(ByteBuffer buffer, SocketChannel sc, SelectionKey key) throws IOException{
        while (buffer.hasRemaining()) {
            try {
                if ((readBytes = sc.read(buffer)) == -1) {
                    throw new IOException();
                }
            } catch (IOException e) {
                connections.remove(key.attachment().toString());
                key.cancel();
                LogTool.log("Connection from " + sc.getRemoteAddress() + " was closed", LogTool.INFO);
                if (buffer.position() == 0) { //If there was no data
                    buffer.clear();
                    return false;
                }else{
                    break;
                }
            }
        }
        return true;
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

    /**
     * Adds a SocketChannel to a queue which eventually will be registered to a
     * selector.
     *
     * @param channel
     */
    public void addSocketChannel(SocketChannel channel) {
        this.channelQueue.add(channel);
        selector.wakeup();
    }

    /**
     * Get the selector from this class.
     *
     * @deprecated
     * @return
     */
    public Selector getSelector() {
        return selector;
    }

    public void switchConnectionID(String oldID, String newID){
        ConnectionContext cc = connections.get(oldID);
        connections.remove(oldID);
        cc.getNode().setId(newID);
        connections.put(newID, cc);
    }
 
    public void close() throws IOException {
        listener.close();
        keepWorking = false;
        selector.wakeup();
    }

    public HashMap<String, ConnectionContext> getConnections() {
        return connections;
    }
    
    public HashMap<String, AbstractJob> getJobs(){
        return jobs;
    }

    private SelectionKey keyWantsToWrite(SelectionKey key) {
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        return key;
    }

    private int getConnectionCounterValue() {
        if (connectionCounter == Integer.MAX_VALUE / 2) {
            connectionCounter = 0;
        }
        return ++connectionCounter;
    }

    //For test purposes
    public static void main(String[] args) {
        MasterNode mn = new MasterNode(null);
        mn.startMasterServer();
    }
}
