/*
 * This is a new ArrayDP class designed to support the new SolutionViewer and
 * produces SolutionViewerInfo.
 * 
 * TODO: Add randomization changes to all of the infestation version functions
 */
package edu.hmc.jane.solving;

import edu.hmc.jane.CostModel;
import edu.hmc.jane.Phi;
import edu.hmc.jane.RegionedCostModel;
import edu.hmc.jane.SimpleCostModel;
import edu.hmc.jane.TimeZones;
import edu.hmc.jane.Tree;
import edu.hmc.jane.TreeSpecs;
import edu.hmc.jane.util.SortedIntegerSet;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class ArrayDP3 {

    // options and variables
    int MODE;   // computation mode
    // 0 = 2D cost only, 1 = 2D with event count, 2 = 3D reconstructable
    static final int COST_ONLY = 0;
    static final int EVENT_COUNT = 1;
    static final int RECONSTRUCTABLE = 2;
    // used in random case (for support values).  
    static final int LOSS_LEFT = 0;
    static final int LOSS_RIGHT = 1;
    static final int COSPECIATION_TRANS = 2;
    static final int COSPECIATION_CIS = 3;
    static final int FTD_PRESERVE_LEFT = 4;
    static final int FTD_PRESERVE_RIGHT = 5;
    static final int DUPLICATION = 6;
    static final int NO_HOST = 7;
    static final int NOTHING_HAPPENS = 8;
    static final int HOSTSWITCH_LEFT = 9;
    static final int HOSTSWITCH_RIGHT = 10;
    static final int INFESTATION_PRESERVE_LEFT = 11;
    static final int INFESTATION_PRESERVE_RIGHT = 12;
    static final int ROOT = 13;
    
    // Problem information
    Tree hostTree, parasiteTree;
    TreeSpecs hostTiming;
    Phi phi;
    TimeZones timeZones;
    boolean[] hostAlive;                      // indicates whether host is currently alive
    SortedIntegerSet[] possibleHostEdges;     //possibleHostEdges[e_P] is a SortedIntegerSet
    //of every host edge that e_P could be put on
    //at the current time (it may also
    //include some it could not be put on)
    LinkedList<Integer>[] needsFTD;     // needsFTD[e_H] is a LinkedList of every
                                        // parasite tip needing to undergo a
                                        // failure to diverge event at e_H
    LinkedList<Integer>[] ftdPositions; // ftdPositions[e_PTip] is a LinkedList of
                                        // every host node where e_PTip needs to
                                        // fail to diverge.
    int[] earliestDescendantFTD;    // earliestDescendantFTD[e_P] stores the earliest time of a failure 
                                    // to diverge event undergone by a descendant of e_P.
    int earliestFTD;                // Time of the first failure to diverge.
    
    // DP tables
    DPTable table;
    HostSwitchSelector hostSwitchSelector;
    InfestationSelector infestationSelector;
    int currentBeforeTime, currentAfterTime;
    private boolean allowHostSwitch = true;
    private boolean allowFTD = true;
    private boolean allowInfest = false;
    
    /*
     * determines whether we should solve this deterministically. If this is
     * true, we will randomly choose between equally cost methods.
     */
    public boolean randomizeEvents = false;
    int numBeforeRootPlacements = 0; // number of equal cost solutions found
    int numAfterRootPlacements = 0;  // number of equal cost solutions found
    
    long numRootPlacements = 0;
    
    HashMap<Integer, MultihostConfiguration[][]> multihostConfigurations;
    CostModel costModel;
    public final int INFINITY = 999999999;     //if you change this value, make
                                               //sure to update the validation code
                                               //in EditCosts.java and CLI.java that
                                               //reports when the users cost choices
                                               //are too large

    /* CONSTRUCTOR */
    public ArrayDP3(TreeSpecs hostTiming, Phi phi, TimeZones timeZones, int mode, CostModel costModel) {
        this.hostTree = hostTiming.hostTree;
        this.parasiteTree = hostTiming.parasiteTree;
        this.hostTiming = hostTiming;
        Phi correctedPhi = phi.newWithoutPolytomies(parasiteTree.size - phi.length());
        this.phi = correctedPhi;
        TimeZones newZones = timeZones.newWithoutPolytomies(hostTree.size - timeZones.getHostSize(),
                                                            parasiteTree.size - timeZones.getParaSize(),
                                                            hostTree, parasiteTree);
        this.timeZones = newZones;
        this.MODE = mode;
        this.costModel = costModel;
        if (this.costModel.getHostSwitchCost() >= INFINITY) {
            this.allowHostSwitch = false;
        } else {
            this.allowHostSwitch = true;
        }
        if (this.costModel.getFailureToDivergeCost() >= INFINITY) {
            this.allowFTD = false;
        } else {
            this.allowFTD = true;
        }
        if (this.costModel.getInfestationCost() >= INFINITY) {
            this.allowInfest = false;
        } else {
            this.allowInfest = true;
        }      
    }

    /* Try to update bestCost from before table. The parasite root edge must
     * start in time for the first failure to diverge event.
     */
    private void updateBeforeBestCostInfo() {
        int e_P = parasiteTree.root;
        if (this.hasMultihostOnlyFTD()) {
            for (int e_H = 0; e_H < hostTree.size; e_H++) {
                if (hostTiming.timeOfNode(e_H) < earliestFTD)
                    table.candidateBeforeBestCostInfo(e_P, e_H);
            }
        } else {
            for (int e_H = 0; e_H < hostTree.size; e_H++)
                table.candidateBeforeBestCostInfo(e_P, e_H);
        }
    }

    /* Try to update bestCost from after table. The parasite root edge must
     * start in time for the first failure to diverge event.
     */
    private void updateAfterBestCostInfo() {
        int e_P = parasiteTree.root;
        if (this.hasMultihostOnlyFTD()) {
            for (int e_H = 0; e_H < hostTree.size; e_H++) {
                if (hostTiming.timeOfNode(e_H) <= earliestFTD)
                    table.candidateAfterBestCostInfo(e_P, e_H);
            }
        } else {
            for (int e_H = 0; e_H < hostTree.size; e_H++)
                table.candidateAfterBestCostInfo(e_P, e_H);
        }
        // FTDD: add support for best solution to be a root tip
    }

    public boolean coinFlipped(int e_P, int e_H, boolean before, int eventType) {
        // gives a 1/numEqualSolutions chance
        long[][] solTable = (before ? table.beforeNumBestSolutions : table.afterNumBestSolutions);        
        Random r = new Random();
        long numNewSolutions = incrementSolutions(e_P, e_H, before, eventType);
        if (eventType == ROOT) {
            return myNextLong(r, this.numRootPlacements) < numNewSolutions;
        }
        return myNextLong(r, solTable[e_P][e_H]) < numNewSolutions;
    }

    
    
    public long myNextLong(Random r, long range) {
        double percent = r.nextDouble();
        long choice = (long)(range * percent);
        return choice;
    }
    /* FTDD: only valid without infestations
     * returns the number of solutions in the most recent event 
     */
    public long incrementSolutions(int e_P, int e_H, boolean before, int eventType) {
        long newSolutions = 0; // this value should never be used
        // before events
        if (eventType == LOSS_LEFT) {
            newSolutions = table.afterNumBestSolutions[e_P][hostTree.node[e_H].Lchild];
            table.beforeNumBestSolutions[e_P][e_H] += newSolutions;
        } else if (eventType == LOSS_RIGHT) {
            newSolutions = table.afterNumBestSolutions[e_P][hostTree.node[e_H].Rchild];
            table.beforeNumBestSolutions[e_P][e_H] += newSolutions;
        } else if (eventType == COSPECIATION_CIS) {
            newSolutions = table.afterNumBestSolutions[parasiteTree.node[e_P].Lchild][hostTree.node[e_H].Lchild]
                         * table.afterNumBestSolutions[parasiteTree.node[e_P].Rchild][hostTree.node[e_H].Rchild];
            table.beforeNumBestSolutions[e_P][e_H] += newSolutions;
        } else if (eventType == COSPECIATION_TRANS) {
            newSolutions = table.afterNumBestSolutions[parasiteTree.node[e_P].Lchild][hostTree.node[e_H].Rchild]
                         * table.afterNumBestSolutions[parasiteTree.node[e_P].Rchild][hostTree.node[e_H].Lchild];
            table.beforeNumBestSolutions[e_P][e_H] += newSolutions;
        } else if (eventType == FTD_PRESERVE_LEFT) {
            newSolutions = table.afterNumBestSolutions[e_P][hostTree.node[e_H].Rchild];
            // we don't need to add the possibilities for the tip because there can only be one possibility
            // (assuming no host switch or hs-without-duplication)
            table.beforeNumBestSolutions[e_P][e_H] += newSolutions;
        } else if (eventType == FTD_PRESERVE_RIGHT) {
            newSolutions = table.afterNumBestSolutions[e_P][hostTree.node[e_H].Lchild];
            // we don't need to add the possibilities for the tip because there can only be one possibility
            // (assuming no host switch or hs-without-duplication)
            table.beforeNumBestSolutions[e_P][e_H] += newSolutions;
        } else if (eventType == NO_HOST) {
            newSolutions = table.afterNumBestSolutions[e_P][e_H];
            table.beforeNumBestSolutions[e_P][e_H] += newSolutions;
        } else if (eventType == NOTHING_HAPPENS) { // AFTER EVENTS
            newSolutions = table.beforeNumBestSolutions[e_P][e_H];
            table.afterNumBestSolutions[e_P][e_H] += newSolutions;
        } else if (eventType == DUPLICATION) {
            newSolutions = table.afterNumBestSolutions[parasiteTree.node[e_P].Lchild][e_H]
                         * table.afterNumBestSolutions[parasiteTree.node[e_P].Rchild][e_H];
            table.afterNumBestSolutions[e_P][e_H] += newSolutions;
        } else if (eventType == HOSTSWITCH_LEFT) {
            newSolutions = 0;
            if (costModel instanceof SimpleCostModel) {
                for (Integer e_Switch: ((UniformCostReconstructableHSS)hostSwitchSelector).getBestSwitchLocations(currentAfterTime, parasiteTree.node[e_P].Lchild, e_H)) {                    
                    newSolutions += (table.afterNumBestSolutions[parasiteTree.node[e_P].Rchild][e_H]
                                  * table.afterNumBestSolutions[parasiteTree.node[e_P].Lchild][e_Switch]);
                }
                table.afterNumBestSolutions[e_P][e_H] += newSolutions;
            } else if (costModel instanceof RegionedCostModel) {
                for (Integer e_Switch: ((RegionedReconstructableHSS)hostSwitchSelector).getBestSwitchLocations(currentAfterTime, parasiteTree.node[e_P].Lchild, e_H)) {
                    newSolutions += (table.afterNumBestSolutions[parasiteTree.node[e_P].Rchild][e_H]
                                  * table.afterNumBestSolutions[parasiteTree.node[e_P].Lchild][e_Switch]);
                }
                table.afterNumBestSolutions[e_P][e_H] += newSolutions;
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (eventType == HOSTSWITCH_RIGHT) {
            newSolutions = 0;
            if (costModel instanceof SimpleCostModel) {
                
                for (Integer e_Switch: ((UniformCostReconstructableHSS)hostSwitchSelector).getBestSwitchLocations(currentAfterTime, parasiteTree.node[e_P].Rchild, e_H)) {   
                    newSolutions += (table.afterNumBestSolutions[parasiteTree.node[e_P].Lchild][e_H]
                                  * table.afterNumBestSolutions[parasiteTree.node[e_P].Rchild][e_Switch]);
                }   
                table.afterNumBestSolutions[e_P][e_H] += newSolutions;
            } else if (costModel instanceof RegionedCostModel) {
                for (Integer e_Switch: ((RegionedReconstructableHSS)hostSwitchSelector).getBestSwitchLocations(currentAfterTime, parasiteTree.node[e_P].Rchild, e_H)) {
                    newSolutions += (table.afterNumBestSolutions[parasiteTree.node[e_P].Lchild][e_H]
                                  * table.afterNumBestSolutions[parasiteTree.node[e_P].Rchild][e_Switch]);
                }
                table.afterNumBestSolutions[e_P][e_H] += newSolutions;
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (eventType == INFESTATION_PRESERVE_LEFT) {
            newSolutions = 0;
            table.beforeNumBestSolutions[e_P][e_H] += newSolutions;
        } else if (eventType == INFESTATION_PRESERVE_RIGHT) {
            newSolutions = 0;
            table.beforeNumBestSolutions[e_P][e_H] += newSolutions;
        } else if (eventType == ROOT && before) {
            newSolutions = table.beforeNumBestSolutions[e_P][e_H];
            this.numRootPlacements += newSolutions;
        } else if (eventType == ROOT && !before) {
            newSolutions = table.afterNumBestSolutions[e_P][e_H];
            this.numRootPlacements += newSolutions;
        } else {
            System.out.println("Error! This is a bug with the code for randomizing.");
        }
        return newSolutions;
    }
    
    /* accept a candidate for before table, update the table if necessary. */
    private void beforeCandidate(int e_P, int e_H, long costInfo, int eventType) {
        int newCost = table.toRelativeCost(costInfo);
        if (table.toRelativeCost(table.getBeforeCostInfo(e_P, e_H)) > newCost) {
            table.setBeforeCostInfo(e_P, e_H, costInfo);
            infestationSelector.updateBestInfestation(currentBeforeTime, e_P, e_H, newCost);
            if (this.randomizeEvents) {
                //Update the number of solutions with this mapping
                ((ThreeDimDPTable)table).setEvent(currentBeforeTime, e_P, e_H, eventType);
                table.beforeNumBestSolutions[e_P][e_H] = 0;
                incrementSolutions(e_P, e_H, true, eventType);
            }
        } else if (this.randomizeEvents
                    && table.toRelativeCost(table.getBeforeCostInfo(e_P, e_H)) == newCost
                    && newCost < INFINITY
                    && coinFlipped(e_P, e_H, true, eventType)) {
            table.setBeforeCostInfo(e_P, e_H, costInfo);
            infestationSelector.updateBestInfestation(currentBeforeTime, e_P, e_H, newCost);
            ((ThreeDimDPTable)table).setEvent(currentBeforeTime, e_P, e_H, eventType);
        }
    }

    /* accept a candidate for before table, update the table if necessary.
     * ASSUMPTION: Multihost Parasite
     */
    private void beforeCandidate(int e_P, int name, Set<Integer> tips, int e_H, long costInfo, int eventType) {
        int newCost = table.toRelativeCost(costInfo, name, tips);
        if (table.toRelativeCost(table.getBeforeCostInfo(e_P, name, tips, e_H), name, tips) > newCost) {
            table.setBeforeCostInfo(e_P, name, tips, e_H, costInfo);
            infestationSelector.updateBestInfestation(currentBeforeTime, e_P, name, tips, e_H, newCost);
            if (this.randomizeEvents)
                table.beforeNumBestSolutions[e_P][e_H] = 1;
        } else if (this.randomizeEvents
                    && table.toRelativeCost(table.getBeforeCostInfo(e_P, name, tips, e_H), name, tips) == newCost
                    && newCost < INFINITY
                    && coinFlipped(e_P, e_H, true, eventType)) {
            table.setBeforeCostInfo(e_P, name, tips, e_H, costInfo);
            infestationSelector.updateBestInfestation(currentBeforeTime, e_P, name, tips, e_H, newCost);
        }
    }

    /* accept a candidate for after table, update the table if necessary */
    private void afterCandidate(int e_P, int e_H, long costInfo, int eventType) {
        int newCost = table.toRelativeCost(costInfo);
        if (table.toRelativeCost(table.getAfterCostInfo(e_P, e_H)) > newCost) {
            table.setAfterCostInfo(e_P, e_H, costInfo);
            hostSwitchSelector.updateBestSwitch(currentAfterTime, e_P, e_H, newCost);
            if (this.randomizeEvents) {
                //Update the number of solutions with this mapping
                ((ThreeDimDPTable)table).setEvent(currentAfterTime, e_P, e_H, eventType);
                table.afterNumBestSolutions[e_P][e_H] = 0;
                incrementSolutions(e_P, e_H, false, eventType);
            }
        } else if (this.randomizeEvents
                    && table.toRelativeCost(table.getAfterCostInfo(e_P, e_H)) == newCost
                    && newCost < INFINITY
                    && coinFlipped(e_P, e_H, false, eventType)) {
            table.setAfterCostInfo(e_P, e_H, costInfo);
            hostSwitchSelector.updateBestSwitch(currentAfterTime, e_P, e_H, newCost);
            ((ThreeDimDPTable)table).setEvent(currentAfterTime, e_P, e_H, eventType);
        }
    }
    
    /* accept a candidate for before table, update the table if necessary.
     * ASSUMPTION: Multihost Parasite
     */
    private void afterCandidate(int e_P, int name, Set<Integer> tips, int e_H, long costInfo, int eventType) {
        int newCost = table.toRelativeCost(costInfo, name, tips);
        if (table.toRelativeCost(table.getAfterCostInfo(e_P, name, tips, e_H), name, tips) > newCost) {
            table.setAfterCostInfo(e_P, name, tips, e_H, costInfo);
            hostSwitchSelector.updateBestSwitch(currentAfterTime, e_P, name, tips, e_H, newCost);
            if (this.randomizeEvents) {
                table.afterNumBestSolutions[e_P][e_H] = 1;
            }
        } else if (this.randomizeEvents
                    && table.toRelativeCost(table.getAfterCostInfo(e_P, name, tips, e_H), name, tips) == newCost
                    && newCost < INFINITY
                    && coinFlipped(e_P, e_H, false, eventType)) {
            table.setAfterCostInfo(e_P, name, tips, e_H, costInfo);
            hostSwitchSelector.updateBestSwitch(currentAfterTime, e_P, name, tips, e_H, newCost);
        }
    }

    /* prepare for the DP process */
    private void initialize() {
        // construct tables
        hostAlive = new boolean[hostTree.size];
        switch (MODE) {
            case COST_ONLY:
                table = new TwoDimDPTable(this);
                break;
            case EVENT_COUNT:
                table = new EventCounterDPTable(this);
                break;
            case RECONSTRUCTABLE:
                table = new ThreeDimDPTable(this);
                break;
            default:
                throw new UnsupportedOperationException();
        }

        if (MODE == RECONSTRUCTABLE) {
            if (costModel instanceof SimpleCostModel) {
                hostSwitchSelector = new UniformCostReconstructableHSS(this);
                infestationSelector = new UniformCostReconstructableIFS(this);
            } else if (costModel instanceof RegionedCostModel) {
                hostSwitchSelector = new RegionedReconstructableHSS(this);
                infestationSelector = new RegionedReconstructableIFS(this);
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            if (costModel instanceof SimpleCostModel) {
                hostSwitchSelector = new UniformCostNonReconstructableHSS(this);
                infestationSelector = new UniformCostNonReconstructableIFS(this);
            } else if (costModel instanceof RegionedCostModel) {
                hostSwitchSelector = new RegionedNonReconstructableHSS(this);
                infestationSelector = new RegionedNonReconstructableIFS(this);
            } else {
                throw new UnsupportedOperationException();
            }
        }

        // associate tips
        for (int e_P = 0; e_P < parasiteTree.size; e_P++) {
            if (phi.hasAHost(e_P)) {
                if (phi.hasMultihostParasites())
                    table.zeroBefore(e_P, phi.getHosts(e_P));
                else
                    table.zeroBefore(e_P, phi.getHost(e_P));
            }
        }

        // set initial host statusus
        for (int i = 0; i < hostTree.size; i++) {
            if (hostTree.isTip(i))
                hostAlive[i] = true;
            else
                hostAlive[i] = false;
        }

        // Compute where failure to diverge events occur, if we are using that
        // model.
        if (this.isOnlyFTDMode())
            computeFTDLocations();

        // initialize possible host edges
        possibleHostEdges = new SortedIntegerSet[parasiteTree.size];

        // only COST_ONLY and EVENT_COUNT mode are supported with the above
        // feature at present
        if (MODE == COST_ONLY || MODE == EVENT_COUNT) {

            //base case: For each tip in the parsite tree, make
            //possibleHostEdges[tip]
            //equal to a list containing any host edges the tip is associated
            //with. For non-tip edges, the list will start empty.
            for (int e_P = 0; e_P < parasiteTree.size; e_P++) {
                if (phi.hasMultihostParasites())
                    possibleHostEdges[e_P] = new SortedIntegerSet(phi.getHosts(e_P));
                else
                    possibleHostEdges[e_P] = new SortedIntegerSet(phi.getHost(e_P));
            }

            //recursive step: each e_P can go anywhere its children can go at the
            //given time
            for (int i = 0; i < parasiteTree.size; i++) {
                int e_P = parasiteTree.postOrder[i];
                updatePossibleHostEdges(e_P);
            }
        } else {
            SortedIntegerSet allPossibilities = new SortedIntegerSet(0, hostTree.size);
            for (int e_P = 0; e_P < parasiteTree.size; e_P++)
                possibleHostEdges[e_P] = allPossibilities;
        }
    }

    /* Compute the locations on the host tree where a parasite must undergo a failure to diverge event.
     * This function also fills in earliestDescendantFtd.
     */
    private void computeFTDLocations() {
        if (!phi.hasMultihostParasites()) {
            needsFTD = null;
            ftdPositions = null;
            earliestDescendantFTD = null;
            earliestFTD = hostTiming.tipTime;
            return;
        } else {
            // infectsDescendant[e_H][e_P] == true iff e_P infects a host that is
            // a descendant of e_H.
            // Recall that Java initializes booleans to false by default.
            boolean infectsDescendant[][] = new boolean[hostTree.size][parasiteTree.size];

            needsFTD = new LinkedList[hostTree.size];
            ftdPositions = new LinkedList[parasiteTree.size];

            earliestDescendantFTD = new int[parasiteTree.size];
            Arrays.fill(earliestDescendantFTD, hostTiming.tipTime);

            // Base case: compute infectsDescendant for all host tips by running
            // through all parasite tips.
            for (int e_P : parasiteTree.getTips()) {
                ftdPositions[e_P] = new LinkedList<Integer>();
                for (int e_H : phi.getHosts(e_P))
                    infectsDescendant[e_H][e_P] = true;
            }

            // Recursive step: Compute the union of the child arrays.
            for (int i = 0; i < hostTree.size; ++i) {
                int e_H = hostTree.postOrder[i];
                needsFTD[e_H] = new LinkedList<Integer>();

                // Host tips were already computed.
                if (!hostTree.isTip(e_H)) {
                    for (int e_P : parasiteTree.getTips()) {
                        boolean child1 = infectsDescendant[hostTree.node[e_H].Lchild][e_P];
                        boolean child2 = infectsDescendant[hostTree.node[e_H].Rchild][e_P];
                        infectsDescendant[e_H][e_P] = child1 || child2;
                        if (child1 && child2) {
                            needsFTD[e_H].addLast(e_P);
                            ftdPositions[e_P].addLast(e_H);
                            candidateEarliestDescendantFTD(e_P, hostTiming.timeOfNode(e_H));
                        }
                    }
                }
            }

            earliestFTD = earliestDescendantFTD[parasiteTree.root];
        }
    }

    /* Recursive helper for computeFtdLocations to fill earliestDescendantFtd.
     * Tries all ancestors of e_P as having a (possibly earliest) descendant
     * needing a failure to diverge at time t.
     * ASSUMPTION: The value stored in earliestDescendantFtd[e_P] is not less than that of e_P's parent.
     */
    private void candidateEarliestDescendantFTD(int e_P, int t) {
        if (t < earliestDescendantFTD[e_P]) {
            earliestDescendantFTD[e_P] = t;
            if (e_P != parasiteTree.root) {
                candidateEarliestDescendantFTD(parasiteTree.node[e_P].parent, t);
            }
        }
    }

    /* compute cost for a loss event where parasite goes on left host children
     * ASSUMPTION: e_H ends at given time
     */
    private long getLeftLossCostInfo(int e_P, int e_H) {
        return table.addEventToCostInfo(CostModel.LOSS,
                                        table.getAfterCostInfo(e_P, hostTree.node[e_H].Lchild));
    }

    private long getLeftLossCostInfo(int e_P, int name, Set<Integer> tips, int e_H) {
        return table.addEventToCostInfo(CostModel.LOSS,
                                        table.getAfterCostInfo(e_P, name, tips, hostTree.node[e_H].Lchild));
    }

    /* compute cost for a loss event where parasite goes on right host children
     * ASSUMPTION: e_H ends at given time
     */
    private long getRightLossCostInfo(int e_P, int e_H) {
        return table.addEventToCostInfo(CostModel.LOSS,
                                        table.getAfterCostInfo(e_P, hostTree.node[e_H].Rchild));
    }

    private long getRightLossCostInfo(int e_P, int name, Set<Integer> tips, int e_H) {
        return table.addEventToCostInfo(CostModel.LOSS,
                                        table.getAfterCostInfo(e_P, name, tips, hostTree.node[e_H].Rchild));
    }

    /* compute cost for a cospeciation event where each branch of host and
     * parasite goes in the same direction
     * ASSUMPTION: e_H ends at given time
     * ASSUMPTION: both e_P and e_H are not tip edges
     */
    private long getCisCospeciationCostInfo(int e_P, int e_H) {
        return table.addEventToCostInfo(CostModel.COSPECIATION,
                table.addCostInfo(   
                table.getAfterCostInfo(parasiteTree.node[e_P].Lchild, hostTree.node[e_H].Lchild),
                table.getAfterCostInfo(parasiteTree.node[e_P].Rchild, hostTree.node[e_H].Rchild)));
    }

    private long getCisCospeciationCostInfo(int e_P, int name, Set<Integer> tips, int e_H) {
        if (isPossiblyTheSame(name, parasiteTree.node[e_P].Lchild)) {
            return table.addEventToCostInfo(CostModel.COSPECIATION,
                    table.addCostInfo(
                    table.getAfterCostInfo(parasiteTree.node[e_P].Lchild, name, tips, hostTree.node[e_H].Lchild),
                    table.getAfterCostInfo(parasiteTree.node[e_P].Rchild, hostTree.node[e_H].Rchild)));
        } else {
            return table.addEventToCostInfo(CostModel.COSPECIATION,
                    table.addCostInfo(
                    table.getAfterCostInfo(parasiteTree.node[e_P].Lchild, hostTree.node[e_H].Lchild),
                    table.getAfterCostInfo(parasiteTree.node[e_P].Rchild, name, tips, hostTree.node[e_H].Rchild)));
        }
    }

    /* compute cost for a cospeciation event where each branch of host and
     * parasite goes in the opposite direction
     * ASSUMPTION: e_H ends at given time
     * ASSUMPTION: both e_P and e_H are not tip edges
     */
    private long getTransCospeciationCostInfo(int e_P, int e_H) {
        return table.addEventToCostInfo(CostModel.COSPECIATION,
                table.addCostInfo(
                table.getAfterCostInfo(parasiteTree.node[e_P].Lchild, hostTree.node[e_H].Rchild),
                table.getAfterCostInfo(parasiteTree.node[e_P].Rchild, hostTree.node[e_H].Lchild)));
    }

    private long getTransCospeciationCostInfo(int e_P, int name, Set<Integer> tips, int e_H) {
        // find which side is the same species as e_p
        if (isPossiblyTheSame(name, parasiteTree.node[e_P].Lchild)) {
            return table.addEventToCostInfo(CostModel.COSPECIATION,
                    table.addCostInfo(
                    table.getAfterCostInfo(parasiteTree.node[e_P].Lchild, name, tips, hostTree.node[e_H].Rchild),
                    table.getAfterCostInfo(parasiteTree.node[e_P].Rchild, hostTree.node[e_H].Lchild)));
        } else {
            return table.addEventToCostInfo(CostModel.COSPECIATION,
                    table.addCostInfo(
                    table.getAfterCostInfo(parasiteTree.node[e_P].Lchild, hostTree.node[e_H].Rchild),
                    table.getAfterCostInfo(parasiteTree.node[e_P].Rchild, name, tips, hostTree.node[e_H].Lchild)));
        }
    }
    
    /* compute cost for a failure to diverge event where the left branch of
     * the host tree gets the tip edge.  
     * ASSUMPTION: We only allow FTD, not infestation
     * ASSUMPTION: e_H ends at given time
     * ASSUMPTION: e_H is not a tip edge
     * ASSUMPTION: e_PTip is the tip edge corresponding to the unique descendant
     *             of e_P that infects a host in each of e_H's subtrees.
     */
    private long getPreserveLeftFailureToDivergeOnlyCostInfo(int e_P, int e_H, int e_PTip) {
        return table.addEventToCostInfo(CostModel.FAILURE_TO_DIVERGE,
                table.addCostInfo(
                table.getAfterCostInfo(e_PTip, hostTree.node[e_H].Lchild),
                table.getAfterCostInfo(e_P, hostTree.node[e_H].Rchild)));
    }

    /* compute cost for a ftd event where the left branch of the host tree gets 
     * the tip edge
     * ASSUMPTION: e_H ends at given time
     * ASSUMPTION: both e_P and e_H are not tip edges
     */
    private long getPreserveLeftFailureToDivergeCostInfo(int e_P, int name, Set<Integer> left, Set<Integer> right, int e_H) {
        return table.addEventToCostInfo(CostModel.FAILURE_TO_DIVERGE,
                table.addCostInfo(
                table.getAfterCostInfo(name, name, left, hostTree.node[e_H].Lchild),
                table.getAfterCostInfo(e_P, name, right, hostTree.node[e_H].Rchild)));
    }
    
    /* compute cost for a failure to diverge event where the right branch of
     * the host tree gets the tip edge.
     * ASSUMPTION: We only allow FTD, not infestation
     * ASSUMPTION: e_H ends at given time
     * ASSUMPTION: e_H is not a tip edge
     * ASSUMPTION: e_PTip is the tip edge corresponding to the unique descendant
     *             of e_P that infects a host in each of e_H's subtrees.
     */
    private long getPreserveRightFailureToDivergeOnlyCostInfo(int e_P, int e_H, int e_PTip) {
        return table.addEventToCostInfo(CostModel.FAILURE_TO_DIVERGE,
                table.addCostInfo(
                table.getAfterCostInfo(e_P, hostTree.node[e_H].Lchild),
                table.getAfterCostInfo(e_PTip, hostTree.node[e_H].Rchild)));
    }

    /* compute cost for a ftd event where the right branch of the host tree gets 
     * the tip edge
     * ASSUMPTION: e_H ends at given time
     * ASSUMPTION: both e_P and e_H are not tip edges 
     */
    private long getPreserveRightFailureToDivergeCostInfo(int e_P, int name, Set<Integer> left, Set<Integer> right, int e_H) {
        return table.addEventToCostInfo(CostModel.FAILURE_TO_DIVERGE,
                table.addCostInfo(
                table.getAfterCostInfo(e_P, name, left, hostTree.node[e_H].Lchild),
                table.getAfterCostInfo(name, name, right, hostTree.node[e_H].Rchild)));
    }
    
    
    /* simply return cost for nothing happens since e_H does not end at
     * the given time
     * ASSUMPTION: e_H is alive before the given time 
     */
    private long getNoHostNodeThereCostInfo(int e_P, int e_H) {
        return table.getAfterCostInfo(e_P, e_H);
    }

    private long getNoHostNodeThereCostInfo(int e_P, int name, Set<Integer> tips, int e_H) {
        return table.getAfterCostInfo(e_P, name, tips, e_H);
    }
    
    /* compute cost for infestation where the right child of e_P infests
     * ASSUPMTION: e_P is not a tip edge
     */
    private long getLeftInfestationCostInfo(int e_P, int name, Set<Integer> left, Set<Integer> right, int e_H) {
        // Check if infestation is prevented due to prior ftd events.
        if (this.hasMultihostOnlyFTD() && earliestDescendantFTD[parasiteTree.node[e_P].Lchild] <= currentAfterTime)
            return table.INFINITY_INFO;    
        int infestLocation = infestationSelector.findBestInfestationLocation(currentAfterTime, parasiteTree.node[e_P].Lchild, e_H);
        if (infestLocation == -1)
            return table.INFINITY_INFO;
        if (possibleNames(parasiteTree.node[e_P].Rchild).isEmpty() || possibleNames(parasiteTree.node[e_P].Lchild).isEmpty())
            return table.INFINITY_INFO;
        return table.addInfestationEventToCostInfo(
                table.addCostInfo(table.getAfterCostInfo(parasiteTree.node[e_P].Rchild, name, left, e_H),
                                  table.getAfterCostInfo(parasiteTree.node[e_P].Lchild, name, right, infestLocation)),
                e_H, infestLocation);
    }
    
    /* compute cost for infestation where the right child of e_P infests
     * ASSUPMTION: e_P is not a tip edge
     */
    private long getRightInfestationCostInfo(int e_P, int name, Set<Integer> left, Set<Integer> right, int e_H) {
        // Check if infestation is prevented due to prior ftd events.
        if (this.hasMultihostOnlyFTD() && earliestDescendantFTD[parasiteTree.node[e_P].Rchild] <= currentAfterTime)
            return table.INFINITY_INFO;
        int infestLocation = infestationSelector.findBestInfestationLocation(currentAfterTime, parasiteTree.node[e_P].Rchild, e_H);
        if (infestLocation == -1)
            return table.INFINITY_INFO;
        if (possibleNames(parasiteTree.node[e_P].Rchild).isEmpty() || possibleNames(parasiteTree.node[e_P].Lchild).isEmpty())
            return table.INFINITY_INFO;
        return table.addInfestationEventToCostInfo(
                table.addCostInfo(table.getAfterCostInfo(parasiteTree.node[e_P].Lchild, name, left, e_H),
                                  table.getAfterCostInfo(parasiteTree.node[e_P].Rchild, name, right, infestLocation)),
                e_H, infestLocation);
    }    

    /* process After(e_P, e_H, time) --> Before(e_P, e_H, time) */
    void afterToBefore() {
        int speciate = hostTiming.nodeAtTime(currentBeforeTime);
        hostAlive[speciate] = true;
        hostAlive[hostTree.node[speciate].Lchild] = false;
        hostAlive[hostTree.node[speciate].Rchild] = false;

        for (int i = 0; i < parasiteTree.size; i++) {
            int e_P = parasiteTree.postOrder[i];
            updatePossibleHostEdges(e_P);

            if (this.isOnlyFTDMode()) {
                // we are in the only-ftd case. (no infestation)

                // See how many parasites (0, 1, or >= 2) descending from e_P need
                // to fail to diverge at speciate. Short-circuit if we find 2
                // or more.
                int parasitesNeedFTD = 0;
                int e_PTip = -1; // A parasite needing to fail to diverge at speciate.
                if (this.hasMultihostOnlyFTD()) {
                    for (int tip : needsFTD[speciate]) {
                        if (parasiteTree.descendant(e_P, tip)) {
                            e_PTip = tip;
                            if (++parasitesNeedFTD >= 2)
                                break;
                        }
                    }
                }

                switch (parasitesNeedFTD) {
                    // No failure to diverge events need to happen. Try other possible events.
                    case 0:
                        // Try LOSS (no need to update associations)
                        if (e_P != parasiteTree.root || phi.hasMultihostParasites()) {                                
                            beforeCandidate(e_P, speciate, getLeftLossCostInfo(e_P, speciate), LOSS_LEFT);  //I suspect there may be some issue here.
                            beforeCandidate(e_P, speciate, getRightLossCostInfo(e_P, speciate), LOSS_RIGHT);
                        }

                        // If parasite node is not a tip and time zones agree, try COSPECIATION.
                        if (!parasiteTree.isTip(e_P) && (timeZones.overlap(e_P, speciate))) {
                            beforeCandidate(e_P, speciate, getCisCospeciationCostInfo(e_P, speciate), COSPECIATION_CIS);
                            beforeCandidate(e_P, speciate, getTransCospeciationCostInfo(e_P, speciate), COSPECIATION_TRANS);
                        }
                        break;

                    // One failure to diverge event needs to happen.
                    case 1:
                        // Ensure that this failure to diverge event respects
                        // earlier ftd events of different parasites.
                        boolean allowFTD = true;
                        if (earliestDescendantFTD[e_P] != hostTiming.timeOfNode(currentBeforeTime)) {
                            if (earliestDescendantFTD[e_P] < earliestDescendantFTD[e_PTip]) {
                                allowFTD = false;
                                break;
                            }
                            for (int par : (List<Integer>) parasiteTree.getDescendants(e_P)) {
                                if ((par != e_PTip) && earliestDescendantFTD[par] < currentBeforeTime) {
                                    allowFTD = false;
                                    break;
                                }
                            }
                        }

                        // Try FAILURE_TO_DIVERGE
                        if (allowFTD) {
                            beforeCandidate(e_P, speciate, getPreserveLeftFailureToDivergeOnlyCostInfo(e_P, speciate, e_PTip), FTD_PRESERVE_LEFT);
                            beforeCandidate(e_P, speciate, getPreserveRightFailureToDivergeOnlyCostInfo(e_P, speciate, e_PTip), FTD_PRESERVE_RIGHT);
                        }
                        break;

                    // More than one failure to diverge event needs to happen.
                    // This cannot be resolved so no candidates are given.
                    default:
                        break;
                }

                // Try NO_HOST_NODE_THERE for alive hosts other than speciate.
                if (e_P != parasiteTree.root || phi.hasMultihostParasites()) {
                    for (int e_H : possibleHostEdges[e_P]) {
                        if (e_H != -1 && hostAlive[e_H] && (e_H != speciate)) {
                            beforeCandidate(e_P, e_H, getNoHostNodeThereCostInfo(e_P, e_H), NO_HOST);
                    }
                }
                }
            } else { // we are allowing infestations!
                if (this.isPossiblyMultihost(e_P)) {
                     // consider every possible name and tips in charge of.

                    MultihostConfiguration config = table.getBeforeConfiguration(e_P, speciate);
                    Set<Integer> possibleNames = config.getPossibleNames();
                    for (int name : possibleNames) {
                        Set<Integer> tips = config.getTips(name);
                        Set<Set<Integer>> possibleSubsets = powerSet(tips);
                        for (Set<Integer> tipSet : possibleSubsets) {
                            if (tipSet.isEmpty())
                                continue;

                            // loss
                            if (e_P != parasiteTree.root || phi.hasMultihostParasites()) {
                                beforeCandidate(e_P, name, tipSet, speciate, getLeftLossCostInfo(e_P, name, tipSet, speciate), LOSS_LEFT);
                                beforeCandidate(e_P, name, tipSet, speciate, getRightLossCostInfo(e_P, name, tipSet, speciate), LOSS_RIGHT);
                            }

                            // cospeciation
                            if (!parasiteTree.isTip(e_P) && (timeZones.overlap(e_P, speciate))) {
                                beforeCandidate(e_P, name, tipSet, speciate, getCisCospeciationCostInfo(e_P, name, tipSet, speciate), COSPECIATION_CIS);
                                beforeCandidate(e_P, name, tipSet, speciate, getTransCospeciationCostInfo(e_P, name, tipSet, speciate), COSPECIATION_TRANS);
                            }

                            // no host there
                            if (e_P != parasiteTree.root || phi.hasMultihostParasites()) {
                                for (int e_H : possibleHostEdges[e_P]) {
                                    if (hostAlive[e_H] && (e_H != speciate))
                                        beforeCandidate(e_P, name, tipSet, e_H, getNoHostNodeThereCostInfo(e_P, name, tipSet, e_H), NO_HOST);
                                }
                            }

                            // failure to diverge
                            Set<Set<Integer>> subsetsOfTipSet = powerSet(tipSet);
                            for (Set<Integer> tipsSet : subsetsOfTipSet) {
                                // make the other set to partition these.
                                Set<Integer> interiorEdgeSet = new HashSet<Integer>(tipSet);
                                interiorEdgeSet.removeAll(tipsSet);

                                /*
                                 * we now have two subsets of this. Check they
                                 * are not empty, and make recursive calls.
                                 */
                                if (tipsSet.isEmpty() || interiorEdgeSet.isEmpty())
                                    continue;

                                beforeCandidate(e_P, name, tipSet, speciate,
                                                getPreserveLeftFailureToDivergeCostInfo(e_P, name, interiorEdgeSet, tipsSet, speciate), FTD_PRESERVE_LEFT);
                                beforeCandidate(e_P, name, tipSet, speciate,
                                                getPreserveRightFailureToDivergeCostInfo(e_P, name, tipsSet, interiorEdgeSet, speciate), FTD_PRESERVE_RIGHT);
                            }
                        }
                    }
                }

                if (e_P != parasiteTree.root || phi.hasMultihostParasites()) {
                    beforeCandidate(e_P, speciate, getLeftLossCostInfo(e_P, speciate), LOSS_LEFT);
                    beforeCandidate(e_P, speciate, getRightLossCostInfo(e_P, speciate), LOSS_RIGHT);
                }

                // If parasite node is not a tip and time zones agree, try COSPECIATION.
                if (!parasiteTree.isTip(e_P) && (timeZones.overlap(e_P, speciate))) {
                    beforeCandidate(e_P, speciate, getCisCospeciationCostInfo(e_P, speciate), COSPECIATION_CIS);
                    beforeCandidate(e_P, speciate, getTransCospeciationCostInfo(e_P, speciate), COSPECIATION_TRANS);
                }

                // Try NO_HOST_NODE_THERE for alive hosts other than speciate.
                if (e_P != parasiteTree.root || phi.hasMultihostParasites()) {
                    for (int e_H : possibleHostEdges[e_P]) {
                        if (hostAlive[e_H] && (e_H != speciate))
                            beforeCandidate(e_P, e_H, getNoHostNodeThereCostInfo(e_P, e_H), NO_HOST);
                    }
                }
            }
        }
    }
    
    /*
     * taken from
     * http://stackoverflow.com/questions/1670862/obtaining-powerset-of-a-set-in-java
     * by user Joao
     */
    public static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
        Set<Set<T>> sets = new HashSet<Set<T>>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<T>());
            return sets;
        }
        List<T> list = new ArrayList<T>(originalSet);
        T head = list.get(0);
        Set<T> rest = new HashSet<T>(list.subList(1, list.size()));
        for (Set<T> set : powerSet(rest)) {
            Set<T> newSet = new HashSet<T>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }

    /* simply return cost for nothing happens between time t and t - 1 on the
     * given pair of edges
     */
    private long getNothingHappensCostInfo(int e_P, int e_H) {
        return table.getBeforeCostInfo(e_P, e_H);
    }

    private long getNothingHappensCostInfo(int e_P, int name, Set<Integer> tips, int e_H) {
        return table.getBeforeCostInfo(e_P, name, tips, e_H);
    }

    /* compute cost for duplication
     * ASSUMPTION: e_P is not a tip edge 
     */
    private long getDuplicationCostInfo(int e_P, int e_H) {
        return table.addEventToCostInfo(CostModel.DUPLICATION,
                table.addCostInfo(
                table.getAfterCostInfo(parasiteTree.node[e_P].Lchild, e_H),
                table.getAfterCostInfo(parasiteTree.node[e_P].Rchild, e_H)));
    }

    private long getDuplicationCostInfo(int e_P, int name, Set<Integer> tips, int e_H) {
        if (this.isPossiblyTheSame(name, parasiteTree.node[e_P].Lchild)) {
            return table.addEventToCostInfo(CostModel.DUPLICATION,
                    table.addCostInfo(
                    table.getAfterCostInfo(parasiteTree.node[e_P].Lchild, name, tips, e_H),
                    table.getAfterCostInfo(parasiteTree.node[e_P].Rchild, e_H)));
        } else {
            return table.addEventToCostInfo(CostModel.DUPLICATION,
                    table.addCostInfo(
                    table.getAfterCostInfo(parasiteTree.node[e_P].Lchild, e_H),
                    table.getAfterCostInfo(parasiteTree.node[e_P].Rchild, name, tips, e_H)));
        }
    }

    /* compute cost for host-switch where the left child of e_P switches host
     * ASSUPMTION: e_P is not a tip edge
     */
    private long getLeftHostSwitchCostInfo(int e_P, int e_H) {
        // Check if host switching is prevented due to prior ftd events.
        if (this.hasMultihostOnlyFTD() && earliestDescendantFTD[parasiteTree.node[e_P].Lchild] <= currentAfterTime) {
            return table.INFINITY_INFO;
        }
        int switchLocation = -7;
        Random r = new Random();
        if (this.randomizeEvents) {
            
            ArrayList<Integer> switchLocations = null;
            if (costModel instanceof SimpleCostModel)
                switchLocations = ((UniformCostReconstructableHSS)hostSwitchSelector).getBestSwitchLocations(currentAfterTime, parasiteTree.node[e_P].Lchild, e_H);
            else if (costModel instanceof RegionedCostModel)
                switchLocations = ((RegionedReconstructableHSS)hostSwitchSelector).getBestSwitchLocations(currentAfterTime, parasiteTree.node[e_P].Lchild, e_H);
            else
                throw new UnsupportedOperationException();    

            if (!switchLocations.isEmpty()) {
                long numSols = 0;
                for (int location : switchLocations)
                    numSols += table.afterNumBestSolutions[parasiteTree.node[e_P].Lchild][location]; //Calculates the number of solutions for the switching branch since they'll
                                                                                  //All have the same non-switching branch
                long choice = myNextLong(r, numSols);

                for (int location : switchLocations) {
                    choice = choice - table.afterNumBestSolutions[parasiteTree.node[e_P].Lchild][location];
                    if (choice < 0) {
                        switchLocation = location;
                        break;
                    }                    
                }
                //Store the chosen switch location for later use.
            if (costModel instanceof SimpleCostModel)
                ((UniformCostReconstructableHSS)hostSwitchSelector).setBestSwitchLocation(currentAfterTime,parasiteTree.node[e_P].Lchild, switchLocation);
            else if (costModel instanceof RegionedCostModel)
                ((RegionedReconstructableHSS)hostSwitchSelector).setBestSwitchLocation(currentAfterTime,parasiteTree.node[e_P].Lchild, switchLocation);
            else
                throw new UnsupportedOperationException();   
                
            } else {
                switchLocation = hostSwitchSelector.findBestSwitchLocation(currentAfterTime, parasiteTree.node[e_P].Lchild, e_H);
            }
        } else {
            switchLocation = hostSwitchSelector.findBestSwitchLocation(currentAfterTime, parasiteTree.node[e_P].Lchild, e_H);
        }
        if (switchLocation == -1)
            return table.INFINITY_INFO;
        return table.addHostSwitchEventToCostInfo(
                table.addCostInfo(table.getAfterCostInfo(parasiteTree.node[e_P].Rchild, e_H),
                table.getAfterCostInfo(parasiteTree.node[e_P].Lchild, switchLocation)),
                e_H, switchLocation);
    }

    /* compute cost for host-switch where the right child of e_P switches host
     * ASSUPMTION: e_P is not a tip edge
     */
    private long getRightHostSwitchCostInfo(int e_P, int e_H) {
        // Check if host switching is prevented due to prior ftd events.
        if (this.hasMultihostOnlyFTD() && earliestDescendantFTD[parasiteTree.node[e_P].Rchild] <= currentAfterTime)
            return table.INFINITY_INFO;
        int switchLocation = -7;
        Random r = new Random();
        if (this.randomizeEvents) {
            
            ArrayList<Integer> switchLocations = null;
            if (costModel instanceof SimpleCostModel)
                switchLocations = ((UniformCostReconstructableHSS)hostSwitchSelector).getBestSwitchLocations(currentAfterTime, parasiteTree.node[e_P].Rchild, e_H);
            else if (costModel instanceof RegionedCostModel)
                switchLocations = ((RegionedReconstructableHSS)hostSwitchSelector).getBestSwitchLocations(currentAfterTime, parasiteTree.node[e_P].Rchild, e_H);
            else
                throw new UnsupportedOperationException();
           
            if (!switchLocations.isEmpty()) {
                long numSols = 0;
                for (int location : switchLocations)
                    numSols += table.afterNumBestSolutions[parasiteTree.node[e_P].Rchild][location]; //Calculates the number of solutions for the switching branch since they'll            
                long choice = myNextLong(r, numSols);                                                     //All have the same non-switching branch
                
                for (int location: switchLocations) {
                    choice = choice - table.afterNumBestSolutions[parasiteTree.node[e_P].Rchild][location];
                    if (choice < 0) {
                        switchLocation = location;
                        break;
                    }
                }
                //Store the switch location for later use

                if (costModel instanceof SimpleCostModel)
                    ((UniformCostReconstructableHSS)hostSwitchSelector).setBestSwitchLocation(currentAfterTime,parasiteTree.node[e_P].Rchild, switchLocation);
                else if (costModel instanceof RegionedCostModel)
                    ((RegionedReconstructableHSS)hostSwitchSelector).setBestSwitchLocation(currentAfterTime,parasiteTree.node[e_P].Rchild, switchLocation);
                else
                    throw new UnsupportedOperationException();
                
            } else { 
                //This is if both best and sBestSwitchLocations are empty.  Should give -1;
                switchLocation = hostSwitchSelector.findBestSwitchLocation(currentAfterTime, parasiteTree.node[e_P].Rchild, e_H);
            }
        } else {
            switchLocation = hostSwitchSelector.findBestSwitchLocation(currentAfterTime, parasiteTree.node[e_P].Rchild, e_H);
        }
        
        if (switchLocation == -1)
            return table.INFINITY_INFO;
        
        
        return table.addHostSwitchEventToCostInfo(
                table.addCostInfo(table.getAfterCostInfo(parasiteTree.node[e_P].Lchild, e_H),
                table.getAfterCostInfo(parasiteTree.node[e_P].Rchild, switchLocation)),
                e_H, switchLocation);
    }

    /* process Before(e_P, e_H, time + 1) --> After(e_P, e_H, time)
     */
    void beforeToAfter() {
        int hostAtNextTime;

        // Loop through and try events
        for (int i = 0; i < parasiteTree.size; i++) {
            int e_P = parasiteTree.postOrder[i];
            for (int e_H : hostTree.postOrder) { //This is the line that increases the count of nodes with first host switches at 3
                if (hostAlive[e_H]) {
                    if (e_P != parasiteTree.root || phi.hasMultihostParasites())
                        afterCandidate(e_P, e_H, getNothingHappensCostInfo(e_P, e_H), NOTHING_HAPPENS);
                    if (parasiteTree.node[e_P].Rchild != -1) {
                        if (hostTiming.tipTime == currentAfterTime + 1)
                            hostAtNextTime = e_H;
                        else
                            hostAtNextTime = hostTiming.nodeAtTime(currentAfterTime + 1);
                        
                        /*
                         * ensures timezones are overlapping AND that events
                         * don't occur where they shouldn't when we are limiting
                         * the edges in polytomies.
                         */
                        if (timeZones.overlap(e_P, hostAtNextTime) &&
                            !(this.hostTree.noMidPolyEvents && this.hostTree.isPolytomyEdge(e_H))) {
                            afterCandidate(e_P, e_H, getDuplicationCostInfo(e_P, e_H), DUPLICATION);
                            if (allowHostSwitch) {
                                afterCandidate(e_P, e_H, getLeftHostSwitchCostInfo(e_P, e_H), HOSTSWITCH_LEFT);
                                afterCandidate(e_P, e_H, getRightHostSwitchCostInfo(e_P, e_H), HOSTSWITCH_RIGHT);
                            }
                        }
                    }

                    // now, handle the multihost case
                    if (this.hasMultipleNodeStates(e_P)) {
                        // consider every possible name and tips in charge of.
                        MultihostConfiguration config = table.getAfterConfiguration(e_P, e_H);
                        Set<Integer> possibleNames = config.getPossibleNames();
                        for (int name : possibleNames) {
                            Set<Integer> tips = config.getTips(name);
                            Set<Set<Integer>> possibleSubsets = powerSet(tips);
                            for (Set<Integer> tipSet : possibleSubsets) {
                                if (tipSet.isEmpty())
                                    continue;

                                // duplication
                                if (parasiteTree.node[e_P].Lchild != -1 && parasiteTree.node[e_P].Rchild != -1)
                                    afterCandidate(e_P, name, tipSet, e_H, getDuplicationCostInfo(e_P, name, tipSet, e_H), DUPLICATION);
                                
                                // nothing happens
                                if (e_P != parasiteTree.root || phi.hasMultihostParasites())
                                    afterCandidate(e_P, name, tipSet, e_H, getNothingHappensCostInfo(e_P, name, tipSet, e_H), NOTHING_HAPPENS);

                                // infestation
                                Set<Set<Integer>> subsetsOfTipSet = powerSet(tipSet);
                                for (Set<Integer> tipsSet : subsetsOfTipSet) {
                                    // make the other set to partition these.
                                    Set<Integer> interiorEdgeSet = new HashSet<Integer>(tipSet);
                                    interiorEdgeSet.removeAll(tipsSet);

                                    /*
                                     * we now have two subsets of this. Check
                                     * they are not empty, and make recursive
                                     * calls.
                                     */
                                    if (tipsSet.isEmpty() || interiorEdgeSet.isEmpty())
                                        continue;
                                    if (parasiteTree.node[e_P].Lchild != -1)
                                        beforeCandidate(e_P, name, tipSet, e_H, getLeftInfestationCostInfo(e_P, name, interiorEdgeSet, tipsSet, e_H), INFESTATION_PRESERVE_LEFT);
                                    if (parasiteTree.node[e_P].Rchild != -1)
                                        beforeCandidate(e_P, name, tipSet, e_H, getRightInfestationCostInfo(e_P, name, tipsSet, interiorEdgeSet, e_H), INFESTATION_PRESERVE_RIGHT);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void decreaseBeforeTime() {
        --currentBeforeTime;
        table.decreaseBeforeTime();
        infestationSelector.decreaseAfterTime();
    }

    private void decreaseAfterTime() {
        --currentAfterTime;
        table.decreaseAfterTime();
        hostSwitchSelector.decreaseAfterTime();
    }

    public int solve() {
        // start time
        boolean isAfter = false;
        currentBeforeTime = hostTiming.tipTime;
        currentAfterTime = hostTiming.tipTime - 1;
        // initialize table, fill in tip associations,
        initialize();
        // process until we reach "after time 0"
        while (currentBeforeTime != 0 || isAfter != true) {
            if (this.randomizeEvents) {
                //System.out.println("currentBeforeTime = " + currentBeforeTime);
            }
            if (isAfter) {
                afterToBefore();
                updateBeforeBestCostInfo();
                decreaseAfterTime();
                isAfter = false;
            } else {
                beforeToAfter();
                updateAfterBestCostInfo();
                decreaseBeforeTime();
                isAfter = true;
            }
        }
        table.doneSolving();
        hostSwitchSelector.doneSolving();
        infestationSelector.doneSolving();
        hostAlive = null;
        possibleHostEdges = null;
        return table.bestCost();
    }

    /* obtain the event counts for an optimal solution
     * only works for MODE 1 - DP table with event counters
     */
    public int[] getEventCount() {
        if (MODE == EVENT_COUNT)
            return ((EventCounterDPTable) table).bestCostEventCounts();
        return null;
    }

    /* obtain the SolutionViewerInfo for this solved problem
     * only works for MODE 2 - 3D DP table
     */
    public SolutionViewerInfo getSolutionViewerInfo() {
        if (MODE == RECONSTRUCTABLE) {
            ThreeDimDPTable costTable = (ThreeDimDPTable) table;           
            return new SolutionViewerInfo(hostTree, parasiteTree, hostTiming, phi,
                                          timeZones, costModel, costTable.getBeforeTable(), costTable.getAfterTable(),
                                          hostSwitchSelector, infestationSelector, costTable.getOptimalStart(),
                                          needsFTD, ftdPositions, earliestFTD, earliestDescendantFTD, costTable.events);
        }
        throw new UnsupportedOperationException();
    }
    
    /* figure out where e_P can go without infinite cost assumming it is a tip
     * (this is part of the factor of n/log n reduction in the special case that the parasite tree height is logarithmic)
     * this is for the current before time, but this method must be called in postorder on e_P for it to work properly
     */
    private void updatePossibleHostEdges(int e_P) {
        //we don't currently support ThreeDimDPTable
        if (MODE != COST_ONLY && MODE != EVENT_COUNT)
            return;

        if (parasiteTree.isTip(e_P)) {
            SortedIntegerSet newHosts = new SortedIntegerSet();
            for (int e_H : possibleHostEdges[e_P]) {
                if (hostAlive[e_H])
                    newHosts.add(e_H);
                else
                    newHosts.add(hostTree.node[e_H].parent);
            }
            possibleHostEdges[e_P] = newHosts;
        } else {
            //figure out possible locations for e_P on over all e_H
            SortedIntegerSet possibleLeftChildLocations = possibleHostEdges[parasiteTree.leftChild(e_P)];
            SortedIntegerSet possibleRightChildLocations = possibleHostEdges[parasiteTree.rightChild(e_P)];
            possibleHostEdges[e_P] = possibleLeftChildLocations.union(possibleRightChildLocations);
        }
    }

    private boolean isPossiblyMultihost(int paraNode) {
        return table.isPossiblyMultihost(paraNode);
    }

    private boolean isMultihost(int paraNode) {
        return phi.getHosts(paraNode).size() > 1;
    }

    private boolean isPossiblyTheSame(int e_P, int ancestor) {
        return parasiteTree.descendant(ancestor, e_P);
    }

    public boolean isInfestFTDMode() {
        return this.allowInfest && this.allowFTD;
    }

    public boolean isOnlyFTDMode() {
        return !this.allowInfest && this.allowFTD;
    }

    public boolean hasMultihostOnlyFTD() {
        return !this.allowInfest && this.allowFTD && phi.hasMultihostParasites();
    }
    
    public boolean hasMultipleNodeStates(int paraNode) {
        return isInfestFTDMode() && isPossiblyMultihost(paraNode);
    }

    public boolean hasMultihostEdges() {
        return isInfestFTDMode() && phi.hasMultihostParasites();
    }

    // return the possible names of a parasite.
    public Set<Integer> possibleNames(int e_P) {
        Set<Integer> possible = new HashSet<Integer>(phi.getMultihostParasites());
        for (int i : phi.getMultihostParasites()) {
            if (!parasiteTree.descendant(e_P, i))
                possible.remove(i);
        }
        return possible;
    }

    public void setRandomizeMode(boolean randomize) {
        this.randomizeEvents = randomize;
    }
}
