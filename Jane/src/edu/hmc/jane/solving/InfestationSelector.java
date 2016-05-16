/* This is an abstract class that defines the functionality of the infestation
 * selector, which selects the place to infest to and store necessary
 * information to reconstruct solution if in that mode
 */

package edu.hmc.jane.solving;

import java.util.Set;

abstract class InfestationSelector {

    // necessary information for infestation
    ArrayDP3 problem;
    DPTable table;

    // CONSTRUCTOR
    public InfestationSelector (ArrayDP3 problem) {
        this.problem = problem;
        this.table = problem.table;
    }

    // update infest location as after table changes
    public abstract void updateBestInfestation(int time, int e_P, int e_H, int newCost);
    public abstract void updateBestInfestation(int time, int e_P, int name, Set<Integer> tips, int e_H, int newCost);

    // COMPUTE the best place to infest
    public abstract int findBestInfestationLocation(int time, int e_P, int e_H);
    public abstract int findBestInfestationLocation(int time, int e_P, int name, Set<Integer> tips, int e_H);
    // OBTAIN the computed best place to infest
    public abstract int getBestInfestationLocation(int time, int e_P, int e_H);
    public abstract int getBestInfestationLocation(int time, int e_P, int name, Set<Integer> tips, int e_H);

    // COMPUTE the best place to infest and returns the cost
    public abstract int findBestInfestationCost(int time, int e_P, int e_H);
    public abstract int findBestInfestationCost(int time, int e_P, int name, Set<Integer> tips, int e_H);
    // OBTAIN the computed best place to infest and returns the cost
    public abstract int getBestInfestationCost(int time, int e_P, int e_H);
    public abstract int getBestInfestationCost(int time, int e_P, int name, Set<Integer> tips, int e_H);

    // update necessary information when we proceed to new time
    public abstract void decreaseAfterTime();

    // release memory as DP solving is finished
    public abstract void doneSolving();
}
