/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

/**
 *
 * @author Mikael
 */
public class RouteJob extends AbstractJob{
    
    byte[] destinationNode, client;
    
    public RouteJob(byte[] destinationNode, byte[] client){
        this.destinationNode = destinationNode;
        this.client = client;
    }

    public byte[] getClient() {
        return client;
    }

    public byte[] getDestinationNode() {
        return destinationNode;
    }
}
