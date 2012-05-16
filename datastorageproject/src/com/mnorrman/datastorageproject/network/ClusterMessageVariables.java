/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.tools.HexConverter;
import java.nio.ByteBuffer;

/**
 *
 * @author Mikael
 */
public class ClusterMessageVariables {
    
    private String from;
    private String jobID;
    private int length;
    
    public ClusterMessageVariables(){
    }
    
    public ClusterMessageVariables(ByteBuffer buffer){
        //Get the sender ID
        from = HexConverter.toHex(buffer.getInt());

        //Get the length of the data
        length = buffer.getInt();

        //Get the jobID and transform into hexstring.
        jobID = HexConverter.toHex(buffer.getInt());
    }

    public String getFrom() {
        return from;
    }

    public String getJobID() {
        return jobID;
    }

    public int getLength() {
        return length;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setJobID(String jobID) {
        this.jobID = jobID;
    }

    public void setLength(int length) {
        this.length = length;
    }
    
    @Override
    public String toString() {
        return "MessageVariables from 0x" + from + " reads: length: " + length + ", jobID: 0x" + jobID;
    }
}
