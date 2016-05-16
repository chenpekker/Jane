/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane;
import java.util.Map;

/**
 * This class contains information about regions,including the region
 * to which each host belongs and the additional cost of host
 * switching/infesting between two regions.
 *
 * @author Tselil Modified by Ki Wan Gkoo
 */
public class TreeRegions {
    // DATA MEMBERS
    public int numRegions;        // regions with no hosts are still counted if
                                  // a larger-numbered region exists.
    private int numHosts;
    private int[][] switch_infestationCost; // the cost of switching/infesting
                                            // from one region to another
    private int[] nodeRegions;    // each node's region (index == node)
    protected boolean hasRegions; // was region information given

    public TreeRegions() {
        numRegions = 1;
        hasRegions = false;
    }
    
    public TreeRegions(Map<Integer, Map<Integer, Integer>> costs,
                         Map<Integer, Integer> regions, int largestRegion) {
        numRegions = largestRegion;
        numHosts = regions.size();
        switch_infestationCost = new int[largestRegion+1][largestRegion+1];
        nodeRegions = new int[numHosts];
        hasRegions = true;

        // Transcribe the regions
        for (Integer i : regions.keySet())
            nodeRegions[i] = regions.get(i);
        
        // Initialize the switch_infestationCost
        for (int i = 0; i < numRegions+1; i++)
            for (int j = 0; j < numRegions+1; j++)
                switch_infestationCost[i][j] = 0;

        // Transcribe the cost to switch/infest from region to region
        for (Integer i : costs.keySet())
            for (Integer j : costs.get(i).keySet())
                switch_infestationCost[i][j] = costs.get(i).get(j);
    }
    
    public int[][] getSwitchCost() {
        return switch_infestationCost;
    }

    public int regionOfNode(int node) {
        if (!hasRegions)
            return 1;
        else
            return nodeRegions[node];
    }

    /*
     * Returns the additional cost for a host switch/an infestation between two regions.
     */
    public int getHostSwitch_InfestationCostBetweenRegions(int from, int to) {
        if (!hasRegions)
            return 0;
        else
            return switch_infestationCost[from][to];
    }

    /*
     * Returns the additional cost for a host switch/an infestation between two nodes.
     */
    public int getHostSwitch_InfestationCostBetweenNodes(int from,
                                                               int to) {
        if (!hasRegions)
            return 0;
        else
            return switch_infestationCost[regionOfNode(from)][regionOfNode(to)];
    }
}
