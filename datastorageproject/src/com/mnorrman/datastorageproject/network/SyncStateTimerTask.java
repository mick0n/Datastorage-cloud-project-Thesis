/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.ServerState;
import com.mnorrman.datastorageproject.network.jobs.SyncStateJob;
import java.util.TimerTask;

/**
 *
 * @author Mikael
 */
public class SyncStateTimerTask extends TimerTask{

    private SlaveNode owner;
    private Main main;
    
    public SyncStateTimerTask(SlaveNode owner, Main main){
        this.owner = owner;
        this.main = main;
    }
    
    @Override
    public void run() {
        if(Main.state.getValue() > ServerState.INDEXING.getValue()){
            SyncStateJob ssj = new SyncStateJob(main);
            owner.createJob(ssj.getJobID(), ssj);
        }
    }    
}
