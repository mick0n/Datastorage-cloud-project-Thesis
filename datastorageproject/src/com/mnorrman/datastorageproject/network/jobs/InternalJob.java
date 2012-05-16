/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.network.InternalTrafficContext;

/**
 *
 * @author Mikael
 */
public abstract class InternalJob extends AbstractJob{

    private InternalTrafficContext context;
    
    public InternalJob(InternalTrafficContext context){
        super();
        this.context = context;
    }
    
    public InternalJob(InternalTrafficContext context, String jobID){
        super(jobID);
        this.context = context;
    }

    public InternalTrafficContext getContext() {
        return context;
    }    
}
