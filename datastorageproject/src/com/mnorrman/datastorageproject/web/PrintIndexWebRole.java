/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author Mikael
 */
public class PrintIndexWebRole implements HttpHandler {

    @Override
    public void handle(HttpExchange he) throws IOException {
        String requestMethod = he.getRequestMethod();
        if (requestMethod.equalsIgnoreCase("GET")) {
            Headers responseHeaders = he.getResponseHeaders();
            responseHeaders.set("Content-Type", "text/HTML");
            he.sendResponseHeaders(200, 0);

            OutputStream responseBody = he.getResponseBody();
            String web = "<html><head><title>Smallserver</title></head><body><h1>Small Server</h1></body></html>";
            responseBody.write(web.getBytes());
            //      Headers requestHeaders = exchange.getRequestHeaders();
            //      Set<String> keySet = requestHeaders.keySet();
            //      Iterator<String> iter = keySet.iterator();
            //      while (iter.hasNext()) {
            //        String key = iter.next();
            //        List values = requestHeaders.get(key);
            //        String s = key + " = " + values.toString() + "\n";
            //        responseBody.write(s.getBytes());
            //      }
            responseBody.flush();
            responseBody.close();
        }
    }
}
