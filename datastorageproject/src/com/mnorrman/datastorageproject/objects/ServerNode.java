
package com.mnorrman.datastorageproject.objects;

import com.mnorrman.datastorageproject.ServerState;
import java.net.InetAddress;

/**
 *
 * @author Mikael Norrman
 */
public class ServerNode {
        
    private InetAddress ipaddress;
    private int port;
    private byte[] id;
    private ServerState state;
    
    private long storageLimit;

    public ServerNode(InetAddress ipaddress, int port, byte[] id) {
        this.ipaddress = ipaddress;
        this.port = port;
        this.id = id;
        this.state = ServerState.NOTRUNNING;
    }

    public InetAddress getIpaddress() {
        return ipaddress;
    }

    public void setIpaddress(InetAddress ipaddress) {
        this.ipaddress = ipaddress;
    }

    public byte[] getId() {
        return id;
    }

    public void setId(byte[] id) {
        this.id = id;
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

    public void setState(ServerState state) {
        this.state = state;
    }

    public ServerState getState() {
        return state;
    }
}
