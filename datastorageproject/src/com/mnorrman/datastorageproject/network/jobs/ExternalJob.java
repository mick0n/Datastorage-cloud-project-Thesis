/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.network.ExternalTrafficContext;

/**
 *
 * @author Mikael
 */
public abstract class ExternalJob extends AbstractJob{

    private ExternalTrafficContext context;
    
    public ExternalJob(ExternalTrafficContext context) {
        this.context = context;
    }

    public ExternalTrafficContext getContext() {
        return context;
    }
}
