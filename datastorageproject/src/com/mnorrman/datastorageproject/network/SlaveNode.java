/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.network.jobs.AbstractJob;
import com.mnorrman.datastorageproject.network.jobs.ConnectJob;
import com.mnorrman.datastorageproject.tools.HexConverter;
import com.mnorrman.datastorageproject.tools.IntConverter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 *
 * @author Mikael
 */
public class SlaveNode extends Thread {

    private Main main;
    private ConnectionContext context;
    private Selector selector;
    private ByteBuffer buffer;
    private boolean keepWorking = true;
    private HashMap<String, AbstractJob> jobs;
    private PriorityQueue<AbstractJob> jobQueue;

    public SlaveNode(Main main) {
        this.main = main;
        try {
            selector = Selector.open();
            context = new ConnectionContext(SocketChannel.open(), Protocol.NULL);
            context.channel.configureBlocking(false);
            jobs = new HashMap<String, AbstractJob>();
            jobQueue = new PriorityQueue<AbstractJob>();
        } catch (IOException e) {
            LogTool.log(e, LogTool.CRITICAL);
        }
    }

    public void startSlaveServer() {
        this.start();
    }

    @Override
    public void run() {

        //The bytebuffer here is used in multiple ways. Even though it is
        //quite big, it can be used for small amounts of data by setting
        //the limit to appropriate sizes.
        buffer = ByteBuffer.allocateDirect(MasterNode.NETWORK_BLOCK_SIZE);

        while (keepWorking) {
            try {
                while (keepWorking && !context.channel.isConnected()) {
                    context.channel.connect(new InetSocketAddress(InetAddress.getByName(Main.properties.getValue("master").toString()), Integer.parseInt(Main.properties.getValue("port").toString())));
                    if (!context.channel.finishConnect()) {
                        LogTool.log("Could not connect to master node, trying again in 10 seconds", LogTool.WARNING);
                        try {
                            sleep(10000);
                        } catch (InterruptedException e) {
                            LogTool.log(e, LogTool.CRITICAL);
                        }
                    } else {
                        context.channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        ConnectJob conJob = new ConnectJob();
                        jobs.put("00000000", conJob);
                        jobQueue.add(conJob);
//                        context.setCommand(Protocol.CONNECT);
//                        context.setTask(new ConnectJob());
                    }
                }

                if (selector.select() <= 0) {
                    continue;
                }

                Iterator it = selector.selectedKeys().iterator();

                while (it.hasNext()) {
                    SelectionKey key = (SelectionKey) it.next();
                    it.remove();

                    //key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);

                    if (key.isValid()) {
                        if (key.isReadable()) {
                            System.out.println("Reading");
                            while (buffer.position() != buffer.capacity()) {
                                System.out.println("Didn't get enough");
                                if (context.channel.read(buffer) == -1) {
                                    //Connection has been terminated, cleanup.                                   
                                    key.cancel();
                                    LogTool.log("Connection to master was closed", LogTool.INFO);
                                    buffer.clear();
                                    return;
                                }
                            }
                            buffer.flip();
                            byte[] from = new byte[4];
                            buffer.get(from);
                            int length = buffer.getInt();

                            System.out.println("From: 0x" + HexConverter.toHex(from) + " with len: " + length);
                            
                            System.out.println("Ok time to see");
                            
                            if (jobs.containsKey(HexConverter.toHex(from))) {
                                System.out.println("Contains!");
                                AbstractJob job = jobs.get(HexConverter.toHex(from));
                                if (job instanceof ConnectJob) {
                                    Main.ID = IntConverter.intToByteArray(buffer.getInt());
                                    System.out.println("My ID: 0x" + HexConverter.toHex(Main.ID));
                                    jobs.remove(HexConverter.toHex(from));
                                }
                            }

//                            if (context.command == Protocol.NULL) {
//                                context.setCommand(Protocol.getCommand(buffer.get()));
//                            }
//
//                            switch (context.command) {
//                                case CONNECT:
//                                    if (((ConnectJob) context.task).getHaveSentCommand()) {
//
//                                        Main.ID = IntConverter.intToByteArray(buffer.getInt());
//                                        System.out.println("My ID: 0x" + HexConverter.toHex(Main.ID));
//                                        context.setCommand(Protocol.NULL);
//                                        context.setTask(null);
//                                    }
//                                    break;
//                                default:
//                                //Hello?! Is there anybody out there?!
//                            }
                        }
//                        if (key.isWritable()) {
//                            System.out.println("Will write once?");
//                            switch (context.command) {
//                                case CONNECT:
//                                    if (!((ConnectJob) context.task).getHaveSentCommand()) {
//                                        buffer.put(Main.ID); //Unknown so far
//                                        buffer.putInt(1);
//                                        buffer.put(Protocol.CONNECT.getValue());
//                                        buffer.rewind();
//                                        context.channel.write(buffer);
//                                        buffer.clear();
//                                        ((ConnectJob) context.task).setHaveSentCommand(true);
//                                    }
//                                    break;
//                                default:
//                                //Hoho? Anybody here?
//                            }
//                        }

                        if (!jobQueue.isEmpty()) {
                            AbstractJob queuedJob = jobQueue.poll();
                            if(!queuedJob.update(context.channel, buffer)){
                                jobQueue.offer(queuedJob);
                            }
//                            if(queuedJob instanceof ConnectJob){
//                                buffer.put(Main.ID); //Unknown so far
//                                buffer.putInt(1);
//                                buffer.put(Protocol.CONNECT.getValue());
//                                buffer.rewind();
//                                context.channel.write(buffer);
//                                buffer.clear();
//                                ((ConnectJob) queuedJob).setHaveSentCommand(true);
//                            }
                        }
                    }

                    selector.selectedKeys().clear();
                }

            } catch (IOException e) {
                LogTool.log(e, LogTool.CRITICAL);
            }

        }
    }
    
    public void createJob(String jobOwner, AbstractJob job){
        if(jobOwner != null && job != null){
            jobs.put(jobOwner, job);
            jobQueue.add(job);
        }
    }

    private SelectionKey keyWantsToWrite(SelectionKey key) {
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        return key;
    }

    public void close() throws IOException {
        buffer.clear();
        buffer.put(Main.ID);
        buffer.putInt(1);
        buffer.put(Protocol.DISCONNECT.getValue());
        buffer.rewind();
        context.channel.write(buffer);
        context.channel.close();
        selector.close();
        keepWorking = false;
    }
}
