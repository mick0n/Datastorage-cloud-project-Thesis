/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.objects.Range;
import com.mnorrman.datastorageproject.objects.ServerNode;
import java.io.*;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 *
 * @author Mikael
 */
public class InternalTrafficHandlerChildProperties {
    
    protected InternalTrafficContext masterContext;
    protected ServerNode thisNode;
    protected HashMap<String, ServerNode> children;
    //My range
    //My children, stuff like that
    
    public InternalTrafficHandlerChildProperties(){
        children = new HashMap<String, ServerNode>();
        loadChildren();
        LogTool.log("InternalTrafficHandlerChildProperties set up properly", LogTool.INFO);
    }
    
    public boolean containsID(String id){
        return children.containsKey(id);
    }
    
    public ServerNode getChild(String id){
        return children.get(id);
    }
    
    public ServerNode addChild(ServerNode node){
        if(children.containsKey(node.getId())){
            ServerNode existingNode = children.get(node.getId());
            existingNode.setIpaddress(node.getIpaddress());
            existingNode.setInternalport(node.getInternalport());
            existingNode.setExternalport(node.getExternalport());
            node.setRange(existingNode.getRange());
            return node;
        }
        
        if(children.size() >= 3)
            throw new IndexOutOfBoundsException("To many elements");

        children.put(node.getId(), node);
        //Then we start fiddling with the range for our new child.
        //First we get the base from the parent and extract the necessary
        //step, remainder and newNodeIndex-values.
        //Step is the amount (in range) that the new child will get. However
        //we still need to figure out what part of the parents range we
        //are going to assign to the new node.
        //
        //Example: Parent range: 5 - 25, step: 5
        //Possible new range for node is: (5-10), 11-15, 16-20 and 21-25
        //Based on the newNodeIndex one of these ranges will be assigned
        //to the new node. The first range is in parenthesis because it 
        //is by default locked to the parent.
        Range base = thisNode.getRange();                

        long step = (long)Math.floor((base.endRange - base.startRange) / 4);
        int remainder = (int)((base.endRange - base.startRange) % 4L);
        int newNodeIndex = children.size()-1;

        //calculate range
        switch(newNodeIndex){
            case 0:
                node.setRange(new Range(base.startRange + (step * 1) + 1, base.startRange + (step * 2)));
                break;
            case 1:
                node.setRange(new Range(base.startRange + (step * 2) + 1, base.startRange + (step * 3)));
                break;
            case 2:
                node.setRange(new Range(base.startRange + (step * 3) + 1, base.startRange + (step * 4) + remainder));
                break;
        }

        //Return the new node.
        return node;
    }
    
    public final void saveChildren(){
        try{    
            File file = new File(Main.properties.getValue("dataPath") + File.separator + "children_");
//            File file = new File("children_");
            PrintWriter pw = new PrintWriter(file);
            for(ServerNode sn : children.values()){
                pw.println(sn.toString());
            }
            pw.flush();
            pw.close(); 
        }catch(IOException e){
            LogTool.log(e, LogTool.CRITICAL);
        }
    }
    
    public final void loadChildren(){
        try{
            File file = new File(Main.properties.getValue("dataPath") + File.separator + "children_");
//            File file = new File("children_");
            if(!file.exists())
                return;
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while((line = br.readLine()) != null){
                StringTokenizer st = new StringTokenizer(line, ",");
                InetAddress ia = InetAddress.getByName(st.nextToken());
                int port = Integer.parseInt(st.nextToken());
                String id = st.nextToken();
                ServerNode newNode = new ServerNode(ia, port, id);
                if(st.hasMoreTokens()){
                    long startRange = Long.parseLong(st.nextToken());
                    long endRange = Long.parseLong(st.nextToken());
                    newNode.setRange(new Range(startRange, endRange));
                }
                children.put(id, newNode);
            }
            br.close();
        }catch(IOException e){
            LogTool.log(e, LogTool.CRITICAL);
        }
    }
    
    public ServerNode getThisNode(){
        return thisNode;
    }
}
