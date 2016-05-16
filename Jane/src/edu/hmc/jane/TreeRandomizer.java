/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane;

import org.apache.commons.math.special.Gamma;

/**
 *
 * @author Tselil
 */
public class TreeRandomizer {
    static private class Interval {
        int minElement;
        int maxElement;

        public Interval(int min, int max) {
            minElement = min;
            maxElement = max;
        }

        public int size() {
            return maxElement - minElement + 1;
        }

        public int numTips() {
            return (size() + 1) / 2;
        }

        @Override
        public String toString() {
            return "( "+minElement+", "+maxElement+" )";
        }
    }

    // DATA MEMBERS:
    static double beta;

    private static double getRelativeProbability(int n, int i) {
        return Math.exp(Gamma.logGamma(1 + beta + i) 
                            + Gamma.logGamma(1 + beta - i + n)
                            + Gamma.logGamma(n)
                            - Gamma.logGamma(2 + 2 * beta + n)
                            - Gamma.logGamma(i)
                            - Gamma.logGamma(n - i));
    }

    private static int drawRandFromBeta(int intervalSize) {
        double sum = 0.0;
        
        for (int i = 1; i < intervalSize - 1; i++)
            sum += getRelativeProbability(intervalSize - 1, i);
        
        double y = Jane.rand.nextDouble() * sum;
        
        for (int i = 1; i < intervalSize - 1; i++){
            y -= getRelativeProbability(intervalSize - 1, i);
            if (y < 0)
                return i;
        }
        
        return intervalSize - 1;
    }

    public static void  generateRandom(Tree random, double b) {
        beta = b;
        // contains all nodes
        Interval all = new Interval(0, random.size - 1);

        // construct parent-child relationships
        random.root = splitInterval( random, all );
        random.preComputePreOrderTraversal();
        // random.precomputeDistance();
    
        random.precomputePostOrderTraversal();
    }

    private static int splitInterval(Tree tree, Interval interval) {
        if (interval.size() == 1) {
            tree.tips.add(interval.minElement); // Add the tip to
                                                // the tree's tip set
            return interval.minElement;
        } else {
            int treeIndex
                = interval.minElement + 2 * drawRandFromBeta(interval.numTips())
                  - 1;
            int Lchild = splitInterval(tree, new Interval(interval.minElement,
                                                          treeIndex - 1));
            int Rchild = splitInterval(tree, new Interval(treeIndex + 1,
                                                          interval.maxElement));
            tree.node[treeIndex].Lchild = Lchild;
            tree.node[treeIndex].Rchild = Rchild;
            tree.node[Lchild].parent = treeIndex;
            tree.node[Rchild].parent = treeIndex;
            return treeIndex;
        }
    }
}
