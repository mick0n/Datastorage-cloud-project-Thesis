/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.ServerState;
import com.mnorrman.datastorageproject.network.Protocol;
import com.mnorrman.datastorageproject.objects.GloballyIndexedDataObject;
import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.objects.ServerNode;
import com.mnorrman.datastorageproject.tools.HexConverter;
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
    
    private ServerNode from;
    
    public SyncLocalIndexJob() {
        super();
        this.localIndexCopy = Main.localIndex.cloneData();
        this.command = SYNC_UPD;
        System.out.println("Job created");
    }
    
    public SyncLocalIndexJob(IndexedDataObject ido, boolean isRemove){
        super();
        this.ido = ido;
        if(!isRemove)
            this.command = SYNC_ADD;
        else
            this.command = SYNC_DEL;
    }
    
    public SyncLocalIndexJob(String ID, ServerNode from){
        super(ID);
        this.from = from;
    }

    @Override
    public boolean readOperation(ByteBuffer buffer) throws IOException {
        ByteBuffer DOBuffer; //Special dataobject-buffer
        int counter = 0;
        while(counter < 15){
            byte command = buffer.get();
            System.out.println("Command was 0x" + HexConverter.toHex(new byte[]{ command }));
            DOBuffer = buffer.slice();
            IndexedDataObject ido = MetaDataComposer.compose(DOBuffer);
//            Main.globalIndex.insert(new GloballyIndexedDataObject(ido, from));
            buffer.position(buffer.position() + 512);
            System.out.println("IDO: " + ido.toString());
            if(buffer.get() == SyncLocalIndexJob.LAST_INDEX)
                break;
            counter++;
        }
        buffer.clear();
        setFinished(true);
        return false;
    }

    @Override
    public boolean writeOperation(SocketChannel s, ByteBuffer buffer) throws IOException {
        buffer.clear();
        Main.state = ServerState.SYNCHRONIZING;
        if(ido != null){
            buffer.put(HexConverter.toByte(Main.ID));
            buffer.putInt(514);
            buffer.put(HexConverter.toByte(getJobID()));
//            buffer.put(Protocol.SYNC_LOCAL_INDEX.getValue());
            buffer.put(command);
            buffer.put(MetaDataComposer.decompose(ido));
            buffer.put(LAST_INDEX);
            buffer.rewind();
            while(buffer.hasRemaining())
                s.write(buffer);
            buffer.clear();
            setFinished(true);
            Main.state = ServerState.IDLE;
            buffer.clear();
            return true;
        }else{
            if(localIndexCopy.isEmpty()){
                setFinished(true);
                Main.state = ServerState.IDLE;
                return true;
            }
            buffer.put(HexConverter.toByte(Main.ID));
            buffer.putInt(8176);
            buffer.put(HexConverter.toByte(getJobID()));
//            buffer.put(Protocol.SYNC_LOCAL_INDEX.getValue());
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
                    Main.state = ServerState.IDLE;
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
            while(buffer.hasRemaining())
                s.write(buffer);
            buffer.clear();
            if(isFinished())
                return true;
            else
                return false;
        }
    }    
}
