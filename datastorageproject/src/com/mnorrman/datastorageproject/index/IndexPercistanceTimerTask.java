/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.index;

import com.mnorrman.datastorageproject.Main;
import java.util.TimerTask;

/**
 *
 * @author Mikael
 */
public class IndexPercistanceTimerTask extends TimerTask{

    private Index master;
    
    public IndexPercistanceTimerTask(Index master){
        this.master = master;
    }
    
    @Override
    public void run() {
        Main.pool.submit(new IndexPercistance(master.getClass().getSimpleName(), master.getData()));
    }
    
}
