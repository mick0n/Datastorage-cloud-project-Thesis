/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.storage;

import java.util.Iterator;

/**
 *
 * @author Mikael
 */
public class BackStorageActiveConnectionCleaner implements Runnable{
    
    private Iterator<DataTicket> iterator;
    
    public BackStorageActiveConnectionCleaner(Iterator<DataTicket> iterator){
        this.iterator = iterator;
    }

    public void run() {
        while(iterator.hasNext()){
            if(iterator.next().isFinished())
                iterator.remove();
        }
    }

}
