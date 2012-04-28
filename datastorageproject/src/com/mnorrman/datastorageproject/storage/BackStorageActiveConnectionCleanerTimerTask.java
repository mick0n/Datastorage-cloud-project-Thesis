/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.storage;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.State;
import java.util.TimerTask;

/**
 *
 * @author Mikael
 */
public class BackStorageActiveConnectionCleanerTimerTask extends TimerTask{

    private BackStorage owner;
    
    public BackStorageActiveConnectionCleanerTimerTask(BackStorage bs){
        this.owner = bs;
    }
    
    @Override
    public void run() {
        if(!owner.activeProcesses.isEmpty())
            Main.pool.submit(new BackStorageActiveConnectionCleaner(owner.activeProcesses.iterator()));
        else{
            if(Main.state != State.CHKINTEG && Main.state != State.INDEXING)
                Main.state = State.IDLE;
        }
    }
    
}
