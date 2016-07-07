package edu.hmc.jane;

/*
Copyright (c) 2009, Chris Conow, Daniel Fielder, Yaniv Ovidia,
Ran Libeskind-Hadas All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the Harvey Mudd College nor the names of its
      contributors may be used to endorse or promote products derived from this
      software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 *
 * @Modified by Ki Wan Gkoo
 * @Modified by Gabriel Quiroz
 */

import edu.hmc.jane.solving.Heuristic;
import edu.hmc.jane.io.FileFormatException;
import edu.hmc.jane.io.NewNexusFileReader;
import edu.hmc.jane.io.TreeFileReader;
import edu.hmc.jane.io.NexusFileReader;
import edu.hmc.jane.io.TarzanFileReader;
import edu.hmc.jane.solving.Embedding;
import edu.hmc.jane.solving.EmbeddingSolver;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;

public class CLI {
    //removed -C flag as option and set Tarzan cost metric as default
    static final int TIP_RANDOMIZATION = 1;
    static final int TREE_RANDOMIZATION = 2;
    //Changed cost presets to 0 for cospeciation, 1 for duplications, 2 for host switches, 1 for losses/sorting, and
    //1 for failiure to diverge
    static int costs[] = {0, 1, 2, 1, 1, 0};
    static CostTuple tuple;
    static int numIter = 30;
    static int popSize = 30;
    static double m = 0.6;
    static double selectStr = .8;
    static String filename = null;
    static boolean tarzan = true;
    static boolean sequentialPolytomy = true;
    static boolean midPolytomyEvents = false;
    static int switchDist = -1;
    static int infestationDist = -1;
    static String outputFile = null;
    static boolean stats = false;
    static int sampleSize;
    static boolean includeOriginal = false;
    static int method=TIP_RANDOMIZATION;
    static double beta = -1.0;
    static String picFilename = null;

    static void usage() {
        System.err.println("usage: jane-cli [-help] [-V] [-c {Cospeciation,Duplication,Switch,Loss,DivergeFailure}] [-m mutation_rate] [-p population_size] [-i generations] [-s selection_strength] [-S max_switch_distance] [-PS allow non-sequential polytomy resolutions] [-PM allow mid-polytomy events] [-o outputFile] [-silent] file");
        System.exit(1);
    }

    static void readArgs(String[] args) {
        int index = 0;

        while (index < args.length) {
            if ("-c".equals(args[index])) {
                index++;
                costs = new int[6];
                
                for (int i = 0; i < 5; ++i) {
                    try {
                        costs[i] = Integer.parseInt(args[index]);
                        if (Math.abs(costs[i]) > 999) {
                            System.err.println("Each event cost must be no more than 999, nor less than -999.");
                            usage();
                        }
                    } catch (Exception e) {
                        System.err.println("The -c flag must be followed with 5 integers representing the costs of various operations.");
                        usage();
                    }
                    index++;
                }
                costs[5] = 0;
            } else if ("-help".equals(args[index]) || "-h".equals(args[index]) || "--h".equals(args[index]) || "--help".equals(args[index]) || "-?".equals(args[index])){
                help();
            } else if ("-o".equals(args[index])) {
                index++;
                try {
                    outputFile = args[index];
                } catch(Exception e) {
                    System.err.println("The -o flag must be followed by a file name.");
                    usage();
                }
                index++;
            } else if ("-m".equals(args[index])) {
                index++;
                try {
                    m = Double.parseDouble(args[index]);
                } catch (Exception e) {
                    System.err.println("The argument following -m must be a number");
                    usage();
                }
                index++;
                if (m < 0 || m > 1) {
                    System.err.println("Mutation rate must be on the interval [0, 1]");
                    usage();
                }
            } else if ("-p".equals(args[index])) {
                index++;
                try {
                    popSize = Integer.parseInt(args[index]);
                } catch (Exception e) {
                    System.err.println("The argument following -p must be a number");
                    usage();
                }
                index++;
                if (popSize <= 0) {
                    System.err.println("The population size must be positive");
                    usage();
                }
            } else if ("-i".equals(args[index])) {
                index++;
                try {
                    numIter = Integer.parseInt(args[index]);
                } catch (Exception e) {
                    System.err.println("The argument following -i must be a number");
                    usage();
                }
                index++;
                if (numIter <= 0) {
                    System.err.println("The number of generations must be positive");
                    usage();
                }
            } else if ("-s".equals(args[index])) {
                index++;
                try {
                    selectStr = Double.parseDouble(args[index]);
                } catch (Exception e) {
                    System.err.println("The argument following -s must be a number");
                    usage();
                }
                index++;
                if (selectStr < 0) {
                    System.err.println("The selection strength must be non-negative");
                    usage();
                }
            } else if ("-V".equals(args[index])) {
                index++;
                Jane.VERBOSE = true;
            }else if ("-PS".equals(args[index])) {
                index++;
                sequentialPolytomy=false;
            } else if ("-PM".equals(args[index])) {
                index++;
                midPolytomyEvents=true;
            } else if ("-S".equals(args[index])) {
                index++;
                try {
                    switchDist = Integer.parseInt(args[index]);
                } catch (Exception e) {
                    System.err.println("The argument following -S must be an integer");
                    usage();
                }
                index++;
                if (switchDist < -1) {
                    System.err.println("The host switch distance must be non-negative, or -1 for unlimited");
                    usage();
                }
            } else if ("-stats".equals(args[index])) {
                index++;
                stats = true;
                try {
                    sampleSize = Integer.parseInt(args[index++]);
                    if (sampleSize <= 0) {
                        System.err.println("The sample size must be positive");
                        usage();
                    }
                } catch (Exception e) {
                    System.err.println("The -stats flag must be followed by the sample size.");
                    usage();
                }
            } else if ("-I".equals(args[index])) {
                index++;
                if (!stats) {
                    System.err.println("-I must come after -stats");
                    usage();
                }
                includeOriginal=true;
            } else if ("-B".equals(args[index])) {
                index++;
                if (!stats) {
                    System.err.println("-B <value> must come after -stats");
                    usage();
                }
                try {
                    beta = Double.parseDouble(args[index++]);
                    method=TREE_RANDOMIZATION;
                } catch (Exception e) {
                    System.err.println("The argument following -B must be a number");
                    usage();
                }
                if (beta < -2.0) {
                    System.err.println("Beta must be at least -2");
                    usage();
                }
            } else if (args[index].equals("-silent")) {
                index++;
                PrintStream devNull = new PrintStream(new OutputStream() {

                    @Override
                    public void write(int b) throws IOException {}

                });

                System.setOut(devNull);
                System.setErr(devNull);
            } else if (args[index].startsWith("-")) {
                System.err.println("Unrecognized switch: " + args[index]);
                usage();
            } else {
                break;
            }
        }
        if (index == args.length - 1) {
            filename = args[index];
        } else if (index >= args.length) {
            System.err.println("No filename provided");
            usage();
        } else {
            System.err.println("Unexpected arguments after the filename " + '"' + args[index] + '"');
            usage();
        }

        tuple = new CostTuple(costs[CostModel.COSPECIATION], costs[CostModel.DUPLICATION], costs[CostModel.LOSS], costs[CostModel.HOST_SWITCH], costs[CostModel.FAILURE_TO_DIVERGE], costs[CostModel.INFESTATION], tarzan);
        
        return;
    }

    public static void main(String[] args) throws FileFormatException, Heuristic.NoValidSolutionException{

        if (!Jane.is64BitJVM()) {
            System.err.println("Warning: You can make Jane about 2x faster "
                             + "by using a 64-Bit JVM instead of the 32-Bit "
                             + "one you are currently using. See the item "
                             + "\"How do I make Jane run faster?\" in the "
                             + "Jane FAQ at cs.hmc.edu/~hadas/jane.");
        }

        readArgs(args);

        TreeFileReader fin = null;
        try {
            fin = new NexusFileReader(filename, false);
            if (!((NexusFileReader) fin).isNexus()) {
                fin = new TarzanFileReader(filename);
            }
            fin = new NexusFileReader(filename, false);
            if (!((NexusFileReader) fin).isNexus()) {
                fin = new NewNexusFileReader(filename);
                if (!((NewNexusFileReader) fin).isNewNexus()) {
                    fin = new TarzanFileReader(filename);
                } else {
                    System.err.println("This file is in CoRe-PA's nexus format. \nOnly the trees' structures, tip names, and tip mappings are processed.");
                }
            }
        } catch (java.io.FileNotFoundException e) {
            System.err.println("Could not find file: " + filename);
            System.exit(1);
        } catch (java.io.IOException e) {
            System.err.println("Error reading file.\n"+e.getLocalizedMessage());
            System.exit(1);
        }

        try{
            final ProblemInstance pi = fin.readProblem();
            
            // polytomy handling
            pi.hostTree.conseqPoly = sequentialPolytomy;
            pi.parasiteTree.conseqPoly = sequentialPolytomy;
            pi.hostTree.noMidPolyEvents = !midPolytomyEvents;
            pi.parasiteTree.noMidPolyEvents = !midPolytomyEvents;
            
            final CostModel c = CostModel.getAppropriate(pi, tuple, switchDist, infestationDist);

            if (!stats) {
                // Solve mode
                Heuristic genetic = new Heuristic(pi.hostTree, pi.parasiteTree, pi.hostRegions, pi.phi, pi.timeZones, c);
                List<TreeSpecs> sols =
                        new LinkedList<TreeSpecs>(
                        genetic.runEvolution(numIter, popSize, m, selectStr, 1).getSomeBestTimings());

                TreeSpecs bestTiming = sols.get(0);

                System.out.println("Best Timing:");
                System.out.println(bestTiming);
                System.out.println("Best Solution:");

                EmbeddingSolver es = new EmbeddingSolver(bestTiming, pi.phi, pi.timeZones, c);
                es.solve();
                Embedding em = new Embedding(es.dpTable.getSolutionViewerInfo());
                System.out.println(em.userString());

                if (outputFile != null && sols.size() > 0) {
                    try {
                        java.io.FileWriter fw = new java.io.FileWriter(outputFile);
                        fw.write(sols.get(0).fileTimingString());
                        fw.close();
                    } catch (java.io.IOException e) {
                        System.err.println("Unable to write to the specified filename");
                        System.err.println(e);
                    }
                }
            } else {
                // Stats mode
                Stats s = new Stats(c, pi, sampleSize, numIter,
                                    popSize, includeOriginal,
                                    method == TIP_RANDOMIZATION,
                                    method == TREE_RANDOMIZATION, beta, m,
                                    selectStr);
                System.out.println(s);

                if (outputFile != null) {
                    try {
                        java.io.FileWriter fw = new java.io.FileWriter(outputFile);
                        fw.write(s.data.toString());
                        fw.close();
                    } catch (java.io.IOException e) {
                        System.err.println("Unable to write to the specified filename");
                        System.err.println(e);
                    }
                }
            }
        } catch (Heuristic.NoValidSolutionException e) {
            System.err.println(e + Heuristic.noValidSolnMessage);
        } catch(Exception e) {
            System.err.println("Something went pretty seriously wrong");
            System.err.println(e);
        }
    }

    private static void help() {
        System.out.println("usage: java Jane [-options] filename\n\n" +
                "Where [-options] include:\n" +
                "\t-help\t\tPrint this help message\n" +
                "\t-V\t\tTurns on verbose output\n" +
                "\t-c <cosp dup switch loss ftd>\n" +
                "\t\t\tThis defines the cost vector to use i.e. -c 0 1 2 3 4 \n" +
                "\t\t\twould cause cospeciations to cost 0, duplications to cost 1, host switches to cost 2, losses/sorting to cost 3, and failures to diverge to cost 4\n" +
                "\t\t\tDefault costs are cospeciation: " + costs[0] + ", duplication: " + costs[1] + ", host switch: " + costs[2] + ", loss/sorting: " + costs[3] + ", failure to diverge: " + costs[4] + "\n" +
                "\t-m <value>\tSets the mutation rate. Appropriate values fall on the interval [0, 1] with 0 being never mutate and 1 being always mutate\n" +
                "\t\t\tDefaults to: " + m + "\n" +
                "\t-p <value>\tSets the initial population size\n" +
                "\t\t\tDefaults to: " + popSize + "\n" +
                "\t-i <value>\tSets the number of generations that the algorithm should run\n" +
                "\t\t\tDefaults to: " + numIter + "\n" +
                "\t-s <value>\tSets the selection strength. 0 is completely random and there is no upper bound\n" +
                "\t\t\tDefaults to: " + selectStr + "\n" +
                "\t-S <value>\tSets the maximum host switch distance allowed. -1 causes the distance to be unlimited\n" +
                "\t\t\tDefaults to: " + switchDist + "\n" +
                "\t-stats <samples>\tSwitches Jane to Stats Mode and sets the number of samples to <samples>\n" +
                "\t-B <value>\tSwitches to Yule model parasite tree randomization with beta equal to the given value\n" +
                "\t-I\tWhen in stats mode, also does a solve of the original tip mapping/trees and prints out some data comparing it to the random sample.\n" +
                "\t-o <filename>\tFor Solve Mode, saves the best host timing to the file <filename>.\n" +
                "\t\t\tFor Stats Mode, saves the sample costs as a comma separated values file (.csv) to the file <filename>.\n");
        System.exit(0);
    }
}
