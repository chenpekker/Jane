/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane;

import java.util.*;

/**
 *
 * @author Tselil, John
 */
public class TreeSpecs {

    // DATA MEMBERS
    // TODO: no longer public!!!
    public int[] nodeAtTime; //stores the internal nodes in increasing time order
    public int[] timeOfNode; //stores the time that each node occurs at for all nodes
    public final int tipTime; //time which all the tips are at
    public Tree hostTree;
    public Tree parasiteTree;
    //private final Tree tree;
    public TimeZones timeZones;

    public TreeSpecs(Tree hostTree, Tree parasiteTree, TimeZones timeZones) {
        this.hostTree = hostTree;
        this.parasiteTree = parasiteTree;
        this.timeZones = timeZones;
        timeOfNode = new int[hostTree.size];
        tipTime = hostTree.numberOfInternalNodes() + 1; // We start numbering timings at 1, not 0
        nodeAtTime = new int[tipTime];

        clearTiming();

        for (int i : hostTree.getTips())
            timeOfNode[i] = tipTime;
    }

    private void clearTiming() {
        Arrays.fill(nodeAtTime, -1);
        Arrays.fill(timeOfNode, -1);
    }

    /*
     * mutates this timing, swapping a randomly chosen pair of nodes at adjacent
     * timings that can be swapped (or doing nothing if there exists no such
     * pair) ASSUMPTION: This timing is for a host tree.
     *
     * Will only switch nodes that either in the same polytomy
     */
    public void mutate() throws InconsistentTreeException {
        /* mutate the host tree, find the polytomy changed and the spaces it moved
         * to get this.
         */
        int[] result = mutateTree(true);
        int hostSpacesMoved = result[0];
        int hostPolytomy = result[1];
        
        // mutate the parasite tree
        mutateTree(false);       
        
        // mutate the timing at the polytomy to correct for mutation.
        mutateTimingAtPolytomy(hostPolytomy, hostSpacesMoved);
        
        // mutate the timing (this is all that matters in the non polytomy case
        mutateTiming();
    }

    public void mutateTiming() {
        // List of possible pairs of nodes to swap
        // We don't store both nodes in the pair, just the one at the later
        // timing.
        List<Integer> pairs = new ArrayList<Integer>();

        for (int i = 2; i < nodeAtTime.length; i++) {
            int earlierNode = nodeAtTime[i - 1];
            int laterNode = nodeAtTime[i];
            //TODO: FIX this hack.  Instead of bypassing this mutation, it should trace earlierNode back to its first non-polytomy ancestor.  Or, change descendant to trace backward instead.
            if (!isInPolytomy(earlierNode, true)) {
            if (!hostTree.descendant(earlierNode, laterNode)
                    && (timeZones.hostsInSameZone(earlierNode, laterNode))
                    && !(this.hostTree.conseqPoly && !samePolytomyStatus(earlierNode, laterNode, true))) {
                pairs.add(nodeAtTime[i]);
                }
            }
        }
        if (pairs.size() > 0) {
            int choice = Jane.rand.nextInt(pairs.size());
            int laterNode = pairs.get(choice);
            int earlierNode = nodeAtTime[timeOfNode[laterNode] - 1];
            swap(laterNode, earlierNode);
        }
    }

    /*
     * mutates timings a given number of times, but, if possible, at the
     * polytomy given.
     * 
     * Assumes non-consecutive polytomy resolution
     */
    private void mutateTimingAtPolytomy(int polytomyName, int numMutations) {
        //list of possible pairs of nodes to swap
        //We don't store both nodes in the pair, just the one at the later
        //timing.
        List<Integer> pairs;
        int mutationsDone = 0;

        for (int j = 0; j < numMutations; j++) {
            /* as many times as possible, move around the polytomy nodes
             * 
             */
            pairs = new ArrayList<Integer>();
            for (int i = 2; i < nodeAtTime.length; i++) {
                int earlierNode = nodeAtTime[i - 1];
                int laterNode = nodeAtTime[i];
                if (!hostTree.descendant(earlierNode, laterNode)
                        && (timeZones.hostsInSameZone(earlierNode, laterNode))
                        && (hostTree.polytomyNameOf(earlierNode) ==polytomyName |
                        hostTree.polytomyNameOf(laterNode) ==polytomyName)) {
                    pairs.add(nodeAtTime[i]);
                }
            }
            if (pairs.size() > 0) {
                int choice = Jane.rand.nextInt(pairs.size());
                int laterNode = pairs.get(choice);
                int earlierNode = nodeAtTime[timeOfNode[laterNode] - 1];
                swap(laterNode, earlierNode);
                mutationsDone++;
            } else {
                break;
            }
        }
        // if we couldn't mutate enough along the polytomy, mutate normally.
        for (int j = 0; j < numMutations-mutationsDone; j++) {
            mutateTiming();
        }
    }

    /*
     * only mutates if consecutive polytomies. Returns the number of spaces
     * moved to make polytomy have consecutive timings and the polytomy where
     * the changes happened
     *
     */
    public int[] mutateTree(boolean host) throws InconsistentTreeException {
        // find the correct tree to edit
        Tree tree;
        if (host) {
            tree = this.hostTree;
        } else {
            tree = this.parasiteTree;
        }
        int[] answer = new int[2];

        // if nothing should be done
        if (!tree.isPolytomyResolution()) {
            answer[0] = 0;
            answer[1] = -1;
            return answer;
        }

        // find a random polytomy on this tree
        int numPolytomies = tree.polytomyCounter;
        Random rand = new Random();
        int chosenPolytomy = rand.nextInt(numPolytomies);
        answer[1] = chosenPolytomy;

        // find the base node of that polytomy
        int polyNode = -17;
        for (int i = 0; i <= tree.lastRealNode; i++) {
            if (tree.node[i].polytomyName == chosenPolytomy) {
                polyNode = i;
                break;
            }
        }
        
        if (polyNode == -17) {
            System.out.println("chosenPolytomy = " + chosenPolytomy);
            System.out.println("tree = " + tree);
            throw new InconsistentTreeException("Tree contains no polytomy of that number.  This shouldn't happen.");
        }

        /*
         * Make the node timings consecutive for the polytomy. this only matters
         * if this is the host tree.
         */
        int spacesMoved = 0;
        if (!tree.conseqPoly && host)
            spacesMoved = movePolytomyTimings(polyNode);

        // find all the polytomy's children and the names of polytomy-resolution nodes
        Vector<Integer> nodesToConsider = new Vector<Integer>();
        nodesToConsider.add(polyNode);
        Vector<Integer> polytomyChildren = new Vector<Integer>();
        int currentSize = 1;

        for (int n = 0; n < currentSize; n++) {
            int currentNode = nodesToConsider.get(n);
            int lchild = tree.leftChild(currentNode);
            int rchild = tree.rightChild(currentNode);
            if (this.samePolytomy(currentNode, lchild, host)) {
                // if it's a polytomy resolution
                nodesToConsider.add(lchild);
                currentSize++;
            } else {
                polytomyChildren.add(lchild);
            }
            
            if (this.samePolytomy(currentNode, rchild, host)) {
                // if it's a polytomy resolution
                nodesToConsider.add(rchild);
                currentSize++;
            } else {
                polytomyChildren.add(rchild);
            }
        }

        // time to start timing the new nodes
        int startTime = -1;
        if (host)
            startTime = timeOfNode(polyNode);

        /*
         * since the polytomy is consecutive, we can swap timings as long as we
         * don't have children sooner than parents
         */
        nodesToConsider.removeElementAt(0);
        newPolytomySubtree(host, polyNode, nodesToConsider, polytomyChildren, startTime);
        tree.precomputePostOrderTraversal();
        tree.preComputePreOrderTraversal();
        answer[0] = spacesMoved;
        return answer;
    }
    
    /*
     * Helper for reorganizing polytomies. Assumes that polytomies have
     * consecutive timings.
     *
     * Will generate a new polytomy subtree starting at startIndex. This will be
     * the host tree iff "host". It will return the timestep after this.
     */
    private int newPolytomySubtree(boolean host, int startIndex,
                                      Vector<Integer> polytomyNodes,
                                      Vector<Integer> childrenNodes, int time) {

        Tree tree;
        if (host)
            tree = this.hostTree;
        else
            tree = this.parasiteTree;

        //if this is the host tree, add timing information
        if (host)
            this.putNodeAtTime(startIndex, time++);

        // split children in two pieces
        int polytomySize = childrenNodes.size();
        Random rand = new Random();
        int partSize = rand.nextInt(polytomySize - 1) + 1;
        Collections.shuffle(childrenNodes);
        
        @SuppressWarnings("unchecked")
        Vector<Integer> firstHalf = (Vector<Integer>) childrenNodes.clone();
        firstHalf.setSize(partSize);
        
        @SuppressWarnings("unchecked")
        Vector<Integer> secondHalf = (Vector<Integer>) childrenNodes.clone();
        secondHalf.removeAll(firstHalf);
        if (Collections.min(firstHalf) > Collections.min(secondHalf)) {
            @SuppressWarnings("unchecked")
            Vector<Integer> temp = (Vector<Integer>) firstHalf.clone();
            firstHalf = (Vector<Integer>) secondHalf.clone();
            secondHalf = temp;
        }
        // add nodes as children
        int left;
        int right;
        if (firstHalf.size() == 1) {
            left = firstHalf.firstElement();
            tree.node[startIndex].Lchild = left;
            tree.node[left].parent = startIndex;
        } else {
            left = polytomyNodes.firstElement();
            tree.node[startIndex].Lchild = left;
            tree.node[left].parent = startIndex;
            polytomyNodes.removeElementAt(0);
        }

        if (secondHalf.size() == 1) {
            right = secondHalf.firstElement();
            tree.node[startIndex].Rchild = right;
            tree.node[right].parent = startIndex;
        } else {
            right = polytomyNodes.firstElement();
            tree.node[startIndex].Rchild = right;
            tree.node[right].parent = startIndex;
            polytomyNodes.removeElementAt(0);
        }

        // recursively add the subtrees
        if (firstHalf.size() != 1)
            time = newPolytomySubtree(host, left, polytomyNodes, firstHalf, time);
        if (secondHalf.size() != 1)
            time = newPolytomySubtree(host, right, polytomyNodes, secondHalf, time);

        // return the current time
        return time;
    }

    /*
     * moves members of a polytomy to consecutive times, starting at the base of
     * that polytomy. (the base is the node that was in the original tree).
     * Returns the total number of spaces moved by the polytomy nodes.
     */
    private int movePolytomyTimings(int polyStart) {
        // Find the end of this shift

        int startTime = timeOfNode(polyStart);
        int latestTime = -1;
        int polySize = 0;
        int spacesMoved = 0;
        
        for (int i = 0; i < hostTree.size; i++) {
            if (this.samePolytomy(polyStart, i, true)) {
                spacesMoved += timeOfNode(i) - startTime - polySize;
                latestTime = Math.max(timeOfNode(i), latestTime);
                polySize++;
            }
        }

        int[] newTimeOfNode = new int[timeOfNode.length];
        int[] newNodeAtTime = new int[nodeAtTime.length];

        int currentSlot = startTime + polySize - 1;
        int polysSeen = 0;
        for (int i = tipTime - 1; i > 0; i--) {
            if (i > latestTime || i <= startTime) {
                // we shouldn't change anything.  These are golden.
                newTimeOfNode[nodeAtTime(i)] = i;
                newNodeAtTime[i] = nodeAtTime(i);
                continue;
            }
            if (!samePolytomy(nodeAtTime(i), polyStart, true)) {
                int newTime = i + polysSeen;
                newTimeOfNode[nodeAtTime(i)] = newTime;
                newNodeAtTime[newTime] = nodeAtTime(i);
            } else {
                newTimeOfNode[nodeAtTime(i)] = currentSlot;
                newNodeAtTime[currentSlot] = nodeAtTime(i);
                currentSlot--;
                polysSeen++;
            }
        }
        
        for (int i = 1; i < newNodeAtTime.length; i++)
            putNodeAtTime(newNodeAtTime[i], i);
        
        return spacesMoved;
    }

    public void putNodeAtTime(int node, int time) {
        nodeAtTime[time] = node;
        timeOfNode[node] = time;
    }

    public int nodeAtTime(int time) {
        return nodeAtTime[time];
    }

    public int timeOfNode(int node) {
        return timeOfNode[node];
    }

    public boolean equals(TreeSpecs other) {
//        if (!this.hostTree.isPolytomyResolution() && !this.parasiteTree.isPolytomyResolution()
//                && !other.hostTree.isPolytomyResolution() && !other.parasiteTree.isPolytomyResolution()) {
            // no polytomies
          return Arrays.equals(this.nodeAtTime, other.nodeAtTime)
                    && this.hostTree.equals(other.hostTree)
                    && this.parasiteTree.equals(other.parasiteTree);
//        }
//        TreeMap<Integer, Integer> thisToOtherHost = new TreeMap<Integer, Integer>();
//        TreeMap<Integer, Integer> thisToOtherPara = new TreeMap<Integer, Integer>();
//        if (!hostTree.equivalentNodes(hostTree.root, other.hostTree, other.hostTree.root, thisToOtherHost)
//                || !parasiteTree.equivalentNodes(parasiteTree.root, other.parasiteTree, other.parasiteTree.root, thisToOtherPara)) {
//            return false;
//        }
//        for (int i = 1; i < this.tipTime; i++) {
//            if (thisToOtherHost.get(this.nodeAtTime[i]) != other.nodeAtTime(i)) {
//                return false;
//            }
//        }
//        return true;

    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TreeSpecs)
            return equals((TreeSpecs) o);
        else
            return false;
    }

    @Override
    /*
     * NetBeans generated this automatically. It generates the hashCode base on
     * the hashCode of nodeAtTime as well as the host tree. According to Ben,
     * this is based off of some sort of polynomial hash function using Horner's
     * rule.
     */
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + Arrays.hashCode(this.nodeAtTime);
        hash = 37 * hash + (this.hostTree != null ? this.hostTree.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {

        StringBuffer output = new StringBuffer("");
        output.append("Time: 0 Node: Dummy Root").append("\n");
        for (int i = 1; i < tipTime; i++) {
            output.append("Time: ").append(i).append(" Node: ").append(nodeAtTime(i)).append(" Name: ").append(hostTree.node[nodeAtTime(i)].name).append("\n");
        }
        for (Object n : hostTree.tips) {
            output.append("Time: ").append(tipTime).append(" Node: ").append(hostTree.node[(Integer) n].name).append("\n");
        }
        output.append("Host Tree:\n");
        output.append(this.hostTree.toString());
        output.append("Parasite Tree:\n");
        output.append(this.parasiteTree.toString());

        return output.toString();
    }

    // ASSUMPTION: This timing is for a host tree.
    public String fileTimingString() {
        // Invert the map so we get O(n) runtime. This could be avoided by
        // using Guava's BiMap or Apache's BidiMap to store origIDToName.
        Map<Integer, Integer> nameToOrigID = new HashMap<Integer, Integer>();
        for (Map.Entry<Integer, Integer> entry : hostTree.origIDToName.entrySet()) {
            nameToOrigID.put(entry.getValue(), entry.getKey());
        }

        int finalTime = tipTime;
        StringBuilder output = new StringBuilder("");

        for (int i = 1; i < finalTime; i++) {
            Object value = nameToOrigID.get(nodeAtTime(i));
            if (value != null) {
                output.append(value).append(",").append(i).append(",").append(timeZones.getHostZone(nodeAtTime(i))).append("\n");
            }
        }

        for (int n : hostTree.tips) {
            Object value = nameToOrigID.get(n);
            if (value != null) {
                output.append(value).append(",").append(tipTime).append(",").append(timeZones.getHostZone(n)).append("\n");
            }
        }

        return output.toString();
    }

    /*
     * swaps the nodes at time1 and time2 in the timing
     */
    private void swap(int node1, int node2) {
        int time1 = timeOfNode[node1];
        int time2 = timeOfNode[node2];

        nodeAtTime[time1] = node2;
        nodeAtTime[time2] = node1;

        timeOfNode[node1] = time2;
        timeOfNode[node2] = time1;
    }

    // Checks that the timing respects ancestry relationships and time zones.
    // ASSUMPTION: This timing is for a host tree.
    public boolean isConsistent() {
        // Check that root is at time 1
        if (timeOfNode(hostTree.root) != 1)
            return false;

        int currentTime;
        int child1Time;
        int child2Time;
        for (int n = 0; n < hostTree.node.length; n++) {
            if (hostTree.isTip(n)) {
                // Check that tips are at final time
                if (timeOfNode(n) != tipTime) {
                    return false;
                }
            } else {
                currentTime = timeOfNode(n);
                child1Time = timeOfNode(hostTree.leftChild(n));
                child2Time = timeOfNode(hostTree.rightChild(n));
                // Check that timing honors ancestry relationships, and that
                // only tips occur at final time
                if (currentTime >= child1Time || currentTime >= child2Time
                        || currentTime == tipTime) {
                    System.out.println("------- Failed timing check.  Details to follow --------");
                    System.out.println("n = " + n);
                    System.out.println("Tree.printWithTiming(this,true) = " + Tree.printWithTiming(this, true));
                    return false;
                }
            }
        }

        // Ensure that time zones are non decreasing when the nodes are
        // considered in the order of their timing.
        if (timeZones.areUsed()) {
            int previousTimeZone = timeZones.getHostZone(nodeAtTime(1));
            int currentTimeZone;
            for (int i = 2; i < nodeAtTime.length; ++i) {
                currentTimeZone = timeZones.getHostZone(nodeAtTime(i));
                if (previousTimeZone > currentTimeZone) {
                    return false;
                }
                previousTimeZone = currentTimeZone;
            }
        }
        return true;
    }

    /*
     * returns true if both nodes are in the same polytomy and are in a
     * polytomy.
     *
     */
    public boolean samePolytomy(int a, int b, boolean host) {
        if (host) {
            return this.hostTree.node[a].polytomyName == this.hostTree.node[b].polytomyName && isInPolytomy(a, host);
        } else {
            return this.parasiteTree.node[a].polytomyName == this.parasiteTree.node[b].polytomyName && isInPolytomy(a, host);
        }
    }

    public boolean samePolytomyStatus(int a, int b, boolean host) {
        if (host) {
            return samePolytomy(a, b, host) || (!isInPolytomy(a, host) && !isInPolytomy(b, host));
        } else {
            return samePolytomy(a, b, host) || (!isInPolytomy(a, host) && !isInPolytomy(b, host));
        }
    }

    // returns if node a is in a polytomy.
    public boolean isInPolytomy(int a, boolean host) {
        if (host)
            return this.hostTree.node[a].polytomyName != -1;
        else
            return this.parasiteTree.node[a].polytomyName != -1;
    }

    public boolean isOriginalNode(int a, boolean host) {
        if (host)
            return a <= this.hostTree.lastRealNode;
        else
            return a <= this.parasiteTree.lastRealNode;
    }

    public int polytomyNameOf(int a, boolean host) {
        if (host)
            return this.hostTree.node[a].polytomyName;
        else
            return this.parasiteTree.node[a].polytomyName;
    }

    /*
     * checks if polytomies are ever seperated in tree timings.
     *
     */
    public void checkPolytomyTiming() throws InconsistentTreeException {
        TreeSet<Integer> polytomiesSeen = new TreeSet<Integer>();
        for (int i = 1; i < tipTime; i++) {
            if (polytomiesSeen.contains(polytomyNameOf(nodeAtTime(i), true))) {
                throw new InconsistentTreeException("Tree's timing seperated polytomies.  This shouldn't happen.");
            }

            if (isInPolytomy(nodeAtTime(i), true)) {
                while (i + 1 < tipTime && samePolytomy(nodeAtTime(i), nodeAtTime(i + 1), true))
                    i++;

                polytomiesSeen.add(polytomyNameOf(nodeAtTime(i), true));
            }
        }
    }
    
    // this exception should NEVER be thrown. it can only occur if somehow
    // we assign a timing that is inconsistent with time zones. errors in files
    // which might cause something like this should be caught before
    // this exception
    // is thrown
    public static final String inconsistentTreeMessage
        = "\nThis was caused by a bug in the genetic algorithm."
           + "\nPlease contact ran@cs.hmc.edu if this occurs.";

    public static class InconsistentTreeException extends Exception {
        public InconsistentTreeException() {
        }

        public InconsistentTreeException(String s) {
            super(s);
        }
    }
}
