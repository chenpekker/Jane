/* This class takes care of infestation for non-regioned / non-reconstructable case */

package edu.hmc.jane.solving;

import edu.hmc.jane.SimpleCostModel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

class UniformCostNonReconstructableIFS extends InfestationSelector {

    int parasiteTreeSize;   // size of tables

    int[] bestInfestationCost, bestInfestationLocation;     // best infestation info
    int[] sBestInfestationCost, sBestInfestationLocation;   // second best infestation info
    int infestationCost;
    private HashMap<Integer, MultihostConfiguration> bestMultiInfestationCost, bestMultiInfestationLocation;     // holder of multihost best infestation info
    private HashMap<Integer, MultihostConfiguration> sBestMultiInfestationCost, sBestMultiInfestationLocation;   // holder of multihost second best infestation info
    private MultihostConfiguration current;

    public UniformCostNonReconstructableIFS (ArrayDP3 problem) {
        super(problem);
        infestationCost = ((SimpleCostModel)problem.costModel).getInfestationCost();
        // initialize tables
        parasiteTreeSize = problem.parasiteTree.size;
        bestInfestationCost = new int[parasiteTreeSize];
        bestInfestationLocation = new int[parasiteTreeSize];
        sBestInfestationCost = new int[parasiteTreeSize];
        sBestInfestationLocation = new int[parasiteTreeSize];
        clearTable();
        
        bestMultiInfestationCost = new HashMap<Integer, MultihostConfiguration>();
        bestMultiInfestationLocation = new HashMap<Integer, MultihostConfiguration>();
        sBestMultiInfestationCost = new HashMap<Integer, MultihostConfiguration>();
        sBestMultiInfestationLocation = new HashMap<Integer, MultihostConfiguration>();
        // initialize multihost parasite maps
        if (problem.hasMultihostEdges()) {         
            initMultis();
        }
    }

    private void clearTable() {
        for (int e_P = 0; e_P < parasiteTreeSize; e_P++) {
            bestInfestationCost[e_P] = problem.costModel.INFINITY;
            bestInfestationLocation[e_P] = -1;
            sBestInfestationCost[e_P] = problem.costModel.INFINITY;
            sBestInfestationLocation[e_P] = -1;
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
            bestMultiInfestationCost.put(e_P, current);
            bestMultiInfestationLocation.put(e_P, current);
            sBestMultiInfestationCost.put(e_P, current);
            sBestMultiInfestationLocation.put(e_P, current);
            for (int name : names) {
                Set<Integer> tips = problem.phi.getHosts(name);
                for (int tip: tips) {
                    bestMultiInfestationCost.get(e_P).setCost(name, tips, problem.costModel.INFINITY);
                    bestMultiInfestationLocation.get(e_P).setCost(name, tips, -1);
                    sBestMultiInfestationCost.get(e_P).setCost(name, tips, problem.costModel.INFINITY);
                    sBestMultiInfestationLocation.get(e_P).setCost(name, tips, -1);
                }
            }
        }
    }

    @Override
    public final void updateBestInfestation(int time, int e_P, int e_H, int newCost) {
        /* If we shouldn't use infestation onto that edge due to polytomies.
         */
        if (this.problem.hostTree.noMidPolyEvents && problem.hostTree.isPolytomyEdge(e_H)) {
            return;
        }
        int newInfestationCost = newCost + infestationCost;
        if (newInfestationCost < bestInfestationCost[e_P] &&
            e_H != bestInfestationLocation[e_P]) {
            // shift best to second best
            sBestInfestationCost[e_P] = bestInfestationCost[e_P];
            sBestInfestationLocation[e_P] = bestInfestationLocation[e_P];
            // update best with new cost
            bestInfestationCost[e_P] = newInfestationCost;
            bestInfestationLocation[e_P] = e_H;
        } else if (newInfestationCost < bestInfestationCost[e_P]) {
            // update best only since e_H does not change
            bestInfestationCost[e_P] = newInfestationCost;
        } else if (e_H != bestInfestationLocation[e_P] &&
                   newInfestationCost < sBestInfestationCost[e_P]) {
            // update second best if not same e_H as best
            sBestInfestationCost[e_P] = newInfestationCost;
            sBestInfestationLocation[e_P] = e_H;
        }
    }
    
    @Override
    public final void updateBestInfestation(int time, int e_P, int name, Set<Integer> tips, int e_H, int newCost) {
        /* If we shouldn't infest onto that edge due to polytomies.
         */
        if (this.problem.hostTree.noMidPolyEvents && problem.hostTree.isPolytomyEdge(e_H)) {
            return;
        }
        int newInfestationCost = newCost + infestationCost;
        if (newInfestationCost < bestMultiInfestationCost.get(e_P).getCost(name, tips) &&
            e_H != bestMultiInfestationLocation.get(e_P).getCost(name, tips)) {
            // shift best to second best
            sBestMultiInfestationCost.get(e_P).setCost(name, tips, bestMultiInfestationCost.get(e_P).getCost(name, tips));
            sBestMultiInfestationLocation.get(e_P).setCost(name, tips, bestMultiInfestationLocation.get(e_P).getCost(name, tips));
            // update best with new cost
            bestMultiInfestationCost.get(e_P).setCost(name, tips, newInfestationCost);
            bestMultiInfestationLocation.get(e_P).setCost(name, tips, e_H);
        } else if (newInfestationCost < bestMultiInfestationCost.get(e_P).getCost(name, tips)) {
            // update best only since e_H does not change
            bestMultiInfestationCost.get(e_P).setCost(name, tips, newInfestationCost);
        } else if (e_H != bestMultiInfestationLocation.get(e_P).getCost(name, tips) &&
                   newInfestationCost < sBestMultiInfestationCost.get(e_P).getCost(name, tips)) {
            // update second best if not same e_H as best
            sBestMultiInfestationCost.get(e_P).setCost(name, tips, newInfestationCost);
            sBestMultiInfestationLocation.get(e_P).setCost(name, tips, e_H);
        }
    }
    
    @Override
    public final int findBestInfestationLocation(int time, int e_P, int e_H) {
        // we do not allow the take-off and landing site to be the same        
        if (e_H == bestInfestationLocation[e_P]) // use second best instead
        {
            return sBestInfestationLocation[e_P];
        }
        // otherwise, use the best one
        return bestInfestationLocation[e_P];
    }
    
    @Override
    public final int findBestInfestationLocation(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // we do not allow the take-off and landing site to be the same
        if (e_H == (int) bestMultiInfestationLocation.get(e_P).getCost(name, tips)) // use second best instead
        {
            return (int) sBestMultiInfestationLocation.get(e_P).getCost(name, tips);
        }
        // otherwise, use the best one
        return (int) bestMultiInfestationLocation.get(e_P).getCost(name, tips);
    }
    
    @Override
    public final int getBestInfestationLocation(int time, int e_P, int e_H) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public final int getBestInfestationLocation(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public final int findBestInfestationCost(int time, int e_P, int e_H) {
        // we do not allow the take-off and landing site to be the same
        if (e_H == bestInfestationLocation[e_P]) // use second best instead
        {
            return sBestInfestationCost[e_P];
        }
        // otherwise, use the best one
        return bestInfestationCost[e_P];
    }
    
    @Override
    public final int findBestInfestationCost(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // we do not allow the take-off and landing site to be the same
        if (e_H == (int) bestMultiInfestationLocation.get(e_P).getCost(name, tips)) {// use second best instead
            return (int) sBestMultiInfestationCost.get(e_P).getCost(name, tips);
        }
        // otherwise, use the best one
        return (int) bestMultiInfestationCost.get(e_P).getCost(name, tips);
    }

    @Override
    public final int getBestInfestationCost(int time, int e_P, int e_H) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public final int getBestInfestationCost(int time, int e_P, int name, Set<Integer> tips, int e_H) {
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
        bestInfestationCost = null;
        sBestInfestationCost = null;
        bestInfestationLocation = null;
        sBestInfestationLocation = null;
        bestMultiInfestationCost = null;
        sBestMultiInfestationCost = null;
        bestMultiInfestationLocation = null;
        sBestMultiInfestationLocation = null;
        current = null;
    }
}
