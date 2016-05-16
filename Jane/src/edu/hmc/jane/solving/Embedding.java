/*
 * This class keeps the embedding of the parasite tree on the host tree.
 * It takes care of modifications to the embedding and also calculates cost.
 */
package edu.hmc.jane.solving;

import edu.hmc.jane.CostModel;
import edu.hmc.jane.HostLocation;
import edu.hmc.jane.HostLocation.HostLocationPair;
import edu.hmc.jane.Tree;
import edu.hmc.jane.solving.SolutionViewerInfo.EventInfo;
import java.util.List;

public class Embedding {

    public SolutionViewerInfo info;            // necessary information from DP
    public HostLocation[] parasitePosition;    // embedding position for each parasite
    public int[] parasiteCost;                 // keep the cost of parasite subtree embedding
    public int currentCost;                    // cost of the current embedding including
                                               // events before parasite root speciation.
    int[][] ancestorAtTime;
    public CostModel costModel;
    public int[] lossOrFtdAtHost;
    public int preRootCost;                    // cost of events along parasite root edge
    public HostLocation parasiteRootEdgeStart; // starting location of parasite root edge

    // CONSTRUCTOR
    public Embedding(SolutionViewerInfo info) {
        this.info = info;
        parasitePosition = new HostLocation[info.parasiteTree.size];
        parasiteCost = new int[info.parasiteTree.size];
        costModel = info.costModel;
        lossOrFtdAtHost = new int[info.hostTree.size];
        constructOriginalEmbedding();
        precomputeAncestorAtTime();
        updateLossOrFtdAtHost();
    }

    // precompute the ancestorAtTime table
    private void precomputeAncestorAtTime() {
        ancestorAtTime = new int[info.hostTree.size][info.hostTree.numTips + 1];
        for (int e_H = 0; e_H < info.hostTree.size; e_H++) {
            // find ancestors of e_H
            int time = info.hostTree.numTips;
            // if v_H has not occured, then no ancestor at that time
            while (time >= info.hostTiming.timeOfNode(e_H)) {
                ancestorAtTime[e_H][time--] = -1;
            }
            int ancestor = e_H;
            while (info.hostTree.node[ancestor].parent != -1) {
                // update the ancestor if needed
                if (time < info.hostTiming.timeOfNode(info.hostTree.node[ancestor].parent)) {
                    ancestor = info.hostTree.node[ancestor].parent;
                }
                // fill in the table
                ancestorAtTime[e_H][time--] = ancestor;
            }
            // fill in the table with the root
            while (time >= 0) {
                ancestorAtTime[e_H][time--] = ancestor;
            }
        }
    }

    // initialize the embedding and its cost to the optimal one in info
    private void constructOriginalEmbedding() {
        parasiteRootEdgeStart = info.optimalStart;
        currentCost = info.getOptParasiteEventCost(info.parasiteTree.root, parasiteRootEdgeStart);
        HostLocation optimalRootSpeciation = info.getOptPlacement(info.parasiteTree.root, parasiteRootEdgeStart);
        int afterRootCost = info.getOptParasiteSpecEventCost(info.parasiteTree.root, optimalRootSpeciation);
        preRootCost = currentCost - afterRootCost;
        constructOptEmbedding(info.parasiteTree.root, optimalRootSpeciation);
    }

    // update position for v_P and its children to the optimal one, recursively
    private void constructOptEmbedding(int v_P, HostLocation l_H) {
        // set the position of v_P
        parasitePosition[v_P] = l_H;
        if (l_H == null || costModel.isInfinity(info.getOptParasiteEventCost(v_P, l_H))) {
            System.err.println("Construction of Optimal Embedding Failed.");
        }
        EventInfo event = info.getOptParasiteSpecEventInfo(v_P, l_H);
        if(info.events != null)
            info.events.add(event);
        
        parasiteCost[v_P] = event.cost;
        if (!info.parasiteTree.isTip(v_P)) {
            // set the positions of v_P's children
            HostLocationPair childrenLocation = info.getOptChildrenPlacement(v_P, l_H);
            constructOptEmbedding(info.parasiteTree.node[v_P].Lchild, childrenLocation.l);
            constructOptEmbedding(info.parasiteTree.node[v_P].Rchild, childrenLocation.r);
        }
    }

    // compute the cost of events occurring before the speciation of the
    // parasite root.
    private int computeCostAlongRoot(HostLocation rootEdgePosition, HostLocation rootPosition) {
        int[] counts = countLossAndFtd(rootEdgePosition, rootPosition, info.parasiteTree.root);
        if (counts[0] == Tree.INFINITE_DISTANCE || counts[1] == Tree.INFINITE_DISTANCE) {
            return costModel.INFINITY;
        } else {
            return counts[0] * costModel.getLossCost()
                + counts[1] * costModel.getFailureToDivergeCost();
        }
    }

    // count the number of losses and failures to diverge in the range
    // [belowParentPosition, childrenPosition) when e_P is the parasite mapping
    // onto this range.
    private int[] countLossAndFtd(HostLocation belowParentPosition, HostLocation childPosition, int e_P) {
        // Each generation is either a loss or an ftd.
        int generations = info.hostTree.generations(belowParentPosition.ID, childPosition.ID);
        // If no descendants of e_P fail to diverge in the range, all generations are losses.
        // This speeds things up when there are few multihost parasites.
        
        if (!info.phi.hasMultihostParasites() ||
                info.earliestDescendantFTD[e_P] >= info.hostTiming.timeOfNode(childPosition.ID)) {
            return new int[] {generations, 0};
        }
        int losses = generations;
        int failuresToDiverge = 0;
        int previousHost = childPosition.ID;
        int currentHost = info.hostTree.node[childPosition.ID].parent;

        for (int i = 0; i < generations; ++i) {
            boolean foundFtdAtGeneration = false;
            for (int e_PTip : info.needsFTD[currentHost]) {
                if (info.parasiteTree.descendant(e_P, e_PTip)) {
                    ++failuresToDiverge;
                    --losses;
                    // A parasite edge should not fail to diverge twice at the same host node.
                    if (foundFtdAtGeneration) {
                        return new int[] {Tree.INFINITE_DISTANCE, Tree.INFINITE_DISTANCE};                        
                    } else {
                        foundFtdAtGeneration = true;
                    }

                    // Ensure that this failure to diverge event respects
                    // earlier ftd events of different parasites.
                    if (info.earliestDescendantFTD[e_P] != info.hostTiming.timeOfNode(currentHost)) {
                        if (info.earliestDescendantFTD[e_P] < info.earliestDescendantFTD[e_PTip]) {
                            return new int[] {Tree.INFINITE_DISTANCE, Tree.INFINITE_DISTANCE};
                        }

                        for (int par : (List<Integer>) info.parasiteTree.getDescendants(e_P)) {
                            if ((par != e_PTip) && info.earliestDescendantFTD[par] < info.hostTiming.timeOfNode(currentHost)) {
                                return new int[] {Tree.INFINITE_DISTANCE, Tree.INFINITE_DISTANCE};
                            }
                        }
                    }

                    // Pick a host tip in sibling subtree infected by e_PTip. The
                    // ftd event guarantees that one exists.
                    int sibling = info.hostTree.getSibling(previousHost);
                    int newHostTip = -1;
                    for (int e_H : info.phi.getHosts(e_PTip)) {
                        if (info.hostTree.descendant(sibling, e_H)) {
                            newHostTip = e_H;
                            break;
                        }
                    }

                    // Count events along the other branch of the ftd.
                    int[] lossAndFtdCounts = countLossAndFtd(new HostLocation(sibling, belowParentPosition.time, true, info.hostTree.getNodeID(sibling)),
                              new HostLocation(newHostTip, belowParentPosition.time, true, info.hostTree.getNodeID(newHostTip)),
                              e_PTip);
                    if (lossAndFtdCounts[0] == Tree.INFINITE_DISTANCE || lossAndFtdCounts[1] == Tree.INFINITE_DISTANCE) {
                        return new int[] {Tree.INFINITE_DISTANCE, Tree.INFINITE_DISTANCE};
                    }
                    
                    losses += lossAndFtdCounts[0];              
                    failuresToDiverge += lossAndFtdCounts[1];
                }
            }
            previousHost = currentHost;
            currentHost = info.hostTree.node[currentHost].parent;
        }

        return new int[] {losses, failuresToDiverge};
    }

    // count the number of occurrence of each type of the five events
    private int[] countEvents() {
        int[] count = new int[5];

        for (int v_P = 0; v_P < info.parasiteTree.size; v_P++) {
            // count events associated to v_P and losses or failures to
            // diverge between v_P and its children
            HostLocation parentPosition = parasitePosition[v_P];
            if (info.parasiteTree.isTip(v_P)) {
                continue;
            }

            // compute children information
            int left = info.parasiteTree.node[v_P].Lchild;
            int right = info.parasiteTree.node[v_P].Rchild;
            HostLocation leftPosition = parasitePosition[left];
            HostLocation rightPosition = parasitePosition[right];
            int left_alive = ancestorAtTime[leftPosition.ID][parentPosition.time];
            int right_alive = ancestorAtTime[rightPosition.ID][parentPosition.time];
            HostLocation left_branch = new HostLocation(left_alive, parentPosition.time, false, info.hostTree.getNodeID(left_alive));
            HostLocation right_branch = new HostLocation(right_alive, parentPosition.time, false, info.hostTree.getNodeID(right_alive));

            // categorize event
            if (parentPosition.isNode) {
                count[CostModel.COSPECIATION]++;
            } else if (parentPosition.ID == left_alive && parentPosition.ID == right_alive) {
                count[CostModel.DUPLICATION]++;
            } else {
                count[CostModel.HOST_SWITCH]++;
            }

            // count losses and failures to diverge
            int[] leftChildCounts = countLossAndFtd(left_branch, leftPosition, info.parasiteTree.node[v_P].Lchild);
            int[] rightChildCounts = countLossAndFtd(right_branch, rightPosition, info.parasiteTree.node[v_P].Rchild);
            count[CostModel.LOSS] += leftChildCounts[0];
            count[CostModel.LOSS] += rightChildCounts[0];
            count[CostModel.FAILURE_TO_DIVERGE] += leftChildCounts[1];
            count[CostModel.FAILURE_TO_DIVERGE] += rightChildCounts[1];
        }
        // Add counts from before the root.
        int[] countsBeforeRoot = countLossAndFtd(parasiteRootEdgeStart, parasitePosition[info.parasiteTree.root], info.parasiteTree.root);
        count[CostModel.LOSS] += countsBeforeRoot[0];
        count[CostModel.FAILURE_TO_DIVERGE] += countsBeforeRoot[1];

        return count;
    }

    // update the cost and solution after moving node v_P to a new position
    // returns costModel.INFINITY if the move is invalid
    public int costIfMove(int v_P, HostLocation newPosition) {

        if (info.parasiteTree.root == v_P) {
            // Case 1: v_P is a root. e_P may need to start earlier
            // to satisfy previous failure to diverge events.

            int newCost = info.getOptParasiteSpecEventCost(v_P, newPosition);
            HostLocation newParasiteRootEdgeLocation;
            if (!info.phi.hasMultihostParasites() || newPosition.time < info.earliestFTD) {
                newParasiteRootEdgeLocation = newPosition;
            } else {
                newParasiteRootEdgeLocation = new HostLocation(ancestorAtTime[newPosition.ID][info.earliestFTD-1], info.earliestFTD-1, false, info.hostTree.getNodeID(ancestorAtTime[newPosition.ID][info.earliestFTD-1]));
            }
            int newPreRootCost = computeCostAlongRoot(newParasiteRootEdgeLocation, newPosition);

            if (costModel.isInfinity(newCost) || costModel.isInfinity(newPreRootCost)) {
                return costModel.INFINITY;
            } else {
                return newCost + newPreRootCost;
            }
        } else {
            // Case 2: v_P is not a root

            // compute position of each related parasite nodes
            HostLocation oldPosition = parasitePosition[v_P];
            int parent = info.parasiteTree.node[v_P].parent;
            HostLocation parentPosition = parasitePosition[parent];
            int sibling = info.parasiteTree.getSibling(v_P);
            HostLocation siblingPosition = parasitePosition[sibling];

            // If the sibling is a multihost tip then any of its hosts are
            // candidates for siblingPosition. If one exists, use a host that
            // descends from parasitePosition. Otherwise valid host switch and
            // duplication events may be incorrectly declared illegal.
            if (info.phi.hasMultihostParasites() && info.parasiteTree.isTip(sibling)
                    && !info.hostTree.descendant(parentPosition.ID, siblingPosition.ID)) {
                for (int host : info.phi.getHosts(sibling)) {
                    if (info.hostTree.descendant(parentPosition.ID, host)) {
                        siblingPosition = new HostLocation(host, info.hostTiming.tipTime, true, info.hostTree.getNodeID(host));
                        break;
                    }
                }
            }

            // takes care of invalid case
            if ((newPosition.time < parentPosition.time)
                    || (newPosition.time == parentPosition.time && newPosition.isNode)) {
                return costModel.INFINITY;
            }

            // find the ancestors of the host locations of v_P and its sibling
            int old_alive = ancestorAtTime[oldPosition.ID][parentPosition.time];
            int new_alive = ancestorAtTime[newPosition.ID][parentPosition.time];
            int sibling_alive = ancestorAtTime[siblingPosition.ID][parentPosition.time];

            // create the positions below parent that old and new v_P are embedded onto
            HostLocation old_branch = new HostLocation(old_alive, parentPosition.time, false, info.hostTree.getNodeID(old_alive));
            HostLocation new_branch = new HostLocation(new_alive, parentPosition.time, false, info.hostTree.getNodeID(new_alive));

            int oldSubtreeCost, newSubtreeCost;
            // the parts of the cost that may change and needs to be calculated are:
            // 1. the cost for embedding v_P and its subtree on the tree
            // 2. the cost for losses/failures to diverge between v_P and its parent
            // 3. the cost for the change in event at its parent
            //    (duplication to host-switch, for example)

            int[] oldLossAndFtdCounts = countLossAndFtd(old_branch, oldPosition, v_P);
            int oldLossCount = oldLossAndFtdCounts[0];
            int oldFtdCount = oldLossAndFtdCounts[1];

            int[] lossAndFtdCounts = countLossAndFtd(new_branch, newPosition, v_P);
            int newLossCount = lossAndFtdCounts[0];
            int newFtdCount = lossAndFtdCounts[1];
            if (newLossCount == Tree.INFINITE_DISTANCE || newFtdCount == Tree.INFINITE_DISTANCE) {
                return costModel.INFINITY;
            }

            if (parentPosition.isNode) {
                // Case 2A: parent is at a node, so it's a cospeciation
                // calculate old cost
                oldSubtreeCost = parasiteCost[v_P]
                        + oldLossCount * costModel.getLossCost()
                        + oldFtdCount * costModel.getFailureToDivergeCost()
                        + costModel.getCospeciationCost();

                // if they are on the same branch of parent, then not cospeciation
                if (new_alive != old_alive) {
                    return costModel.INFINITY;
                }

                // calculate new cost
                int eventCost = info.getOptParasiteSpecEventCost(v_P, newPosition);
                if (costModel.isInfinity(eventCost)) {
                    return costModel.INFINITY;
                }
                newSubtreeCost = eventCost
                        + newLossCount * costModel.getLossCost()
                        + newFtdCount * costModel.getFailureToDivergeCost()
                        + costModel.getCospeciationCost();
            } else {
                // Case 2B: parent is at an edge, so it's a duplication (+ host-switch)
                // calculate old cost (assume one of v_P and its sibling does not switch host)
                if (parentPosition.ID == old_alive && parentPosition.ID == sibling_alive) {
                    // both v_P and its sibling on parent's subtree, so duplication
                    oldSubtreeCost = parasiteCost[v_P]
                            + oldLossCount * costModel.getLossCost()
                            + oldFtdCount * costModel.getFailureToDivergeCost()
                            + costModel.getDuplicationCost();
                } else {
                    // At least one of them not on parent's subtree, so host-switch.
                    if (parentPosition.ID != old_alive) {
                        oldSubtreeCost = parasiteCost[v_P]
                                + oldLossCount * costModel.getLossCost()
                                + oldFtdCount * costModel.getFailureToDivergeCost()
                                + costModel.getHostSwitchCost(parentPosition.ID, old_alive);
                    } else {
                        oldSubtreeCost = parasiteCost[v_P]
                                + oldLossCount * costModel.getLossCost()
                                + oldFtdCount * costModel.getFailureToDivergeCost()
                                + costModel.getHostSwitchCost(parentPosition.ID, sibling_alive);
                    }
                }
                
                // calculate new cost
                int eventCost = info.getOptParasiteSpecEventCost(v_P, newPosition);
                if (costModel.isInfinity(eventCost)) {
                    return costModel.INFINITY;
                }
                if (parentPosition.ID != new_alive && parentPosition.ID != sibling_alive) {
                    // both v_P and its sibling outside parent's subtree
                    return costModel.INFINITY;
                } else if (parentPosition.ID == new_alive && parentPosition.ID == sibling_alive) {
                    // both v_P and its sibling on parent's subtree, so duplication
                    if (costModel.isInfinity(eventCost)) {
                        return info.costModel.INFINITY;
                    }
                    newSubtreeCost = eventCost
                            + newLossCount * costModel.getLossCost()
                            + newFtdCount * costModel.getFailureToDivergeCost()
                            + costModel.getDuplicationCost();
                } else {
                    // At least one of them not on parent's subtree, so host-switch.
                    if (parentPosition.ID != new_alive) {
                        int hostSwitchCost = costModel.getHostSwitchCost(parentPosition.ID, new_alive);

                        if (costModel.isInfinity(hostSwitchCost)) {
                            return costModel.INFINITY;
                        }

                        // Check if host switching is prevented due to prior ftd events.
                        if (info.phi.hasMultihostParasites() && info.earliestDescendantFTD[v_P] <= parentPosition.time) {
                            return costModel.INFINITY;
                        }

                        newSubtreeCost = eventCost
                                + newLossCount * costModel.getLossCost()
                                + newFtdCount * costModel.getFailureToDivergeCost()
                                + hostSwitchCost;
                    } else {
                        int hostSwitchCost = costModel.getHostSwitchCost(parentPosition.ID, sibling_alive);

                        if (costModel.isInfinity(hostSwitchCost)) {
                            return costModel.INFINITY;
                        }
                        
                        // Check if host switching is prevented due to prior ftd events.
                        if (info.phi.hasMultihostParasites() && info.earliestDescendantFTD[sibling] <= parentPosition.time) {
                            return costModel.INFINITY;
                        }

                        newSubtreeCost = eventCost
                                + newLossCount * costModel.getLossCost()
                                + newFtdCount * costModel.getFailureToDivergeCost()
                                + hostSwitchCost;
                    }
                }
            }
            return currentCost - oldSubtreeCost + newSubtreeCost;
        }
    }

    // update the cost and solution after moving node v_P to a new position
    public void move(int v_P, HostLocation newPosition) {
        // When moving the root we must update parasiteRootEdgeStart
        if (v_P == info.parasiteTree.root) {
            if (!info.phi.hasMultihostParasites() || newPosition.time < info.earliestFTD) {
                parasiteRootEdgeStart = newPosition;
            } else {
                parasiteRootEdgeStart = new HostLocation(ancestorAtTime[newPosition.ID][info.earliestFTD-1], info.earliestFTD-1, false, info.hostTree.getNodeID(ancestorAtTime[newPosition.ID][info.earliestFTD-1]));
            }
        }

        int oldCost = currentCost;
        int oldPreRootCost = preRootCost;
        currentCost = costIfMove(v_P, newPosition);
        if (v_P == info.parasiteTree.root) {
            preRootCost = computeCostAlongRoot(parasiteRootEdgeStart, newPosition);
        }
        int costIncrease = currentCost - oldCost;
        int preRootCostIncrease = preRootCost - oldPreRootCost;

        for (int ancestor = v_P; ancestor != -1; ancestor = info.parasiteTree.node[ancestor].parent) {
            parasiteCost[ancestor] += costIncrease - preRootCostIncrease;
        }
        constructOptEmbedding(v_P, newPosition);
        updateLossOrFtdAtHost();
    }

    // check whether v_P is an optimal placement in the current branch
    // and whether this optimal placement is unique
    // 0 = optimal and unique, 1 = optimal but not unique, 2 = not optimal
    public int colorMode(int v_P) {
        int bestCost = costModel.INFINITY, bestCount = 0;
        // loop through all possible locations
        for (int e_H = 0; e_H < info.hostTree.size; e_H++) {
            int begin = (info.hostTree.root == e_H) ? 0 : info.hostTiming.timeOfNode(info.hostTree.node[e_H].parent);
            int end = info.hostTiming.timeOfNode(e_H);
            for (int time = begin; time <= end; time++) {
                HostLocation candidatePosition = new HostLocation(e_H, time, time == end, info.hostTree.getNodeID(e_H));
                int candidateCost = costIfMove(v_P, candidatePosition);
                // better location found
                if (candidateCost < bestCost) {
                    bestCost = candidateCost;
                    bestCount = 1;
                } else if (candidateCost == bestCost) {
                    // another location solution found
                    bestCount++;
                }
            }
        }
        // return the mode accordingly
        if (currentCost == bestCost && bestCount == 1) {
            return 0;
        } else if (currentCost == bestCost) {
            return 1;
        }
        return 2;
    }

    // obtain the optimal placement of v_P on its current branch
    public HostLocation getOptimalPosition(int v_P) {
        int bestCost = costModel.INFINITY;
        HostLocation bestPosition = null;
        // loop through all possible locations
        for (int e_H = 0; e_H < info.hostTree.size; e_H++) {
            int begin = (info.hostTree.root == e_H) ? 0 : info.hostTiming.timeOfNode(info.hostTree.node[e_H].parent);
            int end = info.hostTiming.timeOfNode(e_H);
            for (int time = begin; time <= end; time++) {
                HostLocation candidatePosition = new HostLocation(e_H, time, time == end, info.hostTree.getNodeID(e_H));
                int candidateCost = costIfMove(v_P, candidatePosition);
                // better location found
                if (candidateCost < bestCost) {
                    bestCost = candidateCost;
                    bestPosition = candidatePosition;
                }
            }
        }
        return bestPosition;
    }

    // update the count of losses + failures to diverge at each host
    public void updateLossOrFtdAtHost() {
        for (int e_H = 0; e_H < info.hostTree.size; e_H++) {
            lossOrFtdAtHost[e_H] = 0;
        }
        for (int e_P = 0; e_P < info.parasiteTree.size; e_P++) {
            // count the number of loss/ftd from e_P to its parent
            HostLocation childPosition = parasitePosition[e_P];
            HostLocation parentPosition;
            if (e_P != info.parasiteTree.root) {
                int parent = info.parasiteTree.node[e_P].parent;
                parentPosition = parasitePosition[parent];
            } else {
                parentPosition = parasiteRootEdgeStart;
            }
            int parentTime = parentPosition.time;
            if (childPosition.ID == parentPosition.ID) {
                continue;
            }

            int previous = childPosition.ID;
            for (int current = info.hostTree.node[childPosition.ID].parent; current != -1 && info.hostTiming.timeOfNode(current) > parentTime; current = info.hostTree.node[current].parent) {
                lossOrFtdAtHost[current]++;

                // If a parasite e_PTip fails to diverge then also
                // count events to other hosts infected by e_PTip
                if (info.phi.hasMultihostParasites()) {
                    for (int e_PTip : info.needsFTD[current]) {
                        if (info.parasiteTree.descendant(e_P, e_PTip)) {
                            countLossOrFtdInSubtree(info.hostTree.getSibling(previous), e_PTip);
                        }
                    }
                }

                previous = current;
            }
        }
    }

    // Helper for updateLossOrFtdAtHost that recursively counts loss or ftd
    // events involving ancestors of e_PTip in the subtree rooted at e_H.
    // ASSUMPTION: This function is only called if multihost parasites exist.
    private void countLossOrFtdInSubtree(int e_H, int e_PTip) {
        if (info.hostTree.isTip(e_H)) {
            return;
        }
        lossOrFtdAtHost[e_H]++;
        // If ftd, recurse on both subtrees. Otherwise it is a loss so recurse
        // on the appropriate subtree.
        if (info.needsFTD[e_H].contains(e_PTip)) {
            countLossOrFtdInSubtree(info.hostTree.node[e_H].Lchild, e_PTip);
            countLossOrFtdInSubtree(info.hostTree.node[e_H].Rchild, e_PTip);
        } else {
            for (int host : info.phi.getHosts(e_PTip)) {
                if (info.hostTree.descendant(info.hostTree.node[e_H].Lchild, host)) {
                    countLossOrFtdInSubtree(info.hostTree.node[e_H].Lchild, e_PTip);
                    break;
                } else if (info.hostTree.descendant(info.hostTree.node[e_H].Rchild, host)) {
                    countLossOrFtdInSubtree(info.hostTree.node[e_H].Rchild, e_PTip);
                    break;
                }
            }
        }
    }

    @Override
    public String toString() {
        String s = "Embedding information:\n";
        for (int v_P = 0; v_P < info.parasiteTree.size; v_P++) {
            s += "parasite node " + v_P + " on " + parasitePosition[v_P] + "\n";
            for (int addlHost : info.phi.getHosts(v_P)) {
                if (addlHost != parasitePosition[v_P].ID) {
                    HostLocation copyPosition = new HostLocation(addlHost, info.hostTiming.tipTime, true, info.hostTree.getNodeID(addlHost));
                    s += " also on " + copyPosition + "\n";
                }
            }
        }
        s += "total cost = " + currentCost + " / ";
        int[] eventCount = countEvents();
        s += "#cospeciation = " + eventCount[CostModel.COSPECIATION]
                + ", #duplication = " + eventCount[CostModel.DUPLICATION]
                + ", #host-switch = " + eventCount[CostModel.HOST_SWITCH]
                + ", #loss = " + eventCount[CostModel.LOSS]
                + ", #failure-to-diverge = " + eventCount[CostModel.FAILURE_TO_DIVERGE]
                + "\n";
        return s;
    }

    public String userString() {
        String s = "==================================\n";
        for (int v_P = 0; v_P < info.parasiteTree.size; v_P++) {
            HostLocation position = parasitePosition[v_P];
            EventInfo event = info.getOptParasiteSpecEventInfo(v_P, position);
            s += "Parasite Node: " + info.parasiteTree.node[v_P].name + "\n";
            s += "Association type: " + CostModel.eventTypeName(event.eventType) + "\n";
            s += "Host: " + position.userString(info.hostTree) + "\n";

            for (int addlHost : info.phi.getHosts(v_P)) {
                if (addlHost != position.ID) {
                    HostLocation copyPosition = new HostLocation(addlHost, info.hostTiming.tipTime, true, info.hostTree.getNodeID(addlHost));
                    s += "Host: " + copyPosition.userString(info.hostTree) + "\n";
                }
            }
            if (info.phi.hasMultihostParasites() && info.ftdPositions[v_P] != null) {
                for (int v_H : info.ftdPositions[v_P]) {
                    s += "Failure to Diverge Location: " + v_H + "\n";
                }
            }
            if (event.eventType == CostModel.HOST_SWITCH) {
                HostLocation target;
                if (event.subLoc1.ID == position.ID) {
                    target = event.subLoc2;
                } else {
                    target = event.subLoc1;
                }
                s += "Switch Target: " + target.userString(info.hostTree) + "\n";
            }
            s += "Event Time: " + (position.time + (position.isNode ? 0 : 1)) + "\n";
            s += "Subtree Cost: " + parasiteCost[v_P] + "\n";
            if (v_P == info.parasiteTree.root) {
                s += "Cost Before Parasite Root: " + computeCostAlongRoot(parasiteRootEdgeStart, parasitePosition[info.parasiteTree.root]) + "\n";
            }
            s += "--------------------------" + "\n";
        }
        int[] eventCount = countEvents();
        s += "Cospeciation: " + eventCount[CostModel.COSPECIATION] + "\n";
        s += "Duplication: " + eventCount[CostModel.DUPLICATION] + "\n";
        s += "Host Switch: " + eventCount[CostModel.HOST_SWITCH] + "\n";
        s += "Loss: " + eventCount[CostModel.LOSS] + "\n";
        s += "Failure to Diverge: " + eventCount[CostModel.FAILURE_TO_DIVERGE] + "\n";
        return s;
    }
}
