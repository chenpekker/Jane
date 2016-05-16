/* This class takes care of host switch for regioned / non-reconstructable case */
package edu.hmc.jane.solving;

import edu.hmc.jane.CostModel;
import java.util.Set;

class RegionedNonReconstructableHSS extends HostSwitchSelector {

    boolean[] hostAlive;    // keep track which host is alive
    int hostTreeSize;       // number of hosts to check through
    CostModel costModel;    // information for computing costs

    public RegionedNonReconstructableHSS(ArrayDP3 problem) {
        super(problem);
        // obtain necessary information
        hostTreeSize = problem.hostTree.size;
        hostAlive = problem.hostAlive;
        costModel = problem.costModel;
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
        // set initial cost and switch location
        int bestCost = costModel.INFINITY;
        int bestSwitchLocation = -1;
        // loop through all possible landing sites
        for (int e_Hp = 0; e_Hp < hostTreeSize; e_Hp++) {
            if (hostAlive[e_Hp] && e_Hp != e_H) {
                // compute cost and update if better

                /*
                 * If we shouldn't switch onto that edge due to polytomies.
                 */
                if (this.problem.hostTree.noMidPolyEvents && problem.hostTree.isPolytomyEdge(e_Hp)) {
                    continue;
                }
                int switchCost = costModel.getHostSwitchCost(e_H, e_Hp);
                if (costModel.isInfinity(switchCost)) {
                    continue;
                }
                int embedCost = table.getFromAfter(e_P, e_Hp);
                if (costModel.isInfinity(embedCost)) {
                    continue;
                }
                int newCost = embedCost + switchCost;
                if (newCost < bestCost) {
                    bestCost = newCost;
                    bestSwitchLocation = e_Hp;
                }
            }
        }
        return bestSwitchLocation;
    }
    
    @Override
    public final int findBestSwitchLocation(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // set initial cost and switch location
        int bestCost = costModel.INFINITY;
        int bestSwitchLocation = -1;
        // loop through all possible landing sites
        for (int e_Hp = 0; e_Hp < hostTreeSize; e_Hp++) {
            if (hostAlive[e_Hp] && e_Hp != e_H) {
                // compute cost and update if better

                /*
                 * If we shouldn't switch onto that edge due to polytomies.
                 */
                if (this.problem.hostTree.noMidPolyEvents && problem.hostTree.isPolytomyEdge(e_Hp)) {
                    continue;
                }
                int switchCost = costModel.getHostSwitchCost(e_H, e_Hp);
                if (costModel.isInfinity(switchCost)) {
                    continue;
                }
                int embedCost = table.getFromAfter(e_P, e_Hp);
                if (costModel.isInfinity(embedCost)) {
                    continue;
                }
                int newCost = embedCost + switchCost;
                if (newCost < bestCost) {
                    bestCost = newCost;
                    bestSwitchLocation = e_Hp;
                }
            }
        }
        return bestSwitchLocation;
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
        // use findBestSwitchLocation, but returns infinity if impossible
        int switchLocation = findBestSwitchLocation(time, e_P, e_H);
        return (switchLocation == -1
                ? costModel.INFINITY
                : table.getFromAfter(e_P, switchLocation)
                + costModel.getHostSwitchCost(e_H, switchLocation));
    }
    
    @Override
    public final int findBestSwitchCost(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // use findBestSwitchLocation, but returns infinity if impossible
        int switchLocation = findBestSwitchLocation(time, e_P, name, tips, e_H);
        return (switchLocation == -1
                ? costModel.INFINITY
                : table.getFromAfter(e_P, switchLocation)
                + costModel.getHostSwitchCost(e_H, switchLocation));
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
        // nothing to be done
    }

    @Override
    public final void doneSolving() {
        // nothing to be done
    }
}
