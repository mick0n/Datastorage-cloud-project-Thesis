package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.storage.BackStorage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Mikael Norrman
 */
public class MasterNode extends Thread {

    private Main main;
    private MasterNodeListener listener;
    private Selector selector;
    private ConcurrentLinkedQueue<SocketChannel> channelQueue;
    private ByteBuffer buffer;
    private int readBytes, writtenBytes;
    
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

                    try {
                        processSelectionKey(selKey);
                    } catch (IOException e) {
                        //Remove the channel from this selector
                        selKey.cancel();
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
                    temp.register(selector, temp.validOps(), new ConnectionContext(Protocol.NULL));
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
        if (key.isValid() && key.isConnectable()) {
            SocketChannel sChannel = (SocketChannel) key.channel();
            
            boolean success = sChannel.finishConnect();
            if (!success) {
                //An error occured, remove channel from selector
                key.cancel();
                LogTool.log("Connection from " + sChannel.getRemoteAddress() + " was removed", LogTool.INFO);
            }
        } else if (key.isValid() && key.isReadable()) {
            SocketChannel sChannel = (SocketChannel) key.channel();

            switch (((ConnectionContext) key.attachment()).command) {
                case NULL:
                    
                    //Set limit to 1 since we want to read a command first.
                    buffer.limit(1);
                    readBytes = sChannel.read(buffer);
                    if (readBytes > 0) {
                        buffer.flip();
                        
                        //Set new command in ConnectionContext
                        ((ConnectionContext) key.attachment()).setCommand(Protocol.getCommand(buffer.get()));
                    }
                    break;
                    
                case CONNECT:
                    System.out.println("We wish to connect");
                    break;
                    
                case GET:
                    //Perform get
                    break;

                default:
                    //I don't always go to default in a switch, but when I do
                    //I do it eternally. 
            }

            //If we read -1 bytes then the connection is dead
            if (readBytes == -1) {
                key.cancel();
                LogTool.log("Connection from " + sChannel.getRemoteAddress() + " was closed", LogTool.INFO);
            }

            //Always clean up, clear the buffer (This will also reset the limit
            //of the buffer)
            buffer.clear();
        } else if (key.isValid() && key.isWritable()) {
            SocketChannel sChannel = (SocketChannel) key.channel();

            if (key.attachment() != null) { //Probably unnecessary statement 
                switch (((ConnectionContext) key.attachment()).command) {
                    case GET:
                    case PING:
                        buffer.put((byte) 0x01);
                        buffer.flip();
                        writtenBytes = sChannel.write(buffer);
                        buffer.clear();
                        ((ConnectionContext) key.attachment()).setCommand(Protocol.NULL);
                        System.out.println("Written bytes= " + writtenBytes);
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
    
    //For test purposes
    public static void main(String[] args) {
        MasterNode mn = new MasterNode(null);
        mn.startMasterServer();
    }
}
