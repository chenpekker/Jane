/* This class takes care of host switch for non-regioned / reconstructable case
 */

package edu.hmc.jane.solving;

import edu.hmc.jane.SimpleCostModel;
import java.util.*;
class UniformCostReconstructableHSS extends HostSwitchSelector {

    int[][] bestSwitchCost, sBestSwitchCost;          //First and second best switch costs
    int[][] bestSwitchLocation; //Stores the chosen switch location once ties are broken
    ArrayList<Integer>[][] bestSwitchLocations, sBestSwitchLocations;     // first and second best switch locations
    
    int switchCost;
    int parasiteTreeSize;           // number of parasites to check through
    int tipTime;                    // number of times to check through
    
    private HashMap<Integer, MultihostConfiguration[]> bestMultiSwitchCost, bestMultiSwitchLocation;     // holder of multihost best switch info
    private HashMap<Integer, MultihostConfiguration[]> sBestMultiSwitchCost, sBestMultiSwitchLocation;     // holder of multihost best switch info
    private MultihostConfiguration[] currentMatrix;
    
    public UniformCostReconstructableHSS (ArrayDP3 problem) {
        super(problem);
        switchCost = ((SimpleCostModel)problem.costModel).getHostSwitchCost();
        // initialize tables
        tipTime = problem.hostTiming.tipTime;
        parasiteTreeSize = problem.parasiteTree.size;
        bestSwitchCost = new int[tipTime + 1][parasiteTreeSize];
        bestSwitchLocations = new ArrayList[tipTime + 1][parasiteTreeSize];
        sBestSwitchCost = new int[tipTime + 1][parasiteTreeSize];
        sBestSwitchLocations = new ArrayList[tipTime + 1][parasiteTreeSize];
        bestSwitchLocation = new int[tipTime +1][parasiteTreeSize];
        clearTable();
        
        bestMultiSwitchCost = new HashMap<Integer, MultihostConfiguration[]>();
        bestMultiSwitchLocation = new HashMap<Integer, MultihostConfiguration[]>();
        sBestMultiSwitchCost = new HashMap<Integer, MultihostConfiguration[]>();
        sBestMultiSwitchLocation = new HashMap<Integer, MultihostConfiguration[]>();
        // initialize multihost parasite maps
        if (problem.hasMultihostEdges()) {    
            initMultis();
        }
    }
    
    private void clearTable() {
        for(int time = 0; time <= problem.hostTiming.tipTime; time++) {
            for(int e_P = 0; e_P < problem.parasiteTree.size; e_P++) {
                bestSwitchCost[time][e_P] = problem.costModel.INFINITY;
                bestSwitchLocations[time][e_P] = new ArrayList<Integer>();
                sBestSwitchCost[time][e_P] = problem.costModel.INFINITY;
                sBestSwitchLocations[time][e_P] = new ArrayList<Integer>();
                bestSwitchLocation[time][e_P] = -2;
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
            bestMultiSwitchCost.put(e_P, currentMatrix);
            bestMultiSwitchLocation.put(e_P, currentMatrix);
            sBestMultiSwitchCost.put(e_P, currentMatrix);
            sBestMultiSwitchLocation.put(e_P, currentMatrix);
            for (int time = 0; time < problem.hostTiming.tipTime; time++) {
                for (int name : names) {
                    Set<Integer> tips = problem.phi.getHosts(name);
                    for (int tip: tips) {
                        bestMultiSwitchCost.get(e_P)[time].setCost(name, tips, problem.costModel.INFINITY);
                        bestMultiSwitchLocation.get(e_P)[time].setCost(name, tips, -1);
                        sBestMultiSwitchCost.get(e_P)[time].setCost(name, tips, problem.costModel.INFINITY);
                        sBestMultiSwitchLocation.get(e_P)[time].setCost(name, tips, -1);
                    }
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
        if (newSwitchCost < bestSwitchCost[time][e_P]) {
            // shift best to second best
            if (!bestSwitchLocations[time][e_P].contains(e_H) || bestSwitchLocations[time][e_P].size() != 1) { //Check that we're not moving only e_H to second best
                sBestSwitchCost[time][e_P] = bestSwitchCost[time][e_P];
                sBestSwitchLocations[time][e_P].clear();
                sBestSwitchLocations[time][e_P].addAll(bestSwitchLocations[time][e_P]);
            }
            if (sBestSwitchLocations[time][e_P].contains(e_H)) //remove e_H from second best since it's in best
                sBestSwitchLocations[time][e_P].remove(sBestSwitchLocations[time][e_P].indexOf(e_H));
            // update best with new cost
            bestSwitchCost[time][e_P] = newSwitchCost;
            bestSwitchLocations[time][e_P].clear();
            bestSwitchLocations[time][e_P].add(e_H);
        } else if (newSwitchCost == bestSwitchCost[time][e_P]) {
            //Don't add e_H if it's already there
            if (!bestSwitchLocations[time][e_P].contains(e_H))
                bestSwitchLocations[time][e_P].add(e_H);
        } else if (newSwitchCost < sBestSwitchCost[time][e_P] && !bestSwitchLocations[time][e_P].contains(e_H)) {
            // update second best if not same e_H as best
            sBestSwitchCost[time][e_P] = newSwitchCost;
            sBestSwitchLocations[time][e_P].clear();
            sBestSwitchLocations[time][e_P].add(e_H);
        } else if (newSwitchCost == sBestSwitchCost[time][e_P]) {
            //Don't add e_H if it's already there
            if (!sBestSwitchLocations[time][e_P].contains(e_H))
                sBestSwitchLocations[time][e_P].add(e_H);
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
        if (newSwitchCost < bestMultiSwitchCost.get(e_P)[time].getCost(name, tips) && e_H != bestMultiSwitchLocation.get(e_P)[time].getCost(name, tips)) {
            // shift best to second best
            sBestMultiSwitchCost.get(e_P)[time].setCost(name, tips, bestMultiSwitchCost.get(e_P)[time].getCost(name, tips));
            sBestMultiSwitchLocation.get(e_P)[time].setCost(name, tips, bestMultiSwitchLocation.get(e_P)[time].getCost(name, tips));
            // update best with new cost
            bestMultiSwitchCost.get(e_P)[time].setCost(name, tips, newSwitchCost);
            bestMultiSwitchLocation.get(e_P)[time].setCost(name, tips, e_H);
        } else if (newSwitchCost < bestMultiSwitchCost.get(e_P)[time].getCost(name, tips)) {
            // update best only since e_H does not change
            bestMultiSwitchCost.get(e_P)[time].setCost(name, tips, newSwitchCost);
        } else if (e_H != bestMultiSwitchLocation.get(e_P)[time].getCost(name, tips) && newSwitchCost < sBestMultiSwitchCost.get(e_P)[time].getCost(name, tips)) {
            // update second best if not same e_H as best
            sBestMultiSwitchCost.get(e_P)[time].setCost(name, tips, newSwitchCost);
            sBestMultiSwitchLocation.get(e_P)[time].setCost(name, tips, e_H);
        }
    }

    /*
     * Fix this such that it gives something else from bestSwitchLocations if necessary instead of sBest
     */
    @Override
    public final int findBestSwitchLocation(int time, int e_P, int e_H) {
        // we do not allow the take-off and landing site to be the same
        
        for (Integer i : bestSwitchLocations[time][e_P]) {
            if (i != e_H)
                return i;
        }
            
        if (sBestSwitchLocations[time][e_P].isEmpty())
            return -1;
        return sBestSwitchLocations[time][e_P].get(0);
    }
    
    @Override
    public final int findBestSwitchLocation(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // we do not allow the take-off and landing site to be the same
        if (e_H == (int) bestMultiSwitchLocation.get(e_P)[time].getCost(name, tips)) {// use second best instead
            return (int) sBestMultiSwitchLocation.get(e_P)[time].getCost(name, tips);
        }
        // otherwise, use the best one
        return (int) bestMultiSwitchLocation.get(e_P)[time].getCost(name, tips);
    }

    @Override
    public final int getBestSwitchLocation(int time, int e_P, int e_H) {
        // same function
        if (bestSwitchLocation[time][e_P] != -2)
                return bestSwitchLocation[time][e_P];
        return findBestSwitchLocation(time, e_P, e_H);
    }
    
    @Override
    public final int getBestSwitchLocation(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // same function
        return findBestSwitchLocation(time, e_P, name, tips, e_H);
    }   

    public final ArrayList<Integer> getBestSwitchLocations(int time, int e_P, int e_H) {
        if (bestSwitchLocations[time][e_P].isEmpty())
            return sBestSwitchLocations[time][e_P];
        if (bestSwitchLocations[time][e_P].contains(e_H) && bestSwitchLocations[time][e_P].size() == 1) {
            return sBestSwitchLocations[time][e_P];
        } else if (bestSwitchLocations[time][e_P].contains(e_H)) {
            ArrayList solution = new ArrayList();
            solution.addAll(bestSwitchLocations[time][e_P]);
            solution.remove(bestSwitchLocations[time][e_P].indexOf(e_H));
            return solution;
        }
        return bestSwitchLocations[time][e_P];
    }
    
    @Override
    public final int findBestSwitchCost(int time, int e_P, int e_H) {
        // we do not allow the take-off and landing site to be the same
        for( int i: bestSwitchLocations[time][e_P]) {
            if (i != e_H)
                return bestSwitchCost[time][e_P];
        }
        // otherwise, use the second best one
        return sBestSwitchCost[time][e_P];
    }
    
    
    @Override
    public final int findBestSwitchCost(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // we do not allow the take-off and landing site to be the same
        if (e_H == (int) bestMultiSwitchLocation.get(e_P)[time].getCost(name, tips)) {// use second best instead
            return (int) sBestMultiSwitchCost.get(e_P)[time].getCost(name, tips);
        }
        // otherwise, use the best one
        return (int) bestMultiSwitchCost.get(e_P)[time].getCost(name, tips);
    }
    
    public final void setBestSwitchLocation(int time, int e_P, int e_H) {
        bestSwitchLocation[time][e_P] = e_H;   
    }

    @Override
    public final int getBestSwitchCost(int time, int e_P, int e_H) {
        // same function
        return findBestSwitchCost(time, e_P, e_H);
    }
    
    @Override
    public final int getBestSwitchCost(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // same function
        return findBestSwitchCost(time, e_P, name, tips, e_H);
    }

    @Override
    public final void decreaseAfterTime() {
        // nothing to be done
    }

    @Override
    public final void doneSolving() {
        // nothing to be done
    }
}
