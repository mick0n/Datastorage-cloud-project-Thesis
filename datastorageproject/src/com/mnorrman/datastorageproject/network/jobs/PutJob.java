/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.network.ExternalTrafficContext;
import com.mnorrman.datastorageproject.network.ExternalTrafficHandler;
import com.mnorrman.datastorageproject.network.Protocol;
import com.mnorrman.datastorageproject.objects.ServerNode;
import com.mnorrman.datastorageproject.objects.UnindexedDataObject;
import com.mnorrman.datastorageproject.storage.DataProcessor;
import com.mnorrman.datastorageproject.storage.StoreDataRunnable;
import com.mnorrman.datastorageproject.tools.Hash;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.zip.CRC32;

/**
 *
 * @author Mikael
 */
public class PutJob extends ExternalJob {

    private ExternalTrafficHandler eth;
    private FileChannel output;
    private UnindexedDataObject udo;
    private CRC32 crc;
    private boolean gotUdo;
    private DataProcessor dataProcessor;

    public PutJob(ExternalTrafficContext etc, ExternalTrafficHandler eth, DataProcessor dp) {
        super(etc);
        this.eth = eth;
        this.dataProcessor = dp;
        crc = new CRC32();
        this.gotUdo = false;
    }

    @Override
    public boolean readOperation(ByteBuffer buffer) throws IOException {
        if (udo == null) {
            byte[] stringbytes = new byte[128];
            buffer.get(stringbytes);
            String column = new String(stringbytes, "UTF-8").trim();

            stringbytes = new byte[128];
            buffer.get(stringbytes);
            String row = new String(stringbytes, "UTF-8").trim();

            stringbytes = new byte[128];
            buffer.get(stringbytes);
            String owner = new String(stringbytes, "UTF-8").trim();

            long length = buffer.getLong();

            udo = new UnindexedDataObject(new File("putjob" + Math.random()), column, row, owner, length);

            gotUdo = true;
            buffer.clear();
            return true;
        }

        if(output == null)
            output = new FileOutputStream(udo.getTempFile()).getChannel();

        int currPos = buffer.position();

        if (buffer.hasRemaining()) {
            if (buffer.remaining() > (int) (udo.getLength() - output.size())) {
                buffer.limit(buffer.position() + (int) (udo.getLength() - output.size()));
            }
            output.write(buffer);
            buffer.position(currPos);
            byte[] crcBytes = new byte[buffer.limit() - currPos];
            buffer.get(crcBytes);
            crc.update(crcBytes);
        }

        if (output.size() == udo.getLength()) {
            output.close();
            udo.setChecksum(crc.getValue());
            Main.pool.submit(new StoreDataRunnable(dataProcessor, udo));
//            Main.localIndex.insert(dataProcessor.storeData(udo));
            setFinished(true);
        } else if (output.size() > udo.getLength()) {
            System.out.println("Ärror");
            buffer.clear();
            setFinished(true);
        }

        buffer.clear();
        return false;
    }

    @Override
    public boolean writeOperation(SocketChannel s, ByteBuffer buffer) throws IOException {
        if (udo != null) {
            long hashValue = Hash.get(udo.getColname());
            if (eth.getInternalTrafficHandler().isMaster()) {
                ServerNode rangeOwner = eth.getInternalTrafficHandler().getMasterProperties().getTree().findRange(hashValue);
                if (rangeOwner.getId().equals(eth.getInternalTrafficHandler().getMasterProperties().getTree().getRoot().getServerNode().getId())) {
                    buffer.put(Protocol.OK.getValue());
                    buffer.flip();
                    s.write(buffer);
                } else {
                    buffer.put(Protocol.REDIRECT_CLIENT.getValue());
                    buffer.put(rangeOwner.getIpaddress().getAddress());
                    buffer.putInt(rangeOwner.getExternalport());
//                    buffer.putInt(8998);
                    buffer.flip();
                    s.write(buffer);
                    setFinished(true);
                }
            } else {
                buffer.put(Protocol.OK.getValue());
                buffer.flip();
                s.write(buffer);
            }
        }
        buffer.clear();
        return true;
    }

    public boolean gotUdo() {
        return gotUdo;
    }
}
