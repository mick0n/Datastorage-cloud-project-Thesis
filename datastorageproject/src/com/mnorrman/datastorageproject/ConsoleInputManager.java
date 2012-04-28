
package com.mnorrman.datastorageproject;

import com.mnorrman.datastorageproject.network.jobs.GetDataJob;
import com.mnorrman.datastorageproject.network.jobs.StoreDataJob;
import com.mnorrman.datastorageproject.objects.IndexedDataObject;
import com.mnorrman.datastorageproject.objects.UnindexedDataObject;
import com.mnorrman.datastorageproject.storage.BackStorage;
import com.mnorrman.datastorageproject.storage.DataProcessor;
import com.mnorrman.datastorageproject.tools.MetaDataComposer;
import com.mnorrman.datastorageproject.tools.RawMetaDataPrinter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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
 * @author Mikael Norrman
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


            } else if (command.toLowerCase().equals("clean")) {
                m.storage.clean();


            } else if (command.toLowerCase().equals("storelocal")) {
                try {
                    String colname = st.nextToken();
                    String rowname = st.nextToken();
                    String owner = st.nextToken();
                    File file = new File(st.nextToken());
                    UnindexedDataObject udo = new UnindexedDataObject(file, colname, rowname, owner, file.length());

                    DataProcessor dp = m.getNewDataProcessor();
                    Main.localIndex.insert(dp.storeData(udo));
                    print("Done storing \"" + file.getName() + "\"");
                } catch (NoSuchElementException e) {
                    print("Input parameters were wrong.");
                }

            }else if (command.toLowerCase().equals("storeremote")) {
                try {
                    String colname = st.nextToken();
                    String rowname = st.nextToken();
                    String owner = st.nextToken();
                    File file = new File(st.nextToken());
                    UnindexedDataObject udo = new UnindexedDataObject(new File(colname + "_" + rowname + "_" + Math.random()), colname, rowname, owner, file.length());
                    try{
                        StoreDataJob job = new StoreDataJob(udo, m.getNewDataProcessor());
                        ByteBuffer buffer = ByteBuffer.allocate(BackStorage.BlOCK_SIZE);
                        FileChannel input = new FileInputStream(file).getChannel();
                        while(!job.isFinished()){
                            input.read(buffer);
                            buffer.limit(buffer.position());
                            job.update(buffer);
                            buffer.clear();
                        }
                        input.close();
                    }catch(IOException e){
                        e.printStackTrace();
                    }

                    
                    print("Done storing \"" + file.getName() + "\"");
                } catch (NoSuchElementException e) {
                    print("Input parameters were wrong.");
                }

            }
            else if(command.toLowerCase().equals("get")){
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
                
                
            }else if(command.toLowerCase().equals("get2")){
                String colname = st.nextToken();
                String rowname = st.nextToken();
                int version = 0;
                if(st.hasMoreTokens()){
                    version = Integer.parseInt(st.nextToken());
                }
                if(Main.localIndex.contains(colname, rowname)){
                        IndexedDataObject ido = Main.localIndex.get(colname, rowname, version);
                        GetDataJob job = new GetDataJob(ido, m.getNewDataProcessor());
                        ByteBuffer largebuf = ByteBuffer.allocate(BackStorage.BlOCK_SIZE);
                        try{
                            FileOutputStream fos = new FileOutputStream(new File("testOutput2.txt"));
                            FileChannel fc = fos.getChannel();
                            long total = 0;
                            while(!job.isFinished()){
                                int readBytes = job.getDataProcessor().retrieveData(largebuf, job.getCurrentPosition(), job.getIndexedDataObject());
                                job.update(job.getCurrentPosition() + readBytes);
                                largebuf.flip();
                                fc.write(largebuf);
                                largebuf.clear();
                                System.out.println("crntpos: " + job.getCurrentPosition() + ", len: " + ido.getLength());
                                total += readBytes;
                            }
                            System.out.println("Total : " + total);
                            System.out.println("Done");
                            fc.close();
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
                Main.localIndex.clear().insertAll(m.storage.reindexData());
                
                
            }else if(command.toLowerCase().equals("printindex")){
                for(ArrayList<IndexedDataObject> al : Main.localIndex.getData()){
                    for(IndexedDataObject ido : al){
                        System.out.println(ido);
                    }
                }
                
                
            } else {
                print("Unknown command. Type help for help.");
            }
        } while (!command.toLowerCase().equals("shutdown"));
        
        try{
            m.shutdown();
        }catch(IOException e){
            LogTool.log(e, LogTool.CRITICAL);
        }catch(InterruptedException e){
            LogTool.log(e, LogTool.CRITICAL);
        }
    }

    private void print(String s) {
        System.out.println("Server> " + s);
    }
}
