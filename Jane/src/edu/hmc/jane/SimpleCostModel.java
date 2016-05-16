/**
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane;

/**
 * This class implements a cost model where the host switch cost/infestation
 * cost are constant and host switches/infestations have unlimited distance.
 * It subclasses CostModel, because even though host switch costs/infestation
 * costs returned by this class are constant, you can still ask it what the
 * specific cost of switching/infesting from one specific parasite edge
 * switching/infesting from one specific host edge to another specific
 * host edge is, it's just that it will always give you the same answer.
 * @author John
 *
 * @Modified by Ki Wan Gkoo
 */
public class SimpleCostModel extends CostModel {
    private int hostSwitchCost;
    private int infestationCost;

    public SimpleCostModel(Tree host, Tree parasite, CostTuple costs) {
        super(host, parasite, costs);
        this.hostSwitchCost = costs.toTarzan().getHostSwitchCost();
        this.infestationCost = costs.toTarzan().getInfestationCost();
    }
    
    @Override
    public int getHostSwitchCost(int fromID, int toID) {
        return hostSwitchCost;
    }
    
    @Override
    public int getInfestationCost(int fromID, int toID) {
        return infestationCost;
    }

    @Override
    public int getCost(int eventType) {
        switch(eventType) {
            case COSPECIATION       : return getCospeciationCost();
            case DUPLICATION        : return getDuplicationCost();
            case HOST_SWITCH        : return getHostSwitchCost();
            case LOSS               : return getLossCost();
            case FAILURE_TO_DIVERGE : return getFailureToDivergeCost();
            case INFESTATION        : return getInfestationCost();
            default                 : throw new RuntimeException();
        }
    }
}
