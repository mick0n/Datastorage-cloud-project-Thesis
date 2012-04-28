/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.storage;

import java.nio.channels.FileChannel;

/**
 *
 * @author Mikael
 */
public final class DataTicket {
    
    private boolean finished;
    private FileChannel channel;
    
    public DataTicket(FileChannel newChannel){
        this.finished = false;
        this.channel = newChannel;
    }
    
    public FileChannel getChannel(){
        return channel;
    }
    
    public void finish(){
        finished = true;
    }

    public boolean isFinished() {
        return finished;
    }
    
}
