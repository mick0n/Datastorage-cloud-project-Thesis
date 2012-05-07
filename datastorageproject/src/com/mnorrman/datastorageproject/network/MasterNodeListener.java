
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Mikael Norrman
 */
public class MasterNodeListener extends Thread{
    
    private ServerSocketChannel ssc;
    private MasterNode owner;
    
    /**
     * Creates new instance of MasterNodeListener
     * @param mn The MasterNode to which this listener belongs.
     */
    public MasterNodeListener(MasterNode mn){
        this.owner = mn;
    }

    @Override
    public void run() {
        try{           
            ssc = ServerSocketChannel.open();
            ssc.configureBlocking(true);
            ssc.socket().bind(new InetSocketAddress(InetAddress.getByName(""), Integer.parseInt(Main.properties.getValue("port").toString())));
//             ssc.socket().bind(new InetSocketAddress(InetAddress.getByName(""), 9999));
            
            LogTool.log("MasterNodeListener started listening", LogTool.INFO);
            
            while(ssc.isOpen()){
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(false);
                owner.addSocketChannel(sc);
            }
        }catch(IOException e){
            LogTool.log(e, LogTool.CRITICAL);
        }
    }
    
    /**
     * Close this ServerSocketChannel
     */
    public void close() throws IOException{
        ssc.close();
    }
}
