/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.objects.ServerNode;
import java.util.HashMap;

/**
 *
 * @author Mikael
 */
public class InternalTrafficHandlerChildProperties {
    
    protected InternalTrafficContext masterContext;
    protected HashMap<String, ServerNode> children;
    //My range
    //My children, stuff like that
    
    public InternalTrafficHandlerChildProperties(){
        children = new HashMap<String, ServerNode>();
    }
    
}
