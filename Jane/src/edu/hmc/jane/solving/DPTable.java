/* An interface for the dynamic programming table - at very least, it must store
 * costs, but it can store more information if needed for each DP mode
 */
package edu.hmc.jane.solving;

import edu.hmc.jane.CostModel;
import java.util.SortedSet;
import java.util.Set;

abstract class DPTable {

    // costInfo that means infinity or impossible to embed
    public long INFINITY_INFO;
    // information about DP to be solved
    ArrayDP3 problem;
    int hostTreeSize, parasiteTreeSize;
    CostModel costModel;
    // keeps track of the number of best solutions for each 
    public long[][] beforeNumBestSolutions;
    public long[][] afterNumBestSolutions;
    // keep track of the best cost found
    int bestCost;

    // CONSTRUCTOR
    public DPTable(ArrayDP3 problem) {
        // obtain the problem
        this.problem = problem;

        // obtain the dimensions of the tables
        hostTreeSize = problem.hostTree.size;
        parasiteTreeSize = problem.parasiteTree.size;

        // obtain the cost model
        costModel = problem.costModel;
        bestCost = costModel.INFINITY;

        if (problem.randomizeEvents) {
            // should only be true for the 3D table!
            initializeSolutionCounts();
            clearSolutionCount(true);
            clearSolutionCount(false);
            initializeSolutionCount(true);
            initializeSolutionCount(false);
        }
    }

    public final void initializeSolutionCounts() {
        beforeNumBestSolutions = new long[problem.parasiteTree.size][problem.hostTree.size];
        afterNumBestSolutions = new long[problem.parasiteTree.size][problem.hostTree.size];
    }

    public final void clearSolutionCount(boolean before) {
        if (problem.randomizeEvents) {
            long[][] table = (before ? beforeNumBestSolutions : afterNumBestSolutions);
            for (int e_P = 0; e_P < table.length; e_P++) {
                for (int e_H = 0; e_H < table[0].length; e_H++) {
                    table[e_P][e_H] = 0;
                }
            }
        }

    }

    public final void initializeSolutionCount(boolean before) {
        long[][] table = (before ? beforeNumBestSolutions : afterNumBestSolutions);
        for (int e_P = 0; e_P < table.length; e_P++) {
            if (problem.phi.hasAHost(e_P)) {
                if (problem.phi.hasMultihostParasites()) {
                    for (Integer e_H : problem.phi.getHosts(e_P)) {
                        table[e_P][e_H] = 1;
                    }
                } else {
                    table[e_P][problem.phi.getHost(e_P)] = 1; // there is one way to put this there (base case)
                }
            }
        }
    }

    // obtain cost info from each table
    public abstract long getBeforeCostInfo(int e_P, int e_H);

    public abstract long getAfterCostInfo(int e_P, int e_H);

    // obtain cost info 
    public abstract long getBeforeCostInfo(int e_P, int name, Set<Integer> tips, int e_H);

    public abstract long getAfterCostInfo(int e_P, int name, Set<Integer> tips, int e_H);

    // set cost info in each table
    public abstract void zeroBefore(int e_P, SortedSet<Integer> e_H);

    public abstract void zeroBefore(int e_P, int e_H);

    public abstract void setBeforeCostInfo(int e_P, int e_H, long costInfo);

    public abstract void setAfterCostInfo(int e_P, int e_H, long costInfo);

    public abstract void setBeforeCostInfo(int e_P, int name, Set<Integer> tips, int e_H, long costInfo);

    public abstract void setAfterCostInfo(int e_P, int name, Set<Integer> tips, int e_H, long costInfo);

    /*
     * try to update the best cost for the ROOT. Hence, we don't need a
     * multihost option, since the root is in charge of all the tips of any name
     * it represents.
     */
    public abstract void candidateBeforeBestCostInfo(int e_P, int e_H);

    public abstract void candidateAfterBestCostInfo(int e_P, int e_H);

    // convert costInfo to cost for comparison only
    public abstract int toRelativeCost(long costInfo);

    public abstract int toRelativeCost(long costInfo, int firstName, Set<Integer> firstTips);

    public abstract int toRelativeCost(long costInfo, int firstName, Set<Integer> firstTips, int secondName, Set<Integer> secondTips);

    // add costInfo with the specified event
    public abstract long addEventToCostInfo(int eventType, long costInfo);
    
    // add costInfo with the specified event
    // e_H1 must be the take-off site and e_H2 must be the landing site
    public abstract long addHostSwitchEventToCostInfo(long costInfo, int e_H1, int e_H2);
    
    // add costInfo with the specified event
    // e_H1 must be the take-off site and e_H2 must be the landing site
    public abstract long addInfestationEventToCostInfo(long costInfo, int e_H1, int e_H2);

    // add two costInfo together
    // for host-switch and infestation event,
    // costInfo1 must be from the take-off site and
    // costInfo2 must be from the landing site
    public abstract long addCostInfo(long costInfo1, long costInfo2);

    // return the best cost
    public abstract int bestCost();

    // update time for each table
    public abstract void decreaseBeforeTime();

    public abstract void decreaseAfterTime();

    // release memory after solving
    public abstract void doneSolving();

    // obtain the contents of after[currentAfterTime][e_P][e_H]
    // this will always return an actual cost, not simply a costinfo
    public abstract int getFromAfter(int e_P, int e_H);

    // get the possible configurations.  return null if there are none.
    public abstract MultihostConfiguration getBeforeConfiguration(int e_P, int e_H);

    public abstract MultihostConfiguration getAfterConfiguration(int e_P, int e_H);

    // returns true if e_P could be a multihost tip
    public abstract boolean isPossiblyMultihost(int e_P);
}
