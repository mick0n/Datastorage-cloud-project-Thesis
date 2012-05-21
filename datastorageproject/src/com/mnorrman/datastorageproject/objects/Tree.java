/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.objects;

import com.mnorrman.datastorageproject.ServerState;
import com.mnorrman.datastorageproject.tools.HexConverter;
import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author Mikael
 */
public class Tree {

    private TreeNode root;
    private HashMap<String, TreeNode> flatNodes;
    
    public Tree(ServerNode rootNode){
        flatNodes = new HashMap<String, TreeNode>();
        root = new TreeNode(null, rootNode);
        flatNodes.put(root.serverNode.getId(), root);
    }
    
    public ServerNode findRange(long value){
        TreeNode currentNode = root;
        boolean foundRange;
        while(currentNode != null){
            foundRange = false;
            if(currentNode.children.isEmpty()){
                return currentNode.serverNode;
            }else{
                for(TreeNode tn : currentNode.children){
                    if(value >= tn.getServerNode().getRange().startRange && value <= tn.getServerNode().getRange().endRange){
                        foundRange = true;
                        currentNode = tn;
                        if(currentNode.children.size() > 0)
                            break;
                    }
                }
                if(!foundRange){
                    return currentNode.getServerNode();
                }
            }
        }
        return null;
    }
    
//    public static void main(String[] args) throws Exception{
//        ServerNode root = new ServerNode(InetAddress.getByName("127.0.0.1"), 8888, "00000000");
//        root.setRange(new Range(0, 0xFFFFFFFFL));
//        Tree t = new Tree(root);
//        ServerNode a1 = new ServerNode(InetAddress.getByName("127.0.0.1"), 8000, "00F00000");
//        a1.setRange(new Range(500, 10000));
//        ServerNode a2 = new ServerNode(InetAddress.getByName("127.0.0.1"), 8001, "00F00001");
//        a2.setRange(new Range(10001, 20000));
//        ServerNode a3 = new ServerNode(InetAddress.getByName("127.0.0.1"), 8002, "00F00002");
//        a3.setRange(new Range(20001, 30000));
//        t.addNewNode(a1);
//        t.addNewNode(a2);
//        t.addNewNode(a3);
//        ServerNode rangeOwner = t.findRange(10000L);
//        System.out.println("Range owner: " + rangeOwner.toString());
//    }
    
    public void addNode(String parentID, ServerNode node){
        TreeNode parent = flatNodes.get(parentID);
        TreeNode newTreeNode = new TreeNode(parent, node);
        parent.addChild(newTreeNode);
        flatNodes.put(node.getId(), newTreeNode);
    }
    
    public void addNewNode(ServerNode node){
        if(flatNodes.containsKey(node.getId())){
            ServerNode existingNode = flatNodes.get(node.getId()).serverNode;
            existingNode.setIpaddress(node.getIpaddress());
            existingNode.setPort(node.getPort());
            return;
        }
        
        if(root.children.size() < 3){
            TreeNode newNode = new TreeNode(root, node);
            root.addChild(newNode);
            flatNodes.put(node.getId(), newNode);
        }else{
            //Create queue and add root node
            Queue<TreeNode> nodes = new LinkedList<TreeNode>();
            nodes.add(root);
            Iterator<TreeNode> rootIter = root.toIterator();
            while(rootIter.hasNext())
                nodes.add(rootIter.next());

            //While the queue isn't empty
            while(!nodes.isEmpty()){
                TreeNode temp = nodes.poll();
                if(temp.children.size() < 3){
                    TreeNode newNode = new TreeNode(temp, node);
                    temp.addChild(newNode);
                    flatNodes.put(node.getId(), newNode);
                    return;
                }else{
                    Iterator<TreeNode> iter = temp.toIterator();
                    while(iter.hasNext())
                        nodes.add(iter.next());
                }
            }
        }
    }
    
//    public ServerNode addNode(ServerNode node){
//        //Set up temporary variables
//        TreeNode parent = null;
//        Iterator<TreeNode> iter = null;
//        
//        //Create queue and add root node
//        Queue<TreeNode> nodes = new LinkedList<TreeNode>();
//        nodes.add(root);
//        
//        //While the queue isn't empty
//        while(!nodes.isEmpty()){
//            parent = nodes.poll();
//            if(parent.getServerNode().getState() == ServerState.NOTRUNNING)
//                break;
//            
//            //If the node isn't already fully populated (Maximum 3 children at
//            //this time) then we add the new node to this one.
//            if(parent.children.size() < 3){
//                //Create the new node and add it to both flatNodes and the parent
//                //TreeNode.
//                TreeNode newChild = new TreeNode(parent, node);
//                flatNodes.put(newChild.serverNode.getId(), newChild);
//                parent.addChild(newChild);
//
//                //Then we start fiddling with the range for our new child.
//                //First we get the base from the parent and extract the necessary
//                //step, remainder and newNodeIndex-values.
//                //Step is the amount (in range) that the new child will get. However
//                //we still need to figure out what part of the parents range we
//                //are going to assign to the new node.
//                //
//                //Example: Parent range: 5 - 25, step: 5
//                //Possible new range for node is: (5-10), 11-15, 16-20 and 21-25
//                //Based on the newNodeIndex one of these ranges will be assigned
//                //to the new node. The first range is in parenthesis because it 
//                //is by default locked to the parent.
//                Range base = parent.serverNode.getRange();                
//                
//                long step = (long)Math.floor((base.endRange - base.startRange) / 4);
//                int remainder = (int)((base.endRange - base.startRange) % 4L);
//                int newNodeIndex = parent.children.size()-1;
//
//                //calculate range
//                switch(newNodeIndex){
//                    case 0:
//                        node.setRange(new Range(base.startRange + (step * 1) + 1, base.startRange + (step * 2)));
//                        break;
//                    case 1:
//                        node.setRange(new Range(base.startRange + (step * 2) + 1, base.startRange + (step * 3)));
//                        break;
//                    case 2:
//                        node.setRange(new Range(base.startRange + (step * 3) + 1, base.startRange + (step * 4) + remainder));
//                        break;
//                }
//
//                //Since we have added the node, we jump out of the loop
//                break;
//            }else{
//                //If this node was full we add it's children to the queue so that
//                //we may process them as well.
//                iter = parent.toIterator();
//                while(iter.hasNext()){
//                    nodes.add(iter.next());
//                }
//                iter = null;
//            }
//        }
//    
//        //Return the new node.
//        return node;
//    }
    
    public boolean containsID(String id){
        return flatNodes.containsKey(id);
    }
    
    public TreeNode getNode(String id){
        return flatNodes.get(id);
    }

    public TreeNode getRoot(){
        return root;
    }
    
    public Collection<TreeNode> getFlatNodes(){
        return flatNodes.values();
    }
    
    public void printTreePreOrder(String prefix, TreeNode node){
        System.out.println(prefix + "\t" + node.serverNode.toString());
        Iterator<TreeNode> i = node.toIterator();
        while(i.hasNext())
            printTreePreOrder(prefix + node.serverNode.getId() + ":", i.next());
    }
    
//    public static void main(String[] args) {
//        long start = 0x00000005;
//        long end = 0x00000019;
//        long step = (end - start) / 4;
//        String range1 = (start + (step * 0)) + " - " + (start + (step * 1));
//        String range2 = (start + (step * 1) + 1) + " - " + (start + (step * 2));
//        String range3 = (start + (step * 2) + 1) + " - " + (start + (step * 3));
//        String range4 = (start + (step * 3) + 1) + " - " + (start + (step * 4) + (end - start) % 4);
//        
//        //I am forth
//        System.out.println("Start = " + start + " 0x" + HexConverter.toHex((int)start));
//        System.out.println("End = " + end + " 0x" + HexConverter.toHex((int)end));
//        System.out.println("Total = " + (end - start));
//        System.out.println("Step = " + step);
//        System.out.println("range 1 = " + range1);
//        System.out.println("range 2 = " + range2);
//        System.out.println("range 3 = " + range3);
//        System.out.println("range 4 = " + range4);
//        
//        long start2 = (start + (step * 2) + 1);
//        long end2 = (start + (step * 3));
//        long step2 = (end2 - start2) / 4L;
//        String range12 = (start2 + (step2 * 0)) + " - " + (start2 + (step2 * 1));
//        String range22 = (start2 + (step2 * 1) + 1) + " - " + (start2 + (step2 * 2));
//        String range32 = (start2 + (step2 * 2) + 1) + " - " + (start2 + (step2 * 3));
//        String range42 = (start2 + (step2 * 3) + 1) + " - " + (start2 + (step2 * 4) + (end2 - start2) % 4);
//        
//        //I am forth
//        System.out.println("Start = " + start2 + " 0x" + HexConverter.toHex((int)start2));
//        System.out.println("End = " + end2 + " 0x" + HexConverter.toHex((int)end2));
//        System.out.println("Total = " + (end2 - start2));
//        System.out.println("Step = " + step2);
//        System.out.println("range 1 = " + range12);
//        System.out.println("range 2 = " + range22);
//        System.out.println("range 3 = " + range32);
//        System.out.println("range 4 = " + range42);
//        
//        Range r = new Range(0x0, 0xF);
//        r.startRange = r.startRange << 4;
//        r.endRange = r.endRange << 4 | 0xF;
//        
//        System.out.println("r start = " + HexConverter.toHex((int)r.startRange));
//        System.out.println("r end = " + HexConverter.toHex((int)r.endRange));
//        
//        
//        System.out.println("0x00000000 = " + 0x00000000);
//        System.out.println("0xFFFFFFFF = " + 0xFFFFFFFFL);
//        System.out.println("0x00000400 = " + 0x00000400);
//        System.out.println("0xF7FFFFFF = " + 0xF7FFFFFFL);
//    }
}
