package edu.hmc.jane;

import java.util.*;

/**
 *
 * @author Tselil
 */
public class Tree {

    // MEMBER VARIABLES
    public int numTips;    // number of tip nodes
    public int size;       // including non-tip nodes (and dummy root for host tree)
    public Map<Integer, Integer> origIDToName;
    public Node[] node;   // array of nodes
    public int root;       // index of the root of the array
    // for host tree, this needs to be the index of the dummy root
    // which must be the last node in the array
    public int postOrder[]; // the nodes listed in post order.
    
    /*
     * preOrder is the nodes of the tree listed in preOrder
     * preOrder and depth uniquely identify trees and are used for this purpose.
     * 
     * depth stores the depth in the tree of the nodes in the corresponding 
     *  position of preOrder
     */
    public ArrayList<Integer> preOrder;
    public int depth[];
    
    Set<Integer> tips = new TreeSet<Integer>();
    public static int INFINITE_DISTANCE = 88888888;
    //TODO: Fix this maxDistance business
    public int maxDistance = INFINITE_DISTANCE;
    //polytomy variables
    public boolean hasPolytomy;
    public int polytomyCounter; //keeps track of # of polytomies
    /*
     * keeps track of number of additional edges due to polytomies. Really, this
     * is the number of new nodes in the resolved tree to build the polytomies.
     * A 3-polytomy (an edge becoming three) will contribute 1, a 4 will
     * contribute 2, a 5 will contribute 3, etc.
     */
    public int totalPolytomySize;
    /*
     * keeps track of the ints used so far to name polytomy-groups. It is
     * incremented to name each polytomy different
     */
    public int polytomyNameCounter;
    /*
     * remembers the last node index that really exists. Since we create the
     * polytomy-resolutiton nodes after, they all have indexes that come after.
     * This was we can choose our node for reproduction easily.
     */
    public int lastRealNode;
    /*
     * conseqPoly: if true, keeps nodes of a resolved polytomy in consequtive
     * times noMidPolyEvents: if true, prevents events from happening onto
     * polytomy edges. It does not stop losses/cospeciations from happening on
     * nodes.
     */
    public boolean conseqPoly, noMidPolyEvents, allowFTD = true, allowInfest = false;

    // CONSTRUCTOR (empty tree)
    private Tree(int size, boolean isPolytomy) {
        this.size = size;

        //polytomy business
        hasPolytomy = isPolytomy;
        polytomyCounter = 0;
        totalPolytomySize = 0;
        conseqPoly = true;
        noMidPolyEvents = true;
        if (!isPolytomy) {
            numTips = (size + 1) / 2;
            node = new Node[size];
            for (int i = 0; i < node.length; i++) {
                node[i] = new Node();
            }
        } else {
            node = new PolytomyNode[size];
            for (int i = 0; i < node.length; i++) {
                node[i] = new PolytomyNode(-1); //indicates no polytomy
            }
        }
        lastRealNode = size - 1;

    }

    // CONSTRUCTOR (from adjacency list)
    public Tree(Map<Integer, Vector<Integer>> net, String[] names) {
        this(net, names, false);
    }

    public Tree(Map<Integer, Vector<Integer>> net, String[] names, boolean isPolytomy) {
        this(net.size(), isPolytomy);
        constructFrom(net, names, null);
        numTips = getTips().size();
    }

    // CONSTRUCTOR (from adjacency list)
    public Tree(Map<Integer, Vector<Integer>> net, String[] names, Map<Integer, Integer> origID) {
        this(net, names, origID, false);

    }

    public Tree(Map<Integer, Vector<Integer>> net, String[] names, Map<Integer, Integer> origID, boolean isPolytomy) {
        this(net.size(), isPolytomy);
        constructFrom(net, names, origID);
        numTips = getTips().size();
    }

    // CONSTRUCTOR (random tree, distributed according to Yule Model)
    public Tree(int numTips, double beta) {
        this(numTips * 2 - 1, false);
        TreeRandomizer.generateRandom(this, beta);
    }
    
    /*
     * resolves polytomies from prior tree. Presumes that tree created from has polytomies
     */
    public static Tree withoutPolytomy(Tree oldTree) {
        // if oldTree already had no polytomies
        if (!oldTree.hasPolytomy) {
            return oldTree;
        }
        Tree newTree = new Tree(oldTree.size + oldTree.totalPolytomySize, false);
        newTree.totalPolytomySize = oldTree.totalPolytomySize;
        newTree.conseqPoly = oldTree.conseqPoly;
        newTree.noMidPolyEvents = oldTree.noMidPolyEvents;
        newTree.lastRealNode = oldTree.lastRealNode;
        newTree.polytomyNameCounter = oldTree.size; // for naming new nodes
        newTree.origIDToName = oldTree.origIDToName;
        for (int i = 0; i < oldTree.size; i++) {
            PolytomyNode currentNode = (PolytomyNode) oldTree.node[i];
            if (currentNode.polytomyName == -1) {
                //there is no polytomy
                if (newTree.node[i].parent == -1) {
                    newTree.node[i].parent = currentNode.parent;
                }
                if (currentNode.Lchild < currentNode.Rchild) {
                    newTree.node[i].Lchild = currentNode.Lchild;
                    newTree.node[i].Rchild = currentNode.Rchild;
                } else {
                    newTree.node[i].Lchild = currentNode.Rchild;
                    newTree.node[i].Rchild = currentNode.Lchild;
                }
                newTree.node[i].name = currentNode.name;
            } else {
                // there is polytomy

                if (newTree.node[i].parent == -1) {
                    newTree.node[i].parent = currentNode.parent;
                }
                newTree.node[i].name = currentNode.name;
                newTree.node[i].polytomyName = newTree.polytomyCounter;

                //TODO: Dave: Change polytomy resolution distribution?


                // randomly split children into two non-empty subsets
                int polytomySize = currentNode.children.size();
                Random rand = new Random();
                int partSize = rand.nextInt(polytomySize - 1) + 1;
                Collections.shuffle(currentNode.children);
                Vector<Integer> firstHalf = (Vector<Integer>) currentNode.children.clone();
                firstHalf.setSize(partSize);
                Vector<Integer> secondHalf = (Vector<Integer>) currentNode.children.clone();
                secondHalf.removeAll(firstHalf);

                /*
                 * make intermediate nodes for each. In the case where a subset
                 * has only one element, this is a dummy intermediate node.
                 */
                PolytomyNode oldLeft = new PolytomyNode(currentNode.polytomyName);
                PolytomyNode oldRight = new PolytomyNode(currentNode.polytomyName);
                
                if (Collections.min(firstHalf) < Collections.min(secondHalf)) {             
                    oldLeft.children = firstHalf;          
                    oldRight.children = secondHalf;
                } else {                   
                    oldLeft.children = secondHalf;
                    oldRight.children = firstHalf;
                    
                }
                oldLeft.parent = i; 
                oldRight.parent = i;

                newTree.node[i].Lchild = newTree.nodeWithoutPolytomy(oldLeft);
                newTree.node[newTree.node[i].Lchild].parent = i;           
                newTree.node[i].Rchild = newTree.nodeWithoutPolytomy(oldRight);
                newTree.node[newTree.node[i].Rchild].parent = i;
                newTree.polytomyCounter++;
            }
        }
        newTree.root = newTree.findRoot();
        newTree.precomputePostOrderTraversal();
        newTree.preComputePreOrderTraversal();
        newTree.findTips();
        newTree.lastRealNode = oldTree.lastRealNode;
        return newTree;
    }
    
    public int nodeWithoutPolytomy(PolytomyNode oldNode) {
        Node newNode = new Node();
        int nodesIndex = -42;
        if (oldNode.children.size() == 1) {
            nodesIndex = oldNode.children.get(0);
        } else if (oldNode.children.size() == 2) {
            nodesIndex = polytomyNameCounter++;
            node[nodesIndex] = newNode;
            node[nodesIndex].polytomyName = polytomyCounter;
            if (oldNode.children.get(0) < oldNode.children.get(1)) {
                newNode.Lchild = oldNode.children.get(0);
                newNode.Rchild = oldNode.children.get(1);
                
            } else {
                
                newNode.Lchild = oldNode.children.get(1);
                newNode.Rchild = oldNode.children.get(0);
            }
            node[newNode.Lchild].parent = nodesIndex;          
            node[newNode.Rchild].parent = nodesIndex;
            
            newNode.parent = oldNode.parent;
            
            newNode.name = Integer.toString(nodesIndex);
        } else if (oldNode.children.size() > 2) {
            nodesIndex = polytomyNameCounter++;
            node[nodesIndex] = newNode;
            node[nodesIndex].polytomyName = polytomyCounter;
            //randomly split into non-empty subsets
            int polytomySize = oldNode.children.size();
            Random rand = new Random();
            int partSize = rand.nextInt(polytomySize - 1) + 1;
            Collections.shuffle(oldNode.children);
            Vector<Integer> firstHalf = (Vector<Integer>) oldNode.children.clone();
            firstHalf.setSize(partSize);
            Vector<Integer> secondHalf = (Vector<Integer>) oldNode.children.clone();
            secondHalf.removeAll(firstHalf);

            /*
             * make intermediate nodes for each. In the case where a subset has
             * only one element, this is a dummy intermediate node.
             */
            PolytomyNode oldLeft = new PolytomyNode(oldNode.polytomyName);
            PolytomyNode oldRight = new PolytomyNode(oldNode.polytomyName);
            
            if (Collections.min(firstHalf) < Collections.min(secondHalf)){             
                oldLeft.children = firstHalf;
                oldRight.children = secondHalf;
            } else {
                oldLeft.children = secondHalf;
                oldRight.children = firstHalf;              
            }
            oldLeft.parent = nodesIndex;            
            oldRight.parent = nodesIndex;
            
            newNode.Lchild = nodeWithoutPolytomy(oldLeft);
            node[newNode.Lchild].parent = nodesIndex;
            newNode.Rchild = nodeWithoutPolytomy(oldRight);
            node[newNode.Rchild].parent = nodesIndex;
            newNode.parent = oldNode.parent;
            newNode.name = Integer.toString(nodesIndex);
        }
        return nodesIndex;
    }

    /*
     * If given a node in the original tree, return that. If given a node
     * created by polytomy-resolution, find the node that it is associated with.
     */
    public int findStartNode(int nodeIndex) {
        int parent = node[nodeIndex].parent;
        if (node[nodeIndex].polytomyName == -1 || root == nodeIndex || node[parent].polytomyName != node[nodeIndex].polytomyName) {
            // no polytomy, or top of polytomy
            return nodeIndex;
        }
        // find the closest ancestor who started that polytomyName
        int grandparent = node[parent].parent;
        while (parent != root && node[parent].polytomyName == node[grandparent].polytomyName) {
            parent = grandparent;
            grandparent = node[grandparent].parent;
        }
        return parent;
    }
    
    /*
     * create a post-order sequence of nodes in the tree
     */
    public void precomputePostOrderTraversal() {
        int counter = 0;
        postOrder = new int[size];
        // make sure all nodes are visited
        for (int i = 0; i < size; i++) {
            if (node[i].parent == -1) {
                // run DFS
                counter = postOrderHelper(i, counter);
            }
        }
    }
    
    /*
     * Create a pre-order sequence of nodes in the tree and the corresponding
     * depth of each of those nodes.  Useful for checking tree equality and
     * creating tree hashCode
     */
    public void preComputePreOrderTraversal() {
        if (size != 0) { //change for treegui
            depth = new int[size];
            preOrder = new ArrayList<Integer>(size);
          
            int counter = 0;
            int currLevel = 0;
      
            preOrderHelper(root, counter, currLevel);  
        }   
    }      
   /*
    * Does the actual preOrder traversing algorithm.
    */
    private int preOrderHelper(int from, int counter, int currLevel) {
        preOrder.add(counter, from);
        depth[counter] = currLevel;
        counter++;
        if (node[from].Lchild != -1) {
            counter = preOrderHelper(node[from].Lchild, counter, currLevel + 1);
            counter = preOrderHelper(node[from].Rchild, counter, currLevel + 1);
        }
        return counter;
    }
   
    /*
     * a DFS function to get a post-order sequence of nodes in the tree
     */
    private int postOrderHelper(int from, int counter) {
        if (node[from].Lchild != -1) {
            // visit children
            counter = postOrderHelper(node[from].Lchild, counter);
            counter = postOrderHelper(node[from].Rchild, counter);
        }
        // visit self
        postOrder[counter++] = from;
       
        return counter;
    }

    /*
     * Checks if one node is the ancestor of another. This works for edges if
     * you use their second node for the numbering. A node is considered an
     * ancestor of itself.
     */
    public boolean descendant(int ancestor,int current) {
        int ancestPos = preOrder.indexOf(ancestor);
        int origDep = depth[ancestPos];
        ancestPos++;

        if (ancestor == current)
            return true;
        
        while (ancestPos != preOrder.size() && depth[ancestPos] > origDep) {
            if ( preOrder.get(ancestPos) == current) {
                return true;
            }               
            ancestPos++;
        }
        return false;
    }

    /*
     * Builds a list containing all descendants of a node n.
     */
    public List<Integer> getDescendants(int n) {
        List<Integer> answer = new LinkedList<Integer>();
        answer.add(n);
        
        int descStart = preOrder.indexOf(n);
        int nDepth = depth[descStart];        
        
        descStart++;
        while (descStart != preOrder.size() && depth[descStart] > nDepth) {
            answer.add( (Integer)preOrder.get(descStart));
            descStart++;
        }
        return answer;
    }
    
    /*
     * Counts the number of generations between ancestor and current. 
     */
    public int generations(int ancestor, int current) {
        // just the length of the shortest path from ancestor to descendant
        if (descendant(ancestor, current)) {
            return depth[preOrder.indexOf(current)] - depth[preOrder.indexOf(ancestor)];
        } else {
            return INFINITE_DISTANCE;
        }
    }

    /*
     * Recursively determines whether two edges/nodes are within a given
     * maxDistance.
     */
    
    //TODO: This hasn't been tested, but is not found anywhere in the code.
    public boolean withinN(int from, int to, int maxDist) {
        return getDistance(from, to) <= maxDist;
    }

    private int findRoot() {
        for (int i = 0; i < node.length; i++) {
            if (node[i].parent == -1) {
                return i;
            }
        }
        return -1;
    }

    /*
     * Transcribe parent/child relationships from a pNetwork into the tree.
     */
    public void constructFrom(Map<Integer, Vector<Integer>> net, String[] names, Map<Integer, Integer> originalIDs) {
        if (!hasPolytomy) {
            Vector<Integer> children;
            origIDToName = originalIDs;
            for (Integer n : net.keySet()) {
                if (n >= 0) {
                    node[n].name = names[n];
                    children = net.get(n);
                    if (!children.isEmpty()) {
                        node[n].Lchild = children.elementAt(0);
                        node[children.elementAt(0)].parent = n;
                        if (children.size() > 1) {
                            node[n].Rchild = children.elementAt(1);
                            node[children.elementAt(1)].parent = n;
                        }
                    } else {
                        tips.add(n);
                    }
                }
            }
            root = findRoot();
            precomputePostOrderTraversal();
            preComputePreOrderTraversal();
            //finalizeTree();
        } else {
            Vector<Integer> children;
            origIDToName = originalIDs;
            for (Integer n : net.keySet()) {
                if (n >= 0) {
                    PolytomyNode currentNode = (PolytomyNode) node[n];
                    currentNode.name = names[n];
                    children = net.get(n);
                    if (!children.isEmpty()) {
                        currentNode.Lchild = children.elementAt(0);
                        node[children.elementAt(0)].parent = n;
                        currentNode.children.add(children.elementAt(0));
                        if (children.size() > 1) {
                            currentNode.Rchild = children.elementAt(1);
                            node[children.elementAt(1)].parent = n;
                            currentNode.children.add(children.elementAt(1));
                        }
                        if (children.size() > 2) { // find all additional children

                            //add them to list
                            for (int i = 2; i < children.size(); i++) {
                                node[children.elementAt(i)].parent = n;
                                currentNode.children.add(children.elementAt(i));
                            }

                            //give them a label
                            int polytomyName = polytomyCounter++;
                            currentNode.polytomyName = polytomyName;

                            //calculate size of polytomy
                            int polytomySize = children.size() - 2;
                            totalPolytomySize += polytomySize;


                        }
                    } else {
                        tips.add(n);
                    }
                }
            }
            root = findRoot();
            /*
             * Notice that there is no call to finalizeTree. This is a
             * polytomy-based tree, so that needs to be done once the polytomies
             * are resolved.
             */
        }
    }

    /*
     * computes information needed about the tree, like distances and postorder
     *
     */
    public void finalizeTree() {
        preComputePreOrderTraversal();
        //precomputeDistance();
        precomputePostOrderTraversal();
    }

    public boolean isTip(int n) {
        assert (n < node.length && n >= 0);
        return (node[n].Rchild == -1 && node[n].Lchild == -1);
    }

    public Set<Integer> getTips() {
        return tips;
    }

    public void findTips() {
        for (int i = 0; i < this.size; i++) {
            if (isTip(i)) {
                tips.add(i);
            }
        }
    }

    public int leftChild(int parent) {
        return node[parent].Lchild;
    }

    public int rightChild(int parent) {
        return node[parent].Rchild;
    }
    
    public int getParent (int child) {
        return node[child].parent;
    }

    // obtain the index of the sibling of the given node, or return -1 for root
    // doesn't work for polytomy
    public int getSibling(int n) {
        assert (!this.hasPolytomy);
        if (n == root) {
            return -1;
        }
        int p = node[n].parent;
        if (node[p].Lchild != n) {
            return node[p].Lchild;
        }
        return node[p].Rchild;
    }

    public void addChildrenOf(int parent, Vector<Integer> children) {
        assert (!this.hasPolytomy);
        if (hasPolytomy) {
            PolytomyNode parentNode = (PolytomyNode) node[parent];
            children.addAll(parentNode.children);
        } else {
            if (node[parent].Lchild != -1) {
                children.add(node[parent].Lchild);
            }
            if (node[parent].Rchild != -1) {
                children.add(node[parent].Rchild);
            }
        }
    }

    @Override
    public String toString() {
        String answer = "";
        if (this.hasPolytomy) {
            answer = ("Number of nodes: " + size + ", Number of tips: " + numTips + "\n");
            for (int i = 0; i < size; i++) {
                PolytomyNode currentNode = (PolytomyNode) node[i];
                answer += ("(" + currentNode.parent + ") -> " + i);
                if (node[i].name != null) {
                    answer += ("; Name: " + node[i].name);
                }
                answer += " --> ";
                if (currentNode.children.isEmpty()) {
                    answer += "null";
                }
                for (int j = 0; j < currentNode.children.size(); j++) {
                    answer += ("(" + currentNode.children.get(j) + ") ");
                }
                answer += "\n";
            }
        } else {
            answer = ("Number of nodes: " + size + ", Number of tips: " + numTips + "\n");
            for (int i = 0; i < size; i++) {
                answer += ("(" + node[i].parent + ") -> " + i);
                if (node[i].name != null) {
                    answer += ("; Name: " + node[i].name);
                }
                answer += (" --> (" + node[i].Lchild + ") (" + node[i].Rchild + "); ");
                // polytomy information

                if (node[i].polytomyName != -1) {
                    answer += ("Polytomy group: " + node[i].polytomyName);
                }

                answer += "\n";
            }
        }
        return answer;
    }

    public static String printWithTiming(TreeSpecs timing, boolean host) {
        String answer;
        if (timing.hostTree.hasPolytomy) {
            answer = ("Number of nodes: " + timing.hostTree.size + ", Number of tips: " + timing.hostTree.numTips + "\n");
            for (int i = 0; i < timing.hostTree.size; i++) {
                PolytomyNode currentNode = (PolytomyNode) timing.hostTree.node[i];
                answer += ("(" + currentNode.parent + ") -> " + i + " --> ");
                if (currentNode.children.isEmpty()) {
                    answer += "null";
                }
                for (int j = 0; j < currentNode.children.size(); j++) {
                    answer += ("(" + currentNode.children.get(j) + ") ");
                }
                answer += "\n";
            }
        } else {
            answer = ("Number of nodes: " + timing.hostTree.size + ", Number of tips: " + timing.hostTree.numTips + "\n");
            for (int i = 0; i < timing.hostTree.size; i++) {
                answer += ("(" + timing.hostTree.node[i].parent + ") -> " + i + " --> ("
                        + timing.hostTree.node[i].Lchild + ") (" + timing.hostTree.node[i].Rchild + "); ");
                // polytomy information
                if (timing.hostTree.node[i].polytomyName != -1) {
                    answer += ("Polytomy group: " + timing.hostTree.node[i].polytomyName + "; ");
                }
                answer += ("Timing: " + timing.timeOfNode(i) + "\n");
            }
        }

        return answer;
    }

    /*
     * Gives the undirected distance between two edges (ie. in the host tree,
     * this will be the host switch distance for switching from edge with ID
     * fromID to edge with ID toID.)
     */
    public int getDistance(int fromID, int toID) {
//        return undirectedDist[fromID][toID];
        int fromPos = preOrder.indexOf(fromID);
        int toPos = preOrder.indexOf(toID);
        
        if (fromPos == -1 || toPos == -1)
            return INFINITE_DISTANCE;
        
        int fromDepth = depth[fromPos];
        int toDepth = depth[toPos];
        
        int distance = 0;
        
        while (fromDepth > 0 && toDepth > 0) {
            if (toDepth < fromDepth) {
                fromPos = getParentPos(fromPos);
                distance++;
                fromDepth = fromDepth - 1;
            }
            if (fromDepth < toDepth) {
                toPos = getParentPos(toPos);
                distance++;
                toDepth = toDepth - 1;
            }
            if (fromDepth == toDepth) {
                if (toPos == fromPos) {
                    return  distance;
                } else {
                    fromPos = getParentPos(fromPos);
                    fromDepth = fromDepth -1;
                    distance++;
                }
            }
        }
        return distance;
    }

    /*
     * Given the index of a node in the preOrder array, this function returns
     * the index of that node's parent in the preOrder array.
     */
    private int getParentPos(int position) {
        if (position == 0)
            return position;
        int depthGoal = depth[position];
        while(depth[position] != depthGoal -1) {
            position = position -1;
        }
        return position;
    }
    
    int numberOfLeaves() {
        if (!hasPolytomy) {
            return (size + 1) / 2;
        }
        return tips.size();
    }

    int numberOfInternalNodes() {
        return numberOfLeaves() - 1;
    }

    /*
     * constructs a random TreeSpecs for this tree, guaranteeing that the timing
     * is selected with uniform probability. ASSUMPTION: This tree is a host
     * tree.
     */
    public TreeSpecs getRandomSpecs(TimeZones timeZones, Tree parasiteTree) throws TreeSpecs.InconsistentTreeException {
        Tree host = Tree.withoutPolytomy(this);
        Tree para = Tree.withoutPolytomy(parasiteTree);
        TimeZones newTimeZones = timeZones.newWithoutPolytomies(host.size - this.size, para.size - parasiteTree.size, host, para);
        int[] ordering = host.randTime(host.root, newTimeZones);

        TreeSpecs timing = new TreeSpecs(host, para, newTimeZones);

        for (int i = 0; i < ordering.length; i++) {
            timing.putNodeAtTime(ordering[i], i + 1); //we start numbering timings at 1, not 0
        }
        if (this.conseqPoly) {
            timing.checkPolytomyTiming();
        }
        return timing;
    }

    /*
     * This function returns an ordering of internal nodes in subtree rooted at
     * node v, guaranteeing that the ordering is randomly selected with uniform
     * probability from the set of all orderings for all nodes in this tree that
     * respect the time zone information. ASSUMPTION: This tree is a host tree.
     */
    private int[] randTime(int v, TimeZones timeZones) {
        int[] timing;
        if (isTip(v)) {
            timing = new int[0];
            return timing;
        }
        int[] leftTime = randTime(node[v].Lchild, timeZones);
        int[] rightTime = randTime(node[v].Rchild, timeZones);
        int timingSize = leftTime.length + rightTime.length + 1;
        timing = new int[timingSize];
        timing[0] = v;

        // Fill in the new timing.
        int i = 1;
        int LIndex = 0;
        int RIndex = 0;

        // add the polytomies associated with v
        if (this.conseqPoly) {
            while (node[v].polytomyName != -1 && LIndex < leftTime.length && node[v].polytomyName == node[leftTime[LIndex]].polytomyName) {
                timing[i++] = leftTime[LIndex++];
            }
            while (node[v].polytomyName != -1 && RIndex < rightTime.length && node[v].polytomyName == node[rightTime[RIndex]].polytomyName) {
                timing[i++] = rightTime[RIndex++];
            }
        }

        while (i < timingSize) {

            // If the remaining elements are all in the same list.
            if (LIndex == leftTime.length) {
                while (RIndex != rightTime.length) {
                    timing[i++] = rightTime[RIndex];
                    ++RIndex;
                }
                break;
            } else if (RIndex == rightTime.length) {
                while (LIndex != leftTime.length) {
                    timing[i++] = leftTime[LIndex];
                    ++LIndex;
                }
                break;
            }

            int LNode = leftTime[LIndex];
            int RNode = rightTime[RIndex];

            // handles polytomies already.
            if (timeZones.areUsed() && (timeZones.getHostZone(LNode) < timeZones.getHostZone(RNode))) {
                timing[i++] = LNode;
                ++LIndex;
            } else if (timeZones.areUsed() && (timeZones.getHostZone(LNode) > timeZones.getHostZone(RNode))) {
                timing[i++] = RNode;
                ++RIndex;
            } else { // Both nodes are from the same time zone.
                int timeZone = timeZones.getHostZone(LNode);

                int LIndexZoneEnd = LIndex + 1;
                int RIndexZoneEnd = RIndex + 1;

                if (timeZones.areUsed()) {
                    while ((LIndexZoneEnd != leftTime.length) && (timeZones.getHostZone(leftTime[LIndexZoneEnd]) == timeZone)) {
                        ++LIndexZoneEnd;
                    }
                    while ((RIndexZoneEnd != rightTime.length) && (timeZones.getHostZone(rightTime[RIndexZoneEnd]) == timeZone)) {
                        ++RIndexZoneEnd;
                    }
                } else {
                    LIndexZoneEnd = leftTime.length;
                    RIndexZoneEnd = rightTime.length;
                }

                int LTreeZoneMembers = LIndexZoneEnd - LIndex;
                int RTreeZoneMembers = RIndexZoneEnd - RIndex;
                int totalZoneMembers = LTreeZoneMembers + RTreeZoneMembers;

                // Assign all members from this time zone to the new timing.
                while (LTreeZoneMembers > 0 && RTreeZoneMembers > 0) {
                    int rand = Jane.rand.nextInt(totalZoneMembers);
                    if (rand < LTreeZoneMembers) {
                        int left = leftTime[LIndex++];
                        timing[i++] = left;
                        --LTreeZoneMembers;
                        //ensure polytomy nodes occur sequentially.
                        if (this.conseqPoly) {
                            while (node[left].polytomyName != -1 && LTreeZoneMembers != 0
                                    && node[left].polytomyName == node[leftTime[LIndex]].polytomyName) {
                                timing[i++] = leftTime[LIndex++];
                                --LTreeZoneMembers;
                                --totalZoneMembers;

                            }

                            // right array might also have fellow polytomies
                            while (node[left].polytomyName != -1 && RTreeZoneMembers != 0
                                    && node[left].polytomyName == node[rightTime[RIndex]].polytomyName) {
                                timing[i++] = rightTime[RIndex++];
                                --RTreeZoneMembers;
                                --totalZoneMembers;
                            }
                        }
                    } else {
                        int right = rightTime[RIndex++];
                        timing[i++] = right;
                        --RTreeZoneMembers;

                        if (this.conseqPoly) {
                            //ensure polytomy nodes occur sequentially.
                            while (node[right].polytomyName != -1 && RTreeZoneMembers != 0 && node[right].polytomyName == node[rightTime[RIndex]].polytomyName) {
                                timing[i++] = rightTime[RIndex++];
                                --RTreeZoneMembers;
                                --totalZoneMembers;
                            }
                            // left array might also have fellow polytomies
                            while (node[right].polytomyName != -1 && LTreeZoneMembers != 0 && node[right].polytomyName == node[leftTime[LIndex]].polytomyName) {
                                timing[i++] = leftTime[LIndex++];
                                --LTreeZoneMembers;
                                --totalZoneMembers;
                            }
                        }
                    }
                    --totalZoneMembers;
                }

                // Exactly one of these loops should execute.
                for (int j = 0; j < LTreeZoneMembers; ++j) {
                    timing[i++] = leftTime[LIndex++];
                }
                for (int j = 0; j < RTreeZoneMembers; ++j) {
                    timing[i++] = rightTime[RIndex++];
                }
            }
        }
        return timing;
    }

    public int getNodeID(int index) {
        for (int i = 0; i < postOrder.length; i++) {
            if (postOrder[i] == index) {
                return i;
            }
        }
        return -1;
    }

    public Tree crossTrees(Tree mate, int node, Map<Integer, Integer> thisNewToOldMap,
            Map<Integer, Integer> thisOldToNewMap, Map<Integer, Integer> mateNewToOldMap,
            Map<Integer, Integer> mateOldToNewMap) {
        if (!this.isPolytomyResolution()) {
            return this;
        }
        // all polytomy-resolutions are the same size
        Tree newTree = new Tree(size, false);

        // these traits are the same in this and mate
        newTree.root = root;
        newTree.totalPolytomySize = totalPolytomySize;
        newTree.polytomyCounter = polytomyCounter;
        newTree.lastRealNode = lastRealNode;
        newTree.conseqPoly = this.conseqPoly;
        newTree.noMidPolyEvents = noMidPolyEvents;
        if (node > lastRealNode) {
            System.out.println("Node given was not in acceptable range.");
            return this;
        }

        /*
         * these maps serve to rename the polytomy-resolution nodes. This is
         * necessary so we don't overlap names (the name '17' may be in both the
         * part of this and mate that we are combining). The NewToOld map maps
         * the new name (in newTree) to the parents. OldToNew does the opposite
         */
        newTree.polytomyNameCounter = lastRealNode + 1;

        LinkedList<Integer> thisNodes = new LinkedList<Integer>();
        LinkedList<Integer> mateNodes = new LinkedList<Integer>();
        thisNodes.add(root);
        while (!thisNodes.isEmpty()) {
            int current = thisNodes.poll();
            Node currentNode;

            /*
             * find the node in this associated with that. It may be different
             * from current because of polytomy naming.
             */
            if (current > lastRealNode) {
                int actualValue = thisNewToOldMap.get(current);
                currentNode = this.node[actualValue];
            } else {
                currentNode = this.node[current];
            }
            newTree.node[current].name = currentNode.name;

            newTree.node[current].polytomyName = currentNode.polytomyName;

            int lchild = currentNode.Lchild;
            if (lchild > lastRealNode) {
                int newNode = newTree.polytomyNameCounter++;
                thisNewToOldMap.put(newNode, lchild);
                thisOldToNewMap.put(lchild, newNode);
                lchild = newNode;
            }
            newTree.node[current].Lchild = lchild;
            if (lchild != -1) {
                newTree.node[lchild].parent = current;
            }

            int rchild = currentNode.Rchild;
            if (rchild > lastRealNode) {
                int newNode = newTree.polytomyNameCounter++;
                thisNewToOldMap.put(newNode, rchild);
                thisOldToNewMap.put(rchild, newNode);
                rchild = newNode;
            }
            newTree.node[current].Rchild = rchild;
            if (rchild != -1) {
                newTree.node[rchild].parent = current;
            }
            if (lchild == node) {
                mateNodes.add(lchild);
            } else if (lchild != -1) {
                thisNodes.add(lchild);
            }
            if (rchild == node) {
                mateNodes.add(rchild);
            } else if (lchild != -1) {
                thisNodes.add(rchild);
            }
        } while (!mateNodes.isEmpty()) {

            int current = mateNodes.poll();
            Node currentNode;

            /*
             * find the node in this associated with that. It may be different
             * from current because of polytomy naming.
             */
            if (current > lastRealNode) {
                int actualValue = mateNewToOldMap.get(current);
                currentNode = mate.node[actualValue];
            } else {
                currentNode = mate.node[current];
            }
            newTree.node[current].name = currentNode.name;
            newTree.node[current].polytomyName = currentNode.polytomyName;

            int lchild = currentNode.Lchild;
            if (lchild > lastRealNode) {
                int newNode = newTree.polytomyNameCounter++;
                mateNewToOldMap.put(newNode, lchild);
                mateOldToNewMap.put(lchild, newNode);
                lchild = newNode;
            }
            newTree.node[current].Lchild = lchild;
            if (lchild != -1) {
                newTree.node[lchild].parent = current;
            }

            int rchild = currentNode.Rchild;
            if (rchild > lastRealNode) {
                int newNode = newTree.polytomyNameCounter++;
                mateNewToOldMap.put(newNode, rchild);
                mateOldToNewMap.put(rchild, newNode);
                rchild = newNode;
            }

            newTree.node[current].Rchild = rchild;
            if (lchild != -1) {
                newTree.node[rchild].parent = current;
            }

            // don't add null slots!
            if (lchild != -1) {
                mateNodes.add(lchild);
            }
            if (rchild != -1) {
                mateNodes.add(rchild);
            }
        }
        newTree.root = newTree.findRoot();
        newTree.precomputePostOrderTraversal();
        newTree.preComputePreOrderTraversal();
        //newTree.directedDist = mate.directedDist;
        //newTree.undirectedDist = mate.undirectedDist;
        newTree.findTips();
        return newTree;
    }

    public boolean isPolytomyResolution() {
        return this.totalPolytomySize != 0;
    }

    /*
     * assumes that a and b are resolutions of the same tree
     *
     */
    public boolean equivalentNodes(int thisNode, Tree mate, int mateNode, Map<Integer, Integer> thisToMate) {
        if (thisNode == -1 && mateNode == -1) {
            return true;
        } else if (thisNode == -1 || mateNode == -1) {
            // only one is a dummy (nonexistent) node
            return false;
        }

        if (!(equivalentNodes(node[thisNode].Lchild, mate, mate.node[mateNode].Lchild, thisToMate)
                && equivalentNodes(node[thisNode].Rchild, mate, mate.node[mateNode].Rchild, thisToMate))
                && !(equivalentNodes(node[thisNode].Lchild, mate, mate.node[mateNode].Rchild, thisToMate)
                && equivalentNodes(node[thisNode].Rchild, mate, mate.node[mateNode].Lchild, thisToMate))) {
            // we can't match the children...
            return false;
        }
        if ((thisNode <= this.lastRealNode || mateNode <= this.lastRealNode) && thisNode != mateNode) {
            // a node is an original node, and they are not the same.
            return false;
        } else {
            /*
             * This should only occur in the correct assignment, if such an
             * assignment exists. It may occur multiple times, though.
             *
             */
            thisToMate.put(thisNode, mateNode);
        }

        /*
         * the children are the same, and the nodes are named the same unless
         * they are both polytomy resolutions.
         */
        return true;
    }

    public boolean isPolytomyEdge(int a) {
        return a > this.lastRealNode;
    }

    public boolean samePolytomy(int a, int b) {
        return this.node[a].polytomyName == this.node[b].polytomyName && isInPolytomy(a);
    }

    public boolean samePolytomyStatus(int a, int b) {
        return samePolytomy(a, b) || (!isInPolytomy(a) && !isInPolytomy(b));
    }

    // returns if node a is in a polytomy.
    public boolean isInPolytomy(int a) {
        return this.node[a].polytomyName != -1;
    }

    public boolean isOriginalNode(int a) {
        return a <= this.lastRealNode;
    }

    public int polytomyNameOf(int a) {
        return this.node[a].polytomyName;
    }
    
    // approximate size of this tree in bytes.
//    public int getSize() {
//        int sum = 0;
//        if (hasPolytomy) {
//            // add nodes
//            sum += size*6;
//            
//            // ints, booleans, that all have
//            sum+= 8*4 + 3;
//            
//            // add the set, map
//            sum+=
//        } else {
//            
//        } 
//        
//        return sum;
//    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Tree other = (Tree) obj;

        if (!this.preOrder.equals(other.preOrder)) {
            return false;
        }
        if (!Arrays.equals(this.depth, other.depth)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + this.preOrder.hashCode();
        hash = 23 * hash + Arrays.hashCode(this.depth);
        return hash;
    }
}
