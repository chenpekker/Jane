/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.gui;

import edu.hmc.jane.CostModel;
import edu.hmc.jane.solving.EventSolver;
import edu.hmc.jane.solving.EventSolverSort;
import edu.hmc.jane.solving.SolutionViewerInfo.EventInfo;
import edu.hmc.jane.solving.Solver;
import edu.hmc.jane.solving.SupportPopulation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.event.EventListenerList;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Tselil
 */

/**
 *
 * @Modified by Ki Wan Gkoo and Rebecca Thomas
 */
public class SolutionTableModel extends AbstractTableModel {

    List<EventSolver> eventSolvers;
    int sortCol = 0;
    boolean isSortAsc = true;
    ArrayList<ArrayList<EventSolver>> compressedSolns = null;
    boolean compress = false;
    int expanded = -1;
    public boolean once_compressed = false;
    protected EventListenerList listenerList = new EventListenerList();
    public ArrayList<ArrayList<EventSolver>> compressedSupport;
        
    SolutionTableModel(List<EventSolver> solutions) {
        eventSolvers = solutions; 
    }

    public int getColumnCount() {
        return 7; 
    }

    public int getRowCount() { // adjusts row count based on compression
        if (compress && compressedSolns == null)
            compressedSolns = compressSolns(eventSolvers);
        if (compress && compressedSolns != null && expanded != -1) 
            return compressedSolns.size() + compressedSolns.get(expanded).size(); 
        else if (compress && compressedSolns != null)
            return compressedSolns.size();
        return eventSolvers.size();
    }

    public List<EventSolver> addSolns(List<EventSolver> s) {
        Collections.sort(s, new EventSolverSort());
        for (EventSolver slv : s)
            addSoln(slv);
        return eventSolvers;
    }

    void addSoln(EventSolver slv) {
        boolean acceptable = true;
        for (EventSolver current : eventSolvers) {
            if (current.equals(slv)) {
                acceptable = false;
                break;
            }
        }
        if (acceptable)
            eventSolvers.add(slv);
    }
        
    public Object getValueAt(int x, int y) {
        // convert table row to event solver using row and subrow
        int row = x;
        int subrow = 0;
            
        // adjust for when compressed
        if (compress && expanded != -1) {
            if (x > expanded + compressedSolns.get(expanded).size()) {
                row = x - compressedSolns.get(expanded).size();
            } else if (x > expanded) {
                row = expanded;
                subrow = x - expanded - 1;
            }
        }
            
        // get corresponding costs
        if (compress) {
            switch (y) {
                case 0:
                    if (row == expanded & x != expanded) {
                        return eventSolvers.indexOf(compressedSolns.get(row).get(subrow)) + 1;
                    } else {
                        if (compressedSolns.get(row).size() == 1) {
                            return Integer.toString(compressedSolns.get(row).size()) + " Solution";
                        }
                        return Integer.toString(compressedSolns.get(row).size()) + " Solutions";
                    }
                case 1:
                    return compressedSolns.get(row).get(subrow).getEventCount()[CostModel.COSPECIATION];
                case 2:
                    return compressedSolns.get(row).get(subrow).getEventCount()[CostModel.DUPLICATION];
                case 3:
                    return compressedSolns.get(row).get(subrow).getEventCount()[CostModel.HOST_SWITCH];
                case 4:
                    return compressedSolns.get(row).get(subrow).getEventCount()[CostModel.LOSS];
                case 5:
                    return compressedSolns.get(row).get(subrow).getEventCount()[CostModel.FAILURE_TO_DIVERGE];
                /* Making parts related to infestation disabled
                case 6:
                    return compressedSolns.get(row).get(subrow).getEventCount()[CostModel.INFESTATION];
                case 7:
                    return compressedSolns.get(row).get(subrow).cost;
                */
                case 6:
                    return compressedSolns.get(row).get(subrow).cost;
            }
        } else {
            switch (y) {       
                case 0:
                    return x+1;
                case 1:
                    return eventSolvers.get(x).getEventCount()[CostModel.COSPECIATION];
                case 2:
                    return eventSolvers.get(x).getEventCount()[CostModel.DUPLICATION];
                case 3:
                    return eventSolvers.get(x).getEventCount()[CostModel.HOST_SWITCH];
                case 4:
                    return eventSolvers.get(x).getEventCount()[CostModel.LOSS];
                case 5:
                    return eventSolvers.get(x).getEventCount()[CostModel.FAILURE_TO_DIVERGE];
                /* // Making parts related to infestation disabled
                case 6:
                    return eventSolvers.get(x).getEventCount()[CostModel.INFESTATION];
                case 7:
                    return eventSolvers.get(x).cost;
                */
                case 6:
                    return eventSolvers.get(x).cost;
            }
        }
        return -1;
    }

    @Override
    public Class getColumnClass(int c) {
        return Integer.class;
    }

    public ArrayList<ArrayList<EventSolver>> compressSolns(List<EventSolver> solvers) { // compresses solutions based on event counts
        ArrayList<ArrayList<EventSolver>> solns = new ArrayList(1);
        if (solvers.size() > 0) {
            // init array
            ArrayList<EventSolver> firstSoln = new ArrayList(1);

            // add first solution to the array
            firstSoln.add(solvers.get(0));
            solns.add(firstSoln);
        } else {
            return null;
        }
        // add remaining solutions to correct count array
        for (int soln = 1; soln < solvers.size(); soln++) {
            for (int i = 0; i < solns.size(); i++) {
                if (!solvers.get(soln).diffTimings(solns.get(i).get(0))) {
                    solvers.get(soln).events = null;
                    solns.get(i).add(solvers.get(soln));
                    break;
                } else if (i == solns.size() - 1) { // no matching event counts
                    ArrayList<EventSolver> uniqueSoln = new ArrayList(1);
                    uniqueSoln.add(solvers.get(soln));
                    solns.add(uniqueSoln);
                    break;
                }
            }
        }
        return solns;
    }
    
    public void compressTable(){
        compressedSolns = compressSolns(eventSolvers);
        once_compressed = true;
    }
            
    public void eventConfidence(EventInfo event, SupportPopulation support) {
        if(event == null)
            return;
        
        int numSolns = 0;
        if (compressedSupport == null)
            compressedSupport = compressSolns(support.getSupportPop());

        for (int i = 0; i < compressedSupport.size(); i++) {
            for (int j = 0; j < compressedSupport.get(i).size(); j++) {
                if (compressedSupport.get(i).get(j).events == null)
                    compressedSupport.get(i).get(j).computeEvents();
                if (compressedSupport.get(i).get(j).events.contains(event))
                    numSolns = numSolns + 1;
            }
        }
        
        event.numSolns = numSolns * 100 / support.getSupportPop().size();
        
        //gets confidence for subevents
        eventConfidence(event.subtree1, support);
        eventConfidence(event.subtree2, support);
    }
        
    public void computeConfidence( Solver soln, DrawingObjects panel, SupportPopulation support) {
        eventConfidence(panel.pRoot.event, support);
    }
}
