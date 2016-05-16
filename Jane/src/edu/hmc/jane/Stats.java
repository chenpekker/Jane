package edu.hmc.jane;

import edu.hmc.jane.solving.Heuristic;
import java.text.DecimalFormat;
import java.util.Arrays;

/**
 *
 * @author Tselil
 */
public class Stats {

    // DATA MEMBERS
    public HistoData data;
    protected CostModel costs;
    public static int iterationsComplete = 0;
    boolean includeOrig;

    /* holds information about the cost distribution of the random population;
     * the non-randomized best mapping cost is the element at dist[0]; and is
     * considered one of the data points; thus, it may be the min and is counted
     * in the avg.
     */
    public class HistoData {
        public boolean hasOrig;
        public int orig, max, min, median, quartOne, quartThree;
        public double avg, percentileOfOrig, stdDev; //percentOfOrig is number of solns with cost <= original
        int numInf;
        public int[] dist;
        public int totalCheaperThanOrig;
        public HistoData(int ma, int mi, int med, int q1, int q3, double stD, double a, double p, int[] d, int nI, int orig, boolean hasOrig, int totalCheaperThanOrig) {
            this.hasOrig = hasOrig;
            this.orig = orig;
            max = ma;
            min = mi;
            median = med;
            quartOne = q1;
            quartThree = q3;
            stdDev = stD;
            avg = a;
            percentileOfOrig = p;
            dist = d;
            numInf = nI;
            this.totalCheaperThanOrig=totalCheaperThanOrig;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < data.dist.length; i++) {
                b.append(data.dist[i]);
                b.append("\n");
            }
            return b.toString();
        }
    }

    public Stats( CostModel c, ProblemInstance prob, int rPSize, int numGens,
                  int pSize, boolean includeOrig, boolean isPhiRandomizer,
                  boolean isTreeRandomizer, double beta, double mutationRate, double selectStr ) {
        costs = c;
        this.includeOrig = includeOrig;
        // Calculate the random data. This is an expensive time step!
        if (Jane.VERBOSE) {
            System.out.println("Computing the random sample costs...");
        }
        data = solutionCostDist( prob.hostTree, prob.parasiteTree, prob.hostRegions, prob.phi, prob.timeZones, rPSize, numGens, pSize,
                                     includeOrig, isPhiRandomizer, isTreeRandomizer, beta, mutationRate, selectStr);
        if (Jane.VERBOSE) {
               System.out.println("Random sample costs computed");
        }
    }

    public HistoData solutionCostDist( Tree hostTree, Tree paraTree, TreeRegions hostRegions,
                                       Phi phi, TimeZones timeZones, int numPoints,
                                       int numGen, int popSize, boolean includeOrig,
                                       boolean isPhi, boolean isTree, double beta,
                                       double mutRate, double selectStr) {
        int[] answer = new int[numPoints];
        int comparisonCost = costs.INFINITY;
        int cost = costs.INFINITY;
        int min = costs.INFINITY;
        int max = 0;
        double total = 0;
        int numInft = 0;
        int totalCheaper = 0; //number of solutions with cost <= original
        MappingRandomizer phiGenerator = new MappingRandomizer( phi, hostTree, paraTree );
        Phi mapping;
        Heuristic genetic;
        Tree pTree = paraTree;

        if (includeOrig) {
            try {
                genetic = new Heuristic(hostTree, paraTree, hostRegions, phi, timeZones, costs);
                cost = genetic.runSimpleEvolution(numGen, popSize, mutRate, selectStr);
                if (!costs.isInfinity(cost)){
                    if (cost < min)
                        min = cost;
                    if (cost > max)
                        max = cost;
                    comparisonCost = cost;
                }
            } catch(Exception e) {
                System.err.println("Error when running genetic algorithm");
                System.err.println(e);
            }
        }

        for (int i = 0; i < numPoints; i++){
            // Get the randomized problem aspect.
            if (isTree){
                pTree = new Tree(paraTree.numTips, beta);
                mapping = phiGenerator.generateRandomFrom(pTree);
            } else // already generated random map if tree
                mapping = phiGenerator.generateRandom();

            // Solve the random problem.
            try {
                if (Jane.VERBOSE){
                    System.out.println("Round "+i+" out of "+numPoints);
                }
                genetic = new Heuristic( hostTree, pTree, hostRegions, mapping, timeZones, costs);
                cost = genetic.runSimpleEvolution( numGen, popSize, mutRate, selectStr );
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
            
            if (!costs.isInfinity(cost)) {
                if (cost <= comparisonCost)
                    totalCheaper++;
                if (cost < min)
                    min = cost;
                if (cost > max)
                    max = cost;
                total += cost;
                answer[i] = cost;
            } else {
                numPoints--;
                numInft++;
                answer[i] = -1;
            }

            iterationsComplete++;
        }

        int firstIndex = numInft;
        Arrays.sort(answer);
        int med = answer[ firstIndex + numPoints / 2];
        int q1 = answer[ firstIndex + numPoints / 4];
        int q3 = answer[ firstIndex + numPoints * 3 / 4];

        double avg = total/((double) numPoints);

        // Calculate standard deviation
        double stdDev = 0;
        for (int i = firstIndex; i < answer.length; i++) {
            stdDev += Math.pow(answer[i] - avg, 2);
        }
        stdDev /= numPoints;
        stdDev = Math.sqrt(stdDev);

        double percentile = 100 * ((double) totalCheaper) / ((double) numPoints);
        return new HistoData(max, min, med, q1, q3, stdDev, avg, percentile, answer, numInft, comparisonCost, includeOrig, totalCheaper);
    }

    @Override
    public String toString() {
        DecimalFormat twoD = new DecimalFormat("#.##");

        String statString = "Number of Samples: " + data.dist.length;
        statString += "\nMean: " + (Math.round(data.avg*100.0)/100.0);
        statString += "\nStandard Deviation: " + twoD.format(data.stdDev);
        statString += "\n1st Quartile: " + data.quartOne;
        statString += "\nMedian: " + data.median;
        statString += "\n3rd Quartile: " + data.quartThree;
        if (includeOrig) {
            statString += "\nOriginal Cost: " + data.orig;
            statString += "\n# of Sample Best Costs <= Original: " + data.totalCheaperThanOrig;
            statString += "\n% Sample Best Costs <= Original: " + twoD.format(data.percentileOfOrig) + "%";
        }
        return statString;
    }
    
    // getter for the includeOrig boolean, needed for PValueHistogram
    public boolean getIncludeOrig() {
        return includeOrig;
    }
}
