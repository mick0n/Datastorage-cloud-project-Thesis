
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.network.jobs.AbstractJob;

/**
 * An object used by selector along with a SocketChannel to determine current
 * actions.
 * @author Mikael Norrman
 */
public class ConnectionContext {
    
    protected Protocol command;
    
    protected AbstractJob task;
    
    /**
     * Creates new instance of ConnectionContext.
     * @param command Starting command. Should always be Protocol.NULL.
     */
    public ConnectionContext(Protocol command){
        this.command = command;
    }
    
    /**
     * Set a new command.
     * @param command 
     */
    public void setCommand(Protocol command){
        this.command = command;
        System.out.println("Value is = " + this.command.getValue());
    }
    
    /**
     * Set current task.
     * @param aj 
     */
    public void setTask(AbstractJob aj){
        this.task = aj;
    }
}
