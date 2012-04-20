/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.storage.BackStorage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.AbstractDocument;

/**
 *
 * @author Mikael
 */
public class MasterNode extends Thread{
    
    private MasterNodeListener mnl;
    private Selector selector;
    private ConcurrentLinkedQueue<SocketChannel> channelQueue;
    
    
    private ByteBuffer commandBuffer;
    private ByteBuffer largeBuffer;
    private int readBytes, writtenBytes;
    
    public MasterNode() {
        try{
            selector = Selector.open();
            mnl = new MasterNodeListener(this);
            channelQueue = new ConcurrentLinkedQueue<SocketChannel>();
        }catch(IOException e){
            System.out.println("Error");
            Logger.getLogger("b-log").log(Level.SEVERE, "Error creating the MasterNode selector", e);
        }
    }
    
    public void startMasterServer(){
        mnl.start();
        this.start();
    }

    @Override
    public void run() {
        
        commandBuffer = ByteBuffer.allocateDirect(1024);
        largeBuffer = ByteBuffer.allocateDirect(BackStorage.BlOCK_SIZE);
        int readyChannels;
        
        System.out.println("Starting to select");
        while(true){
            try {
                readyChannels = selector.select();                

            } catch (IOException e) {
                Logger.getLogger("b-log").log(Level.SEVERE, "Error handling event in MasterNode selector", e);
                break;
            }
            
            if(readyChannels > 0){

                Iterator it = selector.selectedKeys().iterator();
                //System.out.println("Number of selected keys: " + selector.selectedKeys().size());
                while (it.hasNext()) {
                    SelectionKey selKey = (SelectionKey)it.next();

                    it.remove();

                    try {
                        processSelectionKey(selKey);
                    } catch (IOException e) {
                        // remove key
                        selKey.cancel();
                    }
                }

            }
            
            if(!channelQueue.isEmpty()){
                try{
                    SocketChannel temp = channelQueue.poll();
                    temp.register(selector, temp.validOps(), new ConnectionContext(Protocol.NULL));
                }catch(NullPointerException e){
                    Logger.getLogger("b-log").log(Level.SEVERE, "Error when reading socketchannel from queue", e);
                }catch(ClosedChannelException e){
                    Logger.getLogger("b-log").log(Level.SEVERE, "Error because of already closed channel", e);
                }
            }
        }
    }   
    
    public void processSelectionKey(SelectionKey key) throws IOException {
        if (key.isValid() && key.isConnectable()) {           
            SocketChannel sChannel = (SocketChannel)key.channel();
            
            System.out.println("Connection event for " + sChannel.getRemoteAddress() + " #1");

            boolean success = sChannel.finishConnect();
            if (!success) {
                // An error occurred; handle it

                // Unregister the channel with this selector
                key.cancel();
            }
        }
        else if (key.isValid() && key.isReadable()) {
            // Get channel with bytes to read
            SocketChannel sChannel = (SocketChannel)key.channel();
            
            System.out.println("Reading event for " + sChannel.getRemoteAddress() + " #2");
            
            if(key.attachment() == null){
                readBytes = sChannel.read(commandBuffer);
                if(readBytes > 0){
                    commandBuffer.flip();
                    ((ConnectionContext)key.attachment()).command = Protocol.PING;
                    System.out.println("Attached value");
                }
            }else{
                readBytes = sChannel.read(commandBuffer);
            }
            
            if(readBytes == -1){
                key.cancel();
                System.out.println("Ended channel");
            }
            
            commandBuffer.clear();
        }
        else if (key.isValid() && key.isWritable()) {
            
            // Get channel that's ready for more bytes
            SocketChannel sChannel = (SocketChannel)key.channel();
            //System.out.println("Writing event for " + sChannel.getRemoteAddress() + " #3");
            
            
            if(key.attachment() != null){
                switch(((ConnectionContext)key.attachment()).command){
                    case PING:
                        largeBuffer.put((byte)0x01);
                        largeBuffer.flip();
                        writtenBytes = sChannel.write(largeBuffer);
                        largeBuffer.clear();
                        key.attach(null);
                        break;
                    default:
                        System.err.println("Nothing to do here");
                }
                
                System.out.println("Written bytes= " + writtenBytes);
            }
            
            
            // See Writing to a SocketChannel
        }
    }
    
    public void addSocketChannel(SocketChannel channel){
        this.channelQueue.add(channel);
        System.out.println("Call wakeup");
        selector.wakeup();
        System.out.println("Should wake up");
    }
    
    public Selector getSelector(){
        return selector;
    }
    
    
    
    public static void main(String[] args) {
        MasterNode mn = new MasterNode();
        mn.startMasterServer();
    }
    
}
