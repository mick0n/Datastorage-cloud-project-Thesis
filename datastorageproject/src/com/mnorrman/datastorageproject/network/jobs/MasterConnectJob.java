/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

/**
 *
 * @author Mikael
 */
public class MasterConnectJob{
//
//    private ClusterNode mn;
//    private SelectionKey key;
//    
//    public MasterConnectJob(String jobID, String fromConnection, ClusterNode mn, SelectionKey key){
//        super(jobID);
//        setFromConnection(fromConnection);
//        this.mn = mn;
//        this.key = key;
//    }
//    
////    public String setConnectionID(String newConnectionID){
////        if(newConnectionID.equals("FFFFFFFF")){
////            newConnectionID = HexConverter.toHex(IntConverter.intToByteArray(new Random().nextInt(Integer.MAX_VALUE/2) + Integer.MAX_VALUE/2));
////        }
////        mn.switchConnectionID(getFromConnection(), newConnectionID);
////        setFromConnection(newConnectionID);
////        System.out.println("Switching connection from 0x" + getFromConnection() + " to 0x" + newConnectionID);
////        return newConnectionID;
////    }
//    
//    @Override
//    public boolean readOperation(ByteBuffer buffer) throws IOException {
//        buffer.rewind();
//        ClusterMessageVariables cmv = new ClusterMessageVariables(buffer);
//        buffer.get();
//        long storageLimit = buffer.getLong();
//        
//        String childID = cmv.getFrom();
//        System.out.println("SlaveiD: " + childID);
//        if(childID.equals("FFFFFFFF")){
//            //Create new SlaveID
//            childID = HexConverter.toHex(new Random().nextInt());
//        }
//        
//        System.out.println("Switching connection from 0x" + getFromConnection() + " to 0x" + childID);
////        
//        //Get the connectionContext, remove the old key mapping and add a new one.
//        mn.switchConnectionID(getFromConnection(), childID);
//        setFromConnection(childID);
//        key.attach(new Pair<Boolean, String>(false, childID));
//        mn.getConnections().get(getFromConnection()).getNode().setStorageLimit(storageLimit);
////        Main.slaveList.put(mn.getConnections().get(getFromConnection()).getNode());
//        
//        
////        MasterConnectJob mcj = new MasterConnectJob(cmv.getJobID(), cmv.getFrom(), getFromConnection(), mn);
////        mn.getJobs().remove(getJobID());
////        mn.createJob(cmv.getJobID(), this);
//        
//        
//        
////        if(fromSlaveNode == null){
////            buffer.rewind();
////            ClusterMessageVariables cmv = new ClusterMessageVariables(buffer);
////            System.err.println("Current job id: " + getJobID());
////            System.err.println("" + cmv.toString());
////            this.fromSlaveNode = cmv.getFrom();
////            mn.getJobs().remove(getJobID());
////            mn.getJobs().put(cmv.getJobID(), this);
////            setJobID(cmv.getJobID());
////            System.err.println("Does jobs contain the new id? " + mn.getJobs().containsKey(cmv.getJobID()));
////        }
//        buffer.clear();
//        return true;
//    }
//
//    @Override
//    public boolean writeOperation(SocketChannel s, ByteBuffer buffer) throws IOException {
//        buffer.put(HexConverter.toByte(Main.ID)); 
//        buffer.putInt(8);
//        buffer.put(HexConverter.toByte(getJobID()));
//        buffer.put(HexConverter.toByte(getFromConnection()));
//        buffer.flip();
//        while(buffer.hasRemaining())
//            s.write(buffer);
//        
//        buffer.clear(); //Always clear buffer
//        setFinished(true);
//        return true;
//    }
}
