/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.solving;

import edu.hmc.jane.*;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tselil
 */

/**
 *
 * @Modified by Ki Wan Gkoo
 */
public class EventSolver extends Solver implements Runnable, Callable<EventSolver> {

    private int[] eventCount;
    public ArrayDP3 dpTable;
    private ArrayDP3 dpTable3;
    private Future doneComputing;
    private CostModel costModel;
    public SolutionViewerInfo info;
    public boolean randomizeDP = false;

    public EventSolver(TreeSpecs hTiming, Phi p, TimeZones tz, CostModel c) {
        super(hTiming, p, tz);
        this.costModel = c;
        dpTable = new ArrayDP3(hTiming, p, tz, ArrayDP3.EVENT_COUNT, c);
    }

    @Override
    public int solve() {
        /*
         * Invokes the dynamic program on the hostNetwork, parasiteTree, and
         * costs and returns the eventCost of the optimal solution
         */
        cost = dpTable.solve();
        countEvents();
        return cost;
    }

    /*
     * Note that when you instantiate a CostModel, all costs are converted to
     * tarzan. This is because internally, we use only tarzan's model. When you
     * ask for the event count from the EventSolver, however, it will make sure
     * that the event counts conform with the model originally specified in the
     * cost tuple passed to the CostModel. Note, that the getEventCount() method
     * in the dp table classes, however, returns the event count in Tarzan
     * format.
     */
    private void countEvents() {
        eventCount = dpTable.getEventCount();
        CostModel cm = dpTable.costModel;
        if (!cm.wasOriginallyTarzan()) {
            eventCount[CostModel.DUPLICATION] += eventCount[CostModel.HOST_SWITCH];
            eventCount[CostModel.DUPLICATION] *= 2;
            eventCount[CostModel.COSPECIATION] *= 2;
            eventCount[CostModel.FAILURE_TO_DIVERGE] *= 2;
            eventCount[CostModel.INFESTATION] *= 2;
        }
    }

    public EventSolver call() {
        run();
        info = dpTable.getSolutionViewerInfo();
        this.dpTable = null; //let it get garabage collected since we don't need it
        return this;
    }

    @Override
    public void computeEvents() {
        // create reconstructable table
        dpTable3 = new ArrayDP3(hostTiming, phi, timeZones, ArrayDP3.RECONSTRUCTABLE, costModel);
        if (randomizeDP) {
            dpTable3.randomizeEvents = true;
        }
        // solve and get events
        if (getDpTable3() != null && getDpTable3().MODE == ArrayDP3.RECONSTRUCTABLE) {
            getDpTable3().solve();
            SolutionViewerInfo solInfo = getDpTable3().getSolutionViewerInfo();
            Embedding embedding = new Embedding(solInfo);
            events = solInfo.getEvents(solInfo.parasiteTree.root, embedding.parasitePosition[solInfo.parasiteTree.root]);
//            if(dpTable3.randomizeEvents)
//                System.out.println("events = " + events);
        }
        // let it get garabage collected
        dpTable3 = null;
    }
    
    public SolutionViewerInfo getSolutionViewerInfo() {
        dpTable3 = new ArrayDP3(hostTiming, phi, timeZones, ArrayDP3.RECONSTRUCTABLE, costModel);
        SolutionViewerInfo solInfo = null;
        if (getDpTable3() != null && getDpTable3().MODE == ArrayDP3.RECONSTRUCTABLE) {
            getDpTable3().solve();
            solInfo = getDpTable3().getSolutionViewerInfo();
        }
        dpTable3 = null;
        return solInfo;
    }

    /**
     * @return the eventCount
     */
    public int[] getEventCount() {
        waitTillComputed();
        return eventCount;
    }

    /**
     * @param eventCount the eventCount to set
     */
    public void setEventCount(int[] eventCount) {
        this.eventCount = eventCount;
    }

    public CostModel getCostModel() {
        return costModel;
    }
    
    /**
     * @return the dpTable
     */
    public ArrayDP3 getDpTable() {
        waitTillComputed();
        return dpTable;
    }

    /**
     * @param dpTable the dpTable to set
     */
    public void setDpTable(ArrayDP3 dpTable) {
        waitTillComputed();
        this.dpTable = dpTable;
    }

    /**
     * @return the dpTable3
     */
    public ArrayDP3 getDpTable3() {
        waitTillComputed();
        return dpTable3;
    }

    /**
     * @param dpTable3 the dpTable3 to set
     */
    public void setDpTable3(ArrayDP3 dpTable3) {
        waitTillComputed();
        this.dpTable3 = dpTable3;
    }

    /**
     * @return the doneComputing
     */
    public boolean isDoneComputing() {
        if (doneComputing == null) {
            return true;
        } 
        return doneComputing.isDone();
    }

    /**
     * @param doneComputing the doneComputing to set
     */
    public void setDoneComputing(Future doneComputing) {
        this.doneComputing = doneComputing;
    }
    
    private void waitTillComputed() {
        while (!isDoneComputing()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(EventSolver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public boolean diffTimings( EventSolver other){
        if (events == null) {
            computeEvents();
        }
        if (other.events == null) {
            other.computeEvents();
        }
        // same number of events
        for (int i = 0; i < CostModel.NUM_EVENTS; i++) {
            if (eventCount[i] != other.eventCount[i]) {
                return true;
            }
        }
        ArrayList<SolutionViewerInfo.EventInfo> otherEvents = other.events;
        
        for (int i = 0; i < otherEvents.size(); i++) {
            if (!events.contains(otherEvents.get(i))) {
                return true;
            }
        }
        return false;
    }
}
