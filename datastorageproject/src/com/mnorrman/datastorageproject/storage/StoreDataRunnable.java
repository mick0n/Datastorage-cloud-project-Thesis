/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.storage;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.objects.UnindexedDataObject;

/**
 *
 * @author Mikael
 */
public class StoreDataRunnable implements Runnable{

    private DataProcessor dp;
    private UnindexedDataObject udo;
    
    public StoreDataRunnable(DataProcessor dp, UnindexedDataObject udo){
        this.dp = dp;
        this.udo = udo;
    }
    
    public void run() {
        Main.localIndex.insert(dp.storeData(udo));
    }    
}
