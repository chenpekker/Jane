package edu.hmc.jane;

public class Node {

    // node member variables
    public int parent; // store -1 for root
    public int Lchild;
    public int Rchild; // both store -1 if node is a tip
    public String name; // species name for tips
    public int polytomyName; // in which polytomy?  -1 means this node isn't in 
                        // a polytomy

    public Node() {
        parent = -1;
        Lchild = -1;
        Rchild = -1;
        polytomyName = -1;
    }
}
