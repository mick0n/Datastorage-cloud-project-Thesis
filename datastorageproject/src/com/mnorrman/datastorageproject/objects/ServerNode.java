
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
    private String id;
    
    
    private ServerState state;
    private Range range;
    private long dataSize;
    private long storageLimit;

    public ServerNode(InetAddress ipaddress, int port, String id) {
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public long getDataSize() {
        return dataSize;
    }

    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }
    
    public void setState(ServerState state) {
        this.state = state;
    }

    public ServerState getState() {
        return state;
    }

    public Range getRange() {
        return range;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    @Override
    public String toString() {
        if(range == null)
            return ipaddress.getHostAddress() + "," + port + "," + id;
        else
            return ipaddress.getHostAddress() + "," + port + "," + id + "," + range.startRange + "," + range.endRange;
    }
}
