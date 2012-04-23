
package com.mnorrman.datastorageproject.storage;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.tools.MetaDataComposer;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.zip.CRC32;

/**
 *
 * @author Mikael Norrman
 */
public class BackStorage {

    /**
     * Through simple test decided to be the most suitable value for file
     * transfers.
     */
    public static final int BlOCK_SIZE = 131072;
    
    private RandomAccessFile fileConnection;

    /**
     * Creates new instance of BackStorage
     */
    public BackStorage() {
    }

    /**
     * Initializes the backStorage values. Seperated from constructor because
     * it must be explicitly called. 
     * @return this BackStorage
     * @throws IOException 
     */
    public BackStorage initialize() throws IOException {
        File file = new File(Main.properties.getValue("dataPath") + File.separator + "datafile_");
        if (!file.exists()) {
            file.createNewFile();
        }

        /**
         *  "r" 	Open for reading only. Invoking any of the write methods of the resulting object will cause an IOException to be thrown.
         *  "rw" 	Open for reading and writing. If the file does not already exist then an attempt will be made to create it.
         *  "rws" 	Open for reading and writing, as with "rw", and also require that every update to the file's content or metadata be written synchronously to the underlying storage device.
         *  "rwd"   	Open for reading and writing, as with "rw", and also require that every update to the file's content be written synchronously to the underlying storage device. 
         */
        fileConnection = new RandomAccessFile(file, "rwd");

        LogTool.log("Backstorage initialized", LogTool.INFO);

        return this;
    }

    /**
     * Read through datafile and return new list of indexedDataObjects.
     * Should be used if no localIndex is present or the current localIndex is
     * damaged.
     * 
     * Read the metadata for each datapart and stores the values in a list.
     * This list is then easily inserted into a localIndex-object.
     * 
     * @return LinkedList containing all indexedDataObjects found in the datafile.
     */
    public LinkedList<IndexedDataObject> reindexData() {
        LogTool.log("Begin reindex of data", LogTool.INFO);

        LinkedList<IndexedDataObject> list = new LinkedList<IndexedDataObject>();

        try {
            FileChannel channel = fileConnection.getChannel();
            
            //If the datafile is empty then there's no data to index.
            if (channel.size() <= 0) {
                LogTool.log("No data in file, jumping out", LogTool.WARNING);
                return list;
            }

            //Set counter variables
            long channelSize = channel.size() - 1, position = 0;
            
            ByteBuffer data = ByteBuffer.allocateDirect(512);
            channel.position(position);
            
            while (channelSize - channel.position() > 0) {
                data.clear();
                channel.read(data);
                data.rewind();

                //RawMetaDataPrinter.print(data); //For troubleshooting

                IndexedDataObject ido = MetaDataComposer.compose(data, position);
                list.add(ido);
                
                //Jump past this datapart (Metadata + binary data) and update
                //position in channel.
                position += (512 + ido.getLength());
                channel.position(position);
            }
        } catch (IOException e) {
            LogTool.log(e, LogTool.CRITICAL);
        }
        LogTool.log("Reindexing complete", LogTool.INFO);
        return list;
    }

    /**
     * Check every piece of data in the backstorage for errors. This method
     * will read the checksum of a file from its meta data, read through the
     * file to get a new checksum and the compare the two to see if they match.
     * 
     * This method will not take any actions to repair any damage however it
     * will return a boolean value to tell the caller whether it was successful
     * or not.
     * @return true if integrityCheck completed successfully or false if an
     * error was encountered.
     */
    public boolean performIntegrityCheck() {
        LogTool.log("Starting integrity check", LogTool.INFO);

        long position = 0; //Position in file
        ByteBuffer meta = ByteBuffer.allocate(512);
        IndexedDataObject temp;
        try {
            FileChannel channel = fileConnection.getChannel();
            
            //If there is no data we don't have to do anything
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

                //Start reading the filedata
                while (totalBytes > 0) {
                    buffer.clear();
                    readBytes = channel.read(buffer);
                    buffer.flip();

                    //If remaining filesize is less than the read bytes then
                    //we must limit the number of bytes used in the crc32.
                    if (readBytes >= totalBytes) {
                        crc.update(buffer.array(), 0, (int) (totalBytes));
                    } else {
                        crc.update(buffer.array(), 0, readBytes);
                    }

                    totalBytes -= readBytes;
                    
                    //An extra security measure to avoid reading to much data.
                    if (totalBytes <= 0) {
                        break;
                    }
                }

                long checksumDataFromData = crc.getValue();

                //Perform comparison
                if (temp.getChecksum() != checksumDataFromData) {
                    LogTool.log("Integrity check failed", LogTool.WARNING);
                    return false;
                }

                //Jump past this datapart (Metadata + binary data) and update
                //position in channel.
                position += (512 + temp.getLength());
                channel.position(position);
                
            } while (channel.size() - channel.position() > 0);
        } catch (IOException e) {
            LogTool.log(e, LogTool.CRITICAL);
        }

        LogTool.log("Integrity check completed without warnings", LogTool.INFO);
        return true;
    }

    /**
     * Get a new FileChannel from this RandomAccessFile-object.
     * @return new instance of FileChannel taken from this RandomAccessFile
     */
    public FileChannel getChannel() {
        return fileConnection.getChannel();
    }
}
