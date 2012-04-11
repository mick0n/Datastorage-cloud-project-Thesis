/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.objects;

import java.net.InetAddress;

/**
 *
 * @author Mikael
 */
public class ServerNode {
        
    private InetAddress ipaddress;
    private int port;
    private String name;
    
    private long storageLimit;

    public ServerNode(InetAddress ipaddress, int port, String name) {
        this.ipaddress = ipaddress;
        this.port = port;
        this.name = name;
    }

    public InetAddress getIpaddress() {
        return ipaddress;
    }

    public void setIpaddress(InetAddress ipaddress) {
        this.ipaddress = ipaddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getStorageLimit() {
        return storageLimit;
    }

    public void setStorageLimit(long storageLimit) {
        this.storageLimit = storageLimit;
    }
}
