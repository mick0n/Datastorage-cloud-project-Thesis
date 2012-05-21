/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 *
 * @author Mikael
 */
public class DualClusterListener extends Thread{
    
    private InternalTrafficHandler ith;
    private ExternalTrafficHandler eth;
    private Selector selector;
    
    public DualClusterListener(InternalTrafficHandler ith, ExternalTrafficHandler eth){
        this.ith = ith;
        this.eth = eth;
        this.setDaemon(true);
    }

    @Override
    public void run() {
        try{
            selector = Selector.open();
            
            ServerSocketChannel clientServerSocket;
            ServerSocketChannel childServerSocket;
            
            clientServerSocket = ServerSocketChannel.open();
            clientServerSocket.configureBlocking(false);
            clientServerSocket.socket().bind(new InetSocketAddress(Integer.parseInt(Main.properties.getValue("externalport").toString())));
            
            childServerSocket = ServerSocketChannel.open();
            childServerSocket.configureBlocking(false);
            childServerSocket.socket().bind(new InetSocketAddress(Integer.parseInt(Main.properties.getValue("internalport").toString())));
            
            clientServerSocket.register(selector, SelectionKey.OP_ACCEPT);
            childServerSocket.register(selector, SelectionKey.OP_ACCEPT);
            
            while(selector.isOpen()){
                selector.select();
                
                if(!selector.isOpen())
                    break;
                
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                
                while(it.hasNext()){
                    SelectionKey key = it.next();
                    it.remove();
                    
                    if(key.isValid() && key.isAcceptable()){
                        System.out.println("Key is acceptable on port " + ((ServerSocketChannel)key.channel()).socket().getLocalPort());
                        ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
                        boolean isClient = ssc.socket().getLocalPort() == Integer.parseInt(Main.properties.getValue("externalport").toString());
                        
                        SocketChannel sc = ssc.accept();
                        System.out.println("Socketchannel is " + sc);
                        sc.configureBlocking(false);
                        if(isClient){
                            System.out.println("Adding client");
                            eth.addSocketChannel(sc);
                        }else{
                            System.out.println("Adding child");
                            ith.addSocketChannel(sc);
                        }
                    }
                }
            }
        }catch(IOException e){
            LogTool.log(e, LogTool.CRITICAL);
        }
    }
    
    /**
     * Close this ServerSocketChannel
     */
    public void close() throws IOException{
        selector.close();
    }
}
