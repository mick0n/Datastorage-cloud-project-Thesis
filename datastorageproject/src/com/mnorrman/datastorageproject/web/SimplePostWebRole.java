/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.web;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.index.IndexPersistence;
import com.mnorrman.datastorageproject.network.jobs.StoreDataJob;
import com.mnorrman.datastorageproject.objects.UnindexedDataObject;
import com.mnorrman.datastorageproject.storage.BackStorage;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author Mikael
 */
public class SimplePostWebRole implements HttpHandler{

    private Main reference;
    
    public SimplePostWebRole(Main m){
        this.reference = m;
    }
    
    public void handle(HttpExchange he) throws IOException {
        
        String requestMethod = he.getRequestMethod();
        if (requestMethod.equalsIgnoreCase("GET")) {
            Headers responseHeaders = he.getResponseHeaders();
            responseHeaders.set("Content-Type", "text/HTML");
            he.sendResponseHeaders(200, 0);

            StringBuilder webString = new StringBuilder();
            webString.append("<html>\r\n");
            webString.append("<head>\r\n");
            webString.append("<title>Datastorage thesis - upload</title>\r\n");
            webString.append("<style type=\"text/css\">\r\n");
            webString.append("h1{ font-weight:bold; font-size:16px; font-family: arial; color: #06189E; }\r\n");
            webString.append("#box{width:400px; height: 250px; -moz-border-radius: 15px; border-radius: 15px; background-color: #A5CFFA; border: 2px solid #06189E; padding: 10px; margin-top:200px;}\r\n");
            webString.append(".text{ font-size: 14px; font-family: arial; color: white;}\r\n");
            webString.append("</style>\r\n");
            webString.append("</head>\r\n");
            webString.append("<body>\r\n");
            webString.append("<div style=\"width:100%;\" align=\"center\">\r\n");
            webString.append("<div id=\"box\">\r\n");
            webString.append("<h1>Datastorage with no name - Upload</h1>");
            webString.append("<p class=\"text\">Welcome to the datastorage with no name!<br>\r\n");
            webString.append("<form method=\"post\" action=\"/post\" enctype=\"multipart/form-data\"> \r\n");
            webString.append("<input type=\"file\" name=\"att\" />\r\n");
            webString.append("<input type=\"submit\" value=\"Upload\" />\r\n");
            webString.append("</form>\r\n");
            webString.append("</p>\r\n");
            webString.append("</div>\r\n");
            webString.append("</div>\r\n");
            webString.append("</body>\r\n");
            webString.append("</html>\r\n");
            
            OutputStream responseStream = he.getResponseBody();
            responseStream.write(webString.toString().getBytes());
            responseStream.flush();
            responseStream.close();
        }else if(requestMethod.equalsIgnoreCase("POST")){
            System.out.println("Content-length: " + he.getRequestHeaders().get("Content-length").get(0));
            long length = Long.parseLong(he.getRequestHeaders().get("Content-length").get(0));
            DataInputStream is = new DataInputStream(he.getRequestBody());
            
            byte[] metadata = new byte[1024];
            
            int readbyte = 0;
            int countBytes = 0;
            while((readbyte = is.read()) != -1){
                metadata[countBytes] = (byte)readbyte;
                
                if(countBytes >= 3 && (char)metadata[countBytes-3] == '\r' && (char)metadata[countBytes-2] == '\n' && (char)metadata[countBytes-1] == '\r' && (char)metadata[countBytes] == '\n'){
                    break; //Found the end of meta
                }
//                System.out.print((char)readbyte);
//                if((char)readbyte == '\r'){
//                    System.out.println(countBytes + ": Carriage return");
//                }else if((char)readbyte == '\n'){
//                    System.out.println(countBytes + ": Line feed");
//                }
                countBytes++;
            }
            System.out.println("Countb: " + countBytes);
            String meta = new String(metadata);
            String column = "webupload";
            String filename = meta.substring(meta.indexOf("filename="), meta.indexOf("\r", meta.indexOf("filename=")));
            String row = filename.substring(filename.indexOf("\"")+1, filename.lastIndexOf("\""));
            
            File tempFile = new File(column + "_" + row + "_" + System.currentTimeMillis());
            FileOutputStream fos = new FileOutputStream(tempFile);
            
            byte[] buffer = null;
            int readbytes = 0;
            long totalBytes = length-countBytes-1;
            
            while(totalBytes > 0){
                buffer = new byte[BackStorage.BlOCK_SIZE];
                readbytes = is.read(buffer);
                fos.write(buffer, 0, readbytes);
                totalBytes -= readbytes;
                if(totalBytes <= 0)
                    break;
            }
            fos.flush();
            fos.close();
            
            RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
            
            long fileLength = tempFile.length()-1;
            long crntPos = 0;
            int crln_count = 0;
            while(crln_count < 2){
                raf.seek(fileLength - crntPos);
                if((char)raf.read() == '\r' && (char)raf.read() == '\n'){
                    System.out.println("Found one at: " + crntPos);
                    crln_count++;
                    if(crln_count == 2)
                        break;
                }
                crntPos++;
            }
            
            raf.getChannel().truncate(fileLength-crntPos);
            raf.close();
            System.out.println("Len: " + (fileLength-crntPos));
            UnindexedDataObject udo = new UnindexedDataObject(new File(column + "_" + row + "_" + Math.random()), column, row, "uploader", fileLength-crntPos);
            
            try{
                StoreDataJob job = new StoreDataJob(udo, reference.getNewDataProcessor());
                ByteBuffer buff = ByteBuffer.allocate(BackStorage.BlOCK_SIZE);
                FileChannel input = new FileInputStream(tempFile).getChannel();
                while(!job.isFinished()){
                    input.read(buff);
                    buff.limit(buff.position());
                    job.update(buff);
                    buff.clear();
                }
                input.close();
                Main.pool.submit(new IndexPersistence(Main.localIndex.getData()));
                System.out.println("Storing should be complete!");
            }catch(IOException e){
                e.printStackTrace();
            }
            
            tempFile.delete();
            
            he.sendResponseHeaders(200, 0);
            he.getResponseBody().close();
        }
    }
    
}
