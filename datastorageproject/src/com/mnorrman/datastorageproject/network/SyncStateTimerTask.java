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

    private InternalTrafficHandler ith;
    private Main main;
    
    public SyncStateTimerTask(InternalTrafficHandler ith, Main main){
        this.ith = ith;
        this.main = main;
    }
    
    @Override
    public void run() {
        if(Main.state.getValue() > ServerState.INDEXING.getValue()){
            SyncStateJob ssj = new SyncStateJob(ith.getMasterContext(), main, ith);
            ith.createJob(ssj.getJobID(), ssj);
        }
    }    
}
