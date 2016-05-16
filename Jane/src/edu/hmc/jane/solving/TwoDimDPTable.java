/* 2D DP table stores only the cost and reuse before and after tables as time
 * gets decreased.
 * This class should be used for general DP during the genetic runs since the
 * optimal cost associated with the timing is the only interested information.
 *
 */
package edu.hmc.jane.solving;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.SortedSet;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;

class TwoDimDPTable extends DPTable {

    private Int2IntMap[] before, after; //think of this as a 2D
    //array of ints
    // holder of multihost parasite info
    private HashMap<Integer, MultihostConfiguration[]> beforeMulti, afterMulti;

    // CONSTRUCTOR
    public TwoDimDPTable(ArrayDP3 problem) {

        // call normal constructor
        super(problem);

        // initialize constants
        INFINITY_INFO = costModel.INFINITY;

        // initialize tables
        before = new Int2IntMap[parasiteTreeSize];
        clearTable(true);
        after = new Int2IntMap[parasiteTreeSize];
        clearTable(false);

        // initialize multihost parasite maps
        if (problem.hasMultihostEdges()) {
            beforeMulti = new HashMap<Integer, MultihostConfiguration[]>();
            afterMulti = new HashMap<Integer, MultihostConfiguration[]>();
            initMultis();
        }
    }

    // fill the table with infinity
    private void clearTable(boolean isBefore) {
        Int2IntMap[] target = (isBefore ? before : after);
        for (int e_P = 0; e_P < parasiteTreeSize; e_P++) {
            target[e_P] = new Int2IntOpenHashMap(); //Using clear here might
            //hurt asympotic
            //performance (I'm not sure)
            //because it iterates over
            //the entire table regardless
            //of how sparsely it is populated
            //I think.
            target[e_P].defaultReturnValue(costModel.INFINITY);
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
            MultihostConfiguration[] currentBefore = new MultihostConfiguration[hostTreeSize];
            MultihostConfiguration[] currentAfter = new MultihostConfiguration[hostTreeSize];
            
            for (int e_H = 0; e_H < hostTreeSize; e_H++) {
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
                currentBefore[e_H] = new MultihostConfiguration(e_P, beforeNameList, beforeTipsList);
                currentAfter[e_H] = new MultihostConfiguration(e_P, afterNameList, afterTipsList);
            }
            beforeMulti.put(e_P, currentBefore);
            afterMulti.put(e_P, currentAfter);
        }
    }

    private void clearMultis(boolean isBefore) {
        HashMap<Integer, MultihostConfiguration[]> target = (isBefore ? beforeMulti : afterMulti);
        for (int e_P = 0; e_P < parasiteTreeSize; e_P++) {
            if (!this.isPossiblyMultihost(e_P)) {
                continue;
            }
            MultihostConfiguration[] current = target.get(e_P);
            for (int e_H = 0; e_H < hostTreeSize; e_H++) {
                current[e_H].clearCosts();
            }
        }
    }

    @Override
    public final long getBeforeCostInfo(int e_P, int e_H) {
        if (!this.isPossiblyMultihost(e_P)) {
            return before[e_P].get(e_H);
        } else {
            // there are multiple possibly configurations.  Try them!
            long bestCostHere = before[e_P].get(e_H);
            bestCostHere = Math.min(this.getBeforeConfiguration(e_P, e_H).bestCost(), bestCostHere);
            return bestCostHere;
        }
    }

    @Override
    public final long getAfterCostInfo(int e_P, int e_H) {
        if (!this.isPossiblyMultihost(e_P)) {
            return after[e_P].get(e_H);
        } else {
            // there are multiple possibly configurations.  Try them!
            long bestCostHere = after[e_P].get(e_H);
            bestCostHere = Math.min(this.getAfterConfiguration(e_P, e_H).bestCost(), bestCostHere);
            return bestCostHere;
        }
    }

    @Override
    public final long getBeforeCostInfo(int e_P, int name, Set<Integer> tips, int e_H) {
        return beforeMulti.get(e_P)[e_H].getCost(name, tips);
    }

    @Override
    public final long getAfterCostInfo(int e_P, int name, Set<Integer> tips, int e_H) {
        return afterMulti.get(e_P)[e_H].getCost(name, tips);
    }

    // we assume this is only used for tips.
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
        before[e_P].put(e_H, 0);
    }

    private void zeroBefore(int e_P, int name, Set<Integer> tips, int e_H) {
        beforeMulti.get(e_P)[e_H].setCost(name, tips, 0);
    }

    @Override
    public final void setBeforeCostInfo(int e_P, int e_H, long costInfo) {
        before[e_P].put(e_H, (int) costInfo);
    }

    @Override
    public final void setAfterCostInfo(int e_P, int e_H, long costInfo) {
        after[e_P].put(e_H, (int) costInfo);
    }

    @Override
    public final void setBeforeCostInfo(int e_P, int name, Set<Integer> tips, int e_H, long costInfo) {
        beforeMulti.get(e_P)[e_H].setCost(name, tips, costInfo);
    }

    @Override
    public final void setAfterCostInfo(int e_P, int name, Set<Integer> tips, int e_H, long costInfo) {
        afterMulti.get(e_P)[e_H].setCost(name, tips, costInfo);
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
    public final void decreaseBeforeTime() {
        clearTable(true);
        if (problem.hasMultihostEdges()) {
            clearMultis(true);
        }
    }

    @Override
    public final void decreaseAfterTime() {
        clearTable(false);
        if (problem.hasMultihostEdges()) {
            clearMultis(false);
        }
    }

    @Override
    public final void candidateBeforeBestCostInfo(int e_P, int e_H) {
        if (bestCost > before[e_P].get(e_H)) {
            bestCost = before[e_P].get(e_H);
        }
    }

    @Override
    public final void candidateAfterBestCostInfo(int e_P, int e_H) {
        if (bestCost > after[e_P].get(e_H)) {
            bestCost = after[e_P].get(e_H);
        }
    }

    @Override
    public final int bestCost() {
        return bestCost;
    }

    @Override
    public final void doneSolving() {
        before = null;
        after = null;
    }

    @Override
    public final int getFromAfter(int e_P, int e_H) {
        return after[e_P].get(e_H);
    }

    // the below deal with multihost parasites when we allow infestations
    @Override
    public final MultihostConfiguration getBeforeConfiguration(int e_P, int e_H) {
        return beforeMulti.get(e_P)[e_H];
    }

    @Override
    public final MultihostConfiguration getAfterConfiguration(int e_P, int e_H) {
        return afterMulti.get(e_P)[e_H];
    }

    @Override
    public final boolean isPossiblyMultihost(int e_P) {
        return problem.hasMultihostEdges() && beforeMulti.containsKey(e_P);
    }
}
