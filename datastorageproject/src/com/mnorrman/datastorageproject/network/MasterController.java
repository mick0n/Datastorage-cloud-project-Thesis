/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.objects.ServerNode;
import com.mnorrman.datastorageproject.objects.TreeNode;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * @author Mikael
 */
public class MasterController {
    
    private TreeNode clusterTree;
    
    public MasterController(){
        clusterTree = new TreeNode(null); // The root element so to say
    }
    
    public void SaveClusterTreeToFile(){
        
    }
    
//    private void storeData(){
//        try{
//            File file = new File(Main.properties.getValue("dataPath") + File.separator + "Slaves_");
//            PrintWriter pw = new PrintWriter(file);
//            for(ServerNode sn : slaves.values()){
//                pw.println(sn.toString());
//            }
//            pw.flush();
//            pw.close();            
//        }catch(IOException e){
//            LogTool.log(e, LogTool.CRITICAL);
//        }
//    }
    
    //Pre-order walk
}
