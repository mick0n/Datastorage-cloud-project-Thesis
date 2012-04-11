/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.storage;

import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.tools.MetaDataComposer;
import com.mnorrman.datastorageproject.tools.RawMetaDataPrinter;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

/**
 *
 * @author Mikael
 */
public class BackStorage {

    public static final int BlOCK_SIZE = 131072;
    public static String dataLocation = "";
    private RandomAccessFile fileConnection;

    public BackStorage() {
    }

    public BackStorage initialize() throws IOException {
        //File file = new File(dataLocation + File.separator + "datafile_");
        File file = new File("datafile_");
        if (!file.exists()) {
            file.createNewFile();
        }

        fileConnection = new RandomAccessFile(file, "rwd");

        Logger.getLogger("b-log").info("BackStorage initialized");

        return this;
    }

    public LinkedList<IndexedDataObject> reindexData() {
        Logger.getLogger("b-log").info("Begin reindex of data");

        LinkedList<IndexedDataObject> list = new LinkedList<IndexedDataObject>();

        try {
            FileChannel channel = fileConnection.getChannel();
            if (channel.size() <= 0) {
                Logger.getLogger("b-log").info("No data in file, jumping out");
                return list;
            }

            long channelSize = channel.size() - 1, position = 0;
            ByteBuffer data = ByteBuffer.allocateDirect(512);
            channel.position(position);
            while (channelSize - channel.position() > 0) {
                data.clear(); //Clear it first
                channel.read(data);
                data.rewind();

                RawMetaDataPrinter.print(data);
                System.out.println("");

                IndexedDataObject ido = MetaDataComposer.compose(data, position);
                list.add(ido);
                position += (512 + ido.getLength());
                channel.position(position);
            }
        } catch (IOException e) {
            Logger.getLogger("b-log").log(Level.SEVERE, "Error in reindexing", e);
        }
        Logger.getLogger("b-log").info("Reindexing complete");
        return list;
    }

    public boolean performIntegrityCheck() {
        Logger.getLogger("b-log").info("Starting integrity check");

        long position = 0;
        ByteBuffer meta = ByteBuffer.allocate(512);
        IndexedDataObject temp;
        try {
            FileChannel channel = fileConnection.getChannel();
            if (channel.size() <= 0) {
                return true;
            }
            channel.position(position);
            do {
                meta.clear();
                channel.read(meta);
                temp = MetaDataComposer.compose(meta, position);

                //Jump past metadata and create new CRC32-object
                channel.position(position + 512);
                CRC32 crc = new CRC32();

                int readBytes = 0;
                long totalBytes = temp.getLength();
                ByteBuffer buffer = ByteBuffer.allocate(BlOCK_SIZE);

                while (totalBytes > 0) {
                    buffer.clear();
                    readBytes = channel.read(buffer);
                    buffer.flip();

                    if (readBytes >= totalBytes) {
                        crc.update(buffer.array(), 0, (int) (totalBytes));
                    } else {
                        crc.update(buffer.array(), 0, readBytes);
                    }

                    totalBytes -= readBytes;
                    if (totalBytes <= 0) {
                        break;
                    }
                }

                long checksumDataFromData = crc.getValue();

                if (temp.getChecksum() != checksumDataFromData) {
                    Logger.getLogger("b-log").warning("Integrity check failed");
                    return false;
                }

                position += (512 + temp.getLength());
                channel.position(position);
            } while (channel.size() - channel.position() > 0);
        } catch (IOException e) {
            Logger.getLogger("b-log").log(Level.SEVERE, "error in integrity check", e);
        }

        Logger.getLogger("b-log").info("Integrity check completed without warnings");
        return true;
    }

    public FileChannel getChannel() {
        return fileConnection.getChannel();
    }
}
