package edu.hmc.jane;
import java.util.Vector;

//same as Node, but allows more than 2 children.
public class PolytomyNode extends Node {
    public Vector<Integer> children;

    public PolytomyNode(int numPolytomy) {
        super();
        children = new Vector<Integer>();
        polytomyName = numPolytomy;
    }    
}
