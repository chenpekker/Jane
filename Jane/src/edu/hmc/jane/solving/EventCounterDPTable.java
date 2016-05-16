/* 2D DP table stores both the cost and a possible set of events that
 * contribute to that cost.
 * This class should be used when we already know the timing that gives optimal
 * solution so that it gives the number of events to display to users.
 */
package edu.hmc.jane.solving;

import edu.hmc.jane.CostModel;
import edu.hmc.jane.util.Int2ObjectOpenHashMapWithDefaultFactory;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.*;
import java.util.concurrent.Callable;

public class EventCounterDPTable extends DPTable {

    /*
     * costInfo will store the position (only e_P and e_H) of the two cells that
     * contribute to the total cost, and event type
     *
     * Encoding of bits in costInfo (8 bytes) bit from LSB Information
     * 0 - 11 e_H for first cell
     * 12 - 23 e_P for first cell
     * 24 1 iff first cell is from before table
     * 25 - 31 unused (should be blank)
     * 32 - 43 e_H for second cell
     * 44 - 55 e_P for second cell
     * 56 1 iff second cell is from before table
     * 57 - 58 unused (should be blank)
     * 59 - 61 event type (from 0 to 5)
     * 62 1 iff event type is from 0 to 5 (something happens)
     * 63 if this bit is 1 and the rest is 0, then means infinity
     */
    private Int2IntMap[] beforeCost, afterCost; //this is basically a 2D array of ints
    // holder of multihost parasite info
    private HashMap<Integer, MultihostConfiguration[]> beforeMulti, afterMulti;
    //this is basically a 3D array of ints
    //FTDD: add capability for multihost parasites
    Int2ObjectMap<int[]>[] beforeEventCounts, afterEventCounts;
    int[] bestEventCounts;

    // CONSTRUCTOR
    public EventCounterDPTable(ArrayDP3 problem) {

        // call normal constructor
        super(problem);

        // initialize constants
        INFINITY_INFO = 1L << 63;

        // initialize tables
        beforeCost = new Int2IntMap[parasiteTreeSize];
        clearTable(true);
        afterCost = new Int2IntMap[parasiteTreeSize];
        clearTable(false);

        // initialize multihost parasite maps

        if (problem.hasMultihostEdges()) {
            beforeMulti = new HashMap<Integer, MultihostConfiguration[]>();
            afterMulti = new HashMap<Integer, MultihostConfiguration[]>();
            initMultis();
        }

        // prepare event count tables
        Int2ObjectOpenHashMapWithDefaultFactory<int[]>[] beforeTmp, afterTmp;
        beforeTmp = new Int2ObjectOpenHashMapWithDefaultFactory[parasiteTreeSize];
        initializeEventCounts(beforeTmp);
        afterTmp = new Int2ObjectOpenHashMapWithDefaultFactory[parasiteTreeSize];
        initializeEventCounts(afterTmp);

        beforeEventCounts = beforeTmp;
        afterEventCounts = afterTmp;


        // keep the event counts of best solution found
        bestEventCounts = new int[CostModel.NUM_EVENTS];
    }

    private void initializeEventCounts(Int2ObjectOpenHashMapWithDefaultFactory<int[]>[] target) {
        for (int e_P = 0; e_P < parasiteTreeSize; e_P++) {
            target[e_P] = new Int2ObjectOpenHashMapWithDefaultFactory<int[]>();
            target[e_P].defaultReturnFactory(new Callable<int[]>() {

                public int[] call() {
                    return new int[CostModel.NUM_EVENTS];
                }
            });
        }
    }

    // fill the table with infinity
    private void clearTable(boolean isBefore) {
        Int2IntMap[] target = (isBefore ? beforeCost : afterCost);
        for (int e_P = 0; e_P < parasiteTreeSize; e_P++) {
            target[e_P] = new Int2IntOpenHashMap(); //Using clear here might
            //hurt asympotic
            //performance (I'm not sure)
            //because it iteratees over
            //the entire table regardless
            //of how sparsely it is populated
            //I think.
            target[e_P].defaultReturnValue(costModel.INFINITY);
        }
    }

    // initialize the multihost configurations
    private void initMultis() {
        for (int e_P = 0; e_P < parasiteTreeSize; e_P++) {
            Set<Integer> names = problem.possibleNames(e_P);
            if (names.isEmpty()) {
                continue;
            }
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

    // encode "before", "e_P" and "e_H"
    @Override
    public final long getBeforeCostInfo(int e_P, int e_H) {
        return (e_P << 12) | e_H | (1 << 24);
//        if (!this.isPossiblyMultihost(e_P)) {
//            return (e_P << 12) | e_H | (1 << 24);
//        } else {
//            long bestCost = (e_P << 12) | e_H | (1 << 24);
//            bestCost = Math.min(this.getBeforeConfiguration(e_P, e_H).bestCost(), bestCost);
//            return bestCost;
//        }
    }

    @Override
    public final long getBeforeCostInfo(int e_P, int name, Set<Integer> tips, int e_H) {
        return (e_P << 12) | e_H | (1 << 24);
        //return beforeMulti.get(e_P)[e_H].getCost(name, tips);
    }

    // encode "after", "e_P" and "e_H"
    @Override
    public final long getAfterCostInfo(int e_P, int e_H) {
        return (e_P << 12) | e_H;
    }

    @Override
    public final long getAfterCostInfo(int e_P, int name, Set<Integer> tips, int e_H) {
        return (e_P << 12) | e_H;
        //return afterMulti.get(e_P)[e_H].getCost(name, tips);
    }

    // decode cell positions and update cell
    // table to be updated needs to be specified first
    private void setCostInfo(Int2IntMap[] costTargetTable,
            Int2ObjectMap<int[]>[] eventCountsTargetTable, int e_P, int e_H, long costInfo) {

        // update cost
        costTargetTable[e_P].put(e_H, toRelativeCost(costInfo));

        // reference to positions
        boolean first_isBefore, second_isBefore;
        int first_e_P, second_e_P, first_e_H, second_e_H;

        // position for the first cell
        first_isBefore = (((costInfo >>> 24) & 1) != 0);
        first_e_P = (int) (costInfo >>> 12) & 0xfff;
        first_e_H = (int) costInfo & 0xfff;
        // add event counts from the first cell
        int[] first_eventCounts = (first_isBefore ? beforeEventCounts : afterEventCounts)[first_e_P].get(first_e_H);
        for (int i = 0; i < CostModel.NUM_EVENTS; i++) {
            eventCountsTargetTable[e_P].get(e_H)[i] = first_eventCounts[i];
        }

        // obtain event type
        int eventType = (int) (costInfo >>> 59) & 7;
        // for cospeciation, duplication, host-switch, failure to diverge, or infestation
        if (((costInfo >>> 62) & 1) != 0 & (eventType < 3 || eventType >= 4)) {
            // position for the second cell
            second_isBefore = (((costInfo >>> 56) & 1) != 0);
            second_e_P = (int) (costInfo >>> 44) & 0xfff;
            second_e_H = (int) (costInfo >>> 32) & 0xfff;
            // add event counts from the second cell
            int[] second_eventCounts = (second_isBefore ? beforeEventCounts : afterEventCounts)[second_e_P].get(second_e_H);
            for (int i = 0; i < CostModel.NUM_EVENTS; i++) {
                eventCountsTargetTable[e_P].get(e_H)[i] += second_eventCounts[i];
            }
        }

        // also count that new event if something happens
        if (((costInfo >>> 62) & 1) != 0) {
            eventCountsTargetTable[e_P].get(e_H)[(int) (costInfo >>> 59) & 7]++;
        }
    }

    // decode cell positions and update cell
    // table to be updated needs to be specified first
    private void setCostInfo(HashMap<Integer, MultihostConfiguration[]> costTargetTable,
            Int2ObjectMap<int[]>[] eventCountsTargetTable, int e_P, int name, Set<Integer> tips, int e_H, long costInfo) {

        // update cost
        costTargetTable.get(e_P)[e_H].setCost(name, tips, toRelativeCost(costInfo, name, tips));

        // reference to positions
        boolean first_isBefore, second_isBefore;
        int first_e_P, second_e_P, first_e_H, second_e_H;

        // position for the first cell
        first_isBefore = (((costInfo >>> 24) & 1) != 0);
        first_e_P = (int) (costInfo >>> 12) & 0xfff;
        first_e_H = (int) costInfo & 0xfff;
        // add event counts from the first cell
        int[] first_eventCounts = (first_isBefore ? beforeEventCounts : afterEventCounts)[first_e_P].get(first_e_H);
        for (int i = 0; i < CostModel.NUM_EVENTS; i++) {
            eventCountsTargetTable[e_P].get(e_H)[i] = first_eventCounts[i];
        }

        // obtain event type
        int eventType = (int) (costInfo >>> 59) & 7;
        // for cospeciation, duplication, host-switch, failure to diverge, or infestation
        if (((costInfo >>> 62) & 1) != 0 & (eventType < 3 || eventType >= 4)) {
            // position for the second cell
            second_isBefore = (((costInfo >>> 56) & 1) != 0);
            second_e_P = (int) (costInfo >>> 44) & 0xfff;
            second_e_H = (int) (costInfo >>> 32) & 0xfff;
            // add event counts from the second cell
            int[] second_eventCounts = (second_isBefore ? beforeEventCounts : afterEventCounts)[second_e_P].get(second_e_H);
            for (int i = 0; i < CostModel.NUM_EVENTS; i++) {
                eventCountsTargetTable[e_P].get(e_H)[i] += second_eventCounts[i];
            }
        }

        // also count that new event if something happens
        if (((costInfo >>> 62) & 1) != 0) {
            eventCountsTargetTable[e_P].get(e_H)[(int) (costInfo >>> 59) & 7]++;
        }
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
        // need to set both cost and event counts to 0
        beforeCost[e_P].put(e_H, 0);

        for (int i = 0; i < CostModel.NUM_EVENTS; i++) {
            beforeEventCounts[e_P].get(e_H)[i] = 0;
        }

    }

    private void zeroBefore(int e_P, int name, Set<Integer> tips, int e_H) {
        beforeMulti.get(e_P)[e_H].setCost(name, tips, 0);
    }

    @Override
    public final void setBeforeCostInfo(int e_P, int e_H, long costInfo) {
        // call helper function with correct target table
        setCostInfo(beforeCost, beforeEventCounts, e_P, e_H, costInfo);
    }

    @Override
    public final void setBeforeCostInfo(int e_P, int name, Set<Integer> tips, int e_H, long costInfo) {
        // call helper function with correct target table
        setCostInfo(beforeMulti, beforeEventCounts, e_P, name, tips, e_H, costInfo);
    }

    @Override
    public final void setAfterCostInfo(int e_P, int e_H, long costInfo) {
        // call helper function with correct target table
        setCostInfo(afterCost, afterEventCounts, e_P, e_H, costInfo);
    }

    @Override
    public final void setAfterCostInfo(int e_P, int name, Set<Integer> tips, int e_H, long costInfo) {
        // call helper function with correct target table
        setCostInfo(afterMulti, afterEventCounts, e_P, name, tips, e_H, costInfo);
    }

    @Override
    public final void candidateBeforeBestCostInfo(int e_P, int e_H) {
        if (beforeCost[e_P].get(e_H) < bestCost) {
            // need to copy both cost and event counts
            bestCost = beforeCost[e_P].get(e_H);
            System.arraycopy(beforeEventCounts[e_P].get(e_H), 0, bestEventCounts, 0, CostModel.NUM_EVENTS);
        }
    }

    @Override
    public final void candidateAfterBestCostInfo(int e_P, int e_H) {
        if (afterCost[e_P].get(e_H) < bestCost) {
            // need to copy both cost and event counts
            bestCost = afterCost[e_P].get(e_H);
            System.arraycopy(afterEventCounts[e_P].get(e_H), 0, bestEventCounts, 0, CostModel.NUM_EVENTS);
        }
    }

    @Override
    public final int toRelativeCost(long costInfo) {
        return toRelativeCost(costInfo, -17, null, -17, null);
//        // if costInfo encode impossible event, then cost is infinity
//        if (costInfo == INFINITY_INFO) {
//            return costModel.INFINITY;
//        }
//
//        // costs to be computed (0 for default)
//        int firstCost = 0, secondCost = 0, eventCost = 0;
//        // reference to position
//        boolean first_isBefore, second_isBefore;
//        int first_e_P, second_e_P, first_e_H, second_e_H;
//
//        // position for the first cell
//        first_isBefore = (((costInfo >>> 24) & 1) != 0);
//        first_e_P = (int) (costInfo >>> 12) & 0xfff;
//        first_e_H = (int) costInfo & 0xfff;
//        // obtain cost of the first cell
//        firstCost = this.findBestCost(first_e_P, first_e_H, first_isBefore);
//        //(first_isBefore ? beforeCost : afterCost)[first_e_P].get(first_e_H);
//
//        // in case something happens
//        if (((costInfo >>> 62) & 1) != 0) {
//            // obtain event type
//            int eventType = (int) (costInfo >>> 59) & 7;
//            // for cospeciation, duplication, host-switch, or failure to diverge
//            if (eventType < 3 || eventType == 4) {
//                // position for the second cell
//                second_isBefore = (((costInfo >>> 56) & 1) != 0);
//                second_e_P = (int) (costInfo >>> 44) & 0xfff;
//                second_e_H = (int) (costInfo >>> 32) & 0xfff;
//                secondCost = this.findBestCost(second_e_P, second_e_H, second_isBefore);
//                //(second_isBefore ? beforeCost : afterCost)[second_e_P].get(second_e_H);
//
//                // obtain the cost for the second cell
//                if (eventType == 2) // special cost for host-switch
//                // note that first_e_H is the take-off site and second_e_H is the landing site
//                {
//                    eventCost = costModel.getHostSwitchCost(first_e_H, second_e_H);
//                } else // constant cost for non host-switch event
//                {
//                    eventCost = costModel.getCost(eventType);
//                }
//            } else // constant cost for non host-switch event
//            {
//                eventCost = costModel.getCost(eventType);
//            }
//        }
//
//        // if any of the costs represents impossible embedding, then return cost infinity
//        if (costModel.isInfinity(firstCost) || costModel.isInfinity(secondCost) || costModel.isInfinity(eventCost)) {
//            return costModel.INFINITY;
//        }
//        // otherwise compute new cost
//        return firstCost + secondCost + eventCost;
    }
    
    @Override
    public final int toRelativeCost(long costInfo, int firstName, Set<Integer> firstTips) {
        return toRelativeCost(costInfo, firstName, firstTips, -17, null);
    }

    @Override
    public final int toRelativeCost(long costInfo, int firstName, Set<Integer> firstTips, int secondName, Set<Integer> secondTips) {
        // if costInfo encode impossible event, then cost is infinity
        if (costInfo == INFINITY_INFO) {
            return costModel.INFINITY;
        }

        // costs to be computed (0 for default)
        int firstCost = 0, secondCost = 0, eventCost = 0;
        // reference to position
        boolean first_isBefore, second_isBefore;
        int first_e_P, second_e_P, first_e_H, second_e_H;

        // position for the first cell
        first_isBefore = (((costInfo >>> 24) & 1) != 0);
        first_e_P = (int) (costInfo >>> 12) & 0xfff;
        first_e_H = (int) costInfo & 0xfff;
        // obtain cost of the first cell
        if (firstTips == null) {
            firstCost = this.findBestCost(first_e_P, first_e_H, first_isBefore);
        } else {
            firstCost = (int) (first_isBefore ? beforeMulti : afterMulti).get(first_e_P)[first_e_H].getCost(firstName, firstTips);
        }
        //(first_isBefore ? beforeCost : afterCost)[first_e_P].get(first_e_H);


        // in case something happens
        if (((costInfo >>> 62) & 1) != 0) {
            // obtain event type
            int eventType = (int) (costInfo >>> 59) & 7;
            // for cospeciation, duplication, host-switch, failure to diverge or infestation
            if (eventType < 3 || eventType == 4 || eventType == 5) {
                // position for the second cell
                second_isBefore = (((costInfo >>> 56) & 1) != 0);
                second_e_P = (int) (costInfo >>> 44) & 0xfff;
                second_e_H = (int) (costInfo >>> 32) & 0xfff;
                if (secondTips == null) {
                    secondCost = this.findBestCost(second_e_P, second_e_H, second_isBefore);
                } else {
                    secondCost = (int) (second_isBefore ? beforeMulti : afterMulti).get(second_e_P)[second_e_H].getCost(secondName, secondTips);
                }
                //(second_isBefore ? beforeCost : afterCost)[second_e_P].get(second_e_H);

                // obtain the cost for the second cell
                if (eventType == 2) { // special cost for host-switch
                // note that first_e_H is the take-off site and second_e_H is the landing site
                    eventCost = costModel.getHostSwitchCost(first_e_H, second_e_H);
                } else if (eventType == 5) { // special cost for infestation
                // note that first_e_H is the take-off site and second_e_H is the landing site
                    eventCost = costModel.getInfestationCost(first_e_H, second_e_H);
                } else { // constant cost for non host-switch event
                    eventCost = costModel.getCost(eventType);
                }
            } else { // constant cost for loss
                eventCost = costModel.getCost(eventType);
            }
        }

        // if any of the costs represents impossible embedding, then return cost infinity
        if (costModel.isInfinity(firstCost) || costModel.isInfinity(secondCost) || costModel.isInfinity(eventCost)) {
            return costModel.INFINITY;
        }
        // otherwise compute new cost
        return firstCost + secondCost + eventCost;
    }

    @Override
    public final long addEventToCostInfo(int eventType, long costInfo) {
        // add event at bit 59, and set bit 62 to 1
        return costInfo | (((long) eventType + 8) << 59);
    }

    @Override
    public final long addHostSwitchEventToCostInfo(long costInfo, int e_H1, int e_H2) {
        // set bit 62 to true and bits 59-61 to host-switch event type (which is 2)
        return costInfo | (10L << 59);
    }
    
    @Override
    public final long addInfestationEventToCostInfo(long costInfo, int e_H1, int e_H2) {
        // set bit 62 to true and bits 59-61 to infestation event type (which is 5)
        return costInfo | (10L << 59);
    }

    @Override
    public final long addCostInfo(long costInfo1, long costInfo2) {
        // shift information of one cell by 32 bits and leave the other at the same bits
        // for host switch and infestation,
        // costInfo1 must contain the take-off site (so it must not be shifted)
        // and costInfo2 must contain the landing site (so it must be shifted)
        return (costInfo2 << 32) | costInfo1;
    }

    @Override
    public final int bestCost() {
        return bestCost;
    }

    // MODE 1 only: obtain the event counts for best solution
    public final int[] bestCostEventCounts() {
        return bestEventCounts;
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
    public final void doneSolving() {
        beforeCost = null;
        beforeEventCounts = null;
        afterCost = null;
        afterEventCounts = null;
    }

    @Override
    public final int getFromAfter(int e_P, int e_H) {
        return afterCost[e_P].get(e_H);
    }

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
        return problem.hasMultihostEdges() && afterMulti.containsKey(e_P);
    }

    public int findBestCost(int e_P, int e_H, boolean before) {
        if (!this.isPossiblyMultihost(e_P)) {
            return (before ? beforeCost : afterCost)[e_P].get(e_H);
        } else {
            int bestCost = (before ? beforeCost : afterCost)[e_P].get(e_H);
            bestCost = Math.min((int) this.getBeforeConfiguration(e_P, e_H).bestCost(), bestCost);
            return bestCost;
        }
    }
}
