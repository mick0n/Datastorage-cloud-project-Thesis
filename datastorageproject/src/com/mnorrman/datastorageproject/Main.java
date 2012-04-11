/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

import com.mnorrman.datastorageproject.storage.BackStorage;
import com.mnorrman.datastorageproject.storage.DataProcessor;
import com.mnorrman.datastorageproject.index.LocalIndex;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mikael
 */
public class Main {

    public static LocalIndex localIndex;
    public static ExecutorService pool;
    public static Timer timer;
    public static PropertiesManager properties;
    public BackStorage storage;
    
    public Main(){
        try{
            storage = new BackStorage().initialize();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
    public DataProcessor getNewDataProcessor(){
        return new DataProcessor(storage.getChannel());
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SingleInstanceThread sit = new SingleInstanceThread();
        if(!sit.tryInstance()){
            System.out.println("Other instance found.");
            System.exit(0);
        }
        
        try{
            Logger.getLogger("b-log").addHandler(new FileHandler("log.txt", true));
        }catch(IOException e){
            e.printStackTrace();
        }
        
        properties = new PropertiesManager();
        
        pool = Executors.newFixedThreadPool(5, Executors.defaultThreadFactory());
        timer = new Timer("TimerThread");
        
        if(properties.getValue("master").toString().equalsIgnoreCase("127.0.0.1")){
            //Start global index
        }
        
        localIndex = new LocalIndex();
        Main m = new Main();
        ConsoleInputManager console = new ConsoleInputManager(m);
        console.start();
    }
    
    private static class SingleInstanceThread extends Thread{
        
        private ServerSocket listener;
        
        public SingleInstanceThread(){
        }
        
        public boolean tryInstance(){
            try{
                listener = new ServerSocket(65533, 0, InetAddress.getByName(null));
                start();
                return true;
            }catch(IOException e){
                //Unable to connect, no need to tell the world.
            }
            return false;
        }

        @Override
        public void run() {
            try{
                while(true){
                    listener.accept();
                }
            }catch(IOException e){
                //Nothing to do here
            }
        }
    }
}
