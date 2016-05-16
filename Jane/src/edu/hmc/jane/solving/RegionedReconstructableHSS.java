/* This class takes care of host switch for regioned / reconstructable case */
package edu.hmc.jane.solving;

import edu.hmc.jane.CostModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

class RegionedReconstructableHSS extends HostSwitchSelector {

    ArrayList<Integer>[][][] bestSwitchLocations;     // store all computed switch location
    int[][][] after;                  // whole DP after table
    
    int[][] bestSwitchLocation; //Indexed by time and e_P
    
    private HashMap<Integer, MultihostConfiguration[][]> bestMultiSwitchLocation;     // store all computed MultiSwitch location
    private HashMap<Integer, MultihostConfiguration[][]> afterMulti;                  // whole DP afterMulti HashMap
    private MultihostConfiguration[][] currentMatrix;
    
    boolean[] hostAlive;            // keep track which host is alive
    int hostTreeSize;               // number of hosts to check through
    int parasiteTreeSize;           // number of parasites to check through
    int tipTime;                    // number of times to check through
    CostModel costModel;            // information for computing costs

    public RegionedReconstructableHSS(ArrayDP3 problem) {
        super(problem);

        // obtain necessary information
        tipTime = problem.hostTiming.tipTime;
        parasiteTreeSize = problem.parasiteTree.size;
        hostTreeSize = problem.hostTree.size;
        hostAlive = problem.hostAlive;
        costModel = problem.costModel;

        // note that to reconstruct, table needs to be 3D
        after = ((ThreeDimDPTable) problem.table).after;
        // note that to reconstruct, hashMap needs to be 3D
        if (problem.hasMultihostEdges())
            afterMulti = ((ThreeDimDPTable) problem.table).afterMulti;

        // initialize table
        bestSwitchLocations = new ArrayList[tipTime + 1][parasiteTreeSize][hostTreeSize];
        bestSwitchLocation = new int[tipTime+1][parasiteTreeSize];
        for (int time = 0; time <= tipTime; time++) {
            for (int e_P = 0; e_P < parasiteTreeSize; e_P++) {
                bestSwitchLocation[time][e_P] = -2;
                for (int e_H = 0; e_H < hostTreeSize; e_H++) {
                    bestSwitchLocations[time][e_P][e_H] = new ArrayList<Integer>();
                }
            }
        }
        
        bestMultiSwitchLocation = new HashMap<Integer, MultihostConfiguration[][]>();
        // initialize hashMap
        if (problem.hasMultihostEdges()) {
            for (int e_P = 0; e_P < parasiteTreeSize; e_P++) {
                Set<Integer> names = problem.possibleNames(e_P);
                if (names.isEmpty()) {
                    continue;
                }
                currentMatrix = new MultihostConfiguration[tipTime][hostTreeSize];

                for (int t = 0; t < tipTime; t++) {
                    for (int e_H = 0; e_H < hostTreeSize; e_H++) {
                        LinkedList<Integer> nameList = new LinkedList<Integer>();
                        LinkedList<Set<Integer>> tipsList = new LinkedList<Set<Integer>>();
                        for (int name : names) {
                            Set<Integer> tips = problem.phi.getHosts(name);
                            nameList.add(name);
                            tipsList.add(tips);
                        }
                        currentMatrix[t][e_H] = new MultihostConfiguration(e_P, nameList, tipsList);
                    }
                }
                bestMultiSwitchLocation.put(e_P, currentMatrix);
                for (int t = 0; t < tipTime; t++) {
                    for (int e_H = 0; e_H < hostTreeSize; e_H++) {
                        for (int time = 0; time < problem.hostTiming.tipTime; time++) {
                            for (int name : names) {
                                Set<Integer> tips = problem.phi.getHosts(name);
                                for (int tip: tips) {
                                    bestMultiSwitchLocation.get(e_P)[time][e_H].setCost(name, tips, -1);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public final void updateBestSwitch(int time, int e_P, int e_H, int newCost) {
        // Nothing to be done
    }
    
    @Override
    public final void updateBestSwitch(int time, int e_P, int name, Set<Integer> tips, int e_H, int newCost) {
        // Nothing to be done
    }

    @Override
    public final int findBestSwitchLocation(int time, int e_P, int e_H) {
        // set initial cost
        int bestCost = costModel.INFINITY;
        // loop through all possible landing sites
        for (int e_Hp = 0; e_Hp < hostTreeSize; e_Hp++) {
            if (hostAlive[e_Hp] && e_Hp != e_H) {
                /*
                 * If we shouldn't switch onto that edge due to polytomies.
                 */
                if (this.problem.hostTree.noMidPolyEvents && problem.hostTree.isPolytomyEdge(e_Hp)) {
                    continue;
                }
                // compute cost and update if better
                int switchCost = costModel.getHostSwitchCost(e_H, e_Hp);
                if (costModel.isInfinity(switchCost)) {
                    continue;
                }
                int embedCost = after[time][e_P][e_Hp];
                if (costModel.isInfinity(embedCost)) {
                    continue;
                }
                int newCost = embedCost + switchCost;
                if (newCost < bestCost) {
                    bestCost = newCost;
                    bestSwitchLocations[time][e_P][e_H].clear();
                    bestSwitchLocations[time][e_P][e_H].add(e_Hp);
                } else if (newCost == bestCost) {
                    bestSwitchLocations[time][e_P][e_H].add(e_Hp);
                }
            }
        }
        
        // we do not allow the take-off and landing site to be the same
        for (Integer i : bestSwitchLocations[time][e_P][e_H]) {
            if (i != e_H)
                return i;
        }
        return -1;
    }
    
    @Override
    public final int findBestSwitchLocation(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // set initial cost
        int bestCost = costModel.INFINITY;
        // loop through all possible landing sites
        for (int e_Hp = 0; e_Hp < hostTreeSize; e_Hp++) {
            if (hostAlive[e_Hp] && e_Hp != e_H) {
                /*
                 * If we shouldn't switch onto that edge due to polytomies.
                 */
                if (this.problem.hostTree.noMidPolyEvents && problem.hostTree.isPolytomyEdge(e_Hp)) {
                    continue;
                }
                // compute cost and update if better
                int switchCost = costModel.getHostSwitchCost(e_H, e_Hp);
                if (costModel.isInfinity(switchCost)) {
                    continue;
                }
                int embedCost = (int) afterMulti.get(e_P)[time][e_H].getCost(name, tips);
                if (costModel.isInfinity(embedCost)) {
                    continue;
                }
                int newCost = embedCost + switchCost;
                if (newCost < bestCost) {
                    bestCost = newCost;
                    bestMultiSwitchLocation.get(e_P)[time][e_H].setCost(name, tips, e_Hp);
                }
            }
        }
        return (int) bestMultiSwitchLocation.get(e_P)[time][e_H].getCost(name, tips);
    }
 
    @Override
    public final int getBestSwitchLocation(int time, int e_P, int e_H) {
        // same function
        if ( bestSwitchLocation[time][e_P] != -2)
            return bestSwitchLocation[time][e_P];
        return findBestSwitchLocation(time, e_P, e_H);
    }
    
    public final void setBestSwitchLocation(int time, int e_P, int e_H){
        bestSwitchLocation[time][e_P] = e_H;
    }
    
    @Override
    public final int getBestSwitchLocation(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // same function
        return findBestSwitchLocation(time, e_P, name, tips, e_H);
    }
    
    public final ArrayList<Integer> getBestSwitchLocations(int time, int e_P, int e_H) {
        return bestSwitchLocations[time][e_P][e_H];
    }
    
    @Override
    public final int findBestSwitchCost(int time, int e_P, int e_H) {
        // use findBestSwitchLocation, but returns infinity if impossible
        int switchLocation = findBestSwitchLocation(time, e_P, e_H);
        return (switchLocation == -1
                ? costModel.INFINITY
                : after[time][e_P][switchLocation] + costModel.getHostSwitchCost(e_H, switchLocation));
    }
    
    @Override
    public final int findBestSwitchCost(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // use findBestSwitchLocation, but returns infinity if impossible
        int switchLocation = findBestSwitchLocation(time, e_P, name, tips, e_H);
        return (switchLocation == -1
                ? costModel.INFINITY
                : ((int) afterMulti.get(e_P)[time][switchLocation].getCost(name, tips)) + costModel.getHostSwitchCost(e_H, switchLocation));
    }
    
    @Override
    public final int getBestSwitchCost(int time, int e_P, int e_H) {
        // use getBestSwitchLocation, but returns infinity if impossible
        int switchLocation = getBestSwitchLocation(time, e_P, e_H);
        return (switchLocation == -1
                ? costModel.INFINITY
                : after[time][e_P][switchLocation] + costModel.getHostSwitchCost(e_H, switchLocation));
    }
    
    @Override
    public final int getBestSwitchCost(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // use getBestSwitchLocation, but returns infinity if impossible
        int switchLocation = getBestSwitchLocation(time, e_P, name, tips, e_H);
        return (switchLocation == -1
                ? costModel.INFINITY
                : ((int) afterMulti.get(e_P)[time][switchLocation].getCost(name, tips)) + costModel.getHostSwitchCost(e_H, switchLocation));
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
