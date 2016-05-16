package edu.hmc.jane.solving;

/*
 * Copyright (c) 2009, Chris Conow, Daniel Fielder, Yaniv Ovidia, Ran
 * Libeskind-Hadas All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. Neither the name of the Harvey Mudd College nor the
 * names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
import edu.hmc.jane.Jane;
import edu.hmc.jane.CostModel;
import edu.hmc.jane.Phi;
import edu.hmc.jane.ProblemInstance;
import edu.hmc.jane.TimeZones;
import edu.hmc.jane.Tree;
import edu.hmc.jane.TreeRegions;
import edu.hmc.jane.TreeSpecs;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class Heuristic {

    Tree hostTree;
    Tree paraTree;
    TreeRegions hostRegions;
    Phi phi;
    TimeZones timeZones;
    boolean tarzan;
    CostModel costs;

    public Heuristic(Tree hTree, Tree pTree, TreeRegions hostRegions, Phi mapping, TimeZones timeZones, CostModel cost) {
        this.hostTree = hTree;
        this.paraTree = pTree;
        this.hostRegions = hostRegions;
        this.phi = mapping;
        this.timeZones = timeZones;
        this.costs = cost;
    }

    /*
     * Runs solver a bunch of times, returns the time it took to run using the
     * specified ExecutorService. Also includes time for reproductions
     */
    public double multiSolve(int solves, ExecutorService e, double mutationRate, double selectStr) throws TreeSpecs.InconsistentTreeException, TreeFormatException {
        Vector<CostSolver> vs = new Vector<CostSolver>();

//        for (int i = 0; i < solves; ++i) {
//            if (!hostTree.hasPolytomy) {
//                TreeSpecs host = hostTree.getRandomSpecs(timeZones, paraTree);
//                CostSolver s = new CostSolver(host, phi, timeZones, costs);
//                vs.add(s);
//            } else {
//                TreeSpecs host = hostTree.getRandomSpecs(timeZones, paraTree);
//                // correct the phi for the new entries.
//                Phi newPhi = phi.newWithoutPolytomies(host.parasiteTree.size - phi.length());
//                CostSolver s = new CostSolver(host, newPhi, host.timeZones, costs);
//                vs.add(s);
//            }
//        }
        try {
            double initialTime = System.currentTimeMillis();
//            e.invokeAll(new LinkedList<Callable<CostSolver>>(vs));
            ProblemInstance prob = new ProblemInstance(hostTree, paraTree, hostRegions, phi, timeZones);
            Generation population = Generation.getRandomPopulation(prob, costs, solves, selectStr, 0);
            Generation next = genNew(population, selectStr);
            return ((System.currentTimeMillis() - initialTime) / (1000d * solves));
        } catch (Exception exec) {
            throw new Error("Solve threads have been interrupted.");
        }
//        catch (InterruptedException ex) {
//            throw new Error("Solve threads have been interrupted.");
//        } 
    }

    /*
     * This is the heuristic used by Jane. It is a runEvolution algorithm which
     * creates a random population of a given size and completely replaces the
     * population every generation by crossing over individuals until a new
     * population of the same size is created.
     */
    public Generation runEvolution(int numIter, int popSize, double mutationRate, double selectStr, int numSolnsKept)
            throws NoValidSolutionException, TreeSpecs.InconsistentTreeException, TreeFormatException {
        ProblemInstance prob = new ProblemInstance(hostTree, paraTree, hostRegions, phi, timeZones);
        Generation population = Generation.getRandomPopulation(prob, costs, popSize, selectStr, numSolnsKept);

        // solve the initial population and print best cost
        if (Jane.VERBOSE) {
            System.out.println("Best Initial Cost: " + population.getBestCostEver());
        }
               
        // Iterate through the specified number of generations
        for (int iter = 1; iter < numIter; iter++) {
            // generate a new population from the old one
            population = genNew(population, mutationRate);

            if (Jane.VERBOSE) {
                System.out.println("Generation " + (iter + 1) + ", best cost: "
                        + population.getBestCostEver()
                        + " with " + population.getNumUniqueBestEverTimingsFound()
                        + " total unique timings found at best cost");
            }
        }
        
        //TODO: Investigate the necessity of this line. 
        population.ensureSolved();
        
        if (Jane.VERBOSE) {
            System.out.println("\nBest Cost: " + population.getBestCostEver());
        }

        return population;
    }

    public int runSimpleEvolution(int numIter, int popSize, double mutationRate, double selectStr)
            throws NoValidSolutionException, TreeSpecs.InconsistentTreeException, TreeFormatException {
        ProblemInstance prob = new ProblemInstance(hostTree, paraTree, hostRegions, phi, timeZones);
        Generation population = Generation.getRandomPopulation(prob, costs, popSize, selectStr, 0);

        for (int iter = 1; iter < numIter; iter++) {
            population = genNew(population, mutationRate);
        }
        return population.getBestCostEver();
    }

    // helper for runEvolution. Generates the new population.
    Generation genNew(Generation oldGeneration, double mutationProb) throws TreeSpecs.InconsistentTreeException, NoValidSolutionException {
        TreeSpecs parent1, parent2, child;
        List<TreeSpecs> newPopulation = new ArrayList<TreeSpecs>(oldGeneration.size());
        for (int i = 0; i < oldGeneration.size(); i++) {
            // Select two solvers randomly and cross them
            parent1 = oldGeneration.weightedSelect();
            parent2 = oldGeneration.weightedSelect();
            child = reproduce(parent1, parent2);
            
            // Mutate with probability mutationProb
            if (Jane.rand.nextDouble() < mutationProb) {
                child.mutate();
            }
            if (child.hostTree.conseqPoly) {
                child.checkPolytomyTiming();
            }
            newPopulation.add(child);
        }

        return new Generation(newPopulation, oldGeneration);
    }

    /*
     * this is the crossover method. It takes two individual timings and creates
     * a valid new one by randomly choosing a crossover node, maintaining the
     * timing of the subtree of the crossover node timing A and the
     * non-conflicting times from timing B, then reconciling the conflicting
     * times.
     */
    private TreeSpecs reproduce(TreeSpecs subTiming, TreeSpecs superTiming) throws TreeSpecs.InconsistentTreeException {
        
        // No crossover necessary when there are no nodes between root and tips.
        if (subTiming.tipTime - subTiming.timeOfNode(subTiming.hostTree.root) <= 1) {
            return subTiming;
        }
        int finalTime = subTiming.tipTime;
        int crossTimeTiming = Jane.rand.nextInt(finalTime - 2) + 2;
        // finds node at cross time.  But, if this is a polytomy-node, it finds the correct version.
        int crossNode = subTiming.hostTree.findStartNode(subTiming.nodeAtTime(crossTimeTiming)); // This is the problem!!!!

        // these maps are used when polytomies exist:
        Map<Integer, Integer> subNtOHost = null;
        Map<Integer, Integer> superNtOHost = null;
        Map<Integer, Integer> subNtOPara = null;
        Map<Integer, Integer> superNtOPara = null;
        Map<Integer, Integer> subOtNHost = null;
        Map<Integer, Integer> superOtNHost = null;
        Map<Integer, Integer> subOtNPara = null;
        Map<Integer, Integer> superOtNPara = null;


        Tree newHost = subTiming.hostTree; // if there is no polytomy
        if (hostTree.isPolytomyResolution()) {
            subNtOHost = new TreeMap<Integer, Integer>();
            superNtOHost = new TreeMap<Integer, Integer>();
            subOtNHost = new TreeMap<Integer, Integer>();
            superOtNHost = new TreeMap<Integer, Integer>();
            newHost = superTiming.hostTree.crossTrees(subTiming.hostTree, crossNode, superNtOHost, superOtNHost, subNtOHost, subOtNHost);
        }

        Tree newPara = subTiming.parasiteTree;
        if (this.paraTree.isPolytomyResolution()) {
            int paraNode = Jane.rand.nextInt(subTiming.parasiteTree.lastRealNode + 1);
            subNtOPara = new TreeMap<Integer, Integer>();
            superNtOPara = new TreeMap<Integer, Integer>();
            subOtNPara = new TreeMap<Integer, Integer>();
            superOtNPara = new TreeMap<Integer, Integer>();
            newPara = superTiming.parasiteTree.crossTrees(subTiming.parasiteTree, paraNode, superNtOPara, superOtNPara, subNtOPara, subOtNPara);
        }

        TimeZones newTimeZones = timeZones.newWithoutPolytomies(newHost.size - hostTree.size, newPara.size - paraTree.size, newHost, newPara);
        TreeSpecs result = new TreeSpecs(newHost, newPara, newTimeZones);
        Vector<Integer> subTreeList = new Vector<Integer>();
        Vector<Integer> superTreeList = new Vector<Integer>();
        
        // Put the nodes from the cross subtree in subTreeList, and the rest of
        // the nodes in the superTreeList
        getNodesFromTiming(crossNode, -1, subTiming, subTreeList, subOtNHost);
        getNodesFromTiming(subTiming.hostTree.root, crossNode, superTiming, superTreeList, superOtNHost);
        // Build the result timing using the node lists

        reconcileTimings(result, superTreeList, subTreeList, superTiming, subTiming, crossNode, superNtOHost, subNtOHost);

        if (result.hostTree.conseqPoly) {
            result.checkPolytomyTiming();
        }
        if (!result.isConsistent()) {
            throw new TreeSpecs.InconsistentTreeException("An inconsistent tree was created by the genetic algorithm.");
        }
        
        return result;
    }

    /*
     * this is a helper method for reproduce. It will fill a supplied vector
     * with all of the internal nodes in the subtree of startNode, excluding
     * nodes in the subtree of endNode. NOTE: the vector is filled in the order
     * of the timings of the nodes; earlier timing will appear before later
     * timing.
     *
     * Polytomy: will return a list of nodes WITH THE NAMES in the child. Since
     * polytomy nodes have variable names, some converstion is necessary.
     * nodeMap holds the way we rename these nodes. It is null if there are no
     * polytomies.
     */
    private void getNodesFromTiming(int startNode, int endNode, TreeSpecs timing, Vector<Integer> nodeList, Map<Integer, Integer> nodeMap) {
        Vector<Integer> nodeToProcess = new Vector<Integer>();
        Integer newNode = -1;
        int tempTime;
        boolean onlyEndNode = false;
        if (!timing.isOriginalNode(startNode, true)) {
            // there is polytomy
            nodeList.add(nodeMap.get(startNode));
        } else {
            nodeList.add(startNode);
        }
        timing.hostTree.addChildrenOf(startNode, nodeToProcess);

        // BFS: Loop through the nodes left to proccess, adding them to the node
        // list in the order of their timings. Add the children of the earliest
        // timing node to the list of nodes left to process, and continue until
        // the tip is reached.
        while (!nodeToProcess.isEmpty()) {
            tempTime = timing.tipTime;
            for (Integer tempNode : nodeToProcess) {
                if (timing.timeOfNode(tempNode) <= tempTime) {
                    // Ignore the subtree of endNode, and continue to explore
                    // the rest of the tree.
                    if (tempNode != endNode) {
                        tempTime = timing.timeOfNode(tempNode);
                        newNode = tempNode;
                        // If there is only one node in the nodes left to process
                        // and that node contains the subtree we don't want, break.
                    } else {
                        onlyEndNode = (nodeToProcess.size() == 1);
                    }
                }
            }
            if (onlyEndNode || timing.hostTree.isTip(newNode)) {
                break;
            }
            if (!timing.isOriginalNode(newNode, true)) {
                // there is polytomy.  rename appropriately.
                nodeList.add(nodeMap.get(newNode));
            } else {
                nodeList.add(newNode);
            }
            nodeToProcess.remove(newNode);
            timing.hostTree.addChildrenOf(newNode, nodeToProcess);
        }
    }

    /*
     * a helper function for reproduce. Assigns time to each node based on
     * location in the subtree/supertree.
     */
    private void reconcileTimings(TreeSpecs result, Vector<Integer> superTreeList, Vector<Integer> subTreeList, TreeSpecs superTiming, TreeSpecs subTiming, int crossNode, Map<Integer, Integer> superNtOMap, Map<Integer, Integer> subNtOMap) {
        int subIndex = 0;
        int superIndex = 0;
        /*
         * the node we last added. This ensures that polytomies happen
         * consecutively.
         */
        int prievNode = 0;
        int superNode, superTime, subNode, subTime;
        
        /*
         * Iterate through superNodes until the parent of the node at crossover
         * is found; the nodes that follow in the superList will have timing
         * conflicts with the sub nodes, and the timings need to be resolved.
         *
         */
        while (superIndex < superTreeList.size()) {
            superNode = superTreeList.get(superIndex++);
            if (!result.isOriginalNode(superNode, true)) {
                // this is a polytomy, and needs renaming to the old version
                int actual = superNtOMap.get(superNode);
                superTime = superTiming.timeOfNode(actual);    //Assign time to non-
                result.putNodeAtTime(superNode, superTime);    //conflicting nodes.
            } else {
                superTime = superTiming.timeOfNode(superNode); //Assign time to non-
                result.putNodeAtTime(superNode, superTime);    //conflicting nodes.
            }
            prievNode = superNode;
            /*
             * the cross node may split a polytomy. add the rest of this
             * polytomy
             */

            if (result.hostTree.leftChild(superNode) == crossNode || result.hostTree.rightChild(superNode) == crossNode) {
                break;
            }
        }
        
        // Resolve the timings for the remainder of the nodes
        for (int i = superIndex + 1; i < subTiming.tipTime; i++) {
            if (subIndex < subTreeList.size()) {
                subNode = subTreeList.get(subIndex);
                // find the actual time, depending on polytomies.
                if (!result.isOriginalNode(subNode, true)) {
                    int actual = subNtOMap.get(subNode);
                    subTime = subTiming.timeOfNode(actual);
                } else {
                    subTime = subTiming.timeOfNode(subNode);
                }

                if (superIndex < superTreeList.size()) {
                    superNode = superTreeList.get(superIndex);
                    if (!result.isOriginalNode(superNode, true)) {
                        int actual = superNtOMap.get(superNode);
                        superTime = superTiming.timeOfNode(actual);
                    } else {
                        superTime = superTiming.timeOfNode(superNode);
                    }

                    /*
                     * the prieviously added node and the new one are in the
                     * same polytomy. We need these to happen consecutively.
                     */
                    if (result.hostTree.conseqPoly && result.samePolytomy(subNode, prievNode, true)) {
                        result.putNodeAtTime(subNode, i);
                        prievNode = subNode;
                        subIndex++;
                    } else if (result.hostTree.conseqPoly && result.samePolytomy(superNode, prievNode, true)) {
                        result.putNodeAtTime(superNode, i);
                        prievNode = superNode;
                        superIndex++;
                    } else if (result.timeZones.getHostZone(superNode) < result.timeZones.getHostZone(subNode)) {
                        result.putNodeAtTime(superNode, i);
                        prievNode = superNode;
                        superIndex++;
                    } else if (result.timeZones.getHostZone(superNode) > result.timeZones.getHostZone(subNode)) {
                        result.putNodeAtTime(subNode, i);
                        prievNode = subNode;
                        subIndex++;
                    } else if (Math.abs(subTime - i) < Math.abs(superTime - i)) {
                        result.putNodeAtTime(subNode, i);
                        prievNode = subNode;
                        subIndex++;
                    } else if (Math.abs(subTime - i) > Math.abs(superTime - i)) {
                        result.putNodeAtTime(superNode, i);
                        prievNode = superNode;
                        superIndex++;
                    } else if (Jane.rand.nextDouble() < 0.5) {
                        result.putNodeAtTime(subNode, i);
                        prievNode = subNode;
                        subIndex++;
                    } else {
                        result.putNodeAtTime(superNode, i);
                        prievNode = superNode;
                        superIndex++;
                    }
                } else {
                    // no more super-nodes.  Add the sub-node.
                    result.putNodeAtTime(subNode, i);
                    prievNode = subNode;
                    subIndex++;
                }
            } else if (superIndex < superTreeList.size()) {
                // no more sub-nodes left.
                result.putNodeAtTime(superTreeList.get(superIndex), i);
                prievNode = superTreeList.get(superIndex);
                superIndex++;
            } else {
                throw new RuntimeException("Error when breeding host timings");
            }
        }
    }

    /*
     * exception for when the entire popluation ends up having infinite costs
     * which means that there arent any valid solutions
     *
     */
    public static class NoValidSolutionException extends Exception {

        public NoValidSolutionException() {
        }

        public NoValidSolutionException(String s) {
            super(s);
        }
    }
    
    public static final String noValidSolnMessage = "\n\nThis was likely caused by time zones combined with limited " + "host switch distance preventing necessary host switches";

    // this exception gets thrown when the file format is correct, but the
    // information inside of it implies something impossible. i.e. a Node being
    // forced to have a time zone that is earlier than its parent's
    public static class TreeFormatException extends Exception {

        public TreeFormatException() {
        }

        public TreeFormatException(String s) {
            super(s);
        }
    }
}
