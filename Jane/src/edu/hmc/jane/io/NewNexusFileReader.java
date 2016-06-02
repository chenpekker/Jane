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

/*
 * NOTE: THIS FILE READER WILL NOT WORK PROPERLY! NEED TO UPDATE IT SO THAT
 * NODES ARE NAMED FROM 0 - TREESIZE
 *
 */
import edu.hmc.jane.Phi;
import edu.hmc.jane.ProblemInstance;
import edu.hmc.jane.TimeZones;
import edu.hmc.jane.Tree;
import edu.hmc.jane.TreeRegions;
import edu.hmc.jane.util.Pair;
import java.io.*;
import java.util.*;

public class NewNexusFileReader extends TreeFileReader {

    private String annoyingExtraData = "";
    boolean knownNexus = false;
    boolean isNexus = false;
    Map<Integer, Vector<Integer>> host = null;
    Map<Integer, String> hostNames = null;
    Map<Integer, Vector<Integer>> parasite = null;
    Map<Integer, String> parasiteNames = null;
    Map<Integer, Integer> hostEntryToName;
    Map<Integer, Integer> parasiteEntryToName;
    String[] pNames;
    String[] hNames;
    Phi phi = null;
    boolean hasHostPolytomy;
    boolean hasParaPolytomy;
    Map<Integer, Pair<Integer>> hostRanks = new HashMap<Integer, Pair<Integer>>();
    Map<Integer, Pair<Integer>> parasiteRanks = new HashMap<Integer, Pair<Integer>>();
    int freeIndex;
    Map<String, String> translation = new HashMap<String, String>();

    public NewNexusFileReader(String filename) throws FileNotFoundException {
        super(filename);
        hasHostPolytomy = false;
        hasParaPolytomy = false;
    }

    public boolean isNewNexus() throws java.io.IOException {
        if (knownNexus) {
            return isNexus;
        }
        BufferedReader check = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
        String s = check.readLine();
        while (s != null && !s.toLowerCase().startsWith("#nexus")) {
            s = check.readLine();
        }
        while (s != null && !s.toLowerCase().startsWith("begin cophylogeny")) {
            s = check.readLine();
        }
        knownNexus = true;
        isNexus = s != null;
        check.close();
        return isNexus;
    }

    String nextLine() throws java.io.IOException {
        StringBuilder curLine = new StringBuilder(annoyingExtraData);
        while (curLine.indexOf(";") == -1) {
            String s = fin.readLine();
            if (s != null) {
                curLine.append(s.trim());
            } else {
                return null;
            }
        }

        String s = curLine.toString();

        annoyingExtraData = s.substring(s.indexOf(";") + 1).trim();

        return s.substring(0, s.indexOf(";"));
    }

    Block nextBlock() throws FileFormatException, java.io.IOException {
        String s = nextLine();
        if (s == null) {
            return null;
        }
        s = s.toLowerCase();
        if (!s.startsWith("begin")) {
            throw new FileFormatException("Unexpected data between blocks in the input file.");
        }
        Block b = new Block();

        b.title = s.substring("begin ".length());
        b.contents = new LinkedList<String>();

        String str;
        for (str = nextLine(); str != null && !(str.toLowerCase().startsWith("endblock") || str.toLowerCase().startsWith("end")); str = nextLine()) {
            for (int open = str.indexOf('['); open != -1; open = str.indexOf('[')) {
                int close = str.indexOf(']', open);
                if (close == -1) {
                    System.err.println("Comment not closed with ]");
                }
                String left = str.substring(0, open);
                String right = str.substring(close + 1, str.length());
                str = left + right;
            }
            b.contents.addLast(str);
        }
        if (str == null) {
            throw new FileFormatException("Block ended without closing");
        }

        return b;
    }

    void readBlock(Block b) throws FileFormatException {
        if ("trees".equals(b.title)) {
            if (b.contents.size() != 3) {
                System.err.println("Expected TRANSLATE, TREE HOST and TREE PARASITE in TREES block.");
            }

            // translate
            String s = b.contents.get(0);
            if (!s.toLowerCase().startsWith("translate")) {
                throw new FileFormatException("Missing keyword TRANSLATE inside TREES data.");
            }
            s = s.substring("translate".length()).trim();
            String[] pairs = s.split(",");
            for (String pair : pairs) {
                String[] map = pair.split("\t");
                if (map.length != 2) {
                    throw new FileFormatException("Missing tab between species code and its translation");
                }
                if (map[1].startsWith("'") && map[1].endsWith("'")) {
                    map[1] = map[1].substring(1, map[1].length() - 1);
                }
                translation.put(map[0], map[1]);
            }

            // host
            s = b.contents.get(1);
            if (!s.toLowerCase().startsWith("tree host")) {
                throw new FileFormatException("Missing keyword HOST inside TREES data.");
            }
            freeIndex = 0;
            host = new HashMap<Integer, Vector<Integer>>();
            hostNames = new HashMap<Integer, String>();
            hostEntryToName = new HashMap<Integer, Integer>();
            TreeParser th = new TreeParser(s.substring(s.indexOf('(')).trim(), host, hostNames, hostRanks, hostEntryToName);
            th.parseTree();
            hasHostPolytomy = th.hasPolytomy;
            hNames = new String[hostNames.size()];
            for (int i = 0; i < hostNames.size(); i++) {
                hNames[i] = hostNames.get(i).intern();
            }

            // parasite
            s = b.contents.get(2);
            if (!s.toLowerCase().startsWith("tree parasite")) {
                throw new FileFormatException("Missing keyword PARASITE inside TREES data.");
            }
            freeIndex = 0;
            parasite = new HashMap<Integer, Vector<Integer>>();
            parasiteNames = new HashMap<Integer, String>();
            parasiteEntryToName = new HashMap<Integer, Integer>();
            TreeParser tp = new TreeParser(s.substring(s.indexOf('(')).trim(), parasite, parasiteNames, parasiteRanks, parasiteEntryToName);
            tp.parseTree();
            hasParaPolytomy = tp.hasPolytomy;
            pNames = new String[parasiteNames.size()];
            for (int i = 0; i < parasiteNames.size(); i++) {
                pNames[i] = parasiteNames.get(i).intern();
            }

        } else if ("cophylogeny".equals(b.title)) {
            boolean phiRead = false;
            for (String s : b.contents) {
                if (!s.toLowerCase().startsWith("phi")) {
                    continue;
                }
                phiRead = true;
                s = s.substring("phi".length()).trim();
                int size = parasite.size();
                phi = new Phi(size);
                String[] pairs = s.split(",");
                for (String pair : pairs) {
                    String[] map = pair.split("\t");
                    if (map.length != 2) {
                        throw new FileFormatException("Missing tab character between host and parasite species in PHI");
                    }
                    if (map[0].startsWith("'") && map[0].endsWith("'")) {
                        map[0] = map[0].substring(1, map[0].length() - 1);
                    }
                    if (map[1].startsWith("'") && map[1].endsWith("'")) {
                        map[1] = map[1].substring(1, map[1].length() - 1);
                    }

                    Integer p = backwardsLookup(parasiteNames, map[0].trim());
                    Integer h = backwardsLookup(hostNames, map[1].trim());
                    if (p == null) {
                        throw new FileFormatException("Unrecognized Parasite: " + map[0]);
                    }
                    if (h == null) {
                        throw new FileFormatException("Unrecognized Host: " + map[1]);
                    }
                    phi.addAssociation(h, p);
                }
            }
            if (!phiRead) {
                throw new FileFormatException("Missing keyword PHI inside COPHYLOGENY data.");
            }
        }
    }

    static Integer backwardsLookup(Map<Integer, String> map, String s) {
        for (Map.Entry<Integer, String> me : map.entrySet()) {
            if (me.getValue().equals(s)) {
                return me.getKey();
            }
        }
        System.out.println(s);
        return null;
    }

    @Override
    public ProblemInstance readProblem() throws FileFormatException, java.io.IOException {
        if (!isNexus) {
            throw new FileFormatException("The file does not contain #NEXUS in the header");
        }

        String str = fin.readLine();
        while (str != null && !str.toLowerCase().startsWith("#nexus")) {
            str = fin.readLine();
        }

        for (Block s = nextBlock(); s != null; s = nextBlock()) {
            readBlock(s);
        }


        if (host == null) {
            throw new FileFormatException("The file does not specify a host tree");
        }
        if (parasite == null) {
            throw new FileFormatException("The file does not specify a parasite tree");
        }
        if (phi == null) {
            throw new FileFormatException("The file does not specify a mapping between parasites and hosts.");
        }

        for (Map.Entry<Integer, Vector<Integer>> par : parasite.entrySet()) {
            if (par.getValue().isEmpty() && !phi.hasAHost(par.getKey())) {
                throw new FileFormatException("Not all parasite tips are mapped to host tips.");
            }
        }

        TimeZones timeZones = new TimeZones(host.size(), parasite.size());
        for (Map.Entry<Integer, Pair<Integer>> entry : hostRanks.entrySet()) {
            int zoneStart = entry.getValue().left;
            int zoneEnd = entry.getValue().right;
            timeZones.setHostZone(entry.getKey(), zoneStart, zoneEnd);
        }

        for (Map.Entry<Integer, Pair<Integer>> entry : parasiteRanks.entrySet()) {
            int zoneStart = entry.getValue().left;
            int zoneEnd = entry.getValue().right;
            timeZones.setParasiteZone(entry.getKey(), zoneStart, zoneEnd);
        }

        timeZones.doneReading();

        Tree hostTree = new Tree(host, hNames, hostEntryToName, hasHostPolytomy);
        hostTree.preComputePreOrderTraversal();  
        Tree parasiteTree = new Tree(parasite, pNames, null, hasParaPolytomy);
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

        TreeRegions treeRegions = new TreeRegions();

        return new ProblemInstance(hostTree, parasiteTree, treeRegions, phi, timeZones);
    }

    class Block {

        String title;
        LinkedList<String> contents;
    }

    class TreeParser {

        int index;
        String tree;
        Map<Integer, Vector<Integer>> relations;
        Map<Integer, String> names;
        Map<Integer, Integer> entryToName = new HashMap<Integer, Integer>();
        Map<Integer, Pair<Integer>> ranks;
        boolean hasPolytomy;

        TreeParser(String t, Map<Integer, Vector<Integer>> r, Map<Integer, String> n, Map<Integer, Pair<Integer>> ranks, Map<Integer, Integer> idToName) {
            index = 0;
            tree = t;
            relations = r;
            names = n;
            this.ranks = ranks;
            this.entryToName = idToName;
            hasPolytomy = false;
        }

        int parseTree() throws FileFormatException {
            Vector<Integer> vi = new Vector<Integer>();
            int myName;
            if (tree.charAt(index) == '(') {
                index++;
                int lchild = parseTree();
                vi.add(lchild);

                if (tree.charAt(index) != ',') {
                    throw new FileFormatException("Malformed tree");
                }
                index++;
                if (tree.charAt(index) == ' ') {
                    index++;
                }

                int rchild = parseTree();
                vi.add(rchild);
                
                while (tree.charAt(index) == ',') {
                    // another child!
                    hasPolytomy = true;
                    index++;
                    if (tree.charAt(index) == ' ') {
                        index++;
                    }
                    int child = parseTree();
                    vi.add(child);
                }

                if (tree.charAt(index) != ')') {
                    throw new FileFormatException("Malformed tree");
                }
                index++;
                while (index < tree.length() && tree.charAt(index) != ')' && tree.charAt(index) != ',') {
                    index++;
                }
                myName = freeIndex;
                freeIndex++;
                if (!entryToName.containsValue(myName)) {
                    entryToName.put(myName, myName);
                }
                relations.put(myName, vi);
                names.put(myName, myName + "");
            } else {
                StringBuilder sb = new StringBuilder();
                while (!"(),:".contains("" + tree.charAt(index))) {
                    sb.append(tree.charAt(index));
                    index++;
                }
                myName = freeIndex;
                freeIndex++;
                if (!entryToName.containsValue(myName)) {
                    entryToName.put(myName, myName);
                }
                relations.put(myName, new Vector<Integer>());
                names.put(myName, translation.get(sb.toString().trim()));
            }

            if (index < tree.length() && tree.charAt(index) == ':') {
                index++;
                if (tree.charAt(index) == '[') {
                    index++;
                    int rangeStart = 0;
                    while (Character.isDigit(tree.charAt(index))) {
                        rangeStart *= 10;
                        rangeStart += tree.charAt(index) - '0';
                        index++;
                    }
                    if (rangeStart < 0) {
                        throw new FileFormatException("Only positive integer time zones are allowed.");
                    }
                    if (tree.charAt(index) == ']') {
                        index++;
                        ranks.put(myName, new Pair<Integer>(rangeStart, rangeStart));
                    } else if (tree.charAt(index) != ',') {
                        throw new FileFormatException("Timings must be of the form [number] or [number,number]");
                    } else {
                        index++;

                        int rangeEnd = 0;
                        while (Character.isDigit(tree.charAt(index))) {
                            rangeEnd *= 10;
                            rangeEnd += tree.charAt(index) - '0';
                            index++;
                        }
                        if (tree.charAt(index) != ']') {
                            System.out.println(tree.substring(index));
                            throw new FileFormatException("Timings must be of the form [number] or [number,number]");
                        }
                        if (rangeStart > rangeEnd) {
                            throw new FileFormatException("The end of a range of time zones must be greater than the start.");
                        }

                        index++;
                        ranks.put(myName, new Pair<Integer>(rangeStart, rangeEnd));
                    }
                } else {
                    while (Character.isDigit(tree.charAt(index)) || tree.charAt(index) == '.') {
                        index++;
                    }
                }
            }

            return myName;
        }
    }
}
