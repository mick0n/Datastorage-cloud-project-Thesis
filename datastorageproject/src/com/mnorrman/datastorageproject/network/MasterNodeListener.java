/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.Main;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mikael
 */
public class MasterNodeListener extends Thread{
    
    private ServerSocketChannel ssc;
    private MasterNode owner;
    
    public MasterNodeListener(MasterNode mn){
        this.owner = mn;
    }

    @Override
    public void run() {
        try{           
            ssc = ServerSocketChannel.open();
            ssc.configureBlocking(true);
            ssc.socket().bind(new InetSocketAddress(InetAddress.getByName(""), Integer.parseInt(Main.properties.getValue("port").toString())));
//            ssc.socket().bind(new InetSocketAddress(InetAddress.getByName(""), 8999));
            
            System.out.println("Starting to listen as master node");
            
            while(ssc.isOpen()){
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(false);
                owner.addSocketChannel(sc);
            }
            /*SocketChannel sc = ssc.accept();
            sc.finishConnect();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int readBytes = 0;
            while((readBytes = sc.read(buffer)) != -1){
                buffer.flip();
                System.out.println("value: " + new String(buffer.array()).trim());
                buffer.clear();
            }
            sc.close();
            */
            //ssc.close();
        }catch(IOException e){
            Logger.getLogger("b-log").log(Level.SEVERE, "Error in serversocketchannel", e);
        }
    }
    
    public void close(){
        try{
            ssc.close();
        }catch(IOException e){
            Logger.getLogger("b-log").log(Level.SEVERE, "Error when closing serversocketchannel", e);
        }
    }
    
}
