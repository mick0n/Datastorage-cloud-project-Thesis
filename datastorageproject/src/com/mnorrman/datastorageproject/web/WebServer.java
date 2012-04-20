/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class WebServer {

    private final InetSocketAddress addr;
    private HttpServer server;
    
    public WebServer(int port) throws IOException{
        this.addr = new InetSocketAddress(port);
        server = HttpServer.create(addr, 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }
    
    public void addWebRole(String path, HttpHandler role){
        server.createContext(path, role);
    }
    
    public static void main(String[] args) throws IOException {
       WebServer ws = new WebServer(8080);
       ws.addWebRole("/", new MainWebRole());
    }
}