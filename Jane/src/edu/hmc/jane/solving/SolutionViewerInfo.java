/*
 * Currently, this class stores all information that a SolutionViewer (new
 * version) needs in order to draw the solution.
 */
package edu.hmc.jane.solving;

import edu.hmc.jane.HostLocation.HostLocationPair;

import edu.hmc.jane.*;

import edu.hmc.jane.Phi;
import edu.hmc.jane.TimeZones;
import edu.hmc.jane.Tree;
import edu.hmc.jane.TreeSpecs;
import edu.hmc.jane.gui.light.LightLabel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public class SolutionViewerInfo {

    public Tree hostTree;
    public Tree parasiteTree;
    public TreeSpecs hostTiming;
    public Phi phi;
    public TimeZones timeZones;
    
    // information about solution from ArrayDP3
    public int[][][] before;
    public int[][][] after;
    HostSwitchSelector hostSwitchSelector;
    InfestationSelector infestationSelector;
    public HostLocation optimalStart;
    public LinkedList<Integer>[] needsFTD;
    public LinkedList<Integer>[] ftdPositions;
    public int earliestFTD;
    public int[] earliestDescendantFTD;
    public ArrayList<EventInfo> events;
    
    // other information
    public CostModel costModel;
    public int[][][] eventsFromDP;
    
    
    // CONSTRUCTOR
    public SolutionViewerInfo(Tree hostTree, Tree parasiteTree,
                              TreeSpecs hostTiming, Phi phi, TimeZones timeZones,
                              CostModel costModel, int[][][] before, int[][][] after,
                              HostSwitchSelector hostSwitchSelector, InfestationSelector infestationSelector,
                              HostLocation optimalStart, LinkedList<Integer>[] needsFTD, 
                              LinkedList<Integer>[] ftdPositions, int earliestFTD, int[] earliestDescendantFTD, int[][][] events) {
        this.hostTree = hostTree;
        this.parasiteTree = parasiteTree;
        this.hostTiming = hostTiming;
        this.phi = phi;
        this.timeZones = timeZones;
        this.before = before;
        this.after = after;
        this.hostSwitchSelector = hostSwitchSelector;
        this.infestationSelector = infestationSelector;
        this.optimalStart = optimalStart;
        this.needsFTD = needsFTD;
        this.ftdPositions = ftdPositions;
        this.earliestFTD = earliestFTD;
        this.earliestDescendantFTD = earliestDescendantFTD;
        this.costModel = costModel;
        this.eventsFromDP = events; 
    }

    /* compute cost for a loss event where parasite goes on left host children
     * ASSUMPTION: e_H ends at given time
     */
    private int getLeftLossCost(int time, int e_P, int e_H) {
        if(costModel.isInfinity(after[time][e_P][hostTree.node[e_H].Lchild]))
            return costModel.INFINITY;
        return after[time][e_P][hostTree.node[e_H].Lchild] + costModel.getLossCost();
    }

    /* compute cost for a loss event where parasite goes on right host children
     * ASSUMPTION: e_H ends at given time 
     */
    private int getRightLossCost(int time, int e_P, int e_H) {
        if(costModel.isInfinity(after[time][e_P][hostTree.node[e_H].Rchild]))
            return costModel.INFINITY;
        return after[time][e_P][hostTree.node[e_H].Rchild] + costModel.getLossCost();
    }

    /* compute cost for a cospeciation event where each branch of host and
     * parasite goes in the opposite direction
     * ASSUMPTION: e_H ends at given time
     * ASSUMPTION: both e_P and e_H are not tip edges 
     */
    private int getTransCospeciationCost(int time, int e_P, int e_H) {
        if(costModel.isInfinity(after[time][parasiteTree.node[e_P].Lchild][hostTree.node[e_H].Rchild]))
            return costModel.INFINITY;
        if(costModel.isInfinity(after[time][parasiteTree.node[e_P].Rchild][hostTree.node[e_H].Lchild]))
            return costModel.INFINITY;
        return after[time][parasiteTree.node[e_P].Lchild][hostTree.node[e_H].Rchild]
                + after[time][parasiteTree.node[e_P].Rchild][hostTree.node[e_H].Lchild]
                + costModel.getCospeciationCost();
    }
    
    /* compute cost for a cospeciation event where each branch of host and
     * parasite goes in the same direction
     * ASSUMPTION: e_H ends at given time
     * ASSUMPTION: both e_P and e_H are not tip edges 
     */
    private int getCisCospeciationCost(int time, int e_P, int e_H) {
        if(costModel.isInfinity(after[time][parasiteTree.node[e_P].Lchild][hostTree.node[e_H].Lchild]))
            return costModel.INFINITY;
        if(costModel.isInfinity(after[time][parasiteTree.node[e_P].Rchild][hostTree.node[e_H].Rchild]))
            return costModel.INFINITY;
        return after[time][parasiteTree.node[e_P].Lchild][hostTree.node[e_H].Lchild]
                + after[time][parasiteTree.node[e_P].Rchild][hostTree.node[e_H].Rchild]
                + costModel.getCospeciationCost();
    }

    /* compute cost for a failure to diverge event where the left branch of
     * the host tree gets the tip edge.
     * ASSUMPTION: e_H ends at given time
     * ASSUMPTION: e_H is not a tip edge
     * ASSUMPTION: e_PTip is the tip edge corresponding to the unique descendant
     *             of e_P that infects a host in each of e_H's subtrees. 
     */
    private long getPreserveLeftFailureToDivergeCostInfo(int time, int e_P, int e_H, int e_PTip) {
        if(costModel.isInfinity(after[time][e_PTip][hostTree.node[e_H].Lchild]))
            return costModel.INFINITY;
        if(costModel.isInfinity(after[time][e_P][hostTree.node[e_H].Rchild]))
            return costModel.INFINITY;
        return after[time][e_PTip][hostTree.node[e_H].Lchild]
                + after[time][e_P][hostTree.node[e_H].Rchild]
                + costModel.getFailureToDivergeCost();
    }

    /* compute cost for a failure to diverge event where the right branch of
     * the host tree gets the tip edge.
     * ASSUMPTION: e_H ends at given time
     * ASSUMPTION: e_H is not a tip edge
     * ASSUMPTION: e_PTip is the tip edge corresponding to the unique descendant
     *             of e_P that infects a host in each of e_H's subtrees. 
     */
    private long getPreserveRightFailureToDivergeCostInfo(int time, int e_P, int e_H, int e_PTip) {
        if(costModel.isInfinity(after[time][e_P][hostTree.node[e_H].Lchild]))
            return costModel.INFINITY;
        if(costModel.isInfinity(after[time][e_PTip][hostTree.node[e_H].Rchild]))
            return costModel.INFINITY;
        return after[time][e_P][hostTree.node[e_H].Lchild]
                + after[time][e_PTip][hostTree.node[e_H].Rchild]
                + costModel.getFailureToDivergeCost();
    }

    /* compute cost for duplication
     * ASSUMPTION: e_P is not a tip edge 
     */
    private int getDuplicationCost(int time, int e_P, int e_H) {
        if(costModel.isInfinity(after[time][parasiteTree.node[e_P].Lchild][e_H]))
            return costModel.INFINITY;
        if(costModel.isInfinity(after[time][parasiteTree.node[e_P].Rchild][e_H]))
            return costModel.INFINITY;
        return after[time][parasiteTree.node[e_P].Lchild][e_H]
                + after[time][parasiteTree.node[e_P].Rchild][e_H]
                + costModel.getDuplicationCost();
    }
    
    /* simply return cost for nothing happens between time t and t - 1 on the
     * given pair of edges 
     */
    private int getNothingHappensCost(int time, int e_P, int e_H) {
        return before[time][e_P][e_H];
    }

    /* compute cost for host-switch where the left child of e_P switches host
     * ASSUPMTION: e_P is not a tip edge  
     */
    private int getLeftHostSwitchCost(int time, int e_P, int e_H) {
        // Check if host switching is prevented due to prior ftd events.
        if (phi.hasMultihostParasites() && earliestDescendantFTD[parasiteTree.node[e_P].Lchild] <= time)
            return costModel.INFINITY;
        if(costModel.isInfinity(after[time][parasiteTree.node[e_P].Rchild][e_H]))
            return costModel.INFINITY;
        int hostSwitchCost = hostSwitchSelector.getBestSwitchCost(time, parasiteTree.node[e_P].Lchild, e_H);
        if(costModel.isInfinity(hostSwitchCost))
            return costModel.INFINITY;
        return hostSwitchCost + after[time][parasiteTree.node[e_P].Rchild][e_H];
    }

    /* compute cost for host-switch where the right child of e_P switches host
     * ASSUPMTION: e_P is not a tip edge 
     */
    private int getRightHostSwitchCost(int time, int e_P, int e_H) {
        // Check if host switching is prevented due to prior ftd events.
        if (phi.hasMultihostParasites() && earliestDescendantFTD[parasiteTree.node[e_P].Rchild] <= time)
            return costModel.INFINITY;
        if(costModel.isInfinity(after[time][parasiteTree.node[e_P].Lchild][e_H]))
            return costModel.INFINITY;
        int hostSwitchCost = hostSwitchSelector.getBestSwitchCost(time, parasiteTree.node[e_P].Rchild, e_H);
        if(costModel.isInfinity(hostSwitchCost))
            return costModel.INFINITY;
        return hostSwitchCost + after[time][parasiteTree.node[e_P].Lchild][e_H];
    }
    
    /* compute cost for infestation where the left child of e_P infests
     * ASSUPMTION: e_P is not a tip edge 
     */
    private int getLeftInfestationCost(int time, int e_P, int e_H) {
        // Check if infestation is prevented due to prior ftd events.
        if (phi.hasMultihostParasites() && earliestDescendantFTD[parasiteTree.node[e_P].Lchild] <= time)
            return costModel.INFINITY;
        if(costModel.isInfinity(before[time][parasiteTree.node[e_P].Rchild][e_H]))
            return costModel.INFINITY;
        int infestationCost = infestationSelector.getBestInfestationCost(time, parasiteTree.node[e_P].Lchild, e_H);
        if(costModel.isInfinity(infestationCost))
            return costModel.INFINITY;
        return infestationCost + before[time][parasiteTree.node[e_P].Rchild][e_H];
    }

    /* compute cost for infestation where the right child of e_P infests
     * ASSUPMTION: e_P is not a tip edge
     */
    private int getRightInfestationCost(int time, int e_P, int e_H) {
        // Check if infestation is prevented due to prior ftd events.
        if (phi.hasMultihostParasites() && earliestDescendantFTD[parasiteTree.node[e_P].Rchild] <= time)
            return costModel.INFINITY;
        if(costModel.isInfinity(before[time][parasiteTree.node[e_P].Lchild][e_H]))
            return costModel.INFINITY;
        int infestationCost = infestationSelector.getBestInfestationCost(time, parasiteTree.node[e_P].Rchild, e_H);
        if(costModel.isInfinity(infestationCost))
            return costModel.INFINITY;
        return infestationCost + before[time][parasiteTree.node[e_P].Lchild][e_H];
    }

    /* obtain the information for optimal event happening with v_P on l_H */
    public EventInfo getOptParasiteSpecEventInfo(int v_P, HostLocation l_H) {   
        // Case 1: v_P is a tip
        if (parasiteTree.isTip(v_P)) {
            // l_H must be the associated tip for the mapping to be possible
            if (l_H.isNode && phi.containsAssociation(l_H.ID, v_P)) {
                return new EventInfo(CostModel.TIP, before[l_H.time][v_P][l_H.ID], null, null, l_H, hostTree.getNodeID(v_P));
            }
        } else {
            // Case 2: v_P is not a tip
            int v_P1 = parasiteTree.node[v_P].Lchild;
            int v_P2 = parasiteTree.node[v_P].Rchild;
                
            int cisCospeciationCost;
            int transCospeciationCost;
            if (l_H.isNode && timeZones.overlap(v_P, l_H.ID)) {
                if (hostTree.isTip(l_H.ID)) {
                return new EventInfo(CostModel.OTHER, costModel.INFINITY, null, null, null, -1);
            }
                cisCospeciationCost = getCisCospeciationCost(l_H.time, v_P, l_H.ID);
                transCospeciationCost = getTransCospeciationCost(l_H.time, v_P, l_H.ID);
            } else {
                cisCospeciationCost = 0;
                transCospeciationCost = 0;
            }

            int duplicationCost = getDuplicationCost(l_H.time, v_P, l_H.ID);
            int leftHostSwitchCost = getLeftHostSwitchCost(l_H.time, v_P, l_H.ID);
            int rightHostSwitchCost = getRightHostSwitchCost(l_H.time, v_P, l_H.ID);
            HostLocation subLoc1;
            HostLocation subLoc2;
            int switchLocation;

            /* If randomize mode is on, we will lookup in eventsFromDP which
                * event was chosen at each intersection and time.
                */
            if (hostSwitchSelector.problem.randomizeEvents) { 
                switch (eventsFromDP[l_H.time][v_P][l_H.ID]) {
                    case (-1) : 
                        break;
                    case ArrayDP3.COSPECIATION_TRANS:
                        subLoc1 = new HostLocation(hostTree.node[l_H.ID].Rchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Rchild));
                        subLoc2 = new HostLocation(hostTree.node[l_H.ID].Lchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Lchild));
                        return new EventInfo(CostModel.COSPECIATION, transCospeciationCost, subLoc1, subLoc2, l_H, hostTree.getNodeID(v_P));
                    case ArrayDP3.COSPECIATION_CIS:
                        subLoc1 = new HostLocation(hostTree.node[l_H.ID].Lchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Lchild));
                        subLoc2 = new HostLocation(hostTree.node[l_H.ID].Rchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Rchild));
                        return new EventInfo(CostModel.COSPECIATION, cisCospeciationCost, subLoc1, subLoc2, l_H, hostTree.getNodeID(v_P));
                    case ArrayDP3.DUPLICATION: //Duplication
                        return new EventInfo(CostModel.DUPLICATION, duplicationCost, l_H, l_H, l_H, hostTree.getNodeID(v_P));
                    case ArrayDP3.HOSTSWITCH_LEFT:
                        switchLocation = hostSwitchSelector.getBestSwitchLocation(l_H.time, v_P1, l_H.ID);
                        subLoc1 = new HostLocation(switchLocation, l_H.time, false, hostTree.getNodeID(switchLocation));
                        subLoc2 = l_H;
                        return new EventInfo(CostModel.HOST_SWITCH, leftHostSwitchCost, subLoc1, subLoc2, l_H, hostTree.getNodeID(v_P));                            
                    case ArrayDP3.HOSTSWITCH_RIGHT: 
                        switchLocation = hostSwitchSelector.getBestSwitchLocation(l_H.time, v_P2, l_H.ID);
                        subLoc1 = l_H;
                        subLoc2 = new HostLocation(switchLocation, l_H.time, false, hostTree.getNodeID(switchLocation));
                        return new EventInfo(CostModel.HOST_SWITCH, rightHostSwitchCost, subLoc1, subLoc2, l_H, hostTree.getNodeID(v_P));
                    default:
                        break;
                }
            }

            if (l_H.isNode) {
                // Case 2A: v_P is mapped onto a node
                // l_H cannot be a tip node
                if (hostTree.isTip(l_H.ID)) {
                    return new EventInfo(CostModel.OTHER, costModel.INFINITY, null, null, null, -1);
                }
                // v_P speciates at a host node, so it's a cospeciation
                // failure to diverge should not have been required
                if (phi.hasMultihostParasites()) {
                    for (int v_PTip : needsFTD[l_H.ID]) {
                        if (parasiteTree.descendant(v_P, v_PTip)) {
                            return new EventInfo(CostModel.OTHER, costModel.INFINITY, null, null, null, -1);
                        }
                    }
                }          
                // subtree1 = host subtree where left children of v_P is associated
                // subtree2 = host subtree where right children of v_P is associated
                if (timeZones.overlap(v_P, l_H.ID)) {
                         
                    
                    if (cisCospeciationCost <= transCospeciationCost && !costModel.isInfinity(cisCospeciationCost)) {
                        subLoc1 = new HostLocation(hostTree.node[l_H.ID].Lchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Lchild));
                        subLoc2 = new HostLocation(hostTree.node[l_H.ID].Rchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Rchild));
                        return new EventInfo(CostModel.COSPECIATION, cisCospeciationCost, subLoc1, subLoc2, l_H, hostTree.getNodeID(v_P));
                    } else if (!costModel.isInfinity(transCospeciationCost)) {
                        subLoc1 = new HostLocation(hostTree.node[l_H.ID].Rchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Rchild));
                        subLoc2 = new HostLocation(hostTree.node[l_H.ID].Lchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Lchild));
                        return new EventInfo(CostModel.COSPECIATION, transCospeciationCost, subLoc1, subLoc2, l_H, hostTree.getNodeID(v_P));
                    }
                }
            } else {
                // Case 2B: v_P is mapped onto an edge
                // Ensure the parasite and host time zones match.
                int hostAtNextTime;
                if (hostTiming.tipTime == l_H.time + 1) {
                    hostAtNextTime = l_H.ID;
                } else {
                    hostAtNextTime = hostTiming.nodeAtTime(l_H.time + 1);
                }
                if (timeZones.overlap(v_P, hostAtNextTime)) {

                    // v_P speciates at a host edge, so it's a duplication (+ host-switch)
                    duplicationCost = getDuplicationCost(l_H.time, v_P, l_H.ID);
                    leftHostSwitchCost = getLeftHostSwitchCost(l_H.time, v_P, l_H.ID);
                    rightHostSwitchCost = getRightHostSwitchCost(l_H.time, v_P, l_H.ID);
                    if (duplicationCost <= leftHostSwitchCost && duplicationCost <= rightHostSwitchCost && !costModel.isInfinity(duplicationCost)) {
                        return new EventInfo(CostModel.DUPLICATION, duplicationCost, l_H, l_H, l_H, hostTree.getNodeID(v_P));
                    } else if (leftHostSwitchCost <= rightHostSwitchCost && !costModel.isInfinity(leftHostSwitchCost)) {
                        switchLocation = hostSwitchSelector.getBestSwitchLocation(l_H.time, v_P1, l_H.ID);
                        subLoc1 = new HostLocation(switchLocation, l_H.time, false, hostTree.getNodeID(switchLocation));
                        subLoc2 = l_H;
                        return new EventInfo(CostModel.HOST_SWITCH, leftHostSwitchCost, subLoc1, subLoc2, l_H, hostTree.getNodeID(v_P));
                    } else if (!costModel.isInfinity(rightHostSwitchCost)) {
                        switchLocation = hostSwitchSelector.getBestSwitchLocation(l_H.time, v_P2, l_H.ID);
                        subLoc1 = l_H;
                        subLoc2 = new HostLocation(switchLocation, l_H.time, false, hostTree.getNodeID(switchLocation));
                        return new EventInfo(CostModel.HOST_SWITCH, rightHostSwitchCost, subLoc1, subLoc2, l_H, hostTree.getNodeID(v_P));
                    }
                }
            }
        }
        return new EventInfo(CostModel.OTHER, costModel.INFINITY, null, null, null, -1);
    }

    /* obtain the information for optimal event happening with e_P or v_P on l_H */
    public EventInfo getOptParasiteEventInfo(int e_P, HostLocation l_H) { 
        int expectedCost;
        if (l_H.isNode) {
            expectedCost = before[l_H.time][e_P][l_H.ID];
        } else {
            expectedCost = after[l_H.time][e_P][l_H.ID];   
        }
        HostLocation subLoc;
        HostLocation subLoc1;
        HostLocation subLoc2;
            /* If randomize mode is on, we will lookup in eventsFromDP which
            * event was chosen at each intersection and time.
            */
        if (hostSwitchSelector.problem.randomizeEvents) {
            switch (eventsFromDP[l_H.time][e_P][l_H.ID]) {  //NOTE: All of the event cases currently appear in eventsFromDP
                case (-1) : //No Event chosen
                    break;
                case ArrayDP3.LOSS_LEFT :
                    subLoc = new HostLocation(hostTree.node[l_H.ID].Lchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Lchild));
                    return new EventInfo(CostModel.LOSS, expectedCost, subLoc, null, l_H, hostTree.getNodeID(e_P));
                case ArrayDP3.LOSS_RIGHT:
                    subLoc = new HostLocation(hostTree.node[l_H.ID].Rchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Rchild));
                    return new EventInfo(CostModel.LOSS, expectedCost, subLoc, null, l_H, hostTree.getNodeID(e_P));
                case ArrayDP3.COSPECIATION_TRANS:
                    subLoc1 = new HostLocation(hostTree.node[l_H.ID].Rchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Rchild));
                    subLoc2 = new HostLocation(hostTree.node[l_H.ID].Lchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Lchild));
                    return new EventInfo(CostModel.COSPECIATION, expectedCost, subLoc1, subLoc2, l_H, hostTree.getNodeID(e_P));
                case ArrayDP3.COSPECIATION_CIS:
                    subLoc1 = new HostLocation(hostTree.node[l_H.ID].Lchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Lchild));
                    subLoc2 = new HostLocation(hostTree.node[l_H.ID].Rchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Rchild));
                    return new EventInfo(CostModel.COSPECIATION, expectedCost, subLoc1, subLoc2, l_H, hostTree.getNodeID(e_P));
                case ArrayDP3.FTD_PRESERVE_LEFT:
                    break; //Currently these are deterministic and will get handled below
                case ArrayDP3.FTD_PRESERVE_RIGHT:
                    break;
                case ArrayDP3.DUPLICATION:
                    return new EventInfo(CostModel.DUPLICATION, expectedCost, l_H, l_H, l_H, hostTree.getNodeID(e_P));
                case ArrayDP3.NO_HOST:
                    break;
                case ArrayDP3.NOTHING_HAPPENS:
                    HostLocation below_l_H;
                    below_l_H = new HostLocation(l_H.ID, l_H.time + 1, l_H.time + 1 == hostTiming.timeOfNode(l_H.ID), hostTree.getNodeID(l_H.ID));
                    return new EventInfo(CostModel.NOTHING, expectedCost, below_l_H, null, below_l_H, -1);
                case ArrayDP3.HOSTSWITCH_LEFT:
                    int v_P1 = parasiteTree.node[e_P].Lchild;
                    int switchLocation = hostSwitchSelector.getBestSwitchLocation(l_H.time, v_P1, l_H.ID);
                    subLoc1 = new HostLocation(switchLocation, l_H.time, false, hostTree.getNodeID(switchLocation));
                    subLoc2 = l_H;
                    return new EventInfo(CostModel.HOST_SWITCH, expectedCost, subLoc1, subLoc2, l_H, hostTree.getNodeID(e_P));
                case ArrayDP3.HOSTSWITCH_RIGHT:
                    int v_P2 = parasiteTree.node[e_P].Rchild;
                    switchLocation = hostSwitchSelector.getBestSwitchLocation(l_H.time, v_P2, l_H.ID);
                    subLoc1 = l_H;
                    subLoc2 = new HostLocation(switchLocation, l_H.time, false, hostTree.getNodeID(switchLocation));
                    return new EventInfo(CostModel.HOST_SWITCH, expectedCost, subLoc1, subLoc2, l_H, hostTree.getNodeID(e_P));                       
                default:
                    break;
                }
            }

        // Case 1: l_H is a host node
        if (l_H.isNode) {
            // Case 1A: l_H is a host tip
            if (hostTree.isTip(l_H.ID)) {
                // can be mapped iff e_P is a tip associated to that host tip
                if (phi.containsAssociation(l_H.ID, e_P)) {
                    return new EventInfo(CostModel.TIP, before[l_H.time][e_P][l_H.ID], null, null, l_H, hostTree.getNodeID(e_P));
                }
            } else {
                // Case 1B: l_H speciates, so it's a cospeciation, loss, or failure to diverge
                expectedCost = before[l_H.time][e_P][l_H.ID];
                if(costModel.isInfinity(expectedCost))
                    return new EventInfo(CostModel.OTHER, expectedCost, null, null, null, -1);

                // See how many parasites (0, 1, or >= 2) descending from e_P need
                // to fail to diverge at l_H. Short-circuit if we find 2
                // or more.
                int parasitesNeedFTDorINFESTATION = 0;
                int e_PTip = -1; // A parasite needing to FTD at l_H.
                if (phi.hasMultihostParasites()) {
                    for (int tip : needsFTD[l_H.ID]) {
                        if (parasiteTree.descendant(e_P, tip)) {
                            e_PTip = tip;
                            if (++parasitesNeedFTDorINFESTATION >= 2) {
                                break;
                            }
                        }
                    }
                }
                
                // No failure to diverge or infestation events need to happen. Try other events.
                if (parasitesNeedFTDorINFESTATION == 0) {
                    // if it's a loss, then e_P is located on a branch below l_H
                    if (expectedCost == getLeftLossCost(l_H.time, e_P, l_H.ID)) {
                        subLoc = new HostLocation(hostTree.node[l_H.ID].Lchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Lchild));
                        return new EventInfo(CostModel.LOSS, expectedCost, subLoc, null, l_H, hostTree.getNodeID(e_P));
                    }
                    if (expectedCost == getRightLossCost(l_H.time, e_P, l_H.ID)) {
                        subLoc = new HostLocation(hostTree.node[l_H.ID].Rchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Rchild));
                        return new EventInfo(CostModel.LOSS, expectedCost, subLoc, null, l_H, hostTree.getNodeID(e_P));
                    }                 
                    if (!parasiteTree.isTip(e_P) && (timeZones.overlap(e_P, l_H.ID))) {                       
                        // if it's a cospeciation, then e_P is located at l_H
                        if (expectedCost == getCisCospeciationCost(l_H.time, e_P, l_H.ID)) {
                            subLoc1 = new HostLocation(hostTree.node[l_H.ID].Lchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Lchild));
                            subLoc2 = new HostLocation(hostTree.node[l_H.ID].Rchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Rchild));
                            return new EventInfo(CostModel.COSPECIATION, expectedCost, subLoc1, subLoc2, l_H, hostTree.getNodeID(e_P));
                        }
                        if (expectedCost == getTransCospeciationCost(l_H.time, e_P, l_H.ID)) {
                            subLoc1 = new HostLocation(hostTree.node[l_H.ID].Rchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Rchild));
                            subLoc2 = new HostLocation(hostTree.node[l_H.ID].Lchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Lchild));
                            return new EventInfo(CostModel.COSPECIATION, expectedCost, subLoc1, subLoc2, l_H, hostTree.getNodeID(e_P));
                        }
                    }
                } else if (parasitesNeedFTDorINFESTATION == 1) {
                    // if it's a failure to diverge, then e_P is located on a branch below l_H and e_PTip is
                    // located on the other branch below l_H.
                    if (expectedCost == getPreserveLeftFailureToDivergeCostInfo(l_H.time, e_P, l_H.ID, e_PTip)) {
                        subLoc1 = new HostLocation(hostTree.node[l_H.ID].Rchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Rchild));
                        subLoc2 = new HostLocation(hostTree.node[l_H.ID].Lchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Lchild));
                        return new EventInfo(CostModel.FAILURE_TO_DIVERGE, expectedCost, subLoc1, subLoc2, l_H, e_PTip, true);
                    }
                    if (expectedCost == getPreserveRightFailureToDivergeCostInfo(l_H.time, e_P, l_H.ID, e_PTip)) {
                        subLoc1 = new HostLocation(hostTree.node[l_H.ID].Lchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Lchild));
                        subLoc2 = new HostLocation(hostTree.node[l_H.ID].Rchild, l_H.time, false, hostTree.getNodeID(hostTree.node[l_H.ID].Rchild));
                        return new EventInfo(CostModel.FAILURE_TO_DIVERGE, expectedCost, subLoc1, subLoc2, l_H, e_PTip, true);
                    }
                }
            }
        } else {
            // Case 2: l_H is a host edge
            // either it's a duplication (+ host-switch), or nothing happens
            expectedCost = after[l_H.time][e_P][l_H.ID];
            if (costModel.isInfinity(expectedCost)) {
                return new EventInfo(CostModel.OTHER, expectedCost, null, null, null, -1);
            }
            if (expectedCost == getNothingHappensCost(l_H.time + 1, e_P, l_H.ID)) {
                // create a HostLocation right below l_H - can be an edge or a node
                HostLocation below_l_H;
                below_l_H = new HostLocation(l_H.ID, l_H.time + 1, l_H.time + 1 == hostTiming.timeOfNode(l_H.ID), hostTree.getNodeID(l_H.ID));
                return new EventInfo(CostModel.NOTHING, expectedCost, below_l_H, null, below_l_H, -1);
            }

            // Ensure the parasite and host time zones match.
            int hostAtNextTime;
            if (hostTiming.tipTime == l_H.time + 1) {
                hostAtNextTime = l_H.ID;
            } else {
                hostAtNextTime = hostTiming.nodeAtTime(l_H.time + 1);
            }
            if (timeZones.overlap(e_P, hostAtNextTime)) {             
                if (expectedCost == getDuplicationCost(l_H.time, e_P, l_H.ID)) {
                    //System.out.println("derplication");
                    return new EventInfo(CostModel.DUPLICATION, expectedCost, l_H, l_H, l_H, hostTree.getNodeID(e_P));
                }
                if (expectedCost == getLeftHostSwitchCost(l_H.time, e_P, l_H.ID)) {
                    int v_P1 = parasiteTree.node[e_P].Lchild;
                    int switchLocation = hostSwitchSelector.getBestSwitchLocation(l_H.time, v_P1, l_H.ID);
                    subLoc1 = new HostLocation(switchLocation, l_H.time, false, hostTree.getNodeID(switchLocation));
                    subLoc2 = l_H;
                    return new EventInfo(CostModel.HOST_SWITCH, expectedCost, subLoc1, subLoc2, l_H, hostTree.getNodeID(e_P));
                }
                if (expectedCost == getRightHostSwitchCost(l_H.time, e_P, l_H.ID)) {
                    int v_P2 = parasiteTree.node[e_P].Rchild;
                    int switchLocation = hostSwitchSelector.getBestSwitchLocation(l_H.time, v_P2, l_H.ID);
                    subLoc1 = l_H;
                    subLoc2 = new HostLocation(switchLocation, l_H.time, false, hostTree.getNodeID(switchLocation));
                    return new EventInfo(CostModel.HOST_SWITCH, expectedCost, subLoc1, subLoc2, l_H, hostTree.getNodeID(e_P));
                }
            }
        }
        return new EventInfo(CostModel.OTHER, costModel.INFINITY, null, null, null, -1);
    }
    

    /* get the cost for optimal event happening with e_P or v_P on l_H */
    public int getOptParasiteEventCost(int e_P, HostLocation l_H) {
        EventInfo v_P_speciate = getOptParasiteEventInfo(e_P, l_H);
        return v_P_speciate.cost;
    }

    /* get the cost for optimal event happening with v_P on l_H */
    public int getOptParasiteSpecEventCost(int v_P, HostLocation l_H) {
        EventInfo v_P_speciate = getOptParasiteSpecEventInfo(v_P, l_H);
        return v_P_speciate.cost;
    }

    /* find the placement of node v_P on host subtree rooted at l_H */
    public HostLocation getOptPlacement(int v_P, HostLocation l_H) {
        if (l_H == null) {
            return null;
        }
        EventInfo eventHere = getOptParasiteEventInfo(v_P, l_H);
        switch (eventHere.eventType) {
            // these events include node v_P - tip or speciation
            case CostModel.COSPECIATION:
            case CostModel.DUPLICATION:
            case CostModel.HOST_SWITCH:
            case CostModel.TIP:
                return l_H;
            // these events does not include v_P yet - just on e_P
            case CostModel.FAILURE_TO_DIVERGE:
            case CostModel.LOSS:
            case CostModel.NOTHING:
                return getOptPlacement(v_P, eventHere.subLoc1);
                
            case CostModel.INFESTATION:
        }
        return null;
    }

    /* get the optimal position of v_P's left and right child if v_P is placed on l_H */
    public HostLocationPair getOptChildrenPlacement(int v_P, HostLocation l_H) {
        // base case: tip - no children
        if (parasiteTree.isTip(v_P)) {
            return new HostLocationPair(null, null);
        }
        // get information for this speciation
        EventInfo v_P_speciate = getOptParasiteSpecEventInfo(v_P, l_H);
        // find the optimal placements of the children
        int v_P1 = parasiteTree.node[v_P].Lchild;
        int v_P2 = parasiteTree.node[v_P].Rchild;
        HostLocation v_P1_placement = getOptPlacement(v_P1, v_P_speciate.subLoc1);
        HostLocation v_P2_placement = getOptPlacement(v_P2, v_P_speciate.subLoc2);
        return new HostLocationPair(v_P1_placement, v_P2_placement);
    }
    
    /* gets a list of events for the tree starting with v_P at 1_H */
    public ArrayList<EventInfo> getEvents(int v_P, HostLocation l_H){
        // get all events after wrapper function
        if (events == null) {
            events = new ArrayList();
            getSubEvents(v_P, l_H, events);
        }
        return events;
    }
    
    /* adds the events for the tree with v_P on 1_H to an array */
    public EventInfo getSubEvents(int v_P, HostLocation l_H, ArrayList<EventInfo> events) {
        if (l_H == null) {
            return null;
        }
        // obtain event information for all events in subtree
        EventInfo event = getOptParasiteEventInfo(v_P, l_H);
        events.add(event);

        if (event.eventType == CostModel.LOSS) {
            HostLocation lossLoc = getOptPlacementEvents(v_P, event.subLoc1, events);
            event.subtree1 = getSubEvents(v_P, lossLoc, events);
            return event;         
        } else if(event.eventType == CostModel.FAILURE_TO_DIVERGE){
            HostLocation lossLoc = getOptPlacementEvents(v_P, event.subLoc1, events);
            event.subtree1 = getSubEvents(v_P, lossLoc, events);

            lossLoc = getOptPlacementEvents(v_P, event.subLoc2, events);
            event.subtree2 = getSubEvents(v_P, lossLoc, events);

            return event;

        } else if (!parasiteTree.isTip(v_P)) {

            // if not tip, then recursive to get children's events
            HostLocationPair childrenLocation = getOptChildrenPlacementEvents(v_P, l_H);

            event.subtree2 = getSubEvents(parasiteTree.node[v_P].Rchild, childrenLocation.r, events);   
            event.subtree1 = getSubEvents(parasiteTree.node[v_P].Lchild, childrenLocation.l, events);
              
        } else {
            HostLocation lossLoc = getOptPlacementEvents(v_P, event.subLoc1, events);
            event.subtree1 = getSubEvents(v_P, lossLoc, events);     

        }
        return event;
    }
    
    /* gets the event tree starting with v_P on 1_H */
    public EventInfo getEventTree(int v_P, HostLocation l_H) {
        if (l_H == null) {
            return null;
        }
        // obtain event information for all events in subtree
        EventInfo event = getOptParasiteEventInfo(v_P, l_H);
        if (event.eventType == CostModel.LOSS) {
            HostLocation lossLoc = getOptPlacementEvents(v_P, event.subLoc1);
            event.subtree1 = getEventTree(v_P, lossLoc);
            event.supportLabel = new LightLabel(""); 

            return event;         
        } else if (event.eventType == CostModel.FAILURE_TO_DIVERGE) {
            HostLocation lossLoc = getOptPlacementEvents(v_P, event.subLoc1);
            event.subtree1 = getEventTree(v_P, lossLoc);

            lossLoc = getOptPlacementEvents(v_P, event.subLoc2);
            event.subtree2 = getEventTree(v_P, lossLoc);
            event.supportLabel = new LightLabel(""); 

            return event;
        } else if (!parasiteTree.isTip(v_P)) {

            // if not tip, then recursive to get children's events
            HostLocationPair childrenLocation = getOptChildrenPlacementEvents(v_P, l_H);


            event.subtree2 = getEventTree(parasiteTree.node[v_P].Rchild, childrenLocation.r);   
            event.subtree1 = getEventTree(parasiteTree.node[v_P].Lchild, childrenLocation.l);
            event.supportLabel = new LightLabel(""); 
        } else {
            HostLocation lossLoc = getOptPlacementEvents(v_P, event.subLoc1);
            event.subtree1 = getEventTree(v_P, lossLoc);     
            event.supportLabel = new LightLabel(""); 
        }
        return event;
    }
  
    public HostLocationPair getOptChildrenPlacementEvents(int v_P, HostLocation l_H) {
        // base case: tip - no children
        if (parasiteTree.isTip(v_P)) {
            return new HostLocationPair(null, null);
        }
        // get information for this speciation
        EventInfo v_P_speciate = getOptParasiteSpecEventInfo(v_P, l_H);
        // find the optimal placements of the children
        int v_P1 = parasiteTree.node[v_P].Lchild;
        int v_P2 = parasiteTree.node[v_P].Rchild;
        HostLocation v_P1_placement = getOptPlacementEvents(v_P1, v_P_speciate.subLoc1);
        HostLocation v_P2_placement = getOptPlacementEvents(v_P2, v_P_speciate.subLoc2);
        return new HostLocationPair(v_P1_placement, v_P2_placement);
    }
        
    /* Used by getEvents */
    public HostLocation getOptPlacementEvents(int v_P, HostLocation l_H, ArrayList<EventInfo> events) {
        if (l_H == null) {
            return null;
        }
        EventInfo eventHere = getOptParasiteEventInfo(v_P, l_H);
        switch (eventHere.eventType) {
            // these events include node v_P - tip or speciation
            case CostModel.COSPECIATION:
            case CostModel.DUPLICATION:
            case CostModel.HOST_SWITCH:
            case CostModel.TIP:
                return l_H;
            // these events does not include v_P yet - just on e_P
            case CostModel.FAILURE_TO_DIVERGE:
            case CostModel.INFESTATION:
            case CostModel.LOSS:
                events.add(eventHere);
                return getOptPlacementEvents(v_P, eventHere.subLoc1, events);
            case CostModel.NOTHING:
                return getOptPlacementEvents(v_P, eventHere.subLoc1, events);
        }
        return null;
    }   
    
    /* Used by getEvent tree */
    public HostLocation getOptPlacementEvents(int v_P, HostLocation l_H) {
        if (l_H == null) {
            return null;
        }
        EventInfo eventHere = getOptParasiteEventInfo(v_P, l_H);
        switch (eventHere.eventType) {
            // these events include node v_P - tip or speciation
            case CostModel.COSPECIATION:
            case CostModel.DUPLICATION:
            case CostModel.HOST_SWITCH:
            case CostModel.TIP:
                return l_H;
            // these events does not include v_P yet - just on e_P
            case CostModel.NOTHING:
                return getOptPlacementEvents(v_P, eventHere.subLoc1);
            case CostModel.FAILURE_TO_DIVERGE:
            case CostModel.INFESTATION:
            case CostModel.LOSS:
                return eventHere.eventLoc;
        }
        return null;
    }

    /* Construct a string that describes the optimal cophylogenetic association
     * for placing e_P or v_P on l_H. This solution may not be the same
     * as the one displayed in the solution viewer window, especially if the
     * user has moved ParasiteNodes to suboptimal locations. 
     */
    public String associationString(int v_P, HostLocation l_H) {
        String s = "parasite node " + v_P + " on " + l_H + " : ";
        // obtain event information
        EventInfo event = getOptParasiteEventInfo(v_P, l_H);
        // check for impossible construction case
        if (l_H == null || costModel.isInfinity(event.cost)) {
            return s + "invalid association\n";
        }
        s += "event type " + CostModel.eventTypeName(event.eventType) + " with total cost " + event.cost + "\n";
        if (!parasiteTree.isTip(v_P)) {
            // if not tip, then recursive to find about about children's embedding
            HostLocationPair childrenLocation = getOptChildrenPlacement(v_P, l_H);
            s += associationString(parasiteTree.node[v_P].Lchild, childrenLocation.l);
            s += associationString(parasiteTree.node[v_P].Rchild, childrenLocation.r);
        }
        return s;
    }
    
    /* keep a package of information about a specific event */
    public static class EventInfo {

        public int eventType;          // see code above
        int cost;               // cost for that event and its subtree
        public EventInfo subtree1;
        public EventInfo subtree2;
        public HostLocation subLoc1;
        public HostLocation subLoc2;
        int e_PTip = -1;        // parasite tip to be placed when event is FTD
        public HostLocation eventLoc;
        public int hostNode;
        public int numSolns;
        public LightLabel supportLabel;

        // CONSTRUCTOR
        public EventInfo(int eventType, int cost, HostLocation subLoc1, HostLocation subLoc2, HostLocation eventLoc, int hostNode) {
            this.eventType = eventType;
            this.cost = cost;
            this.subLoc1 = subLoc1;
            this.subLoc2 = subLoc2;
            this.eventLoc = eventLoc;
            this.hostNode = hostNode;          
        }

        // FAILURE TO DIVERGE CONSTRUCTOR
        public EventInfo(int eventType, int cost, HostLocation subLoc1, HostLocation subLoc2, HostLocation eventLoc, int e_PTip, boolean isFTD) {
            assert(eventType == CostModel.FAILURE_TO_DIVERGE);
            this.eventType = eventType;
            this.cost = cost;
            this.subLoc1 = subLoc1;
            this.subLoc2 = subLoc2;
            this.eventLoc = eventLoc;
            this.e_PTip = e_PTip;
        }
        
        public boolean equals(Object otherEvent) {
            EventInfo other = (EventInfo) otherEvent;
            
            if (eventType == CostModel.LOSS) {
                return this.eventType == other.eventType &&
                       this.hostNode == other.hostNode && // same host node
                       this.subLoc1.nodeID == other.subLoc1.nodeID; // same loss direction
            } else if (eventType == CostModel.HOST_SWITCH) {
                return this.eventType == other.eventType &&
                       this.eventLoc.nodeID == other.eventLoc.nodeID &&
                       this.subLoc1.nodeID == other.subLoc1.nodeID &&
                       this.subLoc2.nodeID == other.subLoc2.nodeID &&
                       this.hostNode == other.hostNode; // same node in orig parasite tree
            } else if (eventType == CostModel.DUPLICATION) {
                return this.eventType == other.eventType &&
                       this.subLoc1.nodeID == other.subLoc1.nodeID &&
                       this.subLoc2.nodeID == other.subLoc2.nodeID &&
                       this.hostNode == other.hostNode; // same node in orig parasite tree
            } else if (eventType == CostModel.FAILURE_TO_DIVERGE) {
                return this.eventType == other.eventType &&
                       this.hostNode == other.hostNode;
            } else if (eventType == CostModel.COSPECIATION) {
                return this.eventType == other.eventType &&
                       this.subLoc1.nodeID == other.subLoc1.nodeID &&
                       this.subLoc2.nodeID == other.subLoc2.nodeID &&
                       this.hostNode == other.hostNode;
            } else if (eventType == CostModel.INFESTATION) {
                return this.eventType == other.eventType &&
                       this.eventLoc.nodeID == other.eventLoc.nodeID &&
                       this.subLoc1.nodeID == other.subLoc1.nodeID &&
                       this.subLoc2.nodeID == other.subLoc2.nodeID &&
                       this.hostNode == other.hostNode;
            }
            return this.eventType == other.eventType &&
                   this.hostNode == other.hostNode;
        }
        
        @Override
        public String toString() {
            return "[Event Type: " + CostModel.eventTypeName(eventType) + 
                    " Location: " + eventLoc + 
                    //" subLoc1 = " + subLoc1 + 
                    //" subLoc2 = " + subLoc2 + 
                    //" hostNode = " + hostNode +
                    //" numSolns = " + supportLevel + 
                    //" Subtree 1: "+ subtree1 + 
                    //" Subtree 2: "+ subtree2 + 
                    //" Parent Event = " + parentEvent + 
                    "]";
        }
    }
}
