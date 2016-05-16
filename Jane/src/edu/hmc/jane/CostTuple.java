/*
 * CostTuple.java
 *
 * Modified on June 19, 2012, 10:00:00 AM
 */

/**
 *
 * @Modified by Ki Wan Gkoo
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane;

/**
 * Note that when you instantiate a CostModel, all costs are converted to tarzan.
 * This is because internally, we use only tarzan's model. When you ask for the
 * event count from the EventSolver, however, it will make sure that the event
 * counts conform with the model originally specified in the cost tuple passed
 * to the CostModel. Note,  that the getEventCount() method in the dp
 * table classes, however, returns the event count in Tarzan format.
 * @author jpeebles
 */
public class CostTuple {
    private int cospeciationCost;
    private int duplicationCost;
    private int lossCost;
    private int hostSwitchCost;
    private int failureToDivergeCost;
    private int infestationCost;

    private boolean tarzan;

    public CostTuple() {
        tarzan=true;
        cospeciationCost=0;
        duplicationCost=1;
        lossCost=2;
        hostSwitchCost=1;
        failureToDivergeCost=1;
        infestationCost=0;
    }

    public CostTuple(int cosp, int dup, int loss, int hs, int ftd, int inf, boolean isTarzan) {
        this.cospeciationCost=cosp;
        this.duplicationCost=dup;
        this.lossCost=loss;
        this.hostSwitchCost=hs;
        this.failureToDivergeCost=ftd;
        this.infestationCost=inf;
        this.tarzan=isTarzan;
    }

    public static CostTuple fromTarzan(int cosp, int dup, int loss, int hs, int ftd, int inf) {
        return new CostTuple(cosp, dup, loss, hs, ftd, inf, true);
    }

    public static CostTuple fromCharleston(int cosp, int dup, int loss, int hs, int ftd, int inf) {
        return new CostTuple(cosp, dup, loss, hs, ftd, inf, false);
    }

    public CostTuple toTarzan() {
        int dup = getDuplicationCost();
        int hs = getHostSwitchCost();
        int cosp = getCospeciationCost();
        int loss = getLossCost();
        int ftd = getFailureToDivergeCost();
        int inf = getInfestationCost();

        if(!isTarzan()) {
            cosp*=2;
            dup*=2;
            hs+=dup;
            ftd*=2;
            inf*=2;
        }

        return new CostTuple(cosp, dup, loss, hs, ftd, inf, true);
    }

    public CostTuple toCharleston() {
        int dup = getDuplicationCost();
        int hs = getHostSwitchCost();
        int cosp = getCospeciationCost();
        int loss = getLossCost();
        int ftd = getFailureToDivergeCost();
        int inf = getInfestationCost();

        if(isTarzan()) {
            ftd/=2;
            hs-=dup;
            dup/=2;
            cosp/=2;
            inf/=2;
        }

        return new CostTuple(cosp, dup, loss, hs, ftd, inf, false);
    }

    /**
     * @return the cospeciationCost
     */
    public int getCospeciationCost() {
        return cospeciationCost;
    }

    /**
     * @param cospeciationCost the cospeciationCost to set, returns
     * true if the cost is different
     */
    public boolean setCospeciationCostAndGetChange(int cospeciationCost) {
        boolean changed = (this.cospeciationCost != cospeciationCost);
        this.cospeciationCost = cospeciationCost;
        return changed;
    }
    public void setCospeciationCost(int cospeciationCost) {
        this.cospeciationCost = cospeciationCost;
    }
    /**
     * @return the duplicationCost
     */
    public int getDuplicationCost() {
        return duplicationCost;
    }

    /**
     * @param duplicationCost the duplicationCost to set, returns
     * true if the cost is different
     */
    public boolean setDuplicationCostAndGetChange(int duplicationCost) {
        boolean changed = (this.duplicationCost != duplicationCost);
        this.duplicationCost = duplicationCost;
        return changed;
    }
    
    public void setDuplicationCost(int duplicationCost) {
        this.duplicationCost = duplicationCost;
    }

    /**
     * @return the lossCost
     */
    public int getLossCost() {
        return lossCost;
    }

    /**
     * @param lossCost the lossCost to set, returns
     * true if the cost is different
     */
    public boolean setLossCostAndGetChange(int lossCost) {
        boolean changed = (this.lossCost != lossCost);
        this.lossCost = lossCost;
        return changed;
    }

    public void setLossCost(int lossCost) {
        this.lossCost = lossCost;
    }

    /**
     * @return the hostSwitchCost
     */
    public int getHostSwitchCost() {
        return hostSwitchCost;
    }

    /**
     * @param hostSwitchCost the hostSwitchCost to set, returns
     * true if the cost is different
     */
    public boolean setHostSwitchCostAndGetChange(int hostSwitchCost) {
        boolean changed = (this.hostSwitchCost != hostSwitchCost);
        this.hostSwitchCost = hostSwitchCost;
        return changed;
    }

    public void setHostSwitchCost(int hostSwitchCost) {
        this.hostSwitchCost = hostSwitchCost;
    }

    /**
     * @return the failureToDivergeCost
     */
    public int getFailureToDivergeCost() {
        return failureToDivergeCost;
    }

    /**
     * @param failureToDivergeCost the failureToDivergeCost to set, returns
     * true if the cost is different
     */
    public boolean setFailureToDivergeCostAndGetChange(int failureToDivergeCost) {
        boolean changed = (this.failureToDivergeCost != failureToDivergeCost);
        this.failureToDivergeCost = failureToDivergeCost;
        return changed;
    }

    public void setFailureToDivergeCost(int failureToDivergeCost) {
        this.failureToDivergeCost = failureToDivergeCost;
    }
    
    /**
     * @return the infestationCost
     */
    public int getInfestationCost() {
        return infestationCost;
    }

    /**
     * @param infestationCost the infestationCost to set, returns
     * true if the cost is different
     */
    public boolean setInfestationCostAndGetChange(int infestationCost) {
        boolean changed = (this.infestationCost != infestationCost);
        this.infestationCost = infestationCost;
        return changed;
    }

    public void setInfestationCost(int infestationCost) {
        this.infestationCost = infestationCost;
    }

    /**
     * @return the tarzan
     */
    public boolean isTarzan() {
        return tarzan;
    }

    /**
     * @param isTarzan the tarzan to set, returns
     *  true if a change occurs
     */
    public boolean setTarzanAndGetChange(boolean isTarzan) {
        boolean changed = (this.tarzan != isTarzan);
        this.tarzan = isTarzan;
        return changed;
    }
    
    public void setTarzan(boolean isTarzan) {
        this.tarzan = isTarzan;
    }
}
