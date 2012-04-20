/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.network.jobs.AbstractJob;

/**
 *
 * @author Mikael
 */
public class ConnectionContext {
    
    protected Protocol command;
    
    protected AbstractJob task;
    
    public ConnectionContext(Protocol command){
        this.command = command;
        System.out.println("Value is = " + this.command.getValue());
    }
    
    public void setTask(AbstractJob aj){
        this.task = aj;
    }
}
