
package com.mnorrman.datastorageproject.index;

import com.mnorrman.datastorageproject.Main;
import java.util.TimerTask;

/**
 *
 * @author Mikael Norrman
 */
public class IndexPersistenceTimerTask extends TimerTask{

    private Index master;
    
    /**
     * Create new IndexPersistenceTimerTask instance
     * @param master The index to save through this task
     */
    public IndexPersistenceTimerTask(Index master){
        this.master = master;
    }
    
    @Override
    public void run() {
        Main.pool.submit(new IndexPersistence(master.getData()));
    }
    
}
