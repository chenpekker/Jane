/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.solving;

import edu.hmc.jane.Tree;
import edu.hmc.jane.Phi;

/**
 *
 * @author Matt Dohlen, Chen Pekker, Gabe Quiroz
 */

public class LowerBound {
    
    public long Infinity = Long.MAX_VALUE;
    public Tree HostTree;
    public Tree ParaTree;
    public Phi Phi;
    public static int dval;
    public static int lval;
    public static int tval;
    
    public int[] DPprep(Tree tree, Tree root, Tree node, Tree postOrder){ //pretty sure we wont need this entierly
    /*  Takes a tree as input and returns
        a list of the edges in that tree in preorder (high edges to low edges) */
       
        // base case
      
        return new int[0];
    }
    
    public int[][] DP(Tree hostTree, Tree parasiteTree, Phi phi, int D, int T, int L){
    /* Takes a hostTree, parasiteTree, tip mapping function phi, and
        duplication cost (D), transfer cost (T), and loss cost (L) and
        returns the DP table C. Cospeciation is assumed to cost 0. */
  //  ArrayList<Integer> Vp = new ArrayList[hostTree.size];
  //  Vp = hostTree.preOrder;
    
    return new int[0][0];
    }
    
    //The DP in python has functions findBest and findPath, but they are not used anywhere in the DP, but they may be usefull to write the code
            }