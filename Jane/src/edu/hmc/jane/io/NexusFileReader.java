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
import beetree.BeeTree;
import java.io.*;
import java.util.*;

public class NexusFileReader extends TreeFileReader {

    private String annoyingExtraData = "";
    boolean knownNexus = false;
    boolean isNexus = false;
    Map<Integer, Vector<Integer>> host = null;
    Map<Integer, String> hostNames = null;
    Map<Integer, Vector<Integer>> parasite = null;
    Map<Integer, String> parasiteNames = null;
    Map<Integer, Integer> hostOrigIDToName = null;
    Map<Integer, Integer> parasiteOrigIDToName = null;
    String[] pNames;
    String[] hNames;
    Phi phi = null;
    boolean hasHostPolytomy;
    boolean hasParaPolytomy;
    Map<Integer, Pair<Integer>> hostRanks = new HashMap<Integer, Pair<Integer>>();
    Map<Integer, Pair<Integer>> parasiteRanks = new HashMap<Integer, Pair<Integer>>();
    int freeIndex;
    boolean loadGui;

    public NexusFileReader(String filename, boolean treeGui) throws FileNotFoundException {
        super(filename);
        hasHostPolytomy = false;
        hasParaPolytomy = false;
        loadGui = treeGui;
    }
    
    // tests to see if the file is a nexus file, returns a boolean
    // has additional testing too 
    public boolean isNexus() throws java.io.IOException {
        if (knownNexus) {
            return isNexus;
        }
        BufferedReader check = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
        String s = check.readLine();

        while (s != null && !s.toLowerCase().startsWith("#nexus")) {
            s = check.readLine();
        }
        if (s != null && s.toLowerCase().startsWith("#nexus")) {
            s = check.readLine();
            // test if the file type is data or taxa (both are types of nexus files), if it isn't, displays error message
            if (s.toLowerCase().startsWith("begin taxa") || s.toLowerCase().startsWith("begin data")) {
                throw new java.io.IOException("Input is either a DATA or TAXA Nexus file type, please convert to Newick format. For help see: www.cs.hmc.edu/~hadas/jane/fileformats.html");
            }
            // test if there is a semicolon after the "begin host" line, if there isn't, displays error message
            if (!s.toLowerCase().contains("host;") && s.toLowerCase().startsWith("begin host")) {
                throw new java.io.IOException("Missing a semicolon directly after 'begin host'");
            }
        }
        while (s != null && !s.toLowerCase().startsWith("begin distribution")) {
            // test if there is a semicolon after the "begin parasite" line, if there isn't, displays error message
            if (s.toLowerCase().startsWith("begin parasite") && !s.toLowerCase().contains("parasite;")) {
                throw new java.io.IOException("Missing a semicolon directly after 'begin parasite'");
            }
            s = check.readLine();
            // test if there is a semicolon after the "begin distribution" line, if there isn't, displays error message
            if (s.toLowerCase().startsWith("begin distribution") && !s.toLowerCase().contains("distribution;")) {
                throw new java.io.IOException("Missing a semicolon directly after 'begin distribution'");
            }
        }
        knownNexus = true;
        isNexus = s != null;
        check.close();
        return isNexus;
    }

    // moves the string to the next line, does additional stuff
    String nextLine() throws java.io.IOException {
        StringBuilder curLine = new StringBuilder(annoyingExtraData);
        while (curLine.indexOf(";") == -1) {
            String s = fin.readLine();
            if (s != null) {
                if (s.indexOf("#") != -1) {
                    s = s.substring(0, s.indexOf("#"));
                }
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

            // checks if a semicolon is missing at the end of the contents line
            if (str.toLowerCase().endsWith("endblock") || str.toLowerCase().endsWith("end")) {  // checks if a semicolon is missing at the end of the contents line
                throw new FileFormatException("Missing a semicolon directly after the contents line");
        }   
            //Test if a Tree is malformed-- if it doesnt have equal numbers of open and closed parentheses
            //Sets up two counts
            int count1 = 0;
            int count2 = 0;
            //Runs a for loop to count the instances of ( and ) characters
            for(int i = 0; i < str.length(); i++){
                if (str.charAt(i) == '('){
                    count1++;
                }
                if (str.charAt(i) == ')'){
                    count2++;
                }
            }
            //if these are not equal, throw the exception
            if (count1 != count2){
                throw new FileFormatException("A tree does not have equal numbers of open and closed parentheses");
            }
            
            //Fix for newick format with parents and lengths at the end of the tree. Begins by testing whether the block is a host or 
            //a parasite, since these are the only blocks with trees.

            if ("host".equals(b.title) || "parasite".equals(b.title)){
                //removes everything from after the last parenthesis
                int last = str.lastIndexOf(')');
                str = str.substring(0, last+1);
                String newS = "";
                //goes into a while loop for the str
                while (true){
                    //finds the first index of a close parenthesis (since this is when a parent would be placed
                    int index = str.indexOf(')');
                    //appends the previous info in str to newS (including the parenthesis)
                    newS += str.substring(0, index + 1);
                    //str gets sliced from the next character after the parenthesis to the end
                    str = str.substring(index+1);
                    //tests if str is now empty, and if it is, it will break from the loop
                    if("".equals(str)){
                        break;
                    }
                    //Test to make sure the next char is not a , or a ), since this would indicate that the next information is a parent
                    //(which we do not want)
                    if(str.charAt(0) != ',' || str.charAt(0) != ')'){
                        //gets the index of the the first instance of a , or ), since this would indicate the parent is finished being 
                        //labled
                        int ID1 = str.indexOf(',');
                        int ID2 = str.indexOf(')');
                        //tests if ID1 or ID2 is negative, since indexOf() produces -1 if there is no instance of the character. We then
                        //set this equal to a large constant
                        if(ID1 < 0){
                            ID1 = 10000000;
                        }
                        if(ID2 < 0){
                                ID2 = 10000000;
                        }
                        //gets the minimum value of ID1, ID2
                        int mini = Math.min(ID1, ID2);
                        //splices str starting from the value above until the end of str
                        str = str.substring(mini);
                    }
                }
                //sets str to be equal to newS (which will be the final tree
                str = newS;
            }//end
            
            b.contents.addLast(str);            
        }
        // if the block does not end with a "endblock;" string, display error
        if (str == null) {
            throw new FileFormatException("Block ended without closing");
        }

        return b;
    }

    void readBlock(Block b) throws FileFormatException {
        if ("host".equals(b.title)) {
            if (b.contents.size() > 1) {
                System.err.println("Unexpected multiple lines inside host block.");
            }
            if (b.contents.size() == 0) {
                throw new FileFormatException("No data inside host block");
            }
        
            String s = b.contents.get(0);
            freeIndex = 0;
            host = new HashMap<Integer, Vector<Integer>>();
            hostNames = new HashMap<Integer, String>();
            hostOrigIDToName = new HashMap<Integer, Integer>();
            TreeParser t = new TreeParser(s.substring(s.indexOf('(')).trim(), host, hostNames, hostRanks, hostOrigIDToName);
            t.parseTree();
            hasHostPolytomy = t.hasPolytomy;
            hNames = new String[hostNames.size()];
            for (int i = 0; i < hostNames.size(); i++) {
                hNames[i] = hostNames.get(i).intern();
                
            }
        } else if ("parasite".equals(b.title)) {
            if (b.contents.size() > 1) {
                System.err.println("Unexpected multiple lines inside parasite block.");
            }
            if (!loadGui && b.contents.size() == 0) {
                throw new FileFormatException("No data inside parasite block");
            }
        
            String s = b.contents.get(0);
            
            freeIndex = 0;
            parasite = new HashMap<Integer, Vector<Integer>>();
            parasiteNames = new HashMap<Integer, String>();
            parasiteOrigIDToName = new HashMap<Integer, Integer>();
            TreeParser t = new TreeParser(s.substring(s.indexOf('(')).trim(), parasite, parasiteNames, parasiteRanks, parasiteOrigIDToName);
            t.parseTree();
            hasParaPolytomy = t.hasPolytomy;
            pNames = new String[parasiteNames.size()];
            for (int i = 0; i < parasiteNames.size(); i++) {
                pNames[i] = parasiteNames.get(i).intern();
            }
        } else if ("distribution".equals(b.title)) {
            if (b.contents.size() != 1) {
                System.err.println("Unexpected multiple lines inside distribution block.");
            }
            if (!loadGui && b.contents.size() == 0) {
                throw new FileFormatException("No data inside distribution block");
            }

            String s = b.contents.get(0);
            phi = parsePhi(s, parasite.size());
        } else {
            System.err.println("Unrecognized block " + b.title);
        }
        
    }

    Phi parsePhi(String s, int size) throws FileFormatException {
        Phi phi = new Phi(size);
        
        // tests to see if line "range" exists, if not, displays error
        if (!s.toLowerCase().startsWith("range")) {
            throw new FileFormatException("Missing the keyword 'range' inside the distribution data block");
        }
        s = s.substring("range".length()).trim();
        
        // checks for both of these errors: Missing a colon between a parasite and a host or comma after the host
        if (!loadGui || (loadGui && s.length() != 0)) { 
            String[] pairs = s.split(",");
            for (String pair : pairs) {
                String[] map = pair.split(":");
                if (map.length != 2) {
                    throw new FileFormatException("Missing a colon between a parasite and a host or comma after the host");
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
        return phi;
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

        for (Block s = nextBlock(); s != null; s = nextBlock()) {
            readBlock(s);
        }

        if (!loadGui) {
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

        Tree hostTree = new Tree(host, hNames, hostOrigIDToName, this.hasHostPolytomy);
        hostTree.preComputePreOrderTraversal();
        Tree parasiteTree = new Tree(parasite, pNames, null, this.hasParaPolytomy);
        parasiteTree.preComputePreOrderTraversal();
        if (timeZones.areUsed()) {
            if (!timeZones.hostZoneAssignmentCompleted()) {
                throw new FileFormatException("Partial time zone assignment in the host tree.");
            }

            if (! loadGui && !timeZones.hostZonesConsecutive()) {
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

            if (!loadGui) {
                for (int p : parasite.keySet()) {
                    for (int h : phi.getHosts(p)) {
                        if (!timeZones.overlap(p, h)) {
                            throw new FileFormatException("A parasite maps to a host in a different time zone.");
                        }
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

        int index;   //Index in the String tree that is curretly being looked at
        String tree; //The tree (hopefully) represented as a String in Newick format
        Map<Integer, Vector<Integer>> relations;  //Contains children of Integer node in Vector
        Map<Integer, String> names;
        Map<Integer, Integer> entryToName = new HashMap<Integer, Integer>();
        Map<Integer, Pair<Integer>> ranks;
        boolean hasPolytomy;

        TreeParser(String t, Map<Integer, Vector<Integer>> r, Map<Integer, String> n, Map<Integer, Pair<Integer>> ranks, Map<Integer, Integer> iDToName) {
            index = 0;
            tree = t;
            relations = r; 
            names = n;
            this.ranks = ranks;
            this.entryToName = iDToName;
            this.hasPolytomy = false;
        }
        /*
         * The code below is commented poorly... I will try to explain. The
         * code promises to return with the index pointing to the character
         * after this (sub)tree ends. Keep that in mind.
         */

        int parseTree() throws FileFormatException {
            int myName = 1;
            if (!loadGui || (loadGui && tree.length() != 1)) { 
                if (tree.charAt(index) == '(') {
                    Vector<Integer> vi = new Vector<Integer>();
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
                    myName = freeIndex;
                    freeIndex++;
                    if (!entryToName.containsKey(freeIndex)) {
                        entryToName.put(freeIndex, myName);
                    }
                    relations.put(myName, vi);
                    names.put(myName, myName + "`"); //Adding the ` to the name fixes any conflicts caused by the actual names of tips being numbers
                    //TODO: See if the ` needs to be added anywhere else
                    } else {
                    StringBuilder sb = new StringBuilder();
                    while (!"(),:".contains("" + tree.charAt(index))) {
                        sb.append(tree.charAt(index));
                        index++;
                    }
                    myName = freeIndex;

                    freeIndex++;
                    if (!entryToName.containsKey(freeIndex)) {
                        entryToName.put(freeIndex, myName);
                    }                   
                    relations.put(myName, new Vector<Integer>());                    
                    names.put(myName, sb.toString().trim()); //I think this is the line that actually maps index in entryToName to the name in tree
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
                        while (Character.isDigit(tree.charAt(index))
                                || tree.charAt(index) == '.') {
                            index++;
                        }
                    }
                }
            }

            return myName;
        }
    }
}
