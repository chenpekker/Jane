/* This class takes care of infestation for regioned / non-reconstructable case */
package edu.hmc.jane.solving;

import edu.hmc.jane.CostModel;
import java.util.Set;

class RegionedNonReconstructableIFS extends InfestationSelector {

    boolean[] hostAlive;    // keep track which host is alive
    int hostTreeSize;       // number of hosts to check through
    CostModel costModel;    // information for computing costs

    public RegionedNonReconstructableIFS(ArrayDP3 problem) {
        super(problem);
        // obtain necessary information
        hostTreeSize = problem.hostTree.size;
        hostAlive = problem.hostAlive;
        costModel = problem.costModel;
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
        // set initial cost and infestation location
        int bestCost = costModel.INFINITY;
        int bestInfestationLocation = -1;
        // loop through all possible landing sites
        for (int e_Hp = 0; e_Hp < hostTreeSize; e_Hp++) {
            if (hostAlive[e_Hp] && e_Hp != e_H) {
                // compute cost and update if better

                /*
                 * If we shouldn't use infestation onto that edge due to polytomies.
                 */
                if (this.problem.hostTree.noMidPolyEvents && problem.hostTree.isPolytomyEdge(e_Hp)) {
                    continue;
                }
                int infestationCost = costModel.getInfestationCost(e_H, e_Hp);
                if (costModel.isInfinity(infestationCost)) {
                    continue;
                }
                int embedCost = table.getFromAfter(e_P, e_Hp);
                if (costModel.isInfinity(embedCost)) {
                    continue;
                }
                int newCost = embedCost + infestationCost;
                if (newCost < bestCost) {
                    bestCost = newCost;
                    bestInfestationLocation = e_Hp;
                }
            }
        }
        return bestInfestationLocation;
    }
    
    @Override
    public final int findBestInfestationLocation(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // set initial cost and infestation location
        int bestCost = costModel.INFINITY;
        int bestInfestationLocation = -1;
        // loop through all possible landing sites
        for (int e_Hp = 0; e_Hp < hostTreeSize; e_Hp++) {
            if (hostAlive[e_Hp] && e_Hp != e_H) {
                // compute cost and update if better

                /*
                 * If we shouldn't use infestation onto that edge due to polytomies.
                 */
                if (this.problem.hostTree.noMidPolyEvents && problem.hostTree.isPolytomyEdge(e_Hp)) {
                    continue;
                }
                int infestationCost = costModel.getInfestationCost(e_H, e_Hp);
                if (costModel.isInfinity(infestationCost)) {
                    continue;
                }
                int embedCost = table.getFromAfter(e_P, e_Hp);
                if (costModel.isInfinity(embedCost)) {
                    continue;
                }
                int newCost = embedCost + infestationCost;
                if (newCost < bestCost) {
                    bestCost = newCost;
                    bestInfestationLocation = e_Hp;
                }
            }
        }
        return bestInfestationLocation;
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
        // use findBestInfestationLocation, but returns infinity if impossible
        int infestationLocation = findBestInfestationLocation(time, e_P, e_H);
        return (infestationLocation == -1
                ? costModel.INFINITY
                : table.getFromAfter(e_P, infestationLocation)
                + costModel.getInfestationCost(e_H, infestationLocation));
    }
    
    @Override
    public final int findBestInfestationCost(int time, int e_P, int name, Set<Integer> tips, int e_H) {
        // use findBestInfestationLocation, but returns infinity if impossible
        int infestationLocation = findBestInfestationLocation(time, e_P, name, tips, e_H);
        return (infestationLocation == -1
                ? costModel.INFINITY
                : table.getFromAfter(e_P, infestationLocation)
                + costModel.getInfestationCost(e_H, infestationLocation));
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
        // don't need to do anything
    }

    @Override
    public final void doneSolving() {
        // nothing to be done
    }
}
