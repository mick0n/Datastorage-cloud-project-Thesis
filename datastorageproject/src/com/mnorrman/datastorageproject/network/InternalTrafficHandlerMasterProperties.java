/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.network;

import com.mnorrman.datastorageproject.LogTool;
import com.mnorrman.datastorageproject.Main;
import com.mnorrman.datastorageproject.objects.Range;
import com.mnorrman.datastorageproject.objects.ServerNode;
import com.mnorrman.datastorageproject.objects.Tree;
import com.mnorrman.datastorageproject.objects.TreeNode;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 *
 * @author Mikael
 */
public class InternalTrafficHandlerMasterProperties {
    
    protected ServerNode rootNode;
    
    protected Tree tree;
    
    protected HashMap<String, ServerNode> children;
    
    public InternalTrafficHandlerMasterProperties(){
        children = new HashMap<String, ServerNode>();
        try{
            rootNode = new ServerNode(InetAddress.getByName("127.0.0.1"), Integer.parseInt(Main.properties.getValue("internalport").toString()), "00000000");
            rootNode.setRange(new Range(0x0, 0xFFFFFFFFL)); //Maximum range for root node
        }catch(UnknownHostException e){
            LogTool.log(e, LogTool.INFO);
        }
        tree = new Tree(rootNode);
        loadTree();
        LogTool.log("InternalTrafficHandlerMasterProperties set up properly", LogTool.INFO);
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
        
        if(children.size() >= 4)
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
        Range base = rootNode.getRange();                

        long step = (long)Math.floor((base.endRange - base.startRange) / 4);
        int remainder = (int)((base.endRange - base.startRange) % 4L);
        int newNodeIndex = children.size()-1;

        //calculate range
        switch(newNodeIndex){
            case 0:
                node.setRange(new Range(base.startRange + (step * 0), base.startRange + (step * 1)));
                break;
            case 1:
                node.setRange(new Range(base.startRange + (step * 1) + 1, base.startRange + (step * 2)));
                break;
            case 2:
                node.setRange(new Range(base.startRange + (step * 2) + 1, base.startRange + (step * 3)));
                break;
            case 3:
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
                String parent = st.nextToken();
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
    
    public void breadthfirstSaveTree(){
        try{    
            Queue<TreeNode> qu = new LinkedList<TreeNode>();
            File file = new File(Main.properties.getValue("dataPath") + File.separator + "tree_");
//            File file = new File("children_");
            PrintWriter pw = new PrintWriter(file);
            
            Iterator<TreeNode> it = tree.getRoot().toIterator();
            while(it.hasNext())
                qu.add(it.next());
            
            while(!qu.isEmpty()){
                TreeNode temp = qu.poll();
                pw.println(temp.getParentNode().getServerNode().getId() + "," + temp.getServerNode().toString());
                Iterator<TreeNode> it2 = temp.toIterator();
                while(it2.hasNext())
                    qu.add(it2.next());
            }
            pw.flush();
            pw.close(); 
        }catch(IOException e){
            LogTool.log(e, LogTool.CRITICAL);
        }
    }
    
    public void loadTree(){
        try{
            File file = new File(Main.properties.getValue("dataPath") + File.separator + "tree_");
//            File file = new File("children_");
            if(!file.exists())
                return;
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while((line = br.readLine()) != null){
                StringTokenizer st = new StringTokenizer(line, ",");
                String parent = st.nextToken();
                InetAddress ia = InetAddress.getByName(st.nextToken());
                int port = Integer.parseInt(st.nextToken());
                String id = st.nextToken();
                ServerNode newNode = new ServerNode(ia, port, id);
                if(st.hasMoreTokens()){
                    long startRange = Long.parseLong(st.nextToken());
                    long endRange = Long.parseLong(st.nextToken());
                    newNode.setRange(new Range(startRange, endRange));
                }
                tree.addNode(parent, newNode);
            }
            br.close();
        }catch(IOException e){
            LogTool.log(e, LogTool.CRITICAL);
        }
    }

    public HashMap<String, ServerNode> getChildren() {
        return children;
    }

    public ServerNode getRootNode() {
        return rootNode;
    }

    public Tree getTree() {
        return tree;
    }
 
    
    
//    public static void main(String[] args) throws Exception{
//        InternalTrafficHandlerMasterProperties ithmp = new InternalTrafficHandlerMasterProperties();
//        ithmp.rootNode = new ServerNode(InetAddress.getByName("127.0.0.1"), 4567, "00000000");
//        ithmp.rootNode.setRange(new Range(0x0, 0xFFFFFFFFL));
//        ithmp.tree = new Tree(ithmp.rootNode);
//        System.out.println("Added servernode: " + ithmp.tree.addNode(new ServerNode(InetAddress.getByName("194.47.47.72"), 5666, "ABCD0123")).toString());
//        System.out.println("Added servernode: " + ithmp.tree.addNode(new ServerNode(InetAddress.getByName("194.47.47.73"), 5667, "EFFF4567")).toString());
//        System.out.println("Added servernode: " + ithmp.tree.addNode(new ServerNode(InetAddress.getByName("194.47.47.74"), 5668, "CCCC3333")).toString());
//        System.out.println("Added servernode: " + ithmp.tree.addNode(new ServerNode(InetAddress.getByName("194.47.47.75"), 5669, "DDDD4444")).toString());
//        System.out.println("Added servernode: " + ithmp.tree.addNode(new ServerNode(InetAddress.getByName("194.47.47.76"), 5669, "DDDD4445")).toString());
//        System.out.println("Added servernode: " + ithmp.tree.addNode(new ServerNode(InetAddress.getByName("194.47.47.77"), 5669, "DDDD4446")).toString());
//        System.out.println("Added servernode: " + ithmp.tree.addNode(new ServerNode(InetAddress.getByName("194.47.47.78"), 5669, "DDDD4447")).toString());
//        System.out.println("Added servernode: " + ithmp.tree.addNode(new ServerNode(InetAddress.getByName("194.47.47.79"), 5669, "DDDD4448")).toString());
//        System.out.println("Added servernode: " + ithmp.tree.addNode(new ServerNode(InetAddress.getByName("194.47.47.80"), 5669, "DDDD4449")).toString());
//        System.out.println("Added servernode: " + ithmp.tree.addNode(new ServerNode(InetAddress.getByName("194.47.47.81"), 5669, "DDDD4454")).toString());
//        System.out.println("Added servernode: " + ithmp.tree.addNode(new ServerNode(InetAddress.getByName("194.47.47.82"), 5669, "DDDD4464")).toString());
//        System.out.println("Added servernode: " + ithmp.tree.addNode(new ServerNode(InetAddress.getByName("194.47.47.83"), 5669, "DDDD4474")).toString());
//        System.out.println("Added servernode: " + ithmp.tree.addNode(new ServerNode(InetAddress.getByName("194.47.47.84"), 5669, "DDDD4374")).toString());
//        ithmp.saveTree();
//        ithmp.breadthfirstSaveTree();
//    }
    
    public static void main(String[] args) throws Exception{
        InternalTrafficHandlerMasterProperties ithmp = new InternalTrafficHandlerMasterProperties();
        ithmp.rootNode = new ServerNode(InetAddress.getByName("127.0.0.1"), 4567, "00000000");
        ithmp.rootNode.setRange(new Range(0x0, 0xFFFFFFFFL));
        ithmp.tree = new Tree(ithmp.rootNode);
        ithmp.loadTree();
        ithmp.tree.printTreePreOrder("", ithmp.tree.getRoot());
    }

}
