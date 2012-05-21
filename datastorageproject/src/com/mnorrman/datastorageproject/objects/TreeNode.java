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
    protected TreeNode parentNode;
    protected Set<TreeNode> children;
    protected ServerNode serverNode;

    public TreeNode(TreeNode parentNode, ServerNode serverNode) {
        children = new HashSet<TreeNode>();
        this.parentNode = parentNode;
        this.serverNode = serverNode;
    }

    public boolean addChild(TreeNode node) {
        return children.add(node);
    }

    public boolean removeChild(TreeNode node) {
        return children.remove(node);
    }

    public Iterator<TreeNode> toIterator() {
        return children.iterator();
    }

    public ServerNode getServerNode() {
        return serverNode;
    }

    public TreeNode getParentNode() {
        return parentNode;
    }
    
}
