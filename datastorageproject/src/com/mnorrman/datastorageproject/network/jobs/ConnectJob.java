/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

/**
 *
 * @author Mikael
 */
public class ConnectJob extends AbstractJob{
    
    private boolean haveSentCommand;
    
    public ConnectJob() {
        this.haveSentCommand = false;
    }

    public void setHaveSentCommand(boolean haveSentCommand) {
        this.haveSentCommand = haveSentCommand;
    }

    public boolean getHaveSentCommand() {
        return haveSentCommand;
    }
    
}
