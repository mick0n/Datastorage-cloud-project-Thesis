package com.mnorrman.datastorageproject;

import java.io.*;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mikael Norrman
 */
public class PropertiesManager {
    
    private File propFile;
    private Properties props;

    /**
     * Create new instance of PropertiesManager
     */
    public PropertiesManager() {
        try{
            propFile = new File("config_");
            if(!propFile.exists()){
                propFile.createNewFile();
                setConfigFile();
            }
            
            props = new Properties();
            
            /****** This part is a fix, it makes sure that backslash is read properly *******/
            String propertiesFileString = "";
            String temp = "";
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(propFile)));
            while((temp = br.readLine()) != null){
                propertiesFileString = propertiesFileString.concat(temp + "\r\n");
            }
            br.close();
            propertiesFileString = propertiesFileString.replace("\\", "\\\\");
            /****** END OF FIX ******/
            
            props.load(new StringReader(propertiesFileString));
        }catch(IOException e){
            Logger.getLogger("b-log").log(Level.SEVERE, "Error when initializing properties", e);
        }
    }
    
    /*
     * An extension of Properties.getProperty()-method.
     */
    public Object getValue(String key){
        return props.getProperty(key);
    }
    
    //Write a new config file with standard values
    private void setConfigFile(){
        try{
            PrintWriter pw = new PrintWriter(propFile);
            pw.println("#Config-file");
            pw.println("#Created " + new Date().toString());
            pw.println("#");
            pw.println("#Warning! Editing this file can lead to malfunction of");
            pw.println("#this software. Make changes with caution!");
            pw.println("#");
            pw.println("#dataPath - The absolute path to where all files are stored.");
            pw.println("#This does not have to be the same location as the runnable");
            pw.println("#file.");
            pw.println("dataPath = " + new File("").getAbsolutePath());
            pw.println("");
            pw.println("");
            pw.println("#indexInterval - the amount of time (in seconds) between each");
            pw.println("#save of the index.");
            pw.println("#A recommended amount is 900 seconds (15 minutes) on small to medium workload.");
            pw.println("indexInterval = 900");
            pw.println("");
            pw.println("");
            pw.println("#master - IP-address or hostname of the master server");
            pw.println("#If this server is the master, use 127.0.0.1");
            pw.println("master = 127.0.0.1");
            pw.println("");
            pw.println("");            
            pw.println("#port - port number used by this server.");
            pw.println("#Always make sure that your port is opened in your firewall.");
            pw.println("port = 8999");
            pw.println("");
            pw.println("");            
            pw.flush();
            pw.close();
        }catch(IOException e){
            Logger.getLogger("b-log").log(Level.SEVERE, "Error when creating config_ file", e);
        }
    }
}
