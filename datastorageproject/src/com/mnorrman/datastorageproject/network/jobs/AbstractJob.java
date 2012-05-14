
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.tools.HexConverter;
import com.mnorrman.datastorageproject.tools.IntConverter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Random;

/**
 * Abstract class defining a job. Is always able to tell whether its finished
 * or not with its task.
 * @author Mikael Norrman
 */
public abstract class AbstractJob {
    
    private boolean finished = false;
    private String jobID;
    private String fromConnection;

    /**
     * Main constructor. Should be used when creating a new job.
     * @param owner 
     */
    public AbstractJob(){
        jobID = "00000000";
        
        //Loop until we get a ID-value that is not equal to 0.
        while(jobID.equals("00000000"))
            jobID = HexConverter.toHex(IntConverter.intToByteArray(new Random().nextInt()));
    }
    
    /**
     * Secondary constructor. Should be used whenever we have a job on two
     * places and they need to have the same ID.
     * @param ID 
     */
    public AbstractJob(String ID){
        this.jobID = ID;
    }
    
    /**
     * Used mainly by MasterNode to keep track of which connection this job
     * belongs to.
     * @param owner 4-byte value determing the ID of the owner.
     */
    public void setFromConnection(String fromConnection){
        this.fromConnection = fromConnection;
    }

    /**
     * Get the owner ID of this job.
     * @return 4-byte owner ID value.
     */
    public String getFromConnection() {
        return fromConnection;
    }   
    
    /**
     * Check if this job is finished
     * @return the current state of this job
     */
    public boolean isFinished() {
        return finished;
    }
    
    /**
     * Set a new value for finished.
     * @param value The new value.
     */
    void setFinished(boolean value){
        finished = value;
    }

    /**
     * Get the ID for this job.
     * @return 
     */
    public String getJobID() {
        return jobID;
    }
    
    public void setJobID(String newJobID){
        this.jobID = newJobID;
    }
    
    /**
     * Method used to let this job perform any tasks it has based on the input data.
     * The buffer should in this case contain the data for this job and the position
     * should be after any ID's and commands.
     * @param buffer Should have necessary data from network.
     * @return true if it should be added to the write-queue or false if not.
     * @throws IOException 
     */
    public abstract boolean readOperation(ByteBuffer buffer) throws IOException;
    
    /**
     * Method used to let this job have access to the socketchannel to write. Should be
     * used in most cases to perform a write task server-side.
     * Data written at one run-through should never be more then the amount set
     * in MasterNode-class as NETWORK_BLOCK_SIZE or else the non-blocking features
     * of this software are in vain.
     * @param s If data needs to be written, this is were they should go.
     * @param buffer Use the same buffer everywhere.
     * @return true if there is no need for any more updates, false if this job
     * should be put back in the queue.
     * @throws IOException
     */
    public abstract boolean writeOperation(SocketChannel s, ByteBuffer buffer) throws IOException;
}
