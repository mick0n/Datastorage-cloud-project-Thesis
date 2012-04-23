
package com.mnorrman.datastorageproject.web;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * This web role prints out a table of all indexes (only columns and rows)
 * in localIndex.
 * @author Mikael Norrman
 */
public class PrintIndexWebRole implements HttpHandler {

    @Override
    public void handle(HttpExchange he) throws IOException {
        String requestMethod = he.getRequestMethod();
        
        //If the HTTP request is GET
        if (requestMethod.equalsIgnoreCase("GET")) {
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
            webString.append("<h1>Datastorage with no name - Local index</h1>\r\n");
            webString.append("<table border=\"1\" class=\"text\" style=\"padding: 3px;\">\r\n");
            webString.append("<tr>\r\n");
            webString.append("<td style=\"border: 1px solid black; padding: 3px;background-color:lightgrey;\">Column name</td>\r\n");
            webString.append("<td style=\"border: 1px solid black; padding: 3px;background-color:lightgrey;\">Row name</td>\r\n");
            webString.append("<td style=\"border: 1px solid black; padding: 3px;background-color:lightgrey;\">Owner name</td>\r\n");
            webString.append("<td style=\"border: 1px solid black; padding: 3px;background-color:lightgrey;\">Version number</td>\r\n");
            webString.append("<td style=\"border: 1px solid black; padding: 3px;background-color:lightgrey;\">Data size</td>\r\n");
            webString.append("<td style=\"border: 1px solid black; padding: 3px;background-color:lightgrey;\">Checksum(CRC32) value</td>\r\n");
            webString.append("</tr>\r\n");
            for(ArrayList<IndexedDataObject> al : Main.localIndex.getData()){
                for(IndexedDataObject ido : al){
                    webString.append("<tr>\r\n");
                    webString.append("<td style=\"border: 1px solid black; padding: 3px;background-color:white;\">" + ido.getColname() + "</td>\r\n");
                    webString.append("<td style=\"border: 1px solid black; padding: 3px;background-color:white;\">" + ido.getRowname() + "</td>\r\n");
                    webString.append("<td style=\"border: 1px solid black; padding: 3px;background-color:white;\">" + ido.getOwner() + "</td>\r\n");
                    webString.append("<td style=\"border: 1px solid black; padding: 3px;background-color:white;\">" + ido.getVersion() + "</td>\r\n");
                    webString.append("<td style=\"border: 1px solid black; padding: 3px;background-color:white;\">" + ido.getLength() + " bytes</td>\r\n");
                    webString.append("<td style=\"border: 1px solid black; padding: 3px;background-color:white;\">" + ido.getChecksum() + "</td>\r\n");
                    webString.append("</tr>\r\n");
                }
            }
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
