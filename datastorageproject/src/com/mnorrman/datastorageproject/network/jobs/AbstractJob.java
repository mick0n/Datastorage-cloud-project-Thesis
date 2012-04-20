/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

/**
 *
 * @author Mikael
 */
public abstract class AbstractJob {
    
    private boolean finished = false;

    public boolean isFinished() {
        return finished;
    }
  
    void setFinished(boolean value){
        finished = value;
    }
}
