/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.index;

import com.mnorrman.datastorageproject.objects.GlobalIndexedDataObject;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mikael
 */
public class IndexPersistenceGlobal implements Runnable{

    private String className;
    private Collection<ArrayList<GlobalIndexedDataObject>> data;

    public IndexPersistenceGlobal(Collection<ArrayList<GlobalIndexedDataObject>> data) {
        this.className = "GlobalIndex";
        this.data = data;
    }
    
    public IndexPersistenceGlobal(){
        this.className = "GlobalIndex";
        this.data = null;
    }
    
    public List<GlobalIndexedDataObject> load(){
        List<GlobalIndexedDataObject> l = new LinkedList<GlobalIndexedDataObject>();
        File file = new File(className + "_");
        
        //If the file exist, try to load the data
        if(file.exists()){
            try{
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));

                Object temp;
                while((temp = ois.readObject()) != null){
                    l.add((GlobalIndexedDataObject)temp);
                }
                Logger.getLogger("b-log").log(Level.FINEST, "{0} was loaded successfully", className);
            }catch(IOException e){
                Logger.getLogger("b-log").log(Level.SEVERE, "Error when loading " + className + " from persistent storage", e);
            }catch(ClassNotFoundException e){
                Logger.getLogger("b-log").log(Level.SEVERE, "Error when loading " + className + " from persistent storage", e);
            }
        }
        
        return l;
    }
    
    @Override
    public void run() {
        try{
            File file = new File(className + "_");
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            
            for(ArrayList<GlobalIndexedDataObject> al : data){
                for(GlobalIndexedDataObject g : al){
                    oos.writeObject(g);
                }
            }
            oos.flush();
            oos.close();
            Logger.getLogger("b-log").log(Level.FINEST, "{0} just saved to persistent  storage successfully", className);
        }catch(IOException e){
            Logger.getLogger("b-log").log(Level.SEVERE, "Error when writing " + className + " to persistent storage", e);
        }
    }
}
