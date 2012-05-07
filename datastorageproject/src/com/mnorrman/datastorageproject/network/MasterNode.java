package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.network.jobs.ConnectJob;
import com.mnorrman.datastorageproject.storage.BackStorage;
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
        buffer = ByteBuffer.allocateDirect(BackStorage.BlOCK_SIZE);
        int readyChannels;

        while (keepWorking) {
            try {
                //Block until theres a minimum of one channel with a ready
                //action
                readyChannels = selector.select();
            } catch (IOException e) {
                LogTool.log(e, LogTool.CRITICAL);
                break;
            }
            
            //Since readyChannels may be zero, we check this first
            if (readyChannels > 0) {

                Iterator it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey selKey = (SelectionKey) it.next();

                    //Remove key, otherwise it will stay in the list forever.
                    it.remove();
                    
                    //Remove the interest key for write.
                    selKey.interestOps(selKey.interestOps() & ~SelectionKey.OP_WRITE);

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

            //After checking all keys we see if there are any channels waiting
            //to be registered with this selector. Always adds a ConnectionContext
            //to a connection.
            if (!channelQueue.isEmpty()) {
                try {
                    SocketChannel temp = channelQueue.poll();
                    
                    //Get a new ID for this connection
                    byte[] newID = IntConverter.intToByteArray(getConnectionCounterValue());
                    
                    //Register this socketChannel, use ID as attachment
                    temp.register(selector, SelectionKey.OP_READ, HexConverter.toHex(newID));
                    
                    //Add new client to our map
                    connections.put(HexConverter.toHex(newID), new ConnectionContext(temp, Protocol.NULL, newID));
                    
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
            
            if(context.command == Protocol.NULL){
                //Set limit to 1 since we want to read a command first.
                    buffer.limit(1);
                    readBytes = sChannel.read(buffer);
                    if(readBytes == -1){
                        buffer.clear();
                    }else{
                        if (readBytes > 0) {
                            buffer.flip();

                            //Set new command in ConnectionContext
                            context.setCommand(Protocol.getCommand(buffer.get()));
                        }
                        buffer.clear();
                    }
            }
            
            //If we read -1 bytes then the connection is dead
            if (readBytes == -1) {
                connections.remove(key.attachment().toString());
                key.cancel();
                LogTool.log("Connection from " + sChannel.getRemoteAddress() + " was closed", LogTool.INFO);
            }
            
            //Process command
            switch (context.command) {
                case CONNECT:
                    //We should tell the slave of his ID, therefore we set a
                    //connectJob.
                    System.out.println("Connection");
                    context.setTask(new ConnectJob());
                    keyWantsToWrite(key);
                    break;
                    
                case GET:
                    //Perform get
                    break;
                
                case PING:
                    //No job for this, we just know that we should reply later
                    keyWantsToWrite(key);
                    break;
                default:
                    //I don't always go to default in a switch, but when I do
                    //I do it eternally. 
            }
        } else if (key.isValid() && key.isWritable()) {
            SocketChannel sChannel = (SocketChannel) key.channel();
            ConnectionContext context = connections.get(key.attachment().toString());

            if (key.attachment() != null) { //Probably unnecessary statement 
                switch (context.command) {
                    case GET:
                        break;
                    case CONNECT:
                        if(context.task instanceof ConnectJob && !context.task.isFinished()){
                            buffer.putInt(0x00000000);
                            buffer.put(context.node.getId());
                            buffer.flip();
                            sChannel.write(buffer);
                            buffer.clear();
                            context.setCommand(Protocol.NULL);
                            context.task = null;
                        }
                        break;
                    case PING:
                        buffer.putInt(0x00000000);
                        buffer.put((byte) 0x01);
                        buffer.flip();
                        buffer.clear();
                        context.setCommand(Protocol.NULL);
                        break;

                    case NULL:
                    default:
                        //I don't always go to default in a switch, but when I do
                        //I do it eternally.
                }

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
        if(connectionCounter == Integer.MAX_VALUE){
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
