
package com.mnorrman.datastorageproject.network.jobs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Abstract class defining a job. Is always able to tell whether its finished
 * or not with its task.
 * @author Mikael Norrman
 */
public abstract class AbstractJob {
    
    private boolean finished = false;
    private String owner;

    /**
     * Main constructor. Should always be used when creating a new job.
     * @param owner 
     */
    public AbstractJob(String owner){
        this.owner = owner;
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
     * Get the owner string of this job.
     * @return The owner string
     */
    public String getOwner() {
        return owner;
    }
    
    /**
     * Method used to update a job without knowing what job it is. Should be
     * used in most cases to perform a task server-side.
     * @param s If data needs to be written, this is were they should go.
     * @param buffer Use the same buffer everywhere.
     * @return true if there is no need for any more updates, false if this job
     * should be put back in the queue.
     * @throws IOException
     */
    public abstract boolean update(SocketChannel s, ByteBuffer buffer) throws IOException;
}
