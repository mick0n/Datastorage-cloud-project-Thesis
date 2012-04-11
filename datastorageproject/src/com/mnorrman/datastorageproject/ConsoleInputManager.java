/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

import com.mnorrman.datastorageproject.storage.DataProcessor;
import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.objects.UnindexedDataObject;
import com.mnorrman.datastorageproject.tools.HexConverter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is mainly used for maintanence and testing during the early part
 * of this project. This will be limited to a set of very simple commands later
 * on so that it is not missused. 
 * @author Mikael
 */
public class ConsoleInputManager extends Thread {

    private Scanner input;
    private Main m;

    public ConsoleInputManager(Main m) {
        this.m = m;
        input = new Scanner(System.in);
    }

    @Override
    public void run() {
        String command = "";
        StringTokenizer st = null;
        print("Welcome to Console Input Manager.");
        do {
            st = new StringTokenizer(input.nextLine().trim(), " ");
            command = st.nextToken();
            if (command.toLowerCase().equals("exit")) {
                //Nothing to do here
            } else if (command.toLowerCase().equals("chkinteg")) {
                m.storage.performIntegrityCheck();


            } else if (command.toLowerCase().equals("reindex")) {
                Main.localIndex.clear();
                Main.localIndex.insertAll(m.storage.reindexData());


            } else if (command.toLowerCase().equals("storelocal")) {
                try {
                    String colname = st.nextToken();
                    String rowname = st.nextToken();
                    String owner = st.nextToken();
                    File file = new File(st.nextToken());
                    UnindexedDataObject udo = new UnindexedDataObject(new FileInputStream(file), colname, rowname, owner, file.length());

                    DataProcessor dp = m.getNewDataProcessor();
                    Main.localIndex.insert(dp.storeData(udo));
                    print("Done storing \"" + file.getName() + "\"");
                } catch (IOException e) {
                    print("An error occured when storing your file.");
                } catch (NoSuchElementException e) {
                    print("Input parameters were wrong.");
                }

            }else if(command.toLowerCase().equals("get")){
                String colname = st.nextToken();
                String rowname = st.nextToken();
                int version = 0;
                if(st.hasMoreTokens()){
                    version = Integer.parseInt(st.nextToken());
                }
                if(Main.localIndex.contains(colname, rowname)){
                        IndexedDataObject ido = Main.localIndex.get(colname, rowname, version);
                        try{
                            FileOutputStream fos = new FileOutputStream(new File("testOutput.txt"));
                            DataProcessor dp = m.getNewDataProcessor();
                            dp.retrieveData(fos, ido);
                            fos.close();
                        }catch(IOException e){
                            Logger.getLogger("b-log").log(Level.SEVERE, "An error occured!", e);
                        }
                }else{
                    print("No such element in table");
                }                
                
                
            }else if(command.toLowerCase().equals("del")){
                String colname = st.nextToken();
                String rowname = st.nextToken();
                IndexedDataObject temp = Main.localIndex.get(colname, rowname);
                DataProcessor dp = m.getNewDataProcessor();
                System.out.println("Success? " + dp.removeData(temp));
                Main.localIndex.remove(temp.getHash());
                
                
            }else if(command.toLowerCase().equals("printindex")){
                for(ArrayList<IndexedDataObject> al : Main.localIndex.getData()){
                    for(IndexedDataObject ido : al){
                        System.out.println(ido);
                    }
                }
                
                
            } else {
                print("Unknown command. Type help for help.");
            }
        } while (!command.toLowerCase().equals("exit"));
        
        System.exit(0);
    }

    private void print(String s) {
        System.out.println("Server> " + s);
    }
}
