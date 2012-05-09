/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.network.MasterNode;
import com.mnorrman.datastorageproject.network.Protocol;
import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.tools.MetaDataComposer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 *
 * @author Mikael
 */
public class SyncLocalIndexJob extends AbstractJob{

    public static final byte SYNC_ADD = 0x01;
    public static final byte SYNC_UPD = 0x02;
    public static final byte SYNC_DEL = 0x03;
    public static final byte LAST_INDEX = 0x10;
    public static final byte NOT_LAST_INDEX = 0x11;
    
    private LinkedList<ArrayList<IndexedDataObject>> localIndexCopy;
    private int outerIndex = 0, innerIndex = 0;
    private IndexedDataObject ido;
    private byte command;
    
    public SyncLocalIndexJob(String owner) {
        super(owner);
        this.localIndexCopy = Main.localIndex.cloneData();
        this.command = SYNC_UPD;
        System.out.println("Job created");
    }
    
    public SyncLocalIndexJob(String owner, IndexedDataObject ido, boolean isRemove){
        super(owner);
        this.ido = ido;
        if(!isRemove)
            this.command = SYNC_ADD;
        else
            this.command = SYNC_DEL;
    }

    @Override
    public boolean update(SocketChannel s, ByteBuffer buffer) throws IOException {
        buffer.clear();
        if(ido != null){
            buffer.put(Main.ID);
            buffer.putInt(514);
            buffer.put(Protocol.SYNC_LOCAL_INDEX.getValue());
            buffer.put(command);
            buffer.put(MetaDataComposer.decompose(ido));
            buffer.put(LAST_INDEX);
            buffer.rewind();
            s.write(buffer);
            buffer.clear();
            setFinished(true);
            return true;
        }else{
            buffer.put(Main.ID);
            buffer.putInt(8176);
            buffer.put(Protocol.SYNC_LOCAL_INDEX.getValue());
            int counter = 15;
            while(counter > 0){
                buffer.put(command);
                buffer.put(MetaDataComposer.decompose(localIndexCopy.get(outerIndex).get(innerIndex)));
                if(innerIndex+1 >= localIndexCopy.get(outerIndex).size()){
                    innerIndex = 0;
                    outerIndex++;
                }else{
                    innerIndex++;
                }
                if(outerIndex == localIndexCopy.size()){
                    buffer.put(LAST_INDEX);
                    setFinished(true);
                    break;
                }
                counter--;
                
                if(counter == 0){
                    buffer.put(LAST_INDEX);
                }else{
                    buffer.put(NOT_LAST_INDEX);
                }
            }
            buffer.rewind();
            int writtenBytes = 0;
            while(writtenBytes < MasterNode.NETWORK_BLOCK_SIZE){
                writtenBytes += s.write(buffer);
            }
            buffer.clear();
            if(isFinished())
                return true;
            else
                return false;
        }
    }    
}
