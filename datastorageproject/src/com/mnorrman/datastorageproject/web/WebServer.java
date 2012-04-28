
package com.mnorrman.datastorageproject.web;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * An internal webserver that can run different roles.
 * @author Mikael Norrman
 */
public class WebServer {

    private final InetSocketAddress addr;
    private HttpServer server;
    
    /**
     * Creates a new instance of WebServer and starts it up on the
     * specified port. It will add any necessary objects by itself.
     * @param port The port on which this webserver should run.
     * @throws IOException 
     */
    public WebServer(int port) throws IOException{
        this.addr = new InetSocketAddress(port);
        server = HttpServer.create(addr, 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }
    
    /**
     * Adds a new role to this server. It will then be accessible from
     * http://<domain name> + path
     * @param path The context path of this role.
     * @param role A role object.
     */
    public void addWebRole(String path, HttpHandler role){
        server.createContext(path, role);
    }

    /**
     * Stop this webserver
     */
    public void close(){
        server.stop(0);
    }
}