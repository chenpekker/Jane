/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.solving;

import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;

/**
 *
 * @author Dave
 */
public class MultihostConfiguration {

    public class EntryInfo {

        int name;
        Set<Integer> tips;

        public EntryInfo(int name, Set<Integer> tips) {
            this.name = name;
            this.tips = tips;
        }

        @Override
        public String toString() {
            return "Name: " + Integer.toString(name) + ", tips: " + tips.toString();
        }
        
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof EntryInfo)) {
                return false;
            }
            EntryInfo otherEntry = (EntryInfo) other;
            return name == otherEntry.name && tips.equals(otherEntry.tips);
        }
        
        @Override
        public int hashCode() {
            return name + 37*tips.hashCode();
        }
    }

    public class CostInfo {

        long cost;
        int roundNumber;

        public CostInfo(long cost, int roundNumber) {
            this.cost = cost;
            this.roundNumber = roundNumber;
        }

        @Override
        public String toString() {
            return Integer.toString((int) cost);
        }
    }
    
    private Set<Integer> possibleNames;
    private HashMap<Integer, Set<Integer>> tips;
    private HashMap<EntryInfo, CostInfo> costs;
    private static final long INFINITE_COST = 999999999;
    private int roundNumber; // we use this so we don't have to delete old entries, which is costly.
    /*
     * this is equal to the index of the parasite node this is associated to in
     * the parasite tree.
     */
    private final int nodeName;

    public MultihostConfiguration(int node) {
        possibleNames = new HashSet<Integer>();
        tips = new HashMap<Integer, Set<Integer>>();
        nodeName = node;
        roundNumber = 0;
        costs = new HashMap<EntryInfo, CostInfo>();
    }

    /*
     * constructor with queue of possible names for this edge.
     */
    public MultihostConfiguration(int node, Queue<Integer> names, Queue<Set<Integer>> newTips) {
        nodeName = node;
        possibleNames = new HashSet<Integer>();
        tips = new HashMap<Integer, Set<Integer>>();
        roundNumber = 0;
        costs = new HashMap<EntryInfo, CostInfo>();

        // the interesting case
        while (!names.isEmpty()) {
            int name = names.poll();
            Set<Integer> currTips = newTips.poll();
            addName(name, currTips);
        }
    }

    public void clearCosts() {
        roundNumber++;
    }

    /*
     * returns true if this worked, false if this name already is associated
     *
     */
    public boolean addName(int name, Set<Integer> newTips) {
        if (possibleNames.contains(name)) {
            return false;
        }
        possibleNames.add(name);
        tips.put(name, newTips);
        //costs.put(new EntryInfo(name, newTips), INFINITE_COST);
        return true;
    }

    public Set<Integer> getPossibleNames() {
        return possibleNames;
    }

    public Set<Integer> getTips(int name) {
        return tips.get(name);
    }

    public void setCost(int name, Set<Integer> tips, long cost) {
        // do we wanna ensure this is a valid thing to add?
        costs.put(new EntryInfo(name, tips), new CostInfo(cost, roundNumber));
    }

    public long getCost(int name, Set<Integer> tips) {
        EntryInfo entry = new EntryInfo(name, tips);
        if (costs.containsKey(entry) && costs.get(entry).roundNumber == roundNumber) {
            return costs.get(entry).cost;
        } else {
            // either we don't have an entry, or one from the prior round.
            return INFINITE_COST;
        }

    }

    public long bestCost() {
        long best = INFINITE_COST;
        for (int name : possibleNames) {
            // only consider when we are in charge of everything.
            best = Math.min(best, getCost(name, tips.get(name)));
        }
        return  best;
    }

    @Override
    public String toString() {
        String result = new String();
        for (EntryInfo currentEntry : costs.keySet()) {
            result += currentEntry.toString() + "-> ";
            result += costs.get(currentEntry).toString() + ". ";
        }
        return result;
    }
}
