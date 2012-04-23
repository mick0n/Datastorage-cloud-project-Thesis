
package com.mnorrman.datastorageproject.index;

import com.mnorrman.datastorageproject.Main;
import java.util.TimerTask;

/**
 *
 * @author Mikael Norrman
 */
public class IndexPersistenceGlobalTimerTask extends TimerTask{

    private Index master;
    
    /**
     * Create new IndexPersistenceGlobalTimerTask instance
     * @param master The index to save through this task
     */
    public IndexPersistenceGlobalTimerTask(Index master){
        this.master = master;
    }
    
    @Override
    public void run() {
        Main.pool.submit(new IndexPersistenceGlobal(master.getData()));
    }
    
}
