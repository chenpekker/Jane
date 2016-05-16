/* This class takes care of infestation for regioned / reconstructable case */
package edu.hmc.jane.solving;

import edu.hmc.jane.CostModel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

class RegionedReconstructableIFS extends InfestationSelector {

    int[][][] bestInfestationLocation;   // store all computed infestation location
    int[][][] after;                // whole DP after table
    private HashMap<Integer, MultihostConfiguration[][]> bestMultiInfestationLocation;     // store all computed MultiInfestation location
    private HashMap<Integer, MultihostConfiguration[][]> afterMulti;                  // whole DP afterMulti HashMap
    private MultihostConfiguration[][] currentMatrix;
    boolean[] hostAlive;            // keep track which host is alive
    int hostTreeSize;               // number of hosts to check through
    int parasiteTreeSize;           // number of parasites to check through
    int tipTime;                    // number of times to check through
    CostModel costModel;            // information for computing costs

    public RegionedReconstructableIFS(ArrayDP3 problem) {
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
        bestInfestationLocation = new int[tipTime + 1][parasiteTreeSize][hostTreeSize];
        for (int time = 0; time <= tipTime; time++) {
            for (int e_P = 0; e_P < parasiteTreeSize; e_P++) {
                for (int e_H = 0; e_H < hostTreeSize; e_H++) {
                    bestInfestationLocation[time][e_P][e_H] = -1;
                }
            }
        }
        
        bestMultiInfestationLocation = new HashMap<Integer, MultihostConfiguration[][]>();
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
                bestMultiInfestationLocation.put(e_P, currentMatrix);
                for (int t = 0; t < tipTime; t++) {
                    for (int e_H = 0; e_H < hostTreeSize; e_H++) {
                        for (int time = 0; time < problem.hostTiming.tipTime; time++) {
                            for (int name : names) {
                                Set<Integer> tips = problem.phi.getHosts(name);
                                for (int tip: tips) {
                                    bestMultiInfestationLocation.get(e_P)[time][e_H].setCost(name, tips, -1);
                                }
                            }
                        }
                    }
                }   
            }
        }
    }

    @Override
    public final void updateBestInfestation(int time, int e_P, int e_H, int newCost) {
        // Nothing to be done
    }
    
    @Override
    public final void updateBestInfestation(int time, int e_P, int name, Set<Integer> tips, int e_H, int newCost) {
        // Nothing to be done
    }

    @Override
    public final int findBestInfestationLocation(int time, int e_P, int e_H) {
        // set initial cost
        int bestCost = costModel.INFINITY;
        // loop through all possible landing sites
        for (int e_Hp = 0; e_Hp < hostTreeSize; e_Hp++) {
            if (hostAlive[e_Hp] && e_Hp != e_H) {
                /*
                 * If we shouldn't use infestation onto that edge due to polytomies.
                 */
                if (this.problem.hostTree.noMidPolyEvents && problem.hostTree.isPolytomyEdge(e_Hp)) {
                    continue;
                }
                // compute cost and update if better
                int infestationCost = costModel.getInfestationCost(e_H, e_Hp);
                if (costModel.isInfinity(infestationCost)) {
                    continue;
                }
                int embedCost = after[time][e_P][e_Hp];
                if (costModel.isInfinity(embedCost)) {
                    continue;
                }
                int newCost = embedCost + infestationCost;
                if (newCost < bestCost) {
                    bestCost = newCost;
                    bestInfestationLocation[time][e_P][e_H] = e_Hp;
                }
            }
        }
        return bestInfestationLocation[time][e_P][e_H];
    }
    
    @Override
    public final int findBestInfestationLocation(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // set initial cost
        int bestCost = costModel.INFINITY;
        // loop through all possible landing sites
        for (int e_Hp = 0; e_Hp < hostTreeSize; e_Hp++) {
            if (hostAlive[e_Hp] && e_Hp != e_H) {
                /*
                 * If we shouldn't infest onto that edge due to polytomies.
                 */
                if (this.problem.hostTree.noMidPolyEvents && problem.hostTree.isPolytomyEdge(e_Hp)) {
                    continue;
                }
                // compute cost and update if better
                int infestationCost = costModel.getInfestationCost(e_H, e_Hp);
                if (costModel.isInfinity(infestationCost)) {
                    continue;
                }
                int embedCost = (int) afterMulti.get(e_P)[time][e_H].getCost(name, tips);
                if (costModel.isInfinity(embedCost)) {
                    continue;
                }
                int newCost = embedCost + infestationCost;
                if (newCost < bestCost) {
                    bestCost = newCost;
                    bestMultiInfestationLocation.get(e_P)[time][e_H].setCost(name, tips, e_Hp);
                }
            }
        }
        return (int) bestMultiInfestationLocation.get(e_P)[time][e_H].getCost(name, tips);
    }

    @Override
    public final int getBestInfestationLocation(int time, int e_P, int e_H) {
        // just look up the table (no computation)
        return bestInfestationLocation[time][e_P][e_H];
    }
    
    @Override
    public final int getBestInfestationLocation(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // just look up the table (no computation)
        return (int) bestMultiInfestationLocation.get(e_P)[time][e_H].getCost(name, tips);
    }
    
    @Override
    public final int findBestInfestationCost(int time, int e_P, int e_H) {
        // use findBestInfestationLocation, but returns infinity if impossible
        int infestationLocation = findBestInfestationLocation(time, e_P, e_H);
        return (infestationLocation == -1
                ? costModel.INFINITY
                : after[time][e_P][infestationLocation] + costModel.getInfestationCost(e_H, infestationLocation));
    }
    
    @Override
    public final int findBestInfestationCost(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // use findBestInfestationLocation, but returns infinity if impossible
        int infestationLocation = findBestInfestationLocation(time, e_P, name, tips, e_H);
        return (infestationLocation == -1
                ? costModel.INFINITY
                : ((int) afterMulti.get(e_P)[time][infestationLocation].getCost(name, tips)) + costModel.getInfestationCost(e_H, infestationLocation));
    }

    @Override
    public final int getBestInfestationCost(int time, int e_P, int e_H) {
        // use getBestInfestationLocation, but returns infinity if impossible
        int infestationLocation = getBestInfestationLocation(time, e_P, e_H);
        return (infestationLocation == -1
                ? costModel.INFINITY
                : after[time][e_P][infestationLocation] + costModel.getInfestationCost(e_H, infestationLocation));
    }
    
    @Override
    public final int getBestInfestationCost(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // use getBestInfestationLocation, but returns infinity if impossible
        int infestationLocation = getBestInfestationLocation(time, e_P, name, tips, e_H);
        return (infestationLocation == -1
                ? costModel.INFINITY
                : ((int) afterMulti.get(e_P)[time][infestationLocation].getCost(name, tips)) + costModel.getInfestationCost(e_H, infestationLocation));
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
