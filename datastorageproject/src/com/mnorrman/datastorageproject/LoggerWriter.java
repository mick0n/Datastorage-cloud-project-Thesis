/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

import java.io.*;

/**
 *
 * @author Mikael
 */
public class LoggerWriter extends FilterWriter{

    public LoggerWriter(Writer arg0) {
        super(arg0);
    }

    @Override
    public void write(String arg0) throws IOException {
        super.write(arg0);
        System.out.println(arg0);
        flush();
    }

    @Override
    public void write(int arg0) throws IOException {
        super.write(arg0);
        System.out.print("" + (char)arg0);
    }
    
    
    
   
    
    public static void main(String[] args) throws IOException{
        PrintWriter pw = new PrintWriter(new LoggerWriter(new FileWriter(new File("log.txt"))));
        
        pw.write("Tjoho");
        try{
            int[] a = new int[1];
            int b = a[4];
        }catch(IndexOutOfBoundsException e){
            e.printStackTrace(pw);
        }
    }
}
