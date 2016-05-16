/**
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package beetree;

import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JLabel;

/**
 *
 * @author beametitiri
 * Modified by Nicole Wein
 */
public class DrawTree {
    public static int ROOT_TYPE = 0;
    public static int INTERNAL_TYPE = 1;
    public static int LEAF_TYPE = 2;
    public static int HOST_TYPE = 0;
    public static int PARASITE_TYPE = 1;

    public class Node implements Comparable<Node> {
        private JLabel label;
        private ArrayList<Node> children = new ArrayList<Node>();
        private Node parent;
        private ArrayList<Node> link = new ArrayList<Node>();
        private int x, y; // Position of node in template
        private int type; // Type of node
        private int treeType;
        private int depth;
        private int minTime;
        private int maxTime;
        private int region;

        public Node(int newx, int newy, Node newparent, int newdepth, int t) {
            x = newx;
            y = newy;
            parent = newparent;
            depth = newdepth;
            label = new JLabel();
            treeType = t;  
        }

        public int getTreeType() {
            return treeType;
        }
    
        public int getDepth() {
            return depth;
        }
    
        public void setDepth(int d) {
            depth = d;
        }

        public void setLabel(String newlabel) {
            label.setText(newlabel);
            int labelx = x;
            
            // Writes labels to the left of node if host, else to the right
            if (treeType == HOST_TYPE) 
                labelx += label.getWidth() - 50;
            else
                labelx += 10;
            
            int labely = y - label.getHeight();
            label.setLocation(labelx, labely);
        }

        public JLabel getLabel() {
            return label;
        }

        public int getType() {
            return type;
        }

        public void setType(int newtype) {
            type = newtype;
        }

        public ArrayList<Node> getChildren() {
            return children;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public Node getParent() {
            return parent;
        }
    
        public void setParent(Node p) {
            parent = p;
        }

        public void setX(int newx) {
            x = newx;
            int labelx = newx;
            
            if (treeType == HOST_TYPE)
                labelx = labelx - 50; 
            else
                labelx = labelx + 10;
            
            label.setLocation(labelx, label.getY());
        }

        public void setY(int newy) {
            y = newy;
            label.setLocation(label.getX(), newy);
        }
        
        /**
         * Returns the node in this node's subtree with the x closest to this
         * node
         */
        public Node getClosest() {
            int proximity = Integer.MAX_VALUE;
            Node closeNode = null;
            Node current;
            int currprox;
            
            for (int i = 0; i < children.size(); i++) {
                current = children.get(i);
                currprox = Math.abs(current.getX() - x);
                
                if (currprox < proximity) {
                    proximity = currprox;
                    closeNode = current;
                }
            }
            
            return closeNode;
        }

        /**
         * dependent on order of creation being top to bottom
         */
        public Node getLowest() {
            /**
             * Returns the node in this node's subtree with the highest y value
             * (positioned lowest on the screen)
             */
            if (children.isEmpty())
                return this;

            Node current = children.get(children.size() - 1);
            
            while (current.type != LEAF_TYPE)
                current = current.children.get(current.children.size() - 1);

            return current;
        }
        
        /**
         * Adds a link between this leaf node and a leaf node on another tree
         */
        public void addLink(Node tolink) {
            link.add(tolink);
        }

        public Node getLastChild() {
            return this.children.get(this.children.size() - 1);
        }
    
        public ArrayList<Node> getLink() {
            return link;
        }
    
        public void setLink(ArrayList<Node> linkList) {
            link = linkList;
        }
        
        /**
         * Returns a string representation of this node
         */
        @Override
        public String toString() {
            return "Node (" + x + ", " + y + ": " + type + ")";
        }
        
        /**
         * Return -1, 0 , or 1 if this node is higher, at the same height, or
         * lower than the specified node. This is used strictly for dealing with
         * the spacial arrangement of leaf nodes
         */
        public int compareTo(Node o) { 
            if (y < o.getY())
                return - 1;
            if (y > o.getY())
                return 1;
            else
                return 0;
        }
    
        public int getRegion() {
            return region;
        }
    
        public void setRegion(int r) {
            region = r;
        }
    
        public int getMinTime() {
            return minTime;
        }
    
        public void setMinTime(int min) {
            minTime = min;
        }
    
        public int getMaxTime() {
            return maxTime;
        }
    
        public void setMaxTime(int max) {
            maxTime = max;
        }
    
        /**
         * Calculates depth of node
         */
        public int findDepth() {
            if (parent == null)
                return 0;
            else
                return 1+parent.findDepth();
        }
    
        /**
         * Moves subtree of node an amount "diff" in the y direction
         */
        public void moveSubtree(int diff, int maxY) {
            for (Node child : getChildren()) {
                int newY = child.getY() + diff;
                child.setY(newY);
                child.moveSubtree(diff, maxY);
            }
        }
        
        /**
         * Returns list of leaves with given node as ancestor
         */
        private ArrayList<Node> makeLeafList(ArrayList<Node> subLeafList) {
            if (type == LEAF_TYPE)
                subLeafList.add(this);
            else {
                for (Node child : children)
                    child.makeLeafList(subLeafList);
            }
            
            Collections.sort(subLeafList);
            return subLeafList;
        }
        
        /**
         * Returns descendent of node with highest y-value
         */
        private Node getLastDescendent() {
            if (type == LEAF_TYPE)
                return this;
            else {
                int maxY = 0;
                Node maxChild = null;
                
                for (Node child : children) {
                    if (child.y > maxY) {
                        maxY = child.y;
                        maxChild = child;
                    }
                }
                
                return maxChild.getLastDescendent();
            }
        }
        
        /**
         * Returns descendent of node with lowest y-value
         */
        private Node getFirstDescendent() {
            if (type == LEAF_TYPE)
                return this;
            else {
                int minY = 99999;
                Node minChild = null;
                
                for (Node child : children) {
                    if (child.y < minY) {
                        minY = child.y;
                        minChild = child;
                    }
                }
                
                return minChild.getFirstDescendent();
            }
        }
    }

    // Tree 
    private Node root;
    private ArrayList<Node> nodes;
    private ArrayList<Node> leaves;
    private int type;
    private int maxdepth;
    private int x, y; // The top left corner of this tree's space
    private int width, height; // The dimensions of this tree's space

    public DrawTree(int newx, int newy, int w, int h, int newtype) {
        x = newx;
        y = newy;
        width = w;
        height = h;
        type = newtype;
        
        if (type == HOST_TYPE)
            root = new Node(x + 10, 0, null, 0, type);
        else
            root = new Node(x + width - 10, 0, null, 0, type);
        
        root.setType(ROOT_TYPE);
        root.setDepth(0);
        nodes = new ArrayList<Node>();
        leaves = new ArrayList<Node>();
        nodes.add(root);
        root.setRegion(0);
        root.setMinTime(1);
        root.setMaxTime(1);
        maxdepth = 0;
    }

    public int getX() {
        return x;
    }
    
    /**
     * Update top left
     */ 
    public void setX(int newX) {
        x = newX;
    }
    
    /**
     * Changes tree width, used when we stretch window
     */
    public void setWidth(int newWidth) {
        width = newWidth;
    }
    
    public ArrayList<Node> getLeaves() {
        return leaves;
    }

    public int getNodeID(Node n, int off) {
        return nodes.indexOf(n)+1+off;
    }

    /**
     * Returns the root of the tree 
     */
    public Node getRoot() {
        return root;
    }
    
    public void setRoot(Node node) {
        root = node;
    }

    /**
     * Returns the number of nodes in the tree
     */
    public int getSize() {
        return nodes.size();
    }

    /**
     * Returns the list of nodes in this tree
     */
    public ArrayList<Node> getNodes() {
        return nodes;
    }

    public int getMaxdepth() {
        return maxdepth;
    }
    
    public void setMaxdepth(int depth) {
        maxdepth = depth;
    }
    
    /**
     * Adds a child to the specified node
     */
    public void addChild(Node parent) { 
        int newdepth = parent.depth + 1;
        
        if (newdepth > maxdepth)
            maxdepth = newdepth;

        if (parent.link != null) {
            for (DrawTree.Node linkedNode : parent.link)
                linkedNode.link.remove(parent);
            parent.link.clear();
        }
        
        int leafy = parent.getLowest().getY() + 1;
        
        Node newnode = new Node(parent.getX(), leafy, parent, newdepth, type); 
        newnode.setType(LEAF_TYPE);
        newnode.setRegion(parent.getRegion());
            
        leaves.remove(parent);

        parent.children.add(newnode);
        
        if (parent.type != ROOT_TYPE)
            parent.setType(INTERNAL_TYPE);
        
        nodes.add(newnode);
        addLeaf(newnode);   
    }
    
    public void addLeaf(Node leaf) {
        leaves.add(leaf);
        Collections.sort(leaves);
    }
    
    /**
     * Remove the given node and its children from the tree
     */
    public void removeChild(Node child) {
        // Delete parent's reference and see if parent is now a leaf
        if (child.type != ROOT_TYPE) {
            Node parent = child.parent;
            parent.children.remove(child);
            
            if (parent.children.isEmpty() && parent.type != ROOT_TYPE) {
                parent.setX(leaves.get(0).getX());  
                parent.setType(LEAF_TYPE);
                int leafTime = leaves.get(0).getMaxTime();
                parent.setMinTime(leafTime);
                parent.setMaxTime(leafTime);
                addLeaf(child.parent);
                parent.setX(leaves.get(0).getX());  
            }
            
            nodes.remove(child);
            leaves.remove(child);
            deleteSubtree(child);
            
            if (parent.children.size() == 1) {
                Node otherChild = parent.children.get(0);
                
                if (otherChild.children.isEmpty())
                    removeChild(otherChild);
                else {
                    parent.children = otherChild.children;
                    
                    for (Node c : otherChild.children)
                        c.parent = parent;
                    
                    nodes.remove(otherChild);
                }
            }
        }
    }

    /**
     * Deletes subtree with given root from node/leaf lists
     */
    private void deleteSubtree(Node root) {
        boolean changeMaxDepth = false;
        
        for (Node c : root.getChildren()) {
            nodes.remove(c);
            leaves.remove(c);
            deleteSubtree(c);
            if (c.depth == maxdepth) 
                changeMaxDepth = true; 
        }
        
        // May need to change maxdepth if child's descendents reach maxdepth
        if (changeMaxDepth) { 
            maxdepth = 0;
            
            for (int i = 0; i < leaves.size(); i++) {
                int leafDepth = leaves.get(i).depth;
                if (leafDepth > maxdepth)
                    maxdepth = leafDepth;
            }
        }
    } 
    
    /**
     * Returns x-coordinate of non-leaf node closest to leaves
     */
    public int getLeafClosest() {
        int closest = 0;
        
        for (Node leaf : leaves) {
            if (Math.abs(leaf.getX() - leaf.parent.getX())
                    < Math.abs(leaf.getX() - closest))
                closest = leaf.parent.getX();
        }
        
        return closest;
    }
    
    /**
     * If coordinate on branch return node of that branch
     */
    public Node findBranch(int x, int y) {
         for (Node current : getNodes())
             if (current != root) {
                 if ((x > current.getX() && x < current.parent.getX())
                        || (x < current.getX() && x > current.parent.getX())) {
                     if (y < current.getY() + 5 && y > current.getY() - 5)
                        return current;
                 }
             }
         return null;
    }
    
    /**
     * Reorders leaves if branch moved
     */
    public void redoLeafList(Node node) {
        ArrayList<Node> subLeafList = node.makeLeafList(new ArrayList<Node>());
        ArrayList<Node> newLeafList = new ArrayList<Node>();
        Node closestLeaf = findClosestLeaf(node);
        
        if (closestLeaf == null) {
            for (Node subLeaf : subLeafList)
                newLeafList.add(subLeaf);
        }
        
        for (Node leaf : leaves) {
            boolean contains = false;
            
            for (Node subLeaf : subLeafList) {
                if (leaf == subLeaf)
                    contains = true;
            }
            
            if (!contains)
                newLeafList.add(leaf);
            
            if (leaf == closestLeaf) {
                for (Node subLeaf : subLeafList)
                    newLeafList.add(subLeaf);
            } 
        }
        
        leaves.clear();
        
        for (Node leaf : newLeafList)
            leaves.add(leaf);
        
        newLeafList.clear();
    }
     
    /**
     * Returns leaf that will be right before node in leaf list in new tree
     */
    public Node findClosestLeaf(Node node) {
        Node closestSibling = findClosestSibling(node);
        
        if (closestSibling != null)
            return closestSibling.getLastDescendent();
        else {
            if (node.parent == root)
                return null;
            else
                return findClosestLeaf(node.parent);
        }
    }
     
    /**
     * Returns sibling of node above and vertically closest to node
     * in new tree (with moved branch)
     */
    private Node findClosestSibling(Node node) {
        Node closestSibling = null;
        int closest = 99999;
        
        for (Node sibling : node.parent.children) {
            int dist = node.y - sibling.y;
            
            if (dist > 0 && dist < closest) {
                closest = dist;
                closestSibling = sibling;
            }
        }
        
        return closestSibling;
    }
     
    /**
     * Returns leaf that will be right after node in leaf list in new tree
     */
    public Node findClosestLeafBelow(Node node) {
         Node closestSibling = findClosestSiblingBelow(node);
         
         if (closestSibling != null)
             return closestSibling.getFirstDescendent();
         else {
             if (node.parent == root)
                 return null;
             else
                return findClosestLeafBelow(node.parent);
         }
     }
     
    /**
     * Returns sibling of node above and vertically closest to node in
     * new tree (with moved branch)
     */
    private Node findClosestSiblingBelow(Node node) {        
        Node closestSibling = null;
        int closest = 99999;
        
        for (Node sibling : node.parent.children) {
            int dist = sibling.y - node.y;
            
            if (dist > 0 && dist < closest) {
                closest = dist;
                closestSibling = sibling;
            }
        }
        
        return closestSibling;
    }
     
    /**
     * Reorders nodes if branch moved
     */
    public void redoNodeList(Node moved) {
        int index = getNodes().indexOf(moved);

        for (Node otherChild : moved.parent.getChildren()) {
            int otherChildIndex = getNodes().indexOf(otherChild);
            
            // Moved node moved up past otherChild
            if (otherChildIndex < index && otherChild.getY() > moved.getY()) {
                getNodes().add(otherChildIndex, moved);
                getNodes().remove(index+1);
                break;
            }
        }
        
        for (int i = moved.parent.getChildren().size() - 1; i > -1; i--) {
            Node otherChild = moved.parent.getChildren().get(i);
            int otherChildIndex = getNodes().indexOf(otherChild);
            
            // Moved node moved down past otherChild
            if (otherChildIndex>index && otherChild.getY() < moved.getY()) {
                getNodes().remove(index);
                getNodes().add(otherChildIndex, moved);
                break;
                // Send moved to this index and break from loop and remove
                // original index (no need to shift)
            }
        }
    }
     
     /**
      * Adds all leaves in node's subtree to leaf list (for loading trees)
      */
    public void addLeaves(Node node) {
        if (node.getType() == LEAF_TYPE) {
            boolean contains = false;
            
            for (Node leaf : leaves) {
                if (node == leaf)
                    contains = true;
            }
            
            if (!contains)
                leaves.add(node);
        } else {
            for (Node child : node.getChildren())
                addLeaves(child);
        }
    }
    
    /**
     * Makes .tree file output for tree
     */
    public String makeTreeOutput(int off) {
        String toReturn = "";
        Node current;
        
        for (int i = 0; i < nodes.size(); i++) {
            current = nodes.get(i);
            toReturn += (i + 1 + off);
            
            int numChildren = current.children.size();
            
            if (numChildren == 0)
                toReturn += "\t" + "null";
            else {
                for (int j = 0; j < numChildren; j++) 
                    toReturn += "\t" + (nodes.indexOf(current.children.get(j))
                                            + 1 + off);
            }
            
            toReturn += "\n";
        }
        
        return toReturn;
    }

    /**
     * Makes .tree file output for node labels
     */
    public String makeNameOutput(int off) {
        String toReturn = "";
        Node current;
        for (int i = 0; i < nodes.size(); i++) {
            current = nodes.get(i);
            String label = current.getLabel().getText();
            boolean isNumber = false;
            
            try {
                Integer.parseInt(label);
                isNumber = true;
            } catch (NumberFormatException nfe) {
            }
            
            if (label.length() == 0 || isNumber)
                toReturn += (i + 1 + off) + "\t" + (i + 1 + off) + "\n";
            else
                toReturn += (i + 1 + off) + "\t" + current.getLabel().getText()
                                + "\n";
        }
        
        return toReturn;
    }
    
    /**
     * Makes .tree file output for time zones
     */
    public String makeTimeOutput(int off) {
        String toReturn = "";
        Node current;
        for (int i = 0; i < nodes.size(); i++) {
            current = nodes.get(i);
            if (current.getMinTime() != current.getMaxTime())
                toReturn += (i + 1 + off) + "\t" + current.getMinTime() + "," 
                                + current.getMaxTime() + "\n";
            else
                toReturn += (i + 1 + off) + "\t" + current.getMinTime() + "\n";
        }
        
        return toReturn;
    }
}
