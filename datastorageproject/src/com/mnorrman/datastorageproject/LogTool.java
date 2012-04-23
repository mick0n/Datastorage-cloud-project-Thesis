
package com.mnorrman.datastorageproject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

/**
 *
 * @author Mikael Norrman
 */
public class LogTool{

    /**
     * Lowest log-level
     */
    public static final int INFO = 0;
    
    /**
     * Medium log-level
     */
    public static final int WARNING = 1; 
    
    /**
     * Highest log-level
     */
    public static final int CRITICAL = 2;
    
    private static int logLevel = CRITICAL;
    private static File logFile;
    private static PrintWriter out;
    
    private LogTool(){ } //Disable instantiation
    
    /**
     * Get the current loglevel
     * @return Integer representing current loglevel
     */
    public static int getLogLevel() {
        return logLevel;
    }

    /**
     * Set the current loglevel
     * @param logLevel New loglevel
     */
    public static void setLogLevel(int logLevel) {
        LogTool.logLevel = logLevel;
    }
        
    //Initiate the logFile and outputStream
    private static void init(){
        logFile = new File("log.txt");
        try{
            if(!logFile.exists())
                logFile.createNewFile();
            out = new PrintWriter(new FileOutputStream(logFile, true));
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Prints an exception to the log.
     * @param e Exception to be printed to log.
     * @param level The severity of the exception.
     */
    public static void log(Exception e, int level){
        if(logFile == null && out == null)
            init();
        printLine(e.toString(), level);
        
        StackTraceElement[] trace = e.getStackTrace();
        for(int a = 0; a < trace.length; a++){
            printLine("\tat " + trace[a].toString(), level);
        }
    }
    
    /**
     * Prints a string to the log.
     * @param s The string that should be printed.
     * @param level The severity of the exception.
     */
    public static void log(String s, int level){
        if(logFile == null && out == null)
            init();
        printLine(s, level);
    }

    private static void printLine(String s, int level){
        if(level >= logLevel){ //Print only if level is higher or equal to the minimum level.
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
            System.out.println(sb.toString()); //Print to system output
            out.println(sb.toString()); //Print to file output
            out.flush();
        }
    }
    
    /**
     * Close outputStream
     */
    public static void close(){
        out.flush();
        out.close();
    }
}
