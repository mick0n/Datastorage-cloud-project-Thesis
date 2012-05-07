
package com.mnorrman.datastorageproject.network.jobs;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.objects.UnindexedDataObject;
import com.mnorrman.datastorageproject.storage.DataProcessor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.zip.CRC32;

/**
 *
 * @author Mikael Norrman
 */
public class StoreDataJob extends AbstractJob{
    
    private FileChannel output;
    private UnindexedDataObject udo;
    private CRC32 crc;
    
    private DataProcessor dataProcessor;
    
    public StoreDataJob(UnindexedDataObject udo, DataProcessor dp) throws IOException{
        super(null);
        this.udo = udo;
        this.dataProcessor = dp;
        
        output = new FileOutputStream(udo.getTempFile()).getChannel();                
        crc = new CRC32();
    }

    @Override
    public boolean update(SocketChannel s, ByteBuffer buffer) {
        setFinished(true);
        return true;
    }
    
    public void update(ByteBuffer data) throws IOException{
        if(data.position() != 0)
            data.flip();
        output.write(data);
        crc.update(data.array(), 0, data.limit());
        
        if(output.size() == udo.getLength()){
            setFinished(true);
            udo.setChecksum(crc.getValue());
            output.close();
            Main.localIndex.insert(dataProcessor.storeData(udo));
        }else if(output.size() > udo.getLength()){
            System.out.println("Huge error occured, too much data!");
        }
    }
    
    public void finishDataProcessor(){
        dataProcessor.finish();
    }
}
