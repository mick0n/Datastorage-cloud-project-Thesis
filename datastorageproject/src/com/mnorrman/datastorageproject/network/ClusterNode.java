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
 * @author Mikael Norrman
 */
public class ClusterNode extends Thread {

    public static final int MAXIMUM_NETWORK_BLOCK_SIZE = 131072;
    private final boolean master;
    private Main main;
    private DualClusterListener listener;
    private Selector selector;
    private TreeNode root;
    private ConcurrentLinkedQueue<Pair<SocketChannel, Boolean>> channelQueue;
    private HashMap<String, ConnectionContext> clientConn;
    private HashMap<String, ConnectionContext> childConn;
    private ByteBuffer buffer;
    
    private HashMap<String, AbstractJob> jobs;
    private Queue<AbstractJob> jobQueue;
    
    private int readBytes;
    private int connectionCounter = 0;
    private boolean keepWorking = true;

    /**
     * Creates new instance of MasterNode class.
     *
     * TODO: Alot of work left on this one!
     *
     * @param main
     */
    public ClusterNode(Main main) {

        if (Main.properties.getValue("master").toString().equals("127.0.0.1")) {
            master = true;
        } else {
            master = false;
        }

        this.main = main; //used for getting DataProcesses from BackStorage
//        this.root = new TreeNode();

        try {
            selector = Selector.open();
//            listener = new DualClusterListener(this);
            channelQueue = new ConcurrentLinkedQueue<Pair<SocketChannel, Boolean>>();
            clientConn = new HashMap<String, ConnectionContext>(1033);
            childConn = new HashMap<String, ConnectionContext>();
            jobs = new HashMap<String, AbstractJob>(1033);
            jobQueue = new LinkedList<AbstractJob>();
        } catch (IOException e) {
            LogTool.log(e, LogTool.CRITICAL);
        }
    }

    /**
     * Initiate threads related to this object
     */
    public void startClusterServer() {
        listener.start();
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
            if (!Main.properties.getValue("master").toString().equals("127.0.0.1")) {
                try {
                    if(!childConn.containsKey("00000000")){
                        childConn.put("00000000", new ConnectionContext(SocketChannel.open()));
                        childConn.get("00000000").channel.configureBlocking(false);
                    }
                    if (!childConn.get("00000000").channel.isConnected()) {
                        Main.state = ServerState.CONNECTING;
                        //childConn.get("00000000").channel.connect(new InetSocketAddress(InetAddress.getByName(Main.properties.getValue("master").toString()), Integer.parseInt(Main.properties.getValue("internalport").toString())));
                        childConn.get("00000000").channel.connect(new InetSocketAddress(InetAddress.getByName(Main.properties.getValue("master").toString()), 8989));
                        while (!childConn.get("00000000").channel.finishConnect()) {
                            LogTool.log("Could not connect to master, trying again in 2 seconds", LogTool.WARNING);
                            try {
                                sleep(2000);
                            } catch (InterruptedException e) {
                                LogTool.log(e, LogTool.CRITICAL);
                            }
                        }
                        childConn.get("00000000").channel.register(selector, SelectionKey.OP_READ, new Pair<Boolean, String>(false, "00000000"));
//                        ConnectToMasterJob ctmj = new ConnectToMasterJob("00000000", this);
//                        jobs.put(ctmj.getJobID(), ctmj);
//                        jobQueue.add(ctmj);
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

                    //Remove later
                    ClusterMessageVariables cmv = null;

                    try {
                        if (key.isValid() && key.isReadable()) {
                            if ((Boolean) ((Pair) key.attachment()).key) {
                                System.out.println("Read client");
                                processClient(cmv, key);
                            } else {
                                System.out.println("Read child");
                                processChild(key);
                            }
//                            ConnectionContext context = childConn.get(key.attachment().toString());
//                            System.out.println("Read: " + context.toString());
//                            if(context.isClient()){
//                                
//                            }else{
//                                
//                            }

//                            //Perform the job
//                            AbstractJob job = jobs.get(cmv.getJobID());
//                            try {
//                                if (job.readOperation(buffer)) {
//                                    //If the readOperation returns true it
//                                    //means it has something to write.
//                                    jobQueue.offer(job);
//                                }
//                            } catch (IOException e) {
//                                LogTool.log(e, LogTool.CRITICAL);
//                            }
//                            if (job.isFinished()) {
//                                jobs.remove(job.getJobID());
//                                childConn.get(key.attachment().toString()).setJobID(null);
//                            }
//                            buffer.clear();
                        }
                    } catch (IOException e) {
                        //Remove the channel from this selector
                        key.cancel();
                        childConn.remove(key.attachment().toString());
                        LogTool.log(e, LogTool.WARNING);
                    }
                }
            }

            //Perform a write operation, if any
            try {
                if (!jobQueue.isEmpty()) {
                    AbstractJob queuedJob = jobQueue.poll();
                    SocketChannel channel = null;
//                    System.out.println("fromcon: 0x" + queuedJob.getFromConnection());
//                    if (clientConn.containsKey(queuedJob.getFromConnection())) {
//                        channel = clientConn.get(queuedJob.getFromConnection()).channel;
//                    } else if (childConn.containsKey(queuedJob.getFromConnection())) {
//                        channel = childConn.get(queuedJob.getFromConnection()).channel;
//                    }

                    System.out.println("Channel = " + channel);

                    if (channel != null && !queuedJob.writeOperation(channel, buffer)) {
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
                    Pair<SocketChannel, Boolean> temp = channelQueue.poll();

                    if (temp.value) {
                        String newID = HexConverter.toHex(getConnectionCounterValue());
                        temp.key.register(selector, SelectionKey.OP_READ, new Pair<Boolean, String>(true, newID));
                        clientConn.put(newID, new ConnectionContext(temp.key, newID));

                        //Add a child connection
                    } else {
                        String temporaryChildID = HexConverter.toHex((short) new Random().nextInt());
                        temp.key.register(selector, SelectionKey.OP_READ, new Pair<Boolean, String>(false, temporaryChildID));
                        childConn.put(temporaryChildID, new ConnectionContext(temp.key));
                    }
                    LogTool.log("Connection from " + temp.key.getRemoteAddress() + " was added to selector", LogTool.INFO);
                } catch (NullPointerException e) {
                    LogTool.log(e, LogTool.CRITICAL);
                } catch (ClosedChannelException e) {
                    LogTool.log(e, LogTool.CRITICAL);
                } catch (IOException e) {
                    LogTool.log(e, LogTool.CRITICAL);
                }
            }


            //Reconfigure maximum size of buffer
            int a = (int) Math.floor(Math.log(childConn.size()) / Math.log(2));
            int b = (int) Math.floor(Math.log(jobQueue.size()) / Math.log(2));
            int c = (int) Math.floor(Math.log(jobs.size()) / Math.log(2));
            int medium = (int) Math.floor((a + b + c) / 3);
            if (medium > 0) {
                buffer.limit(MAXIMUM_NETWORK_BLOCK_SIZE / medium);
            } else {
                buffer.limit(MAXIMUM_NETWORK_BLOCK_SIZE);
            }
            System.out.println("New buffer limit = " + buffer.limit());
        }
    }

    /**
     * Process input that is supposedly from a client. The only case when a
     * child gets here is when it connects.
     *
     * @param cmv ClusterMessageVariables-object. Should always be null at
     * arrival.
     * @param context The ConnectionContext belonging to this connection.
     * @param key The key currently being processed.
     * @throws IOException
     */
    private void processClient(ClusterMessageVariables cmv, SelectionKey key) throws IOException {
        SocketChannel sChannel = (SocketChannel) key.channel();
        String clientID = (String) ((Pair) key.attachment()).value;
        ConnectionContext context = clientConn.get(clientID);

        if (context == null) {
            System.out.println("No attachment @ 0x" + clientID);
        }

        if (context.jobID == null) {
            buffer.limit(1);
        } else {
            buffer.limit(8180); //8192 - 12
        }
        boolean connectionStillOpen = readToBuffer(buffer, sChannel, key);
        if (!connectionStillOpen) {
            return;
        }

        buffer.rewind();

        cmv = new ClusterMessageVariables();
        cmv.setFrom(key.attachment().toString());

        if (context.jobID == null) {
            Protocol command = Protocol.getCommand(buffer.get());
            switch (command) {
                case PUT:

                    //Since we know it is put, we are expecting more data, so fill
                    //up the buffer to full. If no data is received, return.
                    buffer.limit(8180);
                    if (!readToBuffer(buffer, sChannel, key)) {
                        return;
                    }

                    PutJob pj = new PutJob(key.attachment().toString(), main.getNewDataProcessor());
                    jobs.put(pj.getJobID(), pj);
                    context.setJobID(pj.getJobID());
                    cmv.setJobID(pj.getJobID());

//                    buffer.rewind();
                    break;
                default:
                //Y U NO Specify command?
            }
        } else {
            cmv.setLength(buffer.limit());
            cmv.setJobID(context.jobID);
        }
    }

    /**
     * Process input that is confirmed to come from a child.
     *
     * @param cmv ClusterMessageVariables-object. Should always be null at
     * arrival.
     * @param context The ConnectionContext belonging to this connection.
     * @param key The key currently being processed.
     * @throws IOException
     */
    private void processChild(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        String childID = (String) ((Pair) key.attachment()).value;
        ConnectionContext context = childConn.get(childID);

        ClusterMessageVariables cmv = new ClusterMessageVariables();

        buffer.limit(8);
        if(buffer.position() != 0)
            buffer.rewind();
        
        while (buffer.hasRemaining()) {
            try {
                if ((readBytes = sc.read(buffer)) == -1) {
                    childConn.remove(key.attachment().toString());
                    key.cancel();
                    LogTool.log("Connection from " + sc.getRemoteAddress() + " was closed", LogTool.INFO);
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
                childConn.remove(key.attachment().toString());
                key.cancel();
                LogTool.log("Connection from " + sc.getRemoteAddress() + " was closed", LogTool.INFO);
                return;
            }
        }
        buffer.flip();
        cmv = new ClusterMessageVariables(buffer);

        if (cmv.getJobID().equals("00000000") || !jobs.containsKey(cmv.getJobID())) {
            Protocol command = Protocol.getCommand(buffer.get());

            switch (command) {
                case CONNECT:
//                    MasterConnectJob mcj = new MasterConnectJob(cmv.getJobID(), (String)((Pair)key.attachment()).value, this, key);
//                    jobs.put(mcj.getJobID(), mcj);
//                    cmv.setJobID(mcj.getJobID());
                    //jobQueue.add(mcj);
                    break;
                case SYNC_STATE:
                    SyncStateJob ssj = new SyncStateJob(cmv.getJobID(), cmv.getFrom());
                    jobs.put(ssj.getJobID(), ssj);
                    cmv.setJobID(ssj.getJobID());
                    break;
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
        }
        buffer.clear();
    }

    private boolean readToBuffer(ByteBuffer buffer, SocketChannel sc, SelectionKey key) throws IOException {
        while (buffer.hasRemaining()) {
            try {
                if ((readBytes = sc.read(buffer)) == -1) {
                    throw new IOException();
                }
            } catch (IOException e) {
                childConn.remove(key.attachment().toString());
                key.cancel();
                LogTool.log("Connection from " + sc.getRemoteAddress() + " was closed", LogTool.INFO);
                if (buffer.position() == 0) { //If there was no data
                    buffer.clear();
                    return false;
                } else {
                    break;
                }
            }
        }
        return true;
    }

    public void createJob(String jobID, AbstractJob job) {
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
        this.channelQueue.add(new Pair<SocketChannel, Boolean>(channel, false));
        selector.wakeup();
    }

    public synchronized void addSocketChannel(SocketChannel channel, boolean isClient) {
        this.channelQueue.add(new Pair<SocketChannel, Boolean>(channel, isClient));
        selector.wakeup();
    }

    public synchronized void addMasterSocketChannel(SocketChannel channel) {
        this.channelQueue.add(new Pair<SocketChannel, Boolean>(channel, null));
        selector.wakeup();
    }

    public void switchConnectionID(String oldID, String newID) {
        ConnectionContext cc = childConn.get(oldID);
        cc.setServerNode(new ServerNode(cc.channel.socket().getInetAddress(), cc.channel.socket().getLocalPort(), newID));
        childConn.remove(oldID);
        childConn.put(newID, cc);
    }

    public void close() throws IOException {
        listener.close();
        keepWorking = false;
        selector.wakeup();
    }

    public HashMap<String, ConnectionContext> getConnections() {
        return childConn;
    }

    public HashMap<String, AbstractJob> getJobs() {
        return jobs;
    }

    private int getConnectionCounterValue() {
        if (connectionCounter == Integer.MAX_VALUE / 2) {
            connectionCounter = 0;
        }
        return ++connectionCounter;
    }
}
