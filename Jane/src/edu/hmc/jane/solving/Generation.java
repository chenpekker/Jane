/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.solving;

import edu.hmc.jane.*;
import edu.hmc.jane.TreeSpecs.InconsistentTreeException;
import edu.hmc.jane.solving.Heuristic.NoValidSolutionException;
import edu.hmc.jane.util.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * This class holds a population of TreeTimings. It provides methods for
 * constructing a new random population as well as breeding. Intuitively, you
 * can think of it as representing a single generation of the genetic algorithm.
 *
 * @author jpeebles
 */
public class Generation {

    Collection<TreeSpecs> population;
    //we store all the hashes of all the timings we find with the best cost
    //ever. This gets cleared if the best cost changes. We use this for
    //determining whether we have already encountered a particular timing. Note
    //that we don't store all the timings themselves.
    private Set<Integer> bestTimingHashes = new HashSet<Integer>();
    //we will only keep up to this
    //many best timings between generations
    private final int NUM_BEST_TIMINGS_KEPT;
    //cache this many previous timing solutions
    private final int NUM_PREVIOUS_TIMINGS_KEPT;
    //Additionally, we keep the some of the best unique timings ever to display to the
    //user at the end. The amount kept can be configured by changing
    //NUM_BEST_TIMINGS_KEPT. The ones we keep are chosen randomly (uniformly).
    private List<TreeSpecs> bestTimings;
    //We also keep around the results from the 4000 most recent solves, just in
    //case we get a duplicate timing, so we can just look it up, rather than
    //solving it again. The amount kept can be configured by changing
    //NUM_PREVIOUS_TIMINGS_KEPT
    private Map<TreeSpecs, CostSolver> previousSolutions;
    private List<CostSolver> validSolutions; //valid solutions in increasing cost order
    boolean solved = false;
    boolean readyForSelect = false;
    private int bestCost;
    private int avgCost; //average cost not including infinite costs
    private CostModel costModel;
    private final int size;
    double selectionStrength;
    private static ExecutorService exec = Executors.newFixedThreadPool(Jane.getNumThreads(), DaemonThreadFactory.get());
    double[] weights;
    ProblemInstance prob; //contains non-resolved polytomies
    boolean hasAncestors = false; //whether this generation was constructed from
    //a previous one
    int bestCostEver; //best cost ever encountered including in this generation
    boolean betterThanLast = false;
    private int cacheHits;


    /*
     * constructs an empty generation
     */
    private Generation(ProblemInstance prob, CostModel costs, int size, double selectionStrength, int numSolnsSaved) {
        this.prob = prob;
        this.costModel = costs;
        this.size = size;
        this.selectionStrength = selectionStrength;
        this.weights = new double[size];
        this.cacheHits = 0;
        this.bestCost = costModel.INFINITY;
        this.bestCostEver = costModel.INFINITY;
        this.validSolutions = new ArrayList<CostSolver>(size);

        //default is 30
        if (numSolnsSaved < 0) {
            numSolnsSaved = 30;
        }
        if (prob.hostTree.hasPolytomy || prob.parasiteTree.hasPolytomy) {
            NUM_BEST_TIMINGS_KEPT = Math.min(numSolnsSaved, 1000);
        } else {
            NUM_BEST_TIMINGS_KEPT = numSolnsSaved;
        }
        if (prob.hostTree.hasPolytomy || prob.parasiteTree.hasPolytomy) {
            NUM_PREVIOUS_TIMINGS_KEPT = 5000;
        } else {
            NUM_PREVIOUS_TIMINGS_KEPT = 5000;
        }
        
        previousSolutions = new BufferedHashMap<TreeSpecs, CostSolver>(NUM_PREVIOUS_TIMINGS_KEPT);
        bestTimings = new RandomBufferedArrayList<TreeSpecs>(NUM_BEST_TIMINGS_KEPT);
    }

    /*
     * constructs a new generation that is the same as the old one except that
     * the population is now equal to newPopulation
     */
    public Generation(Collection<TreeSpecs> newPopulation, Generation oldGeneration)
            throws NoValidSolutionException {

        this(oldGeneration.prob, oldGeneration.costModel, oldGeneration.size,
                oldGeneration.selectionStrength, oldGeneration.NUM_BEST_TIMINGS_KEPT);

        this.population = newPopulation;
        this.hasAncestors = true;
        oldGeneration.ensureSolved();
        this.bestCostEver = oldGeneration.bestCostEver;
        this.bestTimings.addAll(oldGeneration.bestTimings);
        this.bestTimingHashes.addAll(oldGeneration.bestTimingHashes);
        this.previousSolutions = oldGeneration.previousSolutions;
        this.cacheHits = oldGeneration.cacheHits;
    }

    /*
     * specifying -1 for numSolnsSaved makes it use the default value
     */
    public static Generation getRandomPopulation(ProblemInstance prob, CostModel costs, int size, double selectionStrength, int numSolnsSaved) throws InconsistentTreeException {
        
        Generation gen = new Generation(prob, costs, size, selectionStrength, numSolnsSaved);

        gen.population = new ArrayList<TreeSpecs>(size);

        for (int i = 0; i < size; i++) {
            TreeSpecs newTiming = prob.hostTree.getRandomSpecs(prob.timeZones, prob.parasiteTree);
            if (!newTiming.isConsistent()) {
                throw new TreeSpecs.InconsistentTreeException("An inconsistent tree timing appeared in the initial population.");
            }
            gen.population.add(newTiming);
        }

        return gen;
    }

    /*
     * see bestCostEver variable
     */
    public int getBestCostEver() throws NoValidSolutionException {
        ensureSolved();
        return bestCostEver;
    }

    /*
     * solves all TreeSpecs instances in the population
     */
    private void solveAll() {
        List<Future<CostSolver>> futureSolves = new ArrayList<Future<CostSolver>>(size);

        for (TreeSpecs timing : population) {
            if (previousSolutions.containsKey(timing)) {
                CostSolver c = previousSolutions.get(timing);
                cacheHits++;
                CostSolver.solvesDone++;
                if (c.cost < costModel.INFINITY) {
                    validSolutions.add(c);
                }
            } else {
                Phi newPhi = prob.phi.newWithoutPolytomies(timing.parasiteTree.size - prob.phi.length());
                Future<CostSolver> futureSolve =
                        exec.submit((Callable<CostSolver>) new CostSolver(timing,
                        newPhi, prob.timeZones, costModel));

                futureSolves.add(futureSolve);
            }
        }

        try {
            for (Future<CostSolver> c : futureSolves) {
                if (c.get().cost < costModel.INFINITY) {
                    validSolutions.add(c.get());
                }
            }
            
            Collections.sort(validSolutions);
        } catch (ExecutionException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /*
     * Selects a timing at random, but not uniformly, instead exponentially
     * weighting the decision based on the cost of a timing where a lower cost
     * corresponds to a greater likelihood of selection.
     */
    TreeSpecs weightedSelect() throws NoValidSolutionException {
        ensureReadyForSelect();

        double choice = Jane.rand.nextDouble();

        int index = Arrays.binarySearch(weights, choice);

        if (index < 0) {
            index = -index - 1;
        }

        if (index >= validSolutions.size()) {
            return validSolutions.get(validSolutions.size() - 1).hostTiming;
        } else {
            return validSolutions.get(index).hostTiming;
        }
    }

    public void ensureSolveAttempted() {
        try {
            ensureSolved();
        } catch (NoValidSolutionException ignore) {
        }
    }

    public void ensureSolved() throws NoValidSolutionException {
        if (!solved) {
            solveAll();
            calculatePopulationInfo();

            if (validSolutions.isEmpty()) {
                throw new NoValidSolutionException("No valid solution found.");
            }
            
            if (bestCost < bestCostEver) {
                bestCostEver = bestCost;
                bestTimings.clear();
                bestTimingHashes.clear();
            }
            
            for (CostSolver c : validSolutions) {
                if (c.cost == bestCostEver) {
                    if (!bestTimingHashes.contains(c.hostTiming.hashCode())) {
                        bestTimings.add(c.hostTiming);
                        bestTimingHashes.add(c.hostTiming.hashCode());
                    }
                }
                previousSolutions.put(c.hostTiming, c);
            }
        }
        solved = true;
    }

    private void calculatePopulationInfo() {
        for (CostSolver s : validSolutions) {
            bestCost = Math.min(bestCost, s.cost);
            avgCost += s.cost / validSolutions.size();
        }
    }

    /*
     * returns the fitness value corresponding to cost. This is exponentially
     * weighted.
     */
    private double getFitness(int cost) {
        return Math.exp(selectionStrength * (avgCost - cost));
    }

    /*
     * get things set up so that we can take samples from the population (helper
     * method for weightedSelect())
     */
    private void ensureReadyForSelect() throws NoValidSolutionException {
        ensureSolved();
        if (!readyForSelect) {
            weights = new double[size];
            double total = 0;

            // total fitness (bigger is better) across all solved networks
            // we use a sorted set so that this computation is more numerically stable
            for (CostSolver s : validSolutions) {
                total += getFitness(s.cost);
            }

            // partition the interval [0,1) with each partition proportional to the
            // fitness of the TreeSpecs it corresponds to (we'll use this to randomly
            // choose which timing to return)
            weights[0] = getFitness(validSolutions.get(0).cost) / total;

            for (int i = 1; i < validSolutions.size(); i++) {
                weights[i] = getFitness(validSolutions.get(i).cost) / total + weights[i - 1];
            }

            readyForSelect = true;
        }
    }

    public int size() {
        return size;
    }

    /*
     * returns whether this generation has a best cost that is lower than that
     * of the last generation or false if there is no previous generation or if
     * the current generation has no valid solution
     */
    public boolean betterThanLast() {
        try {
            ensureSolved();
        } catch (NoValidSolutionException e) {
            return false;
        }
        return betterThanLast;
    }


    /*
     * returns all best timings we have stored in bestTimings (up to
     * NUM_BEST_TIMINGS_KEPT), but not necessarily all the best timings we have
     * ever encountered
     */
    public List<TreeSpecs> getSomeBestTimings() {
        ensureSolveAttempted();
        return bestTimings;
    }

    /*
     * returns the number of (probably) unique timings we have ever seen at the
     * current best cost ever.
     */
    public int getNumUniqueBestEverTimingsFound() {
        ensureSolveAttempted();
        return bestTimingHashes.size();
    }

    /*
     * total number of times that a solve could be avoided by using the
     * previousSolutions cache for this and all previous generations.
     */
    public int getCacheHits() {
        ensureSolveAttempted();
        return cacheHits;
    }
}
