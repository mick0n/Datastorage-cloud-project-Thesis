/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.objects.ServerNode;
import com.mnorrman.datastorageproject.tools.HexConverter;
import java.io.*;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;

/**
 *
 * @author Mikael
 */
public class MasterNodeSlaveList {
    
    private LinkedHashMap<String, ServerNode> slaves;
    
    public MasterNodeSlaveList(){
        slaves = new LinkedHashMap<String, ServerNode>();
        loadData();
    }
    
    public void put(ServerNode sn){
        if(slaves.containsKey(sn.getId())){
            slaves.get(sn.getId()).setIpaddress(sn.getIpaddress());
            slaves.get(sn.getId()).setPort(sn.getPort());
        }else{
            slaves.put(sn.getId(), sn);
        }
        storeData();
    }
    
    public void remove(String key){
        slaves.remove(key);
        storeData();
    }
    
    public ServerNode get(String key){
        return slaves.get(key);
    }
    
    private void storeData(){
        try{
            File file = new File(Main.properties.getValue("dataPath") + File.separator + "Slaves_");
            PrintWriter pw = new PrintWriter(file);
            for(ServerNode sn : slaves.values()){
                pw.println(sn.toString());
            }
            pw.flush();
            pw.close();            
        }catch(IOException e){
            LogTool.log(e, LogTool.CRITICAL);
        }
    }
    
    private void loadData(){
        try{
            File file = new File(Main.properties.getValue("dataPath") + File.separator + "Slaves_");
            if(!file.exists())
                return;
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line = "";
            while((line = br.readLine()) != null){
                StringTokenizer st = new StringTokenizer(line, ",");
                InetAddress ia = InetAddress.getByName(st.nextToken());
                int port = Integer.parseInt(st.nextToken());
                String id = st.nextToken();
                slaves.put(id, new ServerNode(ia, port, id));
            }
            br.close();
        }catch(IOException e){
            LogTool.log(e, LogTool.CRITICAL);
        }
    }
}
