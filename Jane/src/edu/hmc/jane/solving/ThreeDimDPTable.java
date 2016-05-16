/*  DP table stores only the cost. It can gives the cost table back to
 * the caller and so that the caller can construct SolutionViewerInfo.
 * This class should be used when a solution needs to be drawn, and we need
 * the whole  tables to allow solution reconstruction / modifications.
 */
package edu.hmc.jane.solving;

import edu.hmc.jane.HostLocation;
import java.util.*;

class ThreeDimDPTable extends DPTable {

    // DP tables
    int[][][] before, after, events;
    // holder of multihost parasite info
    HashMap<Integer, MultihostConfiguration[][]> beforeMulti, afterMulti;
    boolean bestIsBefore;
    int best_e_H, bestTime, bestNodeID;

    // CONSTRUCTOR
    public ThreeDimDPTable(ArrayDP3 problem) {

        // call normal constructor
        super(problem);

        // initialize constants
        INFINITY_INFO = costModel.INFINITY;

        // initialize tables
        int tipTime = problem.hostTiming.tipTime;
        before = new int[tipTime + 1][parasiteTreeSize][hostTreeSize];
        for (int time = 0; time <= tipTime; time++) {
            clearTable(true, time);
        }
        after = new int[tipTime + 1][parasiteTreeSize][hostTreeSize];
        for (int time = 0; time <= tipTime; time++) {
            clearTable(false, time);
        }
        
        events = new int[tipTime +1][parasiteTreeSize][hostTreeSize];
        for (int time = 0; time <= tipTime; time++) {
            for (int e_P = 0 ; e_P < parasiteTreeSize; e_P++) {
                for (int e_H = 0; e_H < hostTreeSize; e_H++) {
                    events[time][e_P][e_H] = -1;
                }
            }
        }

        // initialize multihost parasite maps
        if (problem.hasMultihostEdges()) {
            beforeMulti = new HashMap<Integer, MultihostConfiguration[][]>();
            afterMulti = new HashMap<Integer, MultihostConfiguration[][]>();
            initMultis();
        }
    }

    // fill the table with infinity
    private void clearTable(boolean isBefore, int time) {
        int[][] target = (isBefore ? before : after)[time];
        for (int e_P = 0; e_P < parasiteTreeSize; e_P++) {
            for (int e_H = 0; e_H < hostTreeSize; e_H++) {
                target[e_P][e_H] = costModel.INFINITY;
            }
        }
    }

    // initialize the multihost configurations
    private void initMultis() {
        for (int e_P = 0; e_P < parasiteTreeSize; e_P++) { // for each parasite...
            Set<Integer> names = problem.possibleNames(e_P);
            if (names.isEmpty()) {
                // don't consider cases where this couldn't be mapped to multiple parasites
                continue;
            }
            
            // an array for a configuration for each host on this parasite
            MultihostConfiguration[][] beforeCurrentMatrix = new MultihostConfiguration[hostTreeSize][problem.hostTiming.tipTime];
            MultihostConfiguration[][] afterCurrentMatrix  = new MultihostConfiguration[hostTreeSize][problem.hostTiming.tipTime];
            
            for (int e_H = 0; e_H < hostTreeSize; e_H++) {
                for (int t = 0; t < problem.hostTiming.tipTime; t++) {
                    LinkedList<Integer> beforeNameList = new LinkedList<Integer>();
                    LinkedList<Integer> afterNameList = new LinkedList<Integer>();
                    LinkedList<Set<Integer>> beforeTipsList = new LinkedList<Set<Integer>>();
                    LinkedList<Set<Integer>> afterTipsList = new LinkedList<Set<Integer>>();
                    for (int name : names) {
                        Set<Integer> tips = problem.phi.getHosts(name);
                        beforeNameList.add(name);
                        afterNameList.add(name);
                        beforeTipsList.add(tips);
                        afterTipsList.add(tips);
                    }
                    beforeCurrentMatrix[e_H][t] = new MultihostConfiguration(e_P, beforeNameList, beforeTipsList);
                    afterCurrentMatrix[e_H][t] =  new MultihostConfiguration(e_P, afterNameList,  afterTipsList );
                }
            }
            beforeMulti.put(e_P, beforeCurrentMatrix);
            afterMulti.put(e_P, afterCurrentMatrix);
        }
    }

    @Override
    public final long getBeforeCostInfo(int e_P, int e_H) {
        if (!this.isPossiblyMultihost(e_P)) {
            return before[problem.currentBeforeTime][e_P][e_H];
        } else {
            long bestCostHere = before[problem.currentBeforeTime][e_P][e_H];
            bestCostHere = Math.min(this.getBeforeConfiguration(e_P, e_H).bestCost(), bestCostHere);
            return bestCostHere;
        }
    }

    @Override
    public final long getAfterCostInfo(int e_P, int e_H) {
        if (!this.isPossiblyMultihost(e_P)) {
            return after[problem.currentAfterTime][e_P][e_H];
        } else {
            long bestCostHere = after[problem.currentBeforeTime][e_P][e_H];
            bestCostHere = Math.min(this.getAfterConfiguration(e_P, e_H).bestCost(), bestCostHere);
            return bestCostHere;
        }
    }

    @Override
    public final long getBeforeCostInfo(int e_P, int name, Set<Integer> tips, int e_H) {
        return beforeMulti.get(e_P)[e_H][problem.currentBeforeTime].getCost(name, tips);
    }

    @Override
    public final long getAfterCostInfo(int e_P, int name, Set<Integer> tips, int e_H) {
        return afterMulti.get(e_P)[e_H][problem.currentAfterTime].getCost(name, tips);
    }

    @Override
    public final void zeroBefore(int e_P, SortedSet<Integer> hosts) {
        for (int e_H : hosts) {
            if (hosts.size() == 1 || problem.isOnlyFTDMode()) {
                zeroBefore(e_P, e_H);
            } else {
                Set<Integer> current = new HashSet<Integer>();
                current.add(e_H);
                zeroBefore(e_P, e_P, current, e_H);
            }
        }
    }

    @Override
    public final void zeroBefore(int e_P, int e_H) {
        before[problem.currentBeforeTime][e_P][e_H] = 0;
    }

    private void zeroBefore(int e_P, int name, Set<Integer> tips, int e_H) {
        beforeMulti.get(e_P)[e_H][problem.currentBeforeTime].setCost(name, tips, 0);
    }

    @Override
    public final void setBeforeCostInfo(int e_P, int e_H, long costInfo) {
        before[problem.currentBeforeTime][e_P][e_H] = (int) costInfo;
    }

    @Override
    public final void setAfterCostInfo(int e_P, int e_H, long costInfo) {
        after[problem.currentAfterTime][e_P][e_H] = (int) costInfo;
    }

    @Override
    public final void setBeforeCostInfo(int e_P, int name, Set<Integer> tips, int e_H, long costInfo) {
        beforeMulti.get(e_P)[e_H][problem.currentBeforeTime].setCost(name, tips, costInfo);
    }

    @Override
    public final void setAfterCostInfo(int e_P, int name, Set<Integer> tips, int e_H, long costInfo) {
        afterMulti.get(e_P)[e_H][problem.currentBeforeTime].setCost(name, tips, costInfo);
    }
    
    @Override
    public final void candidateBeforeBestCostInfo(int e_P, int e_H) {
        // this function only works when e_P is the root
        if (before[problem.currentBeforeTime][e_P][e_H] < bestCost) {
            // keep track of the cell position of optimal solution
            bestCost = before[problem.currentBeforeTime][e_P][e_H];
            bestIsBefore = true;
            best_e_H = e_H;
            bestTime = problem.currentBeforeTime;
            bestNodeID = problem.hostTree.getNodeID(e_H);
            if (problem.randomizeEvents) {
                problem.numRootPlacements = 0;
                problem.incrementSolutions(e_P, e_H, true, ArrayDP3.ROOT);
            }
//                this.beforeNumBestSolutions[e_P][e_H] = 1;
        } else if (problem.randomizeEvents &&
                   before[problem.currentAfterTime][e_P][e_H] == bestCost &&
                   bestCost < problem.INFINITY &&
                   problem.coinFlipped(e_P, e_H, true, ArrayDP3.ROOT)) {
            // keep track of the cell position of optimal solution
            bestCost = before[problem.currentBeforeTime][e_P][e_H];
            bestIsBefore = true;
            best_e_H = e_H;
            bestTime = problem.currentBeforeTime;
            bestNodeID = problem.hostTree.getNodeID(e_H);
        }
    }

    @Override
    public final void candidateAfterBestCostInfo(int e_P, int e_H) {
        // this function only works when e_P is the root.  If that should be expanded, make the coinflipping work.  That is all that is holding this back.
        if (after[problem.currentAfterTime][e_P][e_H] < bestCost) {
            // keep track of the cell position of optimal solution
            bestCost = after[problem.currentAfterTime][e_P][e_H];
            bestIsBefore = false;
            best_e_H = e_H;
            bestTime = problem.currentAfterTime;
            bestNodeID = problem.hostTree.getNodeID(e_H);
            if (problem.randomizeEvents) {
                problem.numRootPlacements = 0;
                problem.incrementSolutions(e_P, e_H, false, ArrayDP3.ROOT);
            }
//                this.afterNumBestSolutions[e_P][e_H] = 1;
        } else if (problem.randomizeEvents &&
                   after[problem.currentAfterTime][e_P][e_H] == bestCost &&
                   bestCost < problem.INFINITY &&
                   problem.coinFlipped(e_P, e_H, false, ArrayDP3.ROOT)) {
            // keep track of the cell position of optimal solution
            bestCost = after[problem.currentAfterTime][e_P][e_H];
            bestIsBefore = false;
            best_e_H = e_H;
            bestTime = problem.currentAfterTime;
            bestNodeID = problem.hostTree.getNodeID(e_H);
        }
    }

    @Override
    public final int toRelativeCost(long costInfo) {
        return (int) costInfo;
    }
    
    @Override
    public final int toRelativeCost(long costInfo, int firstName, Set<Integer> firstTips) {
        return (int) costInfo;
    }
    
    @Override
    public final int toRelativeCost(long costInfo, int firstName, Set<Integer> firstTips, int secondName, Set<Integer> secondTips) {
        return (int) costInfo;
    }

    @Override
    public final long addEventToCostInfo(int eventType, long costInfo) {
        if (costInfo == INFINITY_INFO) {
            return INFINITY_INFO;
        }
        return costInfo + costModel.getCost(eventType);
    }

    @Override
    public final long addHostSwitchEventToCostInfo(long costInfo, int e_H1, int e_H2) {
        int switchCost = costModel.getHostSwitchCost(e_H1, e_H2);
        if (costInfo == INFINITY_INFO || costModel.isInfinity(switchCost)) {
            return INFINITY_INFO;
        }
        return costInfo + switchCost;
    }
    
    @Override
    public final long addInfestationEventToCostInfo(long costInfo, int e_H1, int e_H2) {
        int infestationCost = costModel.getInfestationCost(e_H1, e_H2);
        if (costInfo == INFINITY_INFO || costModel.isInfinity(infestationCost)) {
            return INFINITY_INFO;
        }
        return costInfo + infestationCost;
    }
    
    @Override
    public final long addCostInfo(long costInfo1, long costInfo2) {
        if (costInfo1 == INFINITY_INFO || costInfo2 == INFINITY_INFO) {
            return INFINITY_INFO;
        }
        return costInfo1 + costInfo2;
    }

    @Override
    public final int bestCost() {
        return bestCost;
    }

    // needs to change the reference to the DP table, but already clear
    @Override
    public final void decreaseBeforeTime() {
        if (problem.randomizeEvents)
            this.clearSolutionCount(true);
    }

    // needs to change the reference to the DP table, but already clear
    @Override
    public final void decreaseAfterTime() {
        if (problem.randomizeEvents)
            this.clearSolutionCount(false);
    }

    // MODE 2 only: return before DP table for SolutionViewerInfo to use
    public final int[][][] getBeforeTable() {
        return before;
    }

    // MODE 2 only: return after DP table for SolutionViewerInfo to use
    public final int[][][] getAfterTable() {
        return after;
    }

    // MODE 2 only: obtain optimal mapping for the start of the edge before the
    // parasite root.
    public final HostLocation getOptimalStart() {
        return new HostLocation(best_e_H, bestTime, bestIsBefore, bestNodeID);
    }
    
    @Override
    public final void doneSolving() {
        // Nothing to be done here
    }

    @Override
    public int getFromAfter(int e_P, int e_H) {
        return after[problem.currentAfterTime][e_P][e_H];
    }
    
    // the below deal with multihost parasites when we allow infestations
    @Override
    public final MultihostConfiguration getBeforeConfiguration(int e_P, int e_H) {
        return beforeMulti.get(e_P)[e_H][problem.currentBeforeTime];
    }

    @Override
    public final MultihostConfiguration getAfterConfiguration(int e_P, int e_H) {
        return afterMulti.get(e_P)[e_H][problem.currentAfterTime];
    }

    @Override
    public final boolean isPossiblyMultihost(int e_P) {
        return problem.hasMultihostEdges() && beforeMulti.containsKey(e_P);
    }
    
    
    public void setEvent(int time, int e_P, int e_H, int eventType) {
        events[time][e_P][e_H] = eventType;
    }
    
}
