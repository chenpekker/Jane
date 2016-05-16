/*
 * Adds a comparator to sort solutions based on their event counts
 * 
 */
package edu.hmc.jane.solving;

import java.util.Comparator;

/**
 *
 * @author Rebecca
 */
public class EventSolverSort implements Comparator<EventSolver> {
    @Override
    public int compare(EventSolver eSolver1, EventSolver eSolver2) {
        for (int event = 1; event < eSolver1.getEventCount().length; event++) {
            if (eSolver1.getEventCount()[event] > eSolver2.getEventCount()[event]) {
                return -1;
            } else if (eSolver1.getEventCount()[event] < eSolver2.getEventCount()[event]) {
                return 1;
            }
        }
        return 0;
    }
}
