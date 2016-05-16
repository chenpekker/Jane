/* This class takes care of infestation for non-regioned / reconstructable case
 */

package edu.hmc.jane.solving;

import edu.hmc.jane.SimpleCostModel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set; 

class UniformCostReconstructableIFS extends InfestationSelector {

    int[][] bestInfestationCost, bestInfestationLocation;     // best infestation info
    int[][] sBestInfestationCost, sBestInfestationLocation;   // second best infestation info
    int infestationCost;
    int parasiteTreeSize;           // number of parasites to check through
    int tipTime;                    // number of times to check through
    private HashMap<Integer, MultihostConfiguration[]> bestMultiInfestationCost, bestMultiInfestationLocation;     // holder of multihost best infestation info
    private HashMap<Integer, MultihostConfiguration[]> sBestMultiInfestationCost, sBestMultiInfestationLocation;     // holder of multihost best infestation info
    private MultihostConfiguration[] currentMatrix;

    public UniformCostReconstructableIFS (ArrayDP3 problem) {
        super(problem);
        infestationCost = ((SimpleCostModel)problem.costModel).getInfestationCost();
        // initialize tables
        tipTime = problem.hostTiming.tipTime;
        parasiteTreeSize = problem.parasiteTree.size;
        bestInfestationCost = new int[tipTime + 1][parasiteTreeSize];
        bestInfestationLocation = new int[tipTime + 1][parasiteTreeSize];
        sBestInfestationCost = new int[tipTime + 1][parasiteTreeSize];
        sBestInfestationLocation = new int[tipTime + 1][parasiteTreeSize];
        clearTable();
        
        bestMultiInfestationCost = new HashMap<Integer, MultihostConfiguration[]>();
        bestMultiInfestationLocation = new HashMap<Integer, MultihostConfiguration[]>();
        sBestMultiInfestationCost = new HashMap<Integer, MultihostConfiguration[]>();
        sBestMultiInfestationLocation = new HashMap<Integer, MultihostConfiguration[]>();
        // initialize multihost parasite maps
        if (problem.hasMultihostEdges()) {    
            initMultis();
        }
    }
    
    private void clearTable() {
        for(int time = 0; time <= problem.hostTiming.tipTime; time++) {
            for(int e_P = 0; e_P < problem.parasiteTree.size; e_P++) {
                bestInfestationCost[time][e_P] = problem.costModel.INFINITY;
                bestInfestationLocation[time][e_P] = -1;
                sBestInfestationCost[time][e_P] = problem.costModel.INFINITY;
                sBestInfestationLocation[time][e_P] = -1;
            }
        }
    }
    
    // initialize the multihost configurations
    private void initMultis() {
        for (int e_P = 0; e_P < problem.parasiteTree.size; e_P++) {
            Set<Integer> names = problem.possibleNames(e_P);
            if (names.isEmpty()) {
                continue;
            }
            currentMatrix = new MultihostConfiguration[problem.hostTiming.tipTime];

            for (int t = 0; t < problem.hostTiming.tipTime; t++) {
                LinkedList<Integer> nameList = new LinkedList<Integer>();
                LinkedList<Set<Integer>> tipsList = new LinkedList<Set<Integer>>();
                for (int name : names) {
                    Set<Integer> tips = problem.phi.getHosts(name);
                    nameList.add(name);
                    tipsList.add(tips);
                }
                currentMatrix[t] = new MultihostConfiguration(e_P, nameList, tipsList);
            }
            bestMultiInfestationCost.put(e_P, currentMatrix);
            bestMultiInfestationLocation.put(e_P, currentMatrix);
            sBestMultiInfestationCost.put(e_P, currentMatrix);
            sBestMultiInfestationLocation.put(e_P, currentMatrix);
            for (int time = 0; time < problem.hostTiming.tipTime; time++) {
                for (int name : names) {
                    Set<Integer> tips = problem.phi.getHosts(name);
                    for (int tip: tips) {
                        bestMultiInfestationCost.get(e_P)[time].setCost(name, tips, problem.costModel.INFINITY);
                        bestMultiInfestationLocation.get(e_P)[time].setCost(name, tips, -1);
                        sBestMultiInfestationCost.get(e_P)[time].setCost(name, tips, problem.costModel.INFINITY);
                        sBestMultiInfestationLocation.get(e_P)[time].setCost(name, tips, -1);
                    }
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
        if (newInfestationCost < bestInfestationCost[time][e_P] &&
            e_H != bestInfestationLocation[time][e_P]) {
            // shift best to second best
            sBestInfestationCost[time][e_P] = bestInfestationCost[time][e_P];
            sBestInfestationLocation[time][e_P] = bestInfestationLocation[time][e_P];
            // update best with new cost
            bestInfestationCost[time][e_P] = newInfestationCost;
            bestInfestationLocation[time][e_P] = e_H;
        } else if (newInfestationCost < bestInfestationCost[time][e_P]) {
            // update best only since e_H does not change
            bestInfestationCost[time][e_P] = newInfestationCost;
        } else if (e_H != bestInfestationLocation[time][e_P] &&
                   newInfestationCost < sBestInfestationCost[time][e_P]) {
            // update second best if not same e_H as best
            sBestInfestationCost[time][e_P] = newInfestationCost;
            sBestInfestationLocation[time][e_P] = e_H;
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
        if (newInfestationCost < bestMultiInfestationCost.get(e_P)[time].getCost(name, tips) &&
            e_H != bestMultiInfestationLocation.get(e_P)[time].getCost(name, tips)) {
            // shift best to second best
            sBestMultiInfestationCost.get(e_P)[time].setCost(name, tips, bestMultiInfestationCost.get(e_P)[time].getCost(name, tips));
            sBestMultiInfestationLocation.get(e_P)[time].setCost(name, tips, bestMultiInfestationLocation.get(e_P)[time].getCost(name, tips));
            // update best with new cost
            bestMultiInfestationCost.get(e_P)[time].setCost(name, tips, newInfestationCost);
            bestMultiInfestationLocation.get(e_P)[time].setCost(name, tips, e_H);
        } else if (newInfestationCost < bestMultiInfestationCost.get(e_P)[time].getCost(name, tips)) {
            // update best only since e_H does not change
            bestMultiInfestationCost.get(e_P)[time].setCost(name, tips, newInfestationCost);
        } else if (e_H != bestMultiInfestationLocation.get(e_P)[time].getCost(name, tips) &&
                   newInfestationCost < sBestMultiInfestationCost.get(e_P)[time].getCost(name, tips)) {
            // update second best if not same e_H as best
            sBestMultiInfestationCost.get(e_P)[time].setCost(name, tips, newInfestationCost);
            sBestMultiInfestationLocation.get(e_P)[time].setCost(name, tips, e_H);
        }
    }

    @Override
    public final int findBestInfestationLocation(int time, int e_P, int e_H) {
        // we do not allow the take-off and landing site to be the same
        if (e_H == bestInfestationLocation[time][e_P]) {// use second best instead
            return sBestInfestationLocation[time][e_P];
        }
        // otherwise, use the best one
        return bestInfestationLocation[time][e_P];
    }
    
    @Override
    public final int findBestInfestationLocation(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // we do not allow the take-off and landing site to be the same
        if (e_H == (int) bestMultiInfestationLocation.get(e_P)[time].getCost(name, tips)) {// use second best instead
            return (int) sBestMultiInfestationLocation.get(e_P)[time].getCost(name, tips);
        }
        // otherwise, use the best one
        return (int) bestMultiInfestationLocation.get(e_P)[time].getCost(name, tips);
    }

    @Override
    public final int getBestInfestationLocation(int time, int e_P, int e_H) {
        // same function
        return findBestInfestationLocation(time, e_P, e_H);
    }
    
    @Override
    public final int getBestInfestationLocation(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // same function
        return findBestInfestationLocation(time, e_P, name, tips, e_H);
    }
    
    @Override
    public final int findBestInfestationCost(int time, int e_P, int e_H) {
        // we do not allow the take-off and landing site to be the same
        if (e_H == bestInfestationLocation[time][e_P]) {// use second best instead
            return sBestInfestationCost[time][e_P];
        }
        // otherwise, use the best one
        return bestInfestationCost[time][e_P];
    }
    
    @Override
    public final int findBestInfestationCost(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // we do not allow the take-off and landing site to be the same
        if (e_H == (int) bestMultiInfestationLocation.get(e_P)[time].getCost(name, tips)) {// use second best instead
            return (int) sBestMultiInfestationCost.get(e_P)[time].getCost(name, tips);
        }
        // otherwise, use the best one
        return (int) bestMultiInfestationCost.get(e_P)[time].getCost(name, tips);
    }

    @Override
    public final int getBestInfestationCost(int time, int e_P, int e_H) {
        // same function
        return findBestInfestationCost(time, e_P, e_H);
    }
    
    @Override
    public final int getBestInfestationCost(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // same function
        return findBestInfestationCost(time, e_P, name, tips, e_H);
    }

    @Override
    public final void decreaseAfterTime() {
        // Nothing to be done
    }

    @Override
    public final void doneSolving() {
        // Nothing to be done
    }
}
