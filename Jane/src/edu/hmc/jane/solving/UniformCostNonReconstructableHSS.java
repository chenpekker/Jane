/* This class takes care of host switch for non-regioned / non-reconstructable case */

package edu.hmc.jane.solving;

import edu.hmc.jane.SimpleCostModel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

class UniformCostNonReconstructableHSS extends HostSwitchSelector {

    int parasiteTreeSize;   // size of tables

    int[] bestSwitchCost, bestSwitchLocation;     // best switch info
    int[] sBestSwitchCost, sBestSwitchLocation;   // second best switch info
    int switchCost;
    private HashMap<Integer, MultihostConfiguration> bestMultiSwitchCost, bestMultiSwitchLocation;     // holder of multihost best switch info
    private HashMap<Integer, MultihostConfiguration> sBestMultiSwitchCost, sBestMultiSwitchLocation;   // holder of multihost second best switch info
    private MultihostConfiguration current;
    
    public UniformCostNonReconstructableHSS (ArrayDP3 problem) {
        super(problem);
        switchCost = ((SimpleCostModel)problem.costModel).getHostSwitchCost();
        // initialize tables
        parasiteTreeSize = problem.parasiteTree.size;
        bestSwitchCost = new int[parasiteTreeSize];
        bestSwitchLocation = new int[parasiteTreeSize];
        sBestSwitchCost = new int[parasiteTreeSize];
        sBestSwitchLocation = new int[parasiteTreeSize];
        clearTable();
        
        bestMultiSwitchCost = new HashMap<Integer, MultihostConfiguration>();
        bestMultiSwitchLocation = new HashMap<Integer, MultihostConfiguration>();
        sBestMultiSwitchCost = new HashMap<Integer, MultihostConfiguration>();
        sBestMultiSwitchLocation = new HashMap<Integer, MultihostConfiguration>();
        // initialize multihost parasite maps
        if (problem.hasMultihostEdges()) {         
            initMultis();
        }
    }

    private void clearTable() {
        for (int e_P = 0; e_P < parasiteTreeSize; e_P++) {
            bestSwitchCost[e_P] = problem.costModel.INFINITY;
            bestSwitchLocation[e_P] = -1;
            sBestSwitchCost[e_P] = problem.costModel.INFINITY;
            sBestSwitchLocation[e_P] = -1;
        }
    }
    
    // initialize the multihost configurations
    private void initMultis() {
        for (int e_P = 0; e_P < parasiteTreeSize; e_P++) {
            // for each parasite...
            Set<Integer> names = problem.possibleNames(e_P);
            if (names.isEmpty()) {
                // don't consider cases where this couldn't be mapped to multiple parasites
                continue;
            }

            // an array for a configuration for each host on this parasite
            LinkedList<Integer> nameList = new LinkedList<Integer>();
            LinkedList<Set<Integer>> tipsList = new LinkedList<Set<Integer>>();
            for (int name : names) {
                Set<Integer> tips = problem.phi.getHosts(name);
                nameList.add(name);
                tipsList.add(tips);
            }
            current = new MultihostConfiguration(e_P, nameList, tipsList);
            bestMultiSwitchCost.put(e_P, current);
            bestMultiSwitchLocation.put(e_P, current);
            sBestMultiSwitchCost.put(e_P, current);
            sBestMultiSwitchLocation.put(e_P, current);
            for (int name : names) {
                Set<Integer> tips = problem.phi.getHosts(name);
                for (int tip: tips) {
                    bestMultiSwitchCost.get(e_P).setCost(name, tips, problem.costModel.INFINITY);
                    bestMultiSwitchLocation.get(e_P).setCost(name, tips, -1);
                    sBestMultiSwitchCost.get(e_P).setCost(name, tips, problem.costModel.INFINITY);
                    sBestMultiSwitchLocation.get(e_P).setCost(name, tips, -1);
                }
            }
        }
    }
    
    @Override
    public final void updateBestSwitch(int time, int e_P, int e_H, int newCost) {
        /* If we shouldn't switch onto that edge due to polytomies.
         */
        if (this.problem.hostTree.noMidPolyEvents && problem.hostTree.isPolytomyEdge(e_H)) {
            return;
        }
        int newSwitchCost = newCost + switchCost;
        if (newSwitchCost < bestSwitchCost[e_P] && e_H != bestSwitchLocation[e_P]) {
            // shift best to second best
            sBestSwitchCost[e_P] = bestSwitchCost[e_P];
            sBestSwitchLocation[e_P] = bestSwitchLocation[e_P];
            // update best with new cost
            bestSwitchCost[e_P] = newSwitchCost;
            bestSwitchLocation[e_P] = e_H;
        } else if (newSwitchCost < bestSwitchCost[e_P]) {
            // update best only since e_H does not change
            bestSwitchCost[e_P] = newSwitchCost;
        } else if (e_H != bestSwitchLocation[e_P] && newSwitchCost < sBestSwitchCost[e_P]) {
            // update second best if not same e_H as best
            sBestSwitchCost[e_P] = newSwitchCost;
            sBestSwitchLocation[e_P] = e_H;
        }
    }
    
    @Override
    public final void updateBestSwitch(int time, int e_P, int name, Set<Integer> tips, int e_H, int newCost) {
        /* If we shouldn't switch onto that edge due to polytomies.
         */
        if (this.problem.hostTree.noMidPolyEvents && problem.hostTree.isPolytomyEdge(e_H)) {
            return;
        }
        int newSwitchCost = newCost + switchCost;
        if (newSwitchCost < bestMultiSwitchCost.get(e_P).getCost(name, tips) && e_H != bestMultiSwitchLocation.get(e_P).getCost(name, tips)) {
            // shift best to second best
            sBestMultiSwitchCost.get(e_P).setCost(name, tips, bestMultiSwitchCost.get(e_P).getCost(name, tips));
            sBestMultiSwitchLocation.get(e_P).setCost(name, tips, bestMultiSwitchLocation.get(e_P).getCost(name, tips));
            // update best with new cost
            bestMultiSwitchCost.get(e_P).setCost(name, tips, newSwitchCost);
            bestMultiSwitchLocation.get(e_P).setCost(name, tips, e_H);
        } else if (newSwitchCost < bestMultiSwitchCost.get(e_P).getCost(name, tips)) {
            // update best only since e_H does not change
            bestMultiSwitchCost.get(e_P).setCost(name, tips, newSwitchCost);
        } else if (e_H != bestMultiSwitchLocation.get(e_P).getCost(name, tips) && newSwitchCost < sBestMultiSwitchCost.get(e_P).getCost(name, tips)) {
            // update second best if not same e_H as best
            sBestMultiSwitchCost.get(e_P).setCost(name, tips, newSwitchCost);
            sBestMultiSwitchLocation.get(e_P).setCost(name, tips, e_H);
        }
    }

    @Override
    public final int findBestSwitchLocation(int time, int e_P, int e_H) {
        // we do not allow the take-off and landing site to be the same
        if (e_H == bestSwitchLocation[e_P]) // use second best instead
        {
            return sBestSwitchLocation[e_P];
        }
        // otherwise, use the best one
        return bestSwitchLocation[e_P];
    }
    
    @Override
    public final int findBestSwitchLocation(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // we do not allow the take-off and landing site to be the same
        if (e_H == (int) bestMultiSwitchLocation.get(e_P).getCost(name, tips)) // use second best instead
        {
            return (int) sBestMultiSwitchLocation.get(e_P).getCost(name, tips);
        }
        // otherwise, use the best one
        return (int) bestMultiSwitchLocation.get(e_P).getCost(name, tips);
    }

    @Override
    public final int getBestSwitchLocation(int time, int e_P, int e_H) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public final int getBestSwitchLocation(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public final int findBestSwitchCost(int time, int e_P, int e_H) {
        // we do not allow the take-off and landing site to be the same
        if (e_H == bestSwitchLocation[e_P]) // use second best instead
        {
            return sBestSwitchCost[e_P];
        }
        // otherwise, use the best one
        return bestSwitchCost[e_P];
    }
    
    @Override
    public final int findBestSwitchCost(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // we do not allow the take-off and landing site to be the same
        if (e_H == (int) bestMultiSwitchLocation.get(e_P).getCost(name, tips)) {// use second best instead
            return (int) sBestMultiSwitchCost.get(e_P).getCost(name, tips);
        }
        // otherwise, use the best one
        return (int) bestMultiSwitchCost.get(e_P).getCost(name, tips);
    }
    
    @Override
    public final int getBestSwitchCost(int time, int e_P, int e_H) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public final int getBestSwitchCost(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public final void decreaseAfterTime() {
        clearTable();
        initMultis();
    }

    @Override
    public final void doneSolving() {
        // no reconstruction, so delete tables
        bestSwitchCost = null;
        sBestSwitchCost = null;
        bestSwitchLocation = null;
        sBestSwitchLocation = null;
        bestMultiSwitchCost = null;
        sBestMultiSwitchCost = null;
        bestMultiSwitchLocation = null;
        sBestMultiSwitchLocation = null;
        current = null;
    }
}
