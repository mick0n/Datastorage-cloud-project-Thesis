/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

import java.io.*;
import java.util.Date;

/**
 *
 * @author Mikael
 */
public class LogTool{

    public static final int INFO = 0;
    public static final int WARNING = 1;
    public static final int CRITICAL = 2;
    
    public int logLevel;
    private File logFile;
    private PrintWriter out;
    
    public LogTool() {
        logLevel = WARNING;
        init();
    }
    
    public LogTool(int level){
        this.logLevel = level;
        init();
    }

    public int getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }
        
    private void init(){
        logFile = new File("log.txt");
        try{
            if(!logFile.exists())
                logFile.createNewFile();
            out = new PrintWriter(new FileOutputStream(logFile, true));
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void log(Exception e, int level){
        printLine(e.toString(), level);
        
        StackTraceElement[] trace = e.getStackTrace();
        for(int a = 0; a < trace.length; a++){
            printLine("\tat " + trace[a].toString(), level);
        }
    }
    
    private void printLine(String s, int level){
        if(level >= logLevel){
            StringBuilder sb = new StringBuilder();
            sb.append(new Date().toString());
            sb.append(": ");
            switch(level){
                case 0:
                    sb.append("INFO");
                    break;
                case 1:
                    sb.append("WARNING");
                    break;
                case 2:
                    sb.append("CRITICAL");
                    break;
            }
            sb.append(": ");
            sb.append(s);
            System.out.println(sb.toString());
            out.println(sb.toString());
            out.flush();
        }
    }
    
    public void close(){
        out.flush();
        out.close();
    }
    
    public static void main(String[] args) throws IOException{
        LogTool t = new LogTool();
        
        try{
            int[] a = new int[1];
            int b = a[4];
        }catch(IndexOutOfBoundsException e){
            t.log(e, CRITICAL);
        }
    }
}
