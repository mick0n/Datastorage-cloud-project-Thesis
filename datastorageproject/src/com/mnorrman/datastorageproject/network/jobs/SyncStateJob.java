/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Mikael
 */
public class SyncStateJob extends AbstractJob{

    @Override
    public boolean update(SocketChannel s, ByteBuffer buffer) throws IOException {
        return true;
    }
    
}
