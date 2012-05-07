/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.network.jobs.SyncStateJob;
import com.mnorrman.datastorageproject.tools.HexConverter;
import java.util.TimerTask;

/**
 *
 * @author Mikael
 */
public class SyncStateTimerTask extends TimerTask{

    private SlaveNode owner;
    
    public SyncStateTimerTask(SlaveNode owner){
        this.owner = owner;
    }
    
    @Override
    public void run() {
        owner.createJob(HexConverter.toHex(Main.ID), new SyncStateJob(HexConverter.toHex(Main.ID)));
    }    
}
