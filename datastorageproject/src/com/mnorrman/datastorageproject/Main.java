
package com.mnorrman.datastorageproject;

import com.mnorrman.datastorageproject.index.GlobalIndex;
import com.mnorrman.datastorageproject.index.IndexPersistence;
import com.mnorrman.datastorageproject.index.IndexPersistenceGlobal;
import com.mnorrman.datastorageproject.index.LocalIndex;
import com.mnorrman.datastorageproject.network.MasterNode;
import com.mnorrman.datastorageproject.network.MasterNodeSlaveList;
import com.mnorrman.datastorageproject.network.SlaveNode;
import com.mnorrman.datastorageproject.storage.BackStorage;
import com.mnorrman.datastorageproject.storage.DataProcessor;
import com.mnorrman.datastorageproject.storage.DataTicket;
import com.mnorrman.datastorageproject.tools.HexConverter;
import com.mnorrman.datastorageproject.web.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Mikael Norrman
 */
public class Main {

    public static GlobalIndex globalIndex;
    public static MasterNodeSlaveList slaveList;
    public static LocalIndex localIndex;
    public static ExecutorService pool;
    public static Timer timer;
    public static PropertiesManager properties;
    public static ServerState state;
    public static String ID;
    public BackStorage storage;
    public MasterNode masterNode;
    public SlaveNode slaveNode;
    public WebServer webServer;
    
    private File shutdownSafetyFile;

    /**
     * Create new instance of this class. This method initiates major 
     * components such as BackStorage, WebServer and Server nodes.
     */
    public Main() {
        try {
            //Initiate BackStorage
            storage = new BackStorage().initialize();
            
            //Test to see if software was successfully closed last time it
            //shutdown. If not then see if the data is allright!
            shutdownSafetyFile = new File(properties.getValue("dataPath") + File.separator + "safeCheck_");
            if(shutdownSafetyFile.exists()){
                if(!storage.performIntegrityCheck()){
                    LogTool.log("Backstorage integrity check return with errors", LogTool.CRITICAL);
                    if(Boolean.parseBoolean(properties.getValue("autoclean").toString())){
                        storage.clean();
                    }else{
                        LogTool.log("Perform a clean operation to repair the errors", LogTool.CRITICAL);
                    }
                }
                localIndex.clear().insertAll(storage.reindexData());
            }else{
                shutdownSafetyFile.createNewFile();
            }
            
            //Initiate webserver and add necessary roles
            webServer = new WebServer(8429);
            webServer.addWebRole("/", new MainWebRole());
            webServer.addWebRole("/index", new PrintIndexWebRole());
            webServer.addWebRole("/get", new SimpleGetWebRole(this));
            webServer.addWebRole("/post", new SimplePostWebRole(this));
        } catch (IOException e) {
            LogTool.log(e, LogTool.CRITICAL);
        }

        state = ServerState.IDLE;
        
        if (properties.getValue("master").toString().equalsIgnoreCase("127.0.0.1")) {
            masterNode = new MasterNode(this);
            masterNode.startMasterServer();
            ID = "00000000";
        }else{
            slaveNode = new SlaveNode(this);
            slaveNode.startSlaveServer();
            state = ServerState.CONNECTING;
            if(!properties.getValue("serverID").toString().equals("")){
                ID = properties.getValue("serverID").toString();
            }else{
                //When we use this ID, that means we have not connected this slave to a master yet.
                ID = "FFFFFFFF";
            }
            System.out.println("ID= " + ID);
        }
        
        Thread sm = new Thread(new ServerMonitor(masterNode));
        sm.start();
        
//        try{
//            Thread.sleep(5000);
//            slaveNode.createJob(HexConverter.toHex(Main.ID), new SyncLocalIndexJob(HexConverter.toHex(Main.ID)));
//        }catch(InterruptedException e){
//            e.printStackTrace();
//        }
    }

    /**
     * Fetches a new DataProcessor from the BackStorage.
     * @return new DataProcessor object.
     */
    public DataProcessor getNewDataProcessor() {
        //return new DataProcessor(storage.getChannel());
        DataTicket dt = storage.getTicket();
        if(dt != null)
            return new DataProcessor(dt);
        else
            return null;
    }

    public long getCurrentDataSize() throws IOException{
        return storage.getCurrentDataSize();
    }
    
    /**
     * Method used to try and gracefully shut down this server instance.
     * @throws InterruptedException
     * @throws IOException 
     */
    public void shutdown() throws InterruptedException, IOException{
        //End the timer
        timer.cancel();
        
        //Save indexes
        if(globalIndex != null)
            pool.submit(new IndexPersistenceGlobal(globalIndex.getData()));
        pool.submit(new IndexPersistence(localIndex.getData()));
        
        //Close the backstorage
        if(storage != null)
            storage.close();
        
        //Shut down the thread pool. Use timeout and hope that every thread is
        //done by the end of timeout.
        pool.shutdown();
        if(pool.awaitTermination(30, TimeUnit.SECONDS)){
            pool.shutdownNow();
        }

        //Stop network mechanisms from receiving and sending more input data.
        if(masterNode != null)
            masterNode.close();
        if(slaveNode != null){
            slaveNode.close();
        }
        if(webServer != null)
            webServer.close();
        
        //Remove the shutdownsafetyfile to tell us that it was a safe shutdown
        //next time we start. 
        shutdownSafetyFile.delete();
        
        System.exit(0);
    }
    
    /**
     * Main method of this application.
     * @param args the command line arguments, which are not used at the moment.
     */
    public static void main(String[] args) {
        //Check if there's already an instance of this software running.
        //Only one instance is allowed at any given time.
//        SingleInstanceThread sit = new SingleInstanceThread();
//        if (!sit.tryInstance()) {
//            System.out.println("Other instance found.");
//            System.exit(0);
//        }

        LogTool.setLogLevel(LogTool.INFO);

        state = ServerState.NOTRUNNING;
        
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
            globalIndex = new GlobalIndex();
            slaveList = new MasterNodeSlaveList();
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
