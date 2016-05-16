/* This is an abstract class that defines the functionality of the host-switch
 * selector, which selects the place to host-switch to and store necessary
 * information to reconstruct solution if in that mode
 */

package edu.hmc.jane.solving;

import java.util.Set;
import java.util.ArrayList;
abstract class HostSwitchSelector {

    // necessary information for host-switch
    ArrayDP3 problem;
    DPTable table;

    // CONSTRUCTOR
    public HostSwitchSelector (ArrayDP3 problem) {
        this.problem = problem;
        this.table = problem.table;
    }

    // update switch location as after table changes
    public abstract void updateBestSwitch(int time, int e_P, int e_H, int newCost);
    public abstract void updateBestSwitch(int time, int e_P, int name, Set<Integer> tips, int e_H, int newCost);

    // COMPUTE the best place to switch
    public abstract int findBestSwitchLocation(int time, int e_P, int e_H);
    public abstract int findBestSwitchLocation(int time, int e_P, int name, Set<Integer> tips, int e_H);
    
    // OBTAIN the computed best place to switch
    public abstract int getBestSwitchLocation(int time, int e_P, int e_H);
    public abstract int getBestSwitchLocation(int time, int e_P, int name, Set<Integer> tips, int e_H);
    
    // COMPUTE the best place to switch and returns the cost
    public abstract int findBestSwitchCost(int time, int e_P, int e_H);    
    public abstract int findBestSwitchCost(int time, int e_P, int name, Set<Integer> tips, int e_H);
    // OBTAIN the computed best place to switch and returns the cost
    public abstract int getBestSwitchCost(int time, int e_P, int e_H);
    public abstract int getBestSwitchCost(int time, int e_P, int name, Set<Integer> tips, int e_H);

    // update necessary information when we proceed to new time
    public abstract void decreaseAfterTime();

    // release memory as DP solving is finished
    public abstract void doneSolving();
}
