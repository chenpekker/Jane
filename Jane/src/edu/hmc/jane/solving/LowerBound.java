/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.solving;

import java.util.*;
import edu.hmc.jane.Tree;
import edu.hmc.jane.Phi;

/**
 *
 * @author Matt Dohlen, Chen Pekker, Gabe Quiroz
 */

public class LowerBound {
    
    public static int inf = 88888888;
    
    public static int DP(Tree hostTree, Tree parasiteTree, Phi phi, int D, int T, int L){
    /* Takes a hostTree, parasiteTree, tip mapping function phi, and
        duplication cost (D), transfer cost (T), and loss cost (L) and
        returns the DP table C. Cospeciation is assumed to cost 0. */
        int hostSize = hostTree.size;
        int parasiteSize = parasiteTree.size;
        ArrayList<Integer> vhPre = new ArrayList<Integer>(hostSize);
        vhPre = hostTree.preOrder;
        System.out.println(vhPre);
        int[] vhPost = new int[hostSize];
        vhPost = hostTree.postOrder;
        System.out.println(Arrays.toString(vhPost));
        int[] vpPost = new int[parasiteSize];
        vpPost = parasiteTree.postOrder;
        System.out.println(Arrays.toString(vpPost));
        
        HashMap vhMapPost = new HashMap(hostSize + (hostSize / 4));
        HashMap vpMapPost = new HashMap(parasiteSize + (parasiteSize / 4));
        
        for(int i = 0; i < vhPost.length; i++)  
        {
            vhMapPost.put(vhPost[i], i); //stores the hosttree post order values with their indices
        }
        for(int i = 0; i < vpPost.length; i++)
        {
            vpMapPost.put(vpPost[i], i); //stores the parasitetree post order values with their indices
        }
        
        int[][] A = new int[parasiteSize][hostSize];
        int[][] C = new int[parasiteSize][hostSize];
        int[][] O = new int[parasiteSize][hostSize];
        int[][] BestSwitch = new int[parasiteSize][hostSize];
        int[][] Dictionary = new int[parasiteSize][hostSize];
        int[][] Minimums = new int[parasiteSize][hostSize];
        int[][] Obest = new int[parasiteSize][hostSize];
        
        for(int i = 0; i < parasiteSize; i++)
        {
            int vp = vpPost[i];
            int vpLeft = parasiteTree.leftChild(vp);
            int vpRight = parasiteTree.rightChild(vp);
            for(int j = 0; j < hostSize; j++)
            {
                int vh = vhPost[j];
                int vhLeft = hostTree.leftChild(vh);
                int vhRight = hostTree.rightChild(vh);
                if(hostTree.isTip(vh)) //if vh is a tip
                {
                    if(parasiteTree.isTip(vp) && phi.containsAssociation(vh, vp))
                        A[i][j] = 0;
                    else 
                        A[i][j] = inf;
                }
                else //if  vh is not a tip, compute Co
                {
                    int Co = inf;
                    if(!parasiteTree.isTip(vp)) // if vp is also not a tip
                        // compute Co
                        Co = Math.min(C[(int)vpMapPost.get(vpLeft)][(int)vhMapPost.get(vhLeft)] +
                                      C[(int)vpMapPost.get(vpRight)][(int)vhMapPost.get(vhRight)],
                                      C[(int)vpMapPost.get(vpLeft)][(int)vhMapPost.get(vhRight)] +
                                      C[(int)vpMapPost.get(vpRight)][(int)vhMapPost.get(vhLeft)]);
                    // compute Loss
                    int Loss = L + Math.min(C[i][(int)vhMapPost.get(vhLeft)], 
                                            C[i][(int)vhMapPost.get(vhRight)]);
                    A[i][j] = Math.min(Co, Loss);
                }
                int Dup = inf;
                int Switch = inf;
                if(!parasiteTree.isTip(vp)) //if vp is not a tip
                {
                    // compute Dup
                    Dup = D + C[(int)vpMapPost.get(vpLeft)][j] + C[(int)vpMapPost.get(vpRight)][j];
                    //compute Switch (transfer)
                    Switch = T + Math.min(C[(int)vpMapPost.get(vpLeft)][j] + BestSwitch[(int)vpMapPost.get(vpRight)][j],
                                          C[(int)vpMapPost.get(vpRight)][j] + BestSwitch[(int)vpMapPost.get(vpLeft)][j]);
                }
                C[i][j] = Math.min(A[i][j], Math.min(Dup, Switch));
                if(hostTree.isTip(vh)) //if vh is a tip
                    
                    O[i][j] = C[i][j];
                else
                    // compute O
                    O[i][j] = Math.min(C[i][j], 
                            Math.min(O[i][(int)vhMapPost.get(vhLeft)], O[i][(int)vhMapPost.get(vhRight)]));
            }
            // compute BestSwitch values
            BestSwitch[i][(int)vhMapPost.get(vhPre.get(0))] = inf;
            for(int j = 0; j < hostSize; j++)
            {
                int vh = vhPre.get(j);
                int vhLeft = hostTree.leftChild(vh);
                int vhRight = hostTree.rightChild(vh);
                if(vhLeft != -1 && vhRight != -1)
                {
                    BestSwitch[i][(int)vhMapPost.get(vhLeft)] = Math.min(BestSwitch[i][(int)vhMapPost.get(vh)],
                                                                    O[i][(int)vhMapPost.get(vhRight)]);
                    BestSwitch[i][(int)vhMapPost.get(vhRight)] = Math.min(BestSwitch[i][(int)vhMapPost.get(vh)],
                                                                    O[i][(int)vhMapPost.get(vhLeft)]);
                }
            }
        }
        int minSolution = inf;
        int rootIndex = (int)vpMapPost.get(parasiteTree.root);
        for(int i = 0; i < hostSize; i++)
            if(C[rootIndex][i] < minSolution)
                minSolution = C[rootIndex][i];
        return minSolution;
    }
    
    //The DP in python has functions findBest and findPath, but they are not used anywhere in the DP, but they may be usefull to write the code
            }