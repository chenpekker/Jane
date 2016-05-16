package edu.hmc.jane;

/*
 * This class has almost all the information that Jane needs to solve a
 * given problem, save the information about costs and a few things about
 * the genetic algorithm like mutation rate.
 */

/* Polytomy update:  this will have naive, polytomy-containing trees.  They will be
 * resolved by other functions
 * 
 */
public class ProblemInstance {

    public final Tree hostTree;
    public final Tree parasiteTree;
    public final TreeRegions hostRegions;
    public final Phi phi;
    public final TimeZones timeZones;

    public ProblemInstance(Tree hT, Tree pT, TreeRegions hR, Phi phi, TimeZones tZ) {
        hostTree = hT;
        parasiteTree = pT;
        hostRegions = hR;
        this.phi = phi;
        timeZones = tZ;
    }

    public ProblemInstance(ProblemInstance problem) {
        this(problem.hostTree, problem.parasiteTree, problem.hostRegions, problem.phi, problem.timeZones);
    }

    public boolean hasRegions() {
        return hostRegions.hasRegions;
    }

    /*
     * returns whether having maxHostSwitchDistance as the maximum host switch
     * distance might change solutions that would be considered valid. For example
     * having a max host switch distance of 0 would eliminate host switching, so
     * it would make this method return true, whereas a max distance of 999999
     * would probably not, since it is likely larger than the largest possible host
     * switch for a given tree. Note that -1 is treated as infinity.
     */
    public boolean isConstrainedBy1(int maxSwitchDistance) {
        return maxSwitchDistance!=-1 && maxSwitchDistance < hostTree.maxDistance;
    }
    
    public boolean isConstrainedBy2(int maxInfestationDistance) {
        return maxInfestationDistance != -1 && maxInfestationDistance < hostTree.maxDistance;
    }
}
