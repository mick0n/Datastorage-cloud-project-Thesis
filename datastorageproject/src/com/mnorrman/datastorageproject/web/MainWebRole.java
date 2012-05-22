
package com.mnorrman.datastorageproject.web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This web role should work as a portal. Right now it will display links
 * to other predefined roles.
 * @author Mikael Norrman
 */
public class MainWebRole implements HttpHandler {

    @Override
    public void handle(HttpExchange he) throws IOException {
        String requestMethod = he.getRequestMethod();
        
        //If the HTTP request is GET
        if (requestMethod.equalsIgnoreCase("GET")) {
            Headers responseHeaders = he.getResponseHeaders();
            responseHeaders.set("Content-Type", "text/HTML");
            he.sendResponseHeaders(200, 0);

            //Build web content
            StringBuilder webString = new StringBuilder();
            webString.append("<html>\r\n");
            webString.append("<head>\r\n");
            webString.append("<title>Datastorage thesis</title>\r\n");
            webString.append("<style type=\"text/css\">\r\n");
            webString.append("h1{ font-weight:bold; font-size:16px; font-family: arial; color: #06189E; }\r\n");
            webString.append("#box{width:400px; height: 250px; -moz-border-radius: 15px; border-radius: 15px; background-color: #A5CFFA; border: 2px solid #06189E; padding: 10px; margin-top:200px;}\r\n");
            webString.append(".text{ font-size: 14px; font-family: arial; color: white;}\r\n");
            webString.append("</style>\r\n");
            webString.append("</head>\r\n");
            webString.append("<body>\r\n");
            webString.append("<div style=\"width:100%;\" align=\"center\">\r\n");
            webString.append("<div id=\"box\">\r\n");
            webString.append("<h1>Datastorage with no name</h1>");
            webString.append("<p class=\"text\">Welcome to the datastorage with no name!<br>This should be your number one source for data storage!<br/><a href=\"/index\">See index</a><br/><a href=\"/post\">Upload</a><br/><a href=\"/get\">Get data</a><p>\r\n");
            webString.append("</div>\r\n");
            webString.append("</div>\r\n");
            webString.append("</body>\r\n");
            webString.append("</html>\r\n");
            
            OutputStream responseStream = he.getResponseBody();
            responseStream.write(webString.toString().getBytes());
            responseStream.flush();
            responseStream.close();
        }
    }
}
