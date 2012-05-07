package com.mnorrman.datastorageproject.storage;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.ServerState;
import com.mnorrman.datastorageproject.index.IndexPersistence;
import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.tools.MetaDataComposer;
import com.mnorrman.datastorageproject.tools.RawMetaDataPrinter;
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
    protected LinkedList<DataTicket> activeProcesses;

    /**
     * Creates new instance of BackStorage
     */
    public BackStorage() {
    }

    /**
     * Initializes the backStorage values. Seperated from constructor because it
     * must be explicitly called.
     *
     * @return this BackStorage
     * @throws IOException
     */
    public BackStorage initialize() throws IOException {
        File file = new File(Main.properties.getValue("dataPath") + File.separator + "datafile_");
        if (!file.exists()) {
            file.createNewFile();
        }

        /**
         * "r" Open for reading only. Invoking any of the write methods of the
         * resulting object will cause an IOException to be thrown. "rw" Open
         * for reading and writing. If the file does not already exist then an
         * attempt will be made to create it. "rws" Open for reading and
         * writing, as with "rw", and also require that every update to the
         * file's content or metadata be written synchronously to the underlying
         * storage device. "rwd" Open for reading and writing, as with "rw", and
         * also require that every update to the file's content be written
         * synchronously to the underlying storage device.
         */
        fileConnection = new RandomAccessFile(file, "rwd");
        activeProcesses = new LinkedList<DataTicket>();

        Main.timer.scheduleAtFixedRate(new BackStorageActiveConnectionCleanerTimerTask(this), 60 * 1000, 120 * 1000);

        LogTool.log("Backstorage initialized", LogTool.INFO);

        return this;
    }

    /**
     * Read through datafile and return new list of indexedDataObjects. Should
     * be used if no localIndex is present or the current localIndex is damaged.
     *
     * Read the metadata for each datapart and stores the values in a list. This
     * list is then easily inserted into a localIndex-object.
     *
     * @return LinkedList containing all indexedDataObjects found in the
     * datafile.
     */
    public LinkedList<IndexedDataObject> reindexData() {
        Main.state = ServerState.INDEXING;
        LogTool.log("Begin reindex of data", LogTool.INFO);

        LinkedList<IndexedDataObject> list = new LinkedList<IndexedDataObject>();

        try {
            FileChannel channel = fileConnection.getChannel();

            //If the datafile is empty then there's no data to index.
            if (channel.size() <= 0) {
                LogTool.log("No data in file, jumping out", LogTool.WARNING);
                Main.state = ServerState.IDLE;
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
        Main.state = ServerState.IDLE;
        return list;
    }

    /**
     * Check every piece of data in the backstorage for errors. This method will
     * read the checksum of a file from its meta data, read through the file to
     * get a new checksum and the compare the two to see if they match.
     *
     * This method will not take any actions to repair any damage however it
     * will return a boolean value to tell the caller whether it was successful
     * or not.
     *
     * @return true if integrityCheck completed successfully or false if an
     * error was encountered.
     */
    public boolean performIntegrityCheck() {
        Main.state = ServerState.CHKINTEG;
        LogTool.log("Starting integrity check", LogTool.INFO);

        boolean returnValue = true;
        long position = 0; //Position in file
        ByteBuffer meta = ByteBuffer.allocate(512);
        IndexedDataObject temp;
        try {
            FileChannel channel = fileConnection.getChannel();

            //If there is no data we don't have to do anything
            if (channel.size() <= 0) {
                Main.state = ServerState.IDLE;
                return returnValue;
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
                    LogTool.log("Integrity check failed @", LogTool.WARNING);
                    LogTool.log("" + temp.getClearText(), LogTool.WARNING);
                    LogTool.log("Reason: Checksums don't match: " + temp.getChecksum() + " expected, got " + checksumDataFromData, LogTool.WARNING);
                    markForCleanUp(channel, temp);
                    //RawMetaDataPrinter.print(MetaDataComposer.decompose(temp));
                    returnValue = false;
                }

                //Jump past this datapart (Metadata + binary data) and update
                //position in channel.
                position += (512 + temp.getLength());
                channel.position(position);

            } while (channel.size() - channel.position() > 0);
        } catch (IOException e) {
            LogTool.log(e, LogTool.CRITICAL);
        }

        if(returnValue)
            LogTool.log("Integrity check completed without warnings", LogTool.INFO);
        else
            Main.localIndex.clear().insertAll(reindexData());
        Main.state = ServerState.IDLE;
        return returnValue;
    }

    private void markForCleanUp(FileChannel channel, IndexedDataObject ido) throws IOException {
        long currentPos = channel.position();
        channel.position(ido.getOffset() + 511);
        ByteBuffer setFlagBuffer = ByteBuffer.allocate(1);
        setFlagBuffer.put((byte)1);
        setFlagBuffer.flip();
        channel.write(setFlagBuffer);
        channel.position(currentPos);
    }

    public boolean clean() {
        Main.state = ServerState.CLEANING;
        LogTool.log("Starting to clean the backstorage", LogTool.INFO);

        long position = 0; //Position in file
        ByteBuffer meta = ByteBuffer.allocate(512);
        IndexedDataObject temp;
        try {
            FileChannel channel = fileConnection.getChannel();

            //If there is no data we don't have to do anything
            if (channel.size() <= 0) {
                Main.state = ServerState.IDLE;
                return true;
            }

            channel.position(position);
            do {
                meta.clear();
                channel.read(meta);
                temp = MetaDataComposer.compose(meta, position);

                if(temp.isCleanupFlagged()){
                    LogTool.log("Cleaning out \"" + temp.getClearText() + "\"...", LogTool.INFO);
                    long amount = temp.getLength() + 512L;

                    channel.position(temp.getOffset() + amount);
                    channel.transferFrom(channel, temp.getOffset(), channel.size() - (temp.getOffset() + amount));
                    channel.truncate(channel.size() - amount);
                    
                }else{
                    position += (512 + temp.getLength());
                }

                channel.position(position);

            } while (channel.size() - channel.position() > 0);
        } catch (IOException e) {
            LogTool.log(e, LogTool.CRITICAL);
        }

        LogTool.log("Done cleaning backstorage", LogTool.INFO);
        Main.state = ServerState.IDLE;
        return true;
    }

    /**
     * Get new DataTicket. DataTickets are used to show if there are current
     * operations taking place. Used to avoid errors in filetransfers when
     * shutting down this server.
     *
     * @return new DataTicket instance.
     */
    public DataTicket getTicket() {
        if (Main.state != ServerState.CLOSING) {
            DataTicket dt = new DataTicket(fileConnection.getChannel());
            activeProcesses.add(dt);
            if (Main.state != ServerState.RUNNING) {
                Main.state = ServerState.RUNNING;
            }
            return dt;
        } else {
            return null;
        }
    }

    /**
     * Get a new FileChannel from this RandomAccessFile-object.
     *
     * @return new instance of FileChannel taken from this RandomAccessFile
     * @deprecated Replaced with getTicket()-method.
     */
    public FileChannel getChannel(DataTicket ticket) {
        return fileConnection.getChannel();
    }

    /**
     * Closes the connection to the datafile
     */
    public void close() throws IOException {
        Main.state = ServerState.CLOSING;
        while (!activeProcesses.isEmpty()) {
            Main.pool.submit(new BackStorageActiveConnectionCleaner(activeProcesses.iterator()));
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                LogTool.log(e, LogTool.CRITICAL);
            }
        }
        fileConnection.close();
    }
    
    public static void main(String[] args){
        try{
            RandomAccessFile raf = new RandomAccessFile(new File("D:\\Skola\\Exjobb\\Datastorage-cloud-project-Thesis\\datastorageproject\\datafile_"), "rwd");
            FileChannel c = raf.getChannel();

            c.position(9900);
            System.out.println(c.size());
            ByteBuffer b = ByteBuffer.allocate(4);
            b.put(new byte[]{ 0, 45, 2, 3});
            b.flip();
            c.force(true);
            while(c.write(b) == 0){
                System.out.println("Inte än");
            }
            c.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
