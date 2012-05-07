/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.network.jobs.ConnectJob;
import com.mnorrman.datastorageproject.storage.BackStorage;
import com.mnorrman.datastorageproject.tools.HexConverter;
import com.mnorrman.datastorageproject.tools.IntConverter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 *
 * @author Mikael
 */
public class SlaveNode extends Thread{
    
    private Main main;
    private ConnectionContext context;
    private Selector selector;
    private ByteBuffer buffer;
    private int readBytes, writtenBytes;
    
    private boolean keepWorking = true;
    
    public SlaveNode(Main main){
        this.main = main;
        try{
            selector = Selector.open();
            context = new ConnectionContext(SocketChannel.open(), Protocol.NULL);
            context.channel.configureBlocking(false);
        }catch(IOException e){
            LogTool.log(e, LogTool.CRITICAL);
        }
    }
    
    public void startSlaveServer(){
        this.start();
    }

    @Override
    public void run() {
        
        //The bytebuffer here is used in multiple ways. Even though it is
        //quite big, it can be used for small amounts of data by setting
        //the limit to appropriate sizes.
        buffer = ByteBuffer.allocateDirect(BackStorage.BlOCK_SIZE);
        int readyChannels = 0;
        
        while(keepWorking){
            try{
                while(keepWorking && !context.channel.isConnected()){
                    context.channel.connect(new InetSocketAddress(InetAddress.getByName(Main.properties.getValue("master").toString()), Integer.parseInt(Main.properties.getValue("port").toString())));
                    if(!context.channel.finishConnect()){
                        System.out.println("Not working, sleeping for ten secs");
                        try{
                            sleep(10000);
                        }catch(InterruptedException e){
                            LogTool.log(e, LogTool.CRITICAL);
                        }
                    }else{
                        System.out.println("Everything went better then expected");
                        context.channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        context.setCommand(Protocol.CONNECT);
                        context.setTask(new ConnectJob());
                    }
                }
                
                
                readyChannels = selector.select();
                
                if(readyChannels <= 0){
                    System.out.println("hello");
                    continue;
                }
                
                Iterator it = selector.selectedKeys().iterator();
                
                while(it.hasNext()){
                    SelectionKey key = (SelectionKey)it.next();
                    it.remove();
                    
                    key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                    
                    //DETTA FUNKAR INTE SOM TÄNKT; WHY???
                    // Obtain the interest of the key
//                    int readyOps = key.readyOps();
                    // Disable the interest for the operation
                    // that is ready. This prevents the same 
                    // event from being raised multiple times.
//                    key.interestOps(key.interestOps() & ~readyOps);
                    
                    if(key.isValid()){
                        if(key.isReadable()){
                            //Read
                           switch(context.command){
                                case CONNECT:
                                    System.out.println("READ");
                                    if(((ConnectJob)context.task).getHaveSentCommand()){
                                        buffer.limit(8);
                                        context.channel.read(buffer);
                                        buffer.flip();
                                        System.out.println("From: 0x" + HexConverter.toHex(IntConverter.intToByteArray(buffer.getInt())));
                                        //HexConverter.toHex(buffer.array());
                                        System.out.println("My ID: 0x" + HexConverter.toHex(IntConverter.intToByteArray(buffer.getInt())));
                                        context.setCommand(Protocol.NULL);
                                        context.setTask(null);
                                    }
                                    break;
                                default:
                                    //Hello?! Is there anybody out there?!
                            }
                        }
                        if(key.isWritable()){
                            System.out.println("Will write once?");
                            switch(context.command){
                                case CONNECT:
                                    if(!((ConnectJob)context.task).getHaveSentCommand()){
                                        buffer.put(Protocol.CONNECT.getValue());
                                        buffer.flip();
                                        context.channel.write(buffer);
                                        buffer.clear();
                                        ((ConnectJob)context.task).setHaveSentCommand(true);
                                    }
                                    break;
                                default:
                                    //Hoho? Anybody here?
                            }
                        }
                    }
                    
                    selector.selectedKeys().clear();
                }
                
            }catch(IOException e){
                LogTool.log(e, LogTool.CRITICAL);
            }
            
        }
    }
    
    private SelectionKey keyWantsToWrite(SelectionKey key){
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        return key;
    }
    
    public void close(){
        
    }
    
    
}
