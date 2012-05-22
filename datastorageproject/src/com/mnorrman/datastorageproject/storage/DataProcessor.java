
package com.mnorrman.datastorageproject.storage;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.objects.UnindexedDataObject;
import com.mnorrman.datastorageproject.tools.MetaDataComposer;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mikael Norrman
 */
public class DataProcessor {
    
    private FileChannel dataChannel;
    private DataTicket ticket;
    
    
    /**
     * Constructor. Requires a new FileChannel-object from the backstorage.
     * @param channel FileChannel from the backstorage.
     * @deprecated Replaced by DataProcessor(DataTicket ticket)-constructor.
     */
    public DataProcessor(FileChannel channel){
        this.dataChannel = channel;
    }
    
    /**
     * Creates new DataProcessor instance. Takes a DataTicket as argument.
     * @param ticket DataTicket received from BackStorage class.
     */
    public DataProcessor(DataTicket ticket){
        this.ticket = ticket;
    }
    
    /**
     * Retrieve data in a non-blocking environment. When using this method 
     * the finish()-method must be called manually when transfer is complete.
     * @param buffer Use pre-allocated buffer
     * @param position The position in-file that we are going to start at.
     * @param ido The indexedDataObject belonging to the data we are fetching.
     * @return amount of bytes read. May return -1.
     */
    public int retrieveData(ByteBuffer buffer, long position, IndexedDataObject ido){
        try{
            if(buffer.position() != 0)
                buffer.clear();
            
            ticket.getChannel().position(((ido.getOffset() + 512L) + position));
            
            if(ido.getLength() - position < buffer.capacity() ){
                buffer.limit((int)(ido.getLength() - position));
            }
            
            return ticket.getChannel().read(buffer);
        }catch(IOException e){
            LogTool.log(e, LogTool.CRITICAL);
            Logger.getLogger("b-log").log(Level.SEVERE, "An error occured when retrieving data!", e);
        }
        return -1;
    }
    
    /**
     * A method for retrieving data from our backStorage. It does not return any
     * data, it simply returns a boolean value telling if the operation was 
     * successful or not. 
     * @param os An outputstream to which the data will be written.
     * @param ido The indexedDataObject that contains the metadata for the data.
     * @return True if everything went as expected, otherwise false.
     */
    public boolean retrieveData(OutputStream os, IndexedDataObject ido){
        try{
            ByteBuffer buffer = ByteBuffer.allocate(BackStorage.BlOCK_SIZE);
            ticket.getChannel().position(ido.getOffset() + 512);
            
            int readBytes = 0;
            long totalBytes = ido.getLength();

            while(totalBytes > 0){
                buffer.clear();
                readBytes = ticket.getChannel().read(buffer);
                buffer.flip();
                
                if(readBytes >= totalBytes){
                    os.write(buffer.array(), 0, (int)(totalBytes));
                }else{
                    os.write(buffer.array(), 0, readBytes);
                }
                
                totalBytes -= readBytes;
                if(totalBytes <= 0)
                    break;
            }
            os.flush();
            finish();
            return true;
        }catch(IOException e){
            Logger.getLogger("b-log").log(Level.SEVERE, "An error occured when retrieving data!", e);
        }
        finish();
        return false;
    }

    public IndexedDataObject storeData(UnindexedDataObject udo){

        long filesizeBeforeOperation = -1; 
        
        
        
        try{
            ByteBuffer bbb = MetaDataComposer.decompose(udo);
            bbb.position(256);
            long newVersion = bbb.getLong();
            bbb.position(0);
            
            
            FileLock fl = ticket.getChannel().lock();

            long newOffset = ticket.getChannel().size();
                        
            filesizeBeforeOperation = newOffset;
            
            ticket.getChannel().position(newOffset);
            ticket.getChannel().write(bbb);
            
            //This part makes sure that the full amount of bytes are pre-
            //allocated, thus making it easier to rollback the changes.
            //(Since we still know how much data to remove)
            long tempPos = ticket.getChannel().position();
            ByteBuffer voidbuf = ByteBuffer.allocate(1);
            voidbuf.put((byte)0);
            voidbuf.flip();
            if(tempPos+(udo.getLength()-1) < 0)
                ticket.getChannel().position(0);
            else
                ticket.getChannel().position(tempPos+(udo.getLength()-1));
            ticket.getChannel().write(voidbuf);
            ticket.getChannel().position(tempPos);
            
            FileInputStream fis = new FileInputStream(udo.getTempFile());
            FileChannel fc = fis.getChannel();
            
            //Transfer all data from the temporary file into the backstorage.
            ticket.getChannel().transferFrom(fc, ticket.getChannel().position(), udo.getTempFile().length());
            fc.close();
            
            //Remove the temporary file.
            udo.removeTempFile();
            
            fl.release();
            return new IndexedDataObject(udo, newOffset, newVersion);
            
        }catch(IOException e){
            Logger.getLogger("b-log").log(Level.SEVERE, "An error occured when storing data!", e);
            try{
                ticket.getChannel().truncate(filesizeBeforeOperation);
            }catch(IOException e2){
                Logger.getLogger("b-log").log(Level.SEVERE, "An error occured when rolling back changes!", e);
            }
        }
        return null;
    }
    
    public boolean removeData(IndexedDataObject ido){
        long amount = ido.getLength() + 512L;
        
        try{
            ticket.getChannel().position(ido.getOffset() + amount);
            System.out.println("Length of file: " + ticket.getChannel().size());
            System.out.println("Amount = " + amount);
            System.out.println("How much we want to transfer: " + ido.getOffset() + " to " + (ticket.getChannel().size()-(ido.getOffset()+amount)));
            ticket.getChannel().transferFrom(ticket.getChannel(), ido.getOffset(), ticket.getChannel().size()-(ido.getOffset()+amount));
            ticket.getChannel().truncate(ticket.getChannel().size()-amount);
            finish();
            return true;
        }catch(IOException e){
            Logger.getLogger("b-log").log(Level.SEVERE, "Error when removing data", e);
        }
        finish();
        return false;
    }
    
    /**
     * Removes several indexedDataObjects. Designed to try removal of all items
     * in the list, regardless if anyone fails.
     * Practical to use when removing a table cell with multiple versions.
     * @param idos List of IndexedDataObjects
     * @return true if all IDO's were removed successfully. False if any of them
     * failed to be removed.
     */
    public boolean removeData(List<IndexedDataObject> idos){
        boolean value = true;
        for(IndexedDataObject ido : idos){
            if(value)
                value = removeData(ido);
        }
        return value;
    }
    
    /**
     * Tell the backstorage that we are done with this ticket.
     */
    public void finish(){
        this.ticket.finish();
    }
}
