package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.ServerState;
import com.mnorrman.datastorageproject.network.jobs.AbstractJob;
import com.mnorrman.datastorageproject.network.jobs.MasterConnectJob;
import com.mnorrman.datastorageproject.network.jobs.SyncLocalIndexJob;
import com.mnorrman.datastorageproject.network.jobs.SyncStateJob;
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
import java.util.PriorityQueue;
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
            connections = new HashMap<String, ConnectionContext>();
            jobs = new HashMap<String, AbstractJob>();
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
                    if (!queuedJob.writeOperation(connections.get(queuedJob.getOwner()).channel, buffer)) {
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
            
            while(buffer.position() != buffer.capacity()){
                if((readBytes = sChannel.read(buffer)) == -1){
                    //Connection has been terminated, cleanup.
                    connections.remove(key.attachment().toString());
                    key.cancel();
                    LogTool.log("Connection from " + sChannel.getRemoteAddress() + " was closed", LogTool.INFO);
                    buffer.clear();
                    return;
                }
            }            
            
            buffer.flip();
            
            //Get the sender ID
            byte[] fromBytes = new byte[4];
            buffer.get(fromBytes);
            String from = HexConverter.toHex(fromBytes);
            
            //Get the length of the data
            int length = buffer.getInt();

            //Get the jobID and transform into hexstring.
            byte[] jobIDBytes = new byte[4];
            buffer.get(jobIDBytes);
            String jobID = HexConverter.toHex(jobIDBytes);
            
            if (!jobs.containsKey(jobID) || jobID.equals("00000000")) {
                Protocol command = Protocol.getCommand(buffer.get());

                switch (command) {
                    case CONNECT:
                        MasterConnectJob mcj = new MasterConnectJob(jobID, from, key.attachment().toString(), this);
                        jobs.put(mcj.getJobID(), mcj);
//                        jobQueue.add(mcj);
                        break;
                    case SYNC_STATE:
                        SyncStateJob ssj = new SyncStateJob(jobID, from);
                        jobs.put(ssj.getJobID(), ssj);
                        break;
                    case SYNC_LOCAL_INDEX:
                        System.out.println("Sync local index acknowledged");
                        SyncLocalIndexJob slij = new SyncLocalIndexJob(jobID, context.node);
                        jobs.put(slij.getJobID(), slij);
                        break;
                    case GET:

                        break;
                    case PING:

                        break;
                }
            }
            
            //Perform the job
            AbstractJob job = jobs.get(jobID);
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
     * Adds a SocketChannel to a queue which eventually will be registered to
     * a selector.
     * @param channel 
     */
    public void addSocketChannel(SocketChannel channel) {
        this.channelQueue.add(channel);
        selector.wakeup();
    }

    /**
     * Get the selector from this class.
     * @deprecated 
     * @return 
     */
    public Selector getSelector() {
        return selector;
    }

    public void close() throws IOException{
        listener.close();
        keepWorking = false;
        selector.wakeup();
    }

    public HashMap<String, ConnectionContext> getConnections() {
        return connections;
    }
    
    private SelectionKey keyWantsToWrite(SelectionKey key){
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        return key;
    }
    
    private int getConnectionCounterValue(){
        if(connectionCounter == Integer.MAX_VALUE/2){
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
