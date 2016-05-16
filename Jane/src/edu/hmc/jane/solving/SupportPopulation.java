/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.solving;

import edu.hmc.jane.CostModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 *
 * @author summer
 */
public class SupportPopulation {
    List<EventSolver> population;
    public boolean isActive;
    public List<EventSolver> currentSolutions;
    public CostModel c;
    public int popSize;
    
    
    public SupportPopulation() {
        this.isActive = false;
        this.population = null;
        
    }
    public void setCurrentSolutions(List<EventSolver> currentSolutions){
        this.currentSolutions = currentSolutions;
    }
    
    public void setCostModel(CostModel costModel){
        this.c = costModel;
    }
    public void setPopSize(int size){
        this.popSize = size;
    }
    
    public void addPopulation(List<EventSolver> population) {
        this.population = population;
    }
    
    public List<EventSolver> getSupportPop(){
        if(!isActive && currentSolutions != null){
            generateSupportPopulation();
            isActive = true;
        }
        return population;
    }
    
    // Assumes the cost model and current solutions have been set
    private EventSolver selectRandomTiming() {
        Random r = new Random();
        int selectedIndex = r.nextInt(currentSolutions.size());
        Solver selected = currentSolutions.get(selectedIndex);
        
        EventSolver newVersion = new EventSolver(selected.hostTiming, selected.phi, selected.timeZones, c);
        newVersion.randomizeDP = true;
        newVersion.solve();
        newVersion.computeEvents();
        
        return newVersion;
    }

    private void generateSupportPopulation() {
        List<EventSolver> solverList = new ArrayList<EventSolver>();

        for (int i = 0; i < popSize; i++) {
            solverList.add(selectRandomTiming());
        }
        addPopulation(solverList);
    }
    
    public void clearPop(){
        population = null;
        isActive = false;
        currentSolutions = null;
        c = null;
    }
    
}
