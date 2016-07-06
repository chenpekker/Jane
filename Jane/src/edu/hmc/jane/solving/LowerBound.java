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
 * @author cssummer16
 */

public class LowerBound {
    
    public long Infinity = Long.MAX_VALUE;
    public Tree HostTree;
    public Tree ParaTree;
    public Phi Phi;
    public static int dval;
    public static int lval;
    public static int tval;
    
    /*
    Definitly dont know what type root edge name is so we should check on that
    */
    public int[] preorder(Tree tree, Tree rootEdgeName){
        return new int[0];
    }
    
    public int[] postorder(Tree tree, Tree rootEdgeName){
        return new int[0];
    }
    
    public int[][] DP(Tree hostTree, Tree parasiteTree, Phi phi, int D, int T, int L){
        return new int[0][0];
    }
    
    //The DP in python has functions findBest and findPath, but they are not used anywhere in the DP, but they may be usefull to write the code
            }