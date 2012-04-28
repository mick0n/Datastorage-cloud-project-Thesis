
package com.mnorrman.datastorageproject.web;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.network.jobs.GetDataJob;
import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.storage.BackStorage;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * This web role will fetch data from the backstorage and send it through http
 * to client.
 * @author Mikael Norrman
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
        
        //If the HTTP request is GET
        if (requestMethod.equalsIgnoreCase("GET")) {
            
            if(he.getRequestURI().getQuery() != null){
                //Get request parameters from queryString
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

                //For now, only check for column and row, not version
                if(!column.equals("") && !row.equals("")){
                    ido = Main.localIndex.get(column, row);
                }

                Headers responseHeaders = he.getResponseHeaders();
                if(ido == null){
                    responseHeaders.set("Content-Type", "text/plain");
                    he.sendResponseHeaders(404, 0);
                    he.getRequestBody().close();
                    he.getResponseBody().close();
                }else{
                    responseHeaders.set("Content-Type", "application/octet-stream");
                    responseHeaders.set("Content-Length", "" + ido.getLength());
                    responseHeaders.set("Content-Disposition", "attachment; filename=" + row);
                    responseHeaders.set("Cache-Control", "no-cache");
                    he.sendResponseHeaders(200, 0);

                    //Create new job and initiate fetching
                    GetDataJob job = new GetDataJob(ido, reference.getNewDataProcessor());
                    try{
                        OutputStream os = he.getResponseBody();
                        while(!job.isFinished()){
                            os.write(job.getData(), 0, job.getBufferLimit());
                        }
                        os.flush();
                        os.close();
                    }catch(IOException e){
                        LogTool.log(e, LogTool.CRITICAL);
                    }
                }
            }else{
                Headers responseHeaders = he.getResponseHeaders();
                responseHeaders.set("Content-Type", "text/HTML");
                he.sendResponseHeaders(200, 0); //ok

                //Build web content
                StringBuilder webString = new StringBuilder();
                webString.append("<html>\r\n");
                webString.append("<head>\r\n");
                webString.append("<title>Datastorage thesis</title>\r\n");
                webString.append("<style type=\"text/css\">\r\n");
                webString.append("h1{ font-weight:bold; font-size:16px; font-family: arial; color: #06189E; }\r\n");
                webString.append("#box{-moz-border-radius: 15px; border-radius: 15px; background-color: #A5CFFA; border: 2px solid #06189E; padding: 10px; margin-top:20px;}\r\n");
                webString.append(".text{ font-size: 14px; font-family: arial; color: black;}\r\n");
                webString.append("</style>\r\n");
                webString.append("</head>\r\n");
                webString.append("<body>\r\n");
                webString.append("<div style=\"width:100%;\" align=\"left\">\r\n");
                webString.append("<div id=\"box\">\r\n");
                webString.append("<h1>Datastorage with no name - Local index get (Don't support versions yet)</h1>\r\n");
                webString.append("<table class=\"text\" style=\"padding: 3px;border: 1px solid black;\">\r\n");
                webString.append("<tr><td style=\"border: 1px solid black; padding: 3px;background-color:lightgrey;\">Column</td><td colspan=\"100\" style=\"border: 1px solid black; padding: 3px;background-color:white;\">Rows</td></tr>\r\n");
                webString.append("<tr>\r\n");
                webString.append("<td></td>\r\n");
                HashMap<String, ArrayList<IndexedDataObject>> data = Main.localIndex.getDistinctData();
                for(String col : data.keySet()){
                    webString.append("<tr>\r\n");
                    webString.append("<td style=\"border: 1px solid black; padding: 3px;background-color:lightgrey;\">" + col + "</td>\r\n");
                    for(IndexedDataObject idooo : data.get(col)){
                        webString.append("<td style=\"border: 1px solid black; padding: 3px;background-color:white;\"><a href=\"/get?column=" + idooo.getColname() + "&row=" + idooo.getRowname() + "\">" + idooo.getRowname() + "</a></td>\r\n");
                    }
                    webString.append("</tr>\r\n");
                }
                webString.append("</tr>\r\n");
                webString.append("</table>");
                webString.append("</div>\r\n");
                webString.append("</div>\r\n");
                webString.append("</body>\r\n");
                webString.append("</html>\r\n");

                OutputStream response = he.getResponseBody();
                response.write(webString.toString().getBytes());
                response.flush();
                response.close();
            }
        }
    }
    
}
