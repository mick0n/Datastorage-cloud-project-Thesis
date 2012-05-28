/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.network.jobs.ExternalJob;
import com.mnorrman.datastorageproject.network.jobs.PutJob;
import com.mnorrman.datastorageproject.network.jobs.GetJob;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Mikael
 */
public class ExternalTrafficHandler extends Thread {
    
    public static final int MAXIMUM_NETWORK_BLOCK_SIZE = 131072;
    private Main main;
    private InternalTrafficHandler ith;
    private Selector selector;
    private ConcurrentLinkedQueue<SocketChannel> channelQueue;
    private HashMap<Integer, ExternalTrafficContext> connections;
    private ByteBuffer buffer;

    private HashMap<Integer, ExternalJob> jobs;
    private Queue<ExternalJob> jobQueue;
    private int bufferLimit = MAXIMUM_NETWORK_BLOCK_SIZE;
    private boolean keepWorking = true;
    
    private int connectionCounter = 0;

    public ExternalTrafficHandler(Main main, InternalTrafficHandler ith) {

        this.main = main; //used for getting DataProcesses from BackStorage
        this.ith = ith; //used for accessing information about cluster

        try {
            selector = Selector.open();
            channelQueue = new ConcurrentLinkedQueue<SocketChannel>();
            connections = new HashMap<Integer, ExternalTrafficContext>(1033);
            jobs = new HashMap<Integer, ExternalJob>(1033);
            jobQueue = new LinkedList<ExternalJob>();
        } catch (IOException e) {
            LogTool.log(e, LogTool.CRITICAL);
        }
    }

    /**
     * Initiate threads related to this object
     */
    public void startup() {
        this.start();
    }

    @Override
    public void run() {

        //The bytebuffer here is used in multiple ways. Even though it is
        //quite big, it can be used for small amounts of data by setting
        //the limit to appropriate sizes.
        buffer = ByteBuffer.allocateDirect(MAXIMUM_NETWORK_BLOCK_SIZE);
        int readyChannels = 0;

        while (keepWorking) {

            try {
                if (!jobQueue.isEmpty()) {
                    readyChannels = selector.selectNow();
                } else {
                    readyChannels = selector.select();
                }
            } catch (IOException e) {
                LogTool.log(e, LogTool.CRITICAL);
            }

            //Since readyChannels may be zero, we check this first
            if (readyChannels > 0) {

                Iterator it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = (SelectionKey) it.next();

                    //Remove key, otherwise it will stay in the list forever.
                    it.remove();

//                    try {
                        if (key.isValid() && key.isReadable()) {
                            ExternalTrafficContext etc = (ExternalTrafficContext) key.attachment();

                            if(!jobs.containsKey(etc.identifier)){
                                buffer.limit(1);
                            }else if(jobs.get(etc.identifier) instanceof PutJob){
                                if(!((PutJob)jobs.get(etc.identifier)).gotUdo())
                                    buffer.limit(392);                                
                                else
                                    buffer.limit(bufferLimit);
                            }else if(jobs.get(etc.identifier) instanceof GetJob){
                                if(!((GetJob)jobs.get(etc.identifier)).gotUdo())
                                    buffer.limit(256);
                                else
                                    buffer.limit(bufferLimit);
                            }else{
                                buffer.limit(bufferLimit);
                            }

                            while (buffer.hasRemaining()) {
                                try {
                                    if (etc.channel.read(buffer) == -1) {
                                        connections.remove(etc.identifier);
                                        key.cancel();
                                        LogTool.log("Connection from " + etc.channel.socket().getInetAddress().getHostAddress() + ":" + etc.channel.socket().getPort() + " was closed", LogTool.INFO);
                                        break;
                                    }
                                } catch (IOException e) {
                                    connections.remove(etc.identifier);
                                    key.cancel();
                                    LogTool.log("Connection from " + etc.channel.socket().getInetAddress().getHostAddress() + ":" + etc.channel.socket().getPort() + " was closed", LogTool.INFO);
                                    break;
                                }
                            }

                            buffer.flip();

                            if(buffer.limit() == 1){
                                Protocol command = Protocol.getCommand(buffer.get());

                                switch (command) {
                                    case GET:
                                        GetJob gj = new GetJob(etc, this, main.getNewDataProcessor());
                                        jobs.put(gj.getContext().identifier, gj);
                                        break;
                                    case PUT:
                                        PutJob pj = new PutJob(etc, this, main.getNewDataProcessor());
                                        jobs.put(pj.getContext().identifier, pj);
                                        break;
                                }
                            }else{
                                if (jobs.containsKey(etc.identifier)) {
                                    //Perform the job
                                    ExternalJob job = jobs.get(etc.identifier);
                                    try {
                                        if (job.readOperation(buffer)) {
                                            //If the readOperation returns true it
                                            //means it has something to write.
                                            jobQueue.offer(job);
                                        }
                                    } catch (IOException e) {
                                        LogTool.log(e, LogTool.CRITICAL);
                                    }
                                    if (job.isFinished()) {
                                        jobs.remove(etc.identifier);
                                    }
                                }
                            }
                            buffer.clear();
                        }
//                    } catch (IOException e) {
//                        //Remove the channel from this selector
//                        key.cancel();
//                        connections.remove(((ExternalTrafficContext) key.attachment()).identifier);
//                        LogTool.log(e, LogTool.WARNING);
//                    }
                }
            }

            //Perform a write operation, if any
            try {
                if (!jobQueue.isEmpty()) {
                    ExternalJob queuedJob = jobQueue.poll();
                    if (queuedJob.getContext().channel != null) {
                        if (!queuedJob.writeOperation(queuedJob.getContext().channel, buffer)) {
                            jobQueue.offer(queuedJob);
                        } else {
                            if (queuedJob.isFinished()) {
                                jobs.remove(queuedJob.getContext().identifier);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LogTool.log(e, LogTool.CRITICAL);
            }

            //After checking all keys we check if there are any channels waiting
            //to be registered with this selector. Creates a new ID for each
            //connection and stores the id as an attachment while a 
            //connectionContext is stored in a hashmap.
            if (!channelQueue.isEmpty() && connections.size() < 50) {

                SocketChannel sc = channelQueue.poll();

                try {
                    connectionCounter++;
                    if(connectionCounter == Integer.MAX_VALUE){
                        connectionCounter = 0;
                    }
                    int newID = connectionCounter;
                    ExternalTrafficContext etc = new ExternalTrafficContext(newID, sc);
                    sc.register(selector, SelectionKey.OP_READ, etc);
                    connections.put(newID, etc);
                    LogTool.log("Connection from " + sc.socket().getInetAddress().getHostAddress() + ":" + sc.socket().getPort() + " was added to selector", LogTool.INFO);
                } catch (NullPointerException e) {
                    LogTool.log(e, LogTool.CRITICAL);
                } catch (ClosedChannelException e) {
                    LogTool.log(e, LogTool.CRITICAL);
                } catch (IOException e) {
                    LogTool.log(e, LogTool.CRITICAL);
                }
            }
            
            //Reconfigure maximum size of buffer
            int a = (int) Math.floor(Math.log(connections.size()) / Math.log(2));
            int b = (int) Math.floor(Math.log(jobQueue.size()) / Math.log(2));
            int c = (int) Math.floor(Math.log(jobs.size()) / Math.log(2));
            int medium = (int) Math.floor((a + b + c) / 3);
            if (medium > 0) {
                bufferLimit = MAXIMUM_NETWORK_BLOCK_SIZE / medium;
                //buffer.limit(MAXIMUM_NETWORK_BLOCK_SIZE / medium);
            } else {
                bufferLimit = MAXIMUM_NETWORK_BLOCK_SIZE;
//                buffer.limit(MAXIMUM_NETWORK_BLOCK_SIZE);
            }            
        }
    }

    /**
     * Adds a SocketChannel to a queue which eventually will be registered to a
     * selector.
     *
     * @param channel
     */
    public synchronized void addSocketChannel(SocketChannel channel) {
        this.channelQueue.add(channel);
        selector.wakeup();
    }

    public void close() throws IOException {
        keepWorking = false;
        selector.wakeup();
    }

    public HashMap<Integer, ExternalTrafficContext> getConnections() {
        return connections;
    }

    public HashMap<Integer, ExternalJob> getJobs() {
        return jobs;
    }

    public InternalTrafficHandler getInternalTrafficHandler(){
        return ith;
    }
}
