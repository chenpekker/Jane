/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.solving;

import edu.hmc.jane.*;
import edu.hmc.jane.solving.SolutionViewerInfo.EventInfo;
import java.util.ArrayList;

/**
 *
 * @author Tselil
 */
public abstract class Solver implements Runnable , Comparable<Solver> {

    public static int solvesDone = 0;
    public Tree hostTree;
    public Tree paraTree;
    public TreeSpecs hostTiming;
    public Phi phi; // mapping of host tips to parasite tips
    public TimeZones timeZones;
    public int cost; // the cost of the best solution found after the solver runs
    public ArrayDP3 dpTable;
    public ArrayList<EventInfo> events;

    public Solver(TreeSpecs hTiming, Phi p, TimeZones tz) {
        this.hostTiming = hTiming;
        this.hostTree = hTiming.hostTree;
        this.paraTree = hTiming.parasiteTree;
        Phi fixedP = p.newWithoutPolytomies(hTiming.parasiteTree.size-p.length());
        this.phi = fixedP;
        this.timeZones = tz;
    }

    public Solver( ProblemInstance prob, TreeSpecs hTiming) {
        this.hostTiming = hTiming;
        this.hostTree = hTiming.hostTree;
        this.paraTree = hTiming.parasiteTree;
        Phi fixedP = prob.phi.newWithoutPolytomies(hTiming.parasiteTree.size-prob.phi.length());
        this.phi = fixedP;
        this.timeZones = prob.timeZones;
    }

    public void run() {
        solve();
        solvesDone++;
    }

    public int solve() {
        /*Invokes the dynamic program on the hostNetwork, parasiteTree,
        and costs and returns the cost of the optimal solution
         */
        cost = dpTable.solve();
        return cost; //CHECK HERE
    }
    
    public int compareTo(Solver other) {
        return other.cost - cost;
    }

    public boolean equals( EventSolver other) {
        return this.hostTiming.equals(other.hostTiming);
    }
    
    public void computeEvents() {
        if (dpTable != null && dpTable.MODE == ArrayDP3.RECONSTRUCTABLE) {
            SolutionViewerInfo solInfo = dpTable.getSolutionViewerInfo();
            Embedding embedding = new Embedding(solInfo);
            events = solInfo.getEvents(solInfo.parasiteTree.root, embedding.parasitePosition[solInfo.parasiteTree.root]);
        }
        dpTable = null;
    }
}
