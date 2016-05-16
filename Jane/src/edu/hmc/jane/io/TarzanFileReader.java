package edu.hmc.jane.io;

/*
 * Copyright (c) 2009, Chris Conow, Daniel Fielder, Yaniv Ovidia, Ran
 * Libeskind-Hadas All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. Neither the name of the Harvey Mudd College nor the
 * names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/**
 *
 * @Modified by Ki Wan Gkoo
 */
import edu.hmc.jane.Phi;
import edu.hmc.jane.ProblemInstance;
import edu.hmc.jane.TimeZones;
import edu.hmc.jane.Tree;
import edu.hmc.jane.TreeRegions;
import java.io.*;
import java.util.*;

public class TarzanFileReader extends TreeFileReader {

    Map<Integer, Integer> entryToName;
    Map<Integer, Integer> entryToNameH = new TreeMap<Integer, Integer>();
    Map<Integer, Integer> entryToNameP = new TreeMap<Integer, Integer>();
    boolean hasHostPolytomy;
    boolean hasParaPolytomy;
    static final boolean HOST = true;
    static final boolean PARA = false;
    static final int INFINITE_DISTANCE = 410338673;

    public TarzanFileReader(String filename) throws FileNotFoundException {
        super(filename);
        hasHostPolytomy = false;
        hasParaPolytomy = false;
    }

    Map<Integer, Map<Integer, Integer>> readRegionCosts() throws IOException, FileFormatException {
        Map<Integer, Map<Integer, Integer>> answer = new HashMap<Integer, Map<Integer, Integer>>();
        try {
            while (true) {
                String inline = fin.readLine();
                if (inline == null) {
                    break;
                }
                String[] entry = inline.split("\\s");
                if (entry.length < 3) {
                    break;
                }
                Integer firstRegion = Integer.parseInt(entry[0]);
                Integer secondRegion = Integer.parseInt(entry[1]);
                
                String costString = entry[2].toLowerCase();
                Integer cost;
                if (costString.equals("inf") || costString.equals("i") || costString.equals("infinity") || costString.equals("infty")) {
                    cost = INFINITE_DISTANCE;
                } else {
                    cost = Integer.parseInt(entry[2]);
                }
                
                if (!answer.containsKey(firstRegion)) {
                    answer.put(firstRegion, new HashMap<Integer, Integer>());
                }
                answer.get(firstRegion).put(secondRegion, cost);
            }
            return answer;
        } catch (NumberFormatException ex) {
            throw new FileFormatException("Every entry in the cost mapping for regions must be a number.");
        }
    }

    // NOTE: issues with timing of host stuff?
    Map<Integer, Vector<Integer>> readTree(boolean host) throws IOException, FileFormatException {
        try {
            int counter = 0;
            int c1;
            int c2;
            int childName;
            if (host) {
                entryToName = entryToNameH;
            } else {
                entryToName = entryToNameP;
            }

            Map<Integer, Vector<Integer>> answer = new TreeMap<Integer, Vector<Integer>>();
            while (true) {
                String nextLine = fin.readLine();
                if (nextLine.equals(""))
                    break;
                String[] entry = nextLine.split("\\s");
                if (entry.length < 2) {
                    throw new FileFormatException("The keyword null is required to identify tip edges in the .tree file format");
                }
                int name = Integer.parseInt(entry[0]);
                if (!entryToName.containsKey(name)) {
                    entryToName.put(name, counter);
                    name = counter++;
                } else {
                    name = entryToName.get(name);
                }
                Vector<Integer> children = new Vector<Integer>();
                // Allow two children
                for (int i = 1; i < entry.length; i++) {
                    if (!entry[i].equals("null") && entry.length >= 2) {
                        c1 = Integer.parseInt(entry[i]);
                        if (!entryToName.containsKey(c1)) {
                            entryToName.put(c1, counter);
                            childName = counter++;
                        } else {
                            childName = entryToName.get(c1);
                        }
                        children.add(childName);
                    }
                }
                
                if (children.size() > 2 && host) {
                    hasHostPolytomy = true;
                }
                if (children.size() > 2 && !host) {
                    hasParaPolytomy = true;
                }
                /*
                 * if (!entry[1].equals("null")) { c1 =
                 * Integer.parseInt(entry[1]); if
                 * (!entryToName.containsKey(c1)){ entryToName.put(c1, counter);
                 * childName = counter++; } else { childName =
                 * entryToName.get(c1); } children.add(childName); } if
                 * (!entry[2].equals("null")) { c2 = Integer.parseInt(entry[2]);
                 * if (!entryToName.containsKey(c2)){ entryToName.put(c2,
                 * counter); childName = counter++; } else { childName =
                 * entryToName.get(c2); } children.add(childName);
            }
                 */
                answer.put(name, children);
            }

            return answer;

        } catch (NumberFormatException e) {
            throw new FileFormatException("Specifications of tree structure must use only numbers and the word \"null\"");
        }
    }

    String[] readNames(int size, boolean host) throws IOException, FileFormatException {
        try {
            String[] answer = new String[size];
            if (host) {
                entryToName = entryToNameH;
                answer = new String[size + 1]; // accommodate dummy
            } else {
                entryToName = entryToNameP;
            }
            while (true) {
                String nextLine = fin.readLine();
                //Check whether the next line is blank
                if (nextLine.equals("")) { 
                    break;
                }
                String[] entry = nextLine.split("\\s", 2);
                if (entry.length < 2) {
                    throw new FileFormatException("Each host/parasite to be named must be given a name or number");
                }
                int name = Integer.parseInt(entry[0]);
                name = entryToName.get(name);
                answer[name] = entry[1];
            }
            return answer;
        } catch (NumberFormatException ex) {
            throw new FileFormatException("Each host/parasite to be named must be identified by a number");
        }
    }

    Phi readPhi(int maxMap) throws IOException, FileFormatException {
        try {
            Phi answer = new Phi(maxMap);

            while (true) {
                String nextLine = fin.readLine();
                String[] entry;
                if (nextLine == null) {
                    break;
                } else {
                    entry = nextLine.split("\\s");
                }

                if (entry.length < 2) {
                    break;
                }
                int hName = Integer.parseInt(entry[0]);
                hName = entryToNameH.get(hName);
                int pName;
                for (int i = 1; i < entry.length; ++i) {
                    pName = Integer.parseInt(entry[i]);
                    if (!entryToNameP.containsKey(pName))
                        throw new FileFormatException("Parasite node being mapped is not a tip or does not exist");
                    pName = entryToNameP.get(pName);
                    answer.addAssociation(hName, pName);
                }
            }
            return answer;
        } catch (NumberFormatException ex) {
            throw new FileFormatException("Every host and parasite must be identified by a number.");
        }
    }

    Map<Integer, Integer> readRegions() throws IOException, FileFormatException {
        try {
            Map<Integer, Integer> answer = new TreeMap<Integer, Integer>();
            while (true) {
                String[] entry = fin.readLine().split("\\s");
                if (entry.length < 2) {
                    break;
                }

                int host = entryToNameH.get(Integer.parseInt(entry[0]));
                int region = Integer.parseInt(entry[1]);
                if (region <= 0) {
                    throw new FileFormatException("Regions must be positive.");
                }
                answer.put(host, region);
            }
            return answer;
        } catch (NumberFormatException ex) {
            throw new FileFormatException("Every host and region in the region mapping must be identified by a number.");
        }
    }

    void readRanks(TimeZones timeZones, boolean host) throws IOException, FileFormatException {
        boolean doneReading = false;

        if (host) {
            entryToName = entryToNameH;
        } else {
            entryToName = entryToNameP;
        }

        try {
            while (!doneReading) {
                String s = fin.readLine();
                if (s == null) {
                    doneReading = true;
                    continue;
                }
                String[] entry = s.split("[\\s,]");

                // Get and store the next node/zone pair.
                int node;
                switch (entry.length) {
                    case 3:
                        node = entryToName.get(Integer.parseInt(entry[0]));
                        int zoneStart = Integer.parseInt(entry[1]);
                        int zoneEnd = Integer.parseInt(entry[2]);
                        if (zoneStart <= 0) {
                            throw new FileFormatException("Only positive integer time zones are allowed.");
                        }
                        if (zoneStart > zoneEnd) {
                            throw new FileFormatException("The end of a range of time zones must be greater than the start.");
                        }
                        if (host && zoneStart == zoneEnd) {
                            timeZones.setHostZone(node, zoneStart);
                        } else if (host) {
                            timeZones.setHostZone(node, zoneStart, zoneEnd);
                        } else {
                            timeZones.setParasiteZone(node, zoneStart, zoneEnd);
                        }
                        break;
                    case 2:
                        node = entryToName.get(Integer.parseInt(entry[0]));
                        int zone = Integer.parseInt(entry[1]);
                        if (zone <= 0) {
                            throw new FileFormatException("Only positive integer time zones are allowed.");
                        }
                        if (host) {
                            timeZones.setHostZone(node, zone);
                        } else {
                            timeZones.setParasiteZone(node, zone);
                        }
                        break;
                    default:
                        doneReading = true;
                        break;
                }
            }
        } catch (NumberFormatException exc) {
            throw new FileFormatException("All host/parasite vertices and time zones must be represented by numbers");
        }
    }

    boolean readToToken(String token) throws java.io.IOException {
        String s = fin.readLine();
        while (s != null && !token.equals(s)) {
            s = fin.readLine();
        }
        return token.equals(s);
    }

    String readToEitherToken(String firstToken, String secondToken) throws java.io.IOException {
        String s = fin.readLine();
        while (s != null && !s.equals(firstToken) && !s.equals(secondToken)) {
            s = fin.readLine();
        }
        return s;
    }

    void seekString(String s) throws FileFormatException, java.io.IOException {
        if (!readToToken(s)) {
            throw new FileFormatException("Missing token " + s + " in input file. Please choose a valid tarzan tree file");
        }
    }

    public ProblemInstance readProblem() throws FileFormatException, java.io.IOException {
        seekString("HOSTTREE");
        Map<Integer, Vector<Integer>> host = readTree(HOST);

        seekString("HOSTNAMES");
        String[] hostNames = readNames(host.size(), HOST);

        seekString("PARASITETREE");
        Map<Integer, Vector<Integer>> parasite = readTree(PARA);

        seekString("PARASITENAMES");
        String[] parasiteNames = readNames(parasite.size(), PARA);

        seekString("PHI");
        Phi phi = readPhi(parasite.size());

        TimeZones timeZones = new TimeZones(host.size(), parasite.size());

        // Don't force the user to input time zone information but if they do
        // ensure that it is present for both the host and parasite trees.
        String nextToken = readToEitherToken("HOSTRANKS", "HOSTREGIONS");

        if (nextToken != null && nextToken.equals("HOSTRANKS")) {
            readRanks(timeZones, HOST);
            seekString("PARASITERANKS");
            readRanks(timeZones, PARA);
        }

        timeZones.doneReading();

        Tree hostTree = new Tree(host, hostNames, entryToNameH, hasHostPolytomy);
        hostTree.preComputePreOrderTraversal();      
        Tree parasiteTree = new Tree(parasite, parasiteNames, null, hasParaPolytomy);
        parasiteTree.preComputePreOrderTraversal();



        if (timeZones.areUsed()) {
            if (!timeZones.hostZoneAssignmentCompleted()) {
                throw new FileFormatException("Partial time zone assignment in the host tree.");
            }

            if (!timeZones.hostZonesConsecutive()) {
                throw new FileFormatException("No gaps may appear in the host time zones.");
            }

            if (!timeZones.parasiteZoneAssignmentCompleted()) {
                throw new FileFormatException("Partial time zone assignment in the parasite tree.");
            }

            if (!timeZones.respectHostAncestry(hostTree)) {
                throw new FileFormatException("A child's time zone occurs before its parent's time zone in the host tree.");
            }

            if (!timeZones.respectParasiteAncestry(parasiteTree)) {
                throw new FileFormatException("A child's time zone occurs before its parent's time zone in the parasite tree.");
            }

            for (int p : parasite.keySet()) {
                for (int h : phi.getHosts(p)) {
                    if (!timeZones.overlap(p, h)) {
                        throw new FileFormatException("A parasite maps to a host in a different time zone.");
                    }
                }
            }
        }

        for (Map.Entry<Integer, Vector<Integer>> par : parasite.entrySet()) {
            if (par.getValue().isEmpty() && !phi.hasAHost(par.getKey())) {
                throw new FileFormatException("Not all parasite tips are mapped to host tips.");
            }
        }

        TreeRegions treeRegions;

        if ((nextToken != null) && (nextToken.equals("HOSTREGIONS") || readToToken("HOSTREGIONS"))) {
            Map<Integer, Integer> hostRegions = readRegions();

            seekString("REGIONCOSTS");

            Set<Integer> regions = new TreeSet<Integer>();

            regions.addAll(hostRegions.values());

            Map<Integer, Map<Integer, Integer>> costsMapping = readRegionCosts();

            int largestRegion = 0;

            for (Integer r1 : regions) {
                if (r1 > largestRegion) {
                    largestRegion = r1;
                }

                if (!costsMapping.containsKey(r1)) {
                    costsMapping.put(r1, new HashMap<Integer, Integer>());
                }

                for (Integer r2 : regions) {
                    if (!costsMapping.get(r1).containsKey(r2)) {
                        costsMapping.get(r1).put(r2, 0);
                    }
                }
            }

            treeRegions = new TreeRegions(costsMapping, hostRegions, largestRegion);
        } else {
            treeRegions = new TreeRegions();
        }

        return new ProblemInstance(hostTree, parasiteTree, treeRegions, phi, timeZones);
    }
}
