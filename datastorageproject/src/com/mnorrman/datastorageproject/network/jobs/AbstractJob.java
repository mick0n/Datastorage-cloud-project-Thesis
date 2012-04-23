
package com.mnorrman.datastorageproject.network.jobs;

/**
 * Abstract class defining a job. Is always able to tell whether its finished
 * or not with its task.
 * @author Mikael Norrman
 */
public abstract class AbstractJob {
    
    private boolean finished = false;

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
}
