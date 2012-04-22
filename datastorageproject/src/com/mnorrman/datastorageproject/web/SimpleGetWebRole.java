/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.web;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.network.jobs.GetDataJob;
import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.storage.BackStorage;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mikael
 */
public class SimpleGetWebRole implements HttpHandler{

    private Main reference;
    
    public SimpleGetWebRole(Main m){
        this.reference = m;
    }
    
    public void handle(HttpExchange he) throws IOException {
        String column = "", row = "", version = "";
        IndexedDataObject ido = null;
        
        String requestMethod = he.getRequestMethod();
        if (requestMethod.equalsIgnoreCase("GET")) {
            System.out.println("Variables? " + he.getRequestURI().getQuery());
            
            StringTokenizer st = new StringTokenizer(he.getRequestURI().getQuery(), "&");
            while(st.hasMoreTokens()){
                String token = st.nextToken();
                if(token.substring(0, token.indexOf("=")).equalsIgnoreCase("column")){
                    column = token.substring(token.indexOf("=")+1);
                    continue;
                }
                if(token.substring(0, token.indexOf("=")).equalsIgnoreCase("row")){
                    row = token.substring(token.indexOf("=")+1);
                    continue;
                }
                if(token.substring(0, token.indexOf("=")).equalsIgnoreCase("version")){
                    version = token.substring(token.indexOf("=")+1);
                    continue;
                }
            }
            
            if(!column.equals("") && !row.equals("")){
                ido = Main.localIndex.get(column, row);
            }
            
            System.out.println(ido);
            
            Headers responseHeaders = he.getResponseHeaders();
            if(ido == null){
                responseHeaders.set("Content-Type", "text/plain");
                he.sendResponseHeaders(404, 0);
                he.close();
            }else{
                responseHeaders.set("Content-Type", "application/octet-stream");
                responseHeaders.set("Content-Length", "" + ido.getLength());
                responseHeaders.set("Content-Disposition", "attachment; filename=" + row);
                responseHeaders.set("Cache-Control", "no-cache");
                he.sendResponseHeaders(200, 0);
                
                GetDataJob job = new GetDataJob(ido, reference.getNewDataProcessor());
                ByteBuffer largebuf = ByteBuffer.allocate(BackStorage.BlOCK_SIZE);
                try{
                    OutputStream os = he.getResponseBody();
                    
                    while(!job.isFinished()){
                        int readBytes = job.getDataProcessor().retrieveData(largebuf, job.getCurrentPosition(), job.getIndexedDataObject());
                        job.update(job.getCurrentPosition() + readBytes);
                        largebuf.flip();
                        os.write(largebuf.array());
                        largebuf.clear();
                    }
                    System.out.println("Done: " + job.getCurrentPosition());
                    os.flush();
                    os.close();
                }catch(IOException e){
                    Logger.getLogger("b-log").log(Level.SEVERE, "An error occured!", e);
                }
            }
        }
    }
    
}
