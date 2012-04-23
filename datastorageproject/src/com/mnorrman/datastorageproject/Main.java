
package com.mnorrman.datastorageproject;

import com.mnorrman.datastorageproject.index.LocalIndex;
import com.mnorrman.datastorageproject.network.MasterNode;
import com.mnorrman.datastorageproject.storage.BackStorage;
import com.mnorrman.datastorageproject.storage.DataProcessor;
import com.mnorrman.datastorageproject.web.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Mikael Norrman
 */
public class Main {

    public static LocalIndex localIndex;
    public static ExecutorService pool;
    public static Timer timer;
    public static PropertiesManager properties;
    public BackStorage storage;
    public MasterNode masterNode;
    public WebServer webServer;

    /**
     * Create new instance of this class. This method initiates major 
     * components such as BackStorage, WebServer and Server nodes.
     */
    public Main() {
        try {
            //Initiate BackStorage
            storage = new BackStorage().initialize();
            
            //Initiate webserver and add necessary roles
            webServer = new WebServer(8429);
            webServer.addWebRole("/", new MainWebRole());
            webServer.addWebRole("/index", new PrintIndexWebRole());
            webServer.addWebRole("/get", new SimpleGetWebRole(this));
            webServer.addWebRole("/post", new SimplePostWebRole(this));
        } catch (IOException e) {
            LogTool.log(e, LogTool.CRITICAL);
        }

        if (properties.getValue("master").toString().equalsIgnoreCase("127.0.0.1")) {
            masterNode = new MasterNode(this);
            masterNode.startMasterServer();
        }

        //MasterNodeListener mdl = new MasterNodeListener();
    }

    /**
     * Fetches a new DataProcessor from the BackStorage.
     * @return new DataProcessor object.
     */
    public DataProcessor getNewDataProcessor() {
        return new DataProcessor(storage.getChannel());
    }

    /**
     * Main method of this application.
     * @param args the command line arguments, which are not used at the moment.
     */
    public static void main(String[] args) {
        //Check if there's already an instance of this software running.
        //Only one instance is allowed at any given time.
        SingleInstanceThread sit = new SingleInstanceThread();
        if (!sit.tryInstance()) {
            System.out.println("Other instance found.");
            System.exit(0);
        }

        LogTool.setLogLevel(LogTool.INFO);

        //Propertiesmanager handles information from "config_"-file.
        properties = new PropertiesManager();
        
        System.out.println("Prop: " + properties.getValue("dataPath"));

        //General purpose objects. 
        pool = Executors.newFixedThreadPool(5, Executors.defaultThreadFactory());
        timer = new Timer("TimerThread");

        //Initiate indexes. Check if this server is the master and if so, start
        //global index as well.
        if (properties.getValue("master").toString().equalsIgnoreCase("127.0.0.1")) {
            //Start global index
        }
        //always start localIndex.
        localIndex = new LocalIndex();
        
        /******Done initiating static variables*********/
        
        //Create new Instance
        Main m = new Main();
        
        //Add a consoleInputManager
        ConsoleInputManager console = new ConsoleInputManager(m);
        console.start();
    }

    /**
     * A class for detecting existing instances.
     * 
     * Uses a ServerSocket and tries to listen on port 65533.
     * If the ServerSocket initiates successfully then everything is fine, but
     * if it can't listen on the port then there is another instance and the
     * main class is notified.
     */
    private static class SingleInstanceThread extends Thread {

        private ServerSocket listener;

        /**
         * Create a new instance of this class.
         */
        public SingleInstanceThread() {
        }

        /**
         * Start a new ServerSocket to see if another instance is already
         * running.
         * @return True if ServerSocket starts to listen successfully, false
         * if it couldn't and an exception gets thrown.
         */
        public boolean tryInstance() {
            try {
                listener = new ServerSocket(65533, 0, InetAddress.getByName(null));
                start();
                return true;
            } catch (IOException e) {
                //Unable to connect, no need to tell the world.
            }
            return false;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    listener.accept(); //Accept but don't use returned socket.
                }
            } catch (IOException e) {
                //Nothing to do here
            }
        }
    }
}
