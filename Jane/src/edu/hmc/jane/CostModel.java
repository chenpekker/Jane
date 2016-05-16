package edu.hmc.jane;


/**
 * This class is the superclass for all of the concrete classes that implement
 * CostModel in Jane. When implementing the n^4 algorithm, regardless of the
 * underlying type of whichever cost model class you are using, you should
 * declare it as a CostModel, eg.
 *
 * <code>CostModel costs = new HostSwitchDistanceRegionCostModel();</code>
 * 
 * @author John
 */

/**
 *
 * @Modified by Ki Wan Gkoo
 */

public abstract class CostModel {

    public static final int COSPECIATION = 0;
    public static final int DUPLICATION = 1;
    public static final int HOST_SWITCH = 2;
    public static final int LOSS = 3;
    public static final int FAILURE_TO_DIVERGE = 4;
    public static final int INFESTATION = 5;

    public static final int TIP = 6;
    public static final int NOTHING = 7;
    public static final int OTHER = -1;
    public static final int STATIC_INFINITY=999999999;

    public static final int NUM_EVENTS = 5;
    
    private int cospeciationCost;
    private int duplicationCost;
    private int hostSwitchCost;
    private int lossCost;
    private int failureToDivergeCost;
    private int infestationCost;
    
    public final int INFINITY = 999999999; //if you change this value, make
                                         //sure to update the validation code
                                         //in EditCosts.java and CLI.java that
                                         //reports when the users cost choices
                                         //are too large

    protected final Tree host;
    protected final Tree parasite;

    public final boolean originallyTarzan; //whether the CostTuple supplied to us was
    //in tarzan format or not, note that regardless of if it was in the
    //tarzan format, all costs
    //will still get converted to tarzan format, so all costs returned by
    //methods like getCospeciationCost() will always be in tarzan format


    public CostModel(Tree host, Tree parasite, CostTuple costs) {

        this.host = host;
        this.parasite = parasite;

        this.originallyTarzan = costs.isTarzan();

        costs = costs.toTarzan();

        this.cospeciationCost = costs.getCospeciationCost();
        this.duplicationCost = costs.getDuplicationCost();
        this.hostSwitchCost = costs.getHostSwitchCost();
        this.lossCost = costs.getLossCost();
        this.failureToDivergeCost = costs.getFailureToDivergeCost();
        this.infestationCost = costs.getInfestationCost();
    }

    /*
     * Returns the cost for the given event type. Note that since the host switch
     * cost may be variable, this throws an error if you ask it for the host switch
     * cost.
     */
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
    
    public int getCospeciationCost() {
        return cospeciationCost;
    }

    public int getDuplicationCost() {
        return duplicationCost;
    }
    
    public int getHostSwitchCost() {
        return hostSwitchCost;
    }

    public int getLossCost() {
        return lossCost;
    }
    
    public int getFailureToDivergeCost() {
        return failureToDivergeCost;
    }
        
    public int getInfestationCost() {
        return infestationCost;
    }

    /*
     * Returns the event cost of host switching between two edges.
     */
    public abstract int getHostSwitchCost(int fromID, int toID);
    
    /*
     * Returns the event cost of infestation between two edges.
     */
    public abstract int getInfestationCost(int fromID, int toID);

    public boolean isInfinity(int cost) {
        if(cost>INFINITY)
            throw new RuntimeException("Total Cost Exceeded Infinity: " + cost);
        return cost==INFINITY;
    }

    public static CostModel getAppropriate(ProblemInstance prob, CostTuple costs, int maxSwitchDistance1, int maxSwitchDistance2) {
        if (maxSwitchDistance1 == 0 && maxSwitchDistance2 == 0 && !prob.hasRegions()) {
            costs.setHostSwitchCost(CostModel.STATIC_INFINITY);
            costs.setInfestationCost(CostModel.STATIC_INFINITY);
            return new SimpleCostModel(prob.hostTree, prob.parasiteTree, costs);
        } else if (prob.isConstrainedBy1(maxSwitchDistance1) || prob.isConstrainedBy2(maxSwitchDistance2) || prob.hasRegions()) {
            return new RegionedCostModel(prob, costs, maxSwitchDistance1, maxSwitchDistance2);
        } else {
            return new SimpleCostModel(prob.hostTree, prob.parasiteTree, costs);
        }
    }

    public static final String eventTypeName(int eventType) {
        switch(eventType){
            case 0: return "Cospeciation";
            case 1: return "Duplication";
            case 2: return "Host Switch";
            case 3: return "Loss";
            case 4: return "Failure To Diverge";
            case 5: return "Infestation";
            case 6: return "Tip";
            case 7: return "Nothing";
            default: return "Other";
        }
    }

    public boolean wasOriginallyTarzan() {
        return originallyTarzan;
    }
}
