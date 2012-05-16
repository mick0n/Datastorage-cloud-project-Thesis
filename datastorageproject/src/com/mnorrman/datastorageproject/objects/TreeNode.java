/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject.objects;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author Mikael
 */
public class TreeNode {
    
    private Set<TreeNode> children;
    private Pair<String, ServerNode> thisChild;
    
    public TreeNode(Pair<String, ServerNode> child){
        children = new HashSet<TreeNode>();
        this.thisChild = child;
    }
    
    public boolean addChild(TreeNode node){
        return children.add(node);
    }
    
    public boolean removeChild(TreeNode node){
        return children.remove(node);
    }
    
    public Iterator<TreeNode> toIterator(){
        return children.iterator();
    }
    
    public Pair<String, ServerNode> getThisChild(){
        return thisChild;
    }
    
}
