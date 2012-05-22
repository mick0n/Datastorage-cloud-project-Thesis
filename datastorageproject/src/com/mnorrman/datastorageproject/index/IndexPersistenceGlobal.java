
package com.mnorrman.datastorageproject.index;

import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.objects.GloballyIndexedDataObject;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mikael Norrman
 */
public class IndexPersistenceGlobal implements Runnable{

    private String className;
    private Collection<ArrayList<GloballyIndexedDataObject>> data;

    /**
     * Create new instance of IndexPersistenceGlobal
     * @param data The data that should be saved
     */
    public IndexPersistenceGlobal(Collection<ArrayList<GloballyIndexedDataObject>> data) {
        this.className = "GlobalIndex";
        this.data = data;
    }
    
    /**
     * Create new instance of IndexPersistence. This constructor is mainly used
     * when reading data.
     */
    public IndexPersistenceGlobal(){
        this.className = "GlobalIndex";
        this.data = null;
    }
    
    /**
     * Read indexdata from GlobalIndex_ file. If the file doesn't exist an empty
     * list will be returned.
     * @return List containing all indexdata.
     */
    public List<GloballyIndexedDataObject> load(){
        List<GloballyIndexedDataObject> l = new LinkedList<GloballyIndexedDataObject>();
        File file = new File(Main.properties.getValue("dataPath") + File.separator + className + "_");
        
        //If the file exist, try to load the data
        if(file.exists()){
            try{
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));

                Object temp;
                while((temp = ois.readObject()) != null){
                    l.add((GloballyIndexedDataObject)temp);
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
            File file = new File(Main.properties.getValue("dataPath") + File.separator + className + "_");
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            
            for(ArrayList<GloballyIndexedDataObject> al : data){
                for(GloballyIndexedDataObject g : al){
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
