package edu.hmc.jane;

/**
 * This concrete class contains the functionality necessary to limit host switch distance/infestation distance,
 * and have costs depend on the region you are switching/infesting from/to, as well
 * as have constant costs for everything else (eg. cospeciation, loss, duplication, failure to diverge)
 * 
 * @author John Modified by Ki Wan Gkoo
 */
public class RegionedCostModel extends CostModel {
    protected final int defaultHostSwitchCost;
    protected final int defaultInfestationCost;
    private final int maxHostSwitchDistance;
    private final int maxInfestationDistance;
    protected TreeRegions regions;

    public RegionedCostModel(ProblemInstance prob, CostTuple costs, int maxSwitchD, int maxInfestationD) {
        super(prob.hostTree, prob.parasiteTree, costs);

        this.defaultHostSwitchCost = costs.toTarzan().getHostSwitchCost();
        this.defaultInfestationCost = costs.toTarzan().getInfestationCost();

        if (maxSwitchD == -1)
            this.maxHostSwitchDistance = INFINITY;
        else
            this.maxHostSwitchDistance = maxSwitchD;
        if (maxInfestationD == -1)
            this.maxInfestationDistance = INFINITY;
        else
            this.maxInfestationDistance = maxInfestationD;

        this.regions = prob.hostRegions;
    }

    @Override
    public int getHostSwitchCost(int fromID, int toID) {
        if (host.getDistance(fromID, toID) <= maxHostSwitchDistance) {
            return defaultHostSwitchCost +
                    regions.getHostSwitch_InfestationCostBetweenNodes(fromID, toID);
        }

        return INFINITY;
    }
    
    @Override
    public int getInfestationCost(int fromID, int toID) {
        if (host.getDistance(fromID, toID) <= maxInfestationDistance) {
            return defaultInfestationCost +
                    regions.getHostSwitch_InfestationCostBetweenNodes(fromID, toID);
        }
        
        return INFINITY;
    }
}
