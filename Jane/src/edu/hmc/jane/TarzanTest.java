package edu.hmc.jane;

/*
Copyright (c) 2009, Chris Conow, Daniel Fielder, Yaniv Ovidia, Ran Libeskind-Hadas
All rights reserved.

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
public class TarzanTest
{
    /*
        static int[] costs = {0,1,1,2};

        public static void main(String[] args) throws IOException, FileFormatException
        {
                TreeFileReader fin = new TarzanFileReader(args[0]);
                TreeFileReader.ProblemInstance pi = fin.readProblem();
                CLI.rand = new Random(0);

                Map<Integer, uselessAssoc> tarzanSolution = parseTSolution(new BufferedReader(new InputStreamReader(new FileInputStream(args[1]))), pi.hostNames, pi.parasiteNames);

                PNetwork hostTree = new PNetwork(pi.host, pi.hostRanks, pi.hostNames, pi.regionCosts, pi.hostRegions);
                PNetwork parasiteTree = new PNetwork(pi.parasite, pi.parasiteRanks, pi.parasiteNames);

                Solver.costs = costs;

                Assoc assoc = magic(parasiteTree.root().name, tarzanSolution, hostTree, parasiteTree, pi.phi);
                //Solver sv2 = new Solver(hostTree, parasiteTree, pi.phi, costs);
                //System.out.println(sv2.traverse(assoc));
                PNetwork zomg = Breeder.applyAssociation(assoc, hostTree);
                zoning(zomg, pi.hostRanks);
                Solver sv = new Solver(zomg, parasiteTree, pi.phi, costs, -1, ArrayDP.ASSOCIATION);
                //System.out.println(zomg.timingString());
                sv.solve();
                //System.out.println("\n\n\n\n\n\n\n\n\n");
                //System.out.println(sv.optSol);
                //System.out.println(sv.cos);
                //System.out.println(sv.dup);
                //System.out.println(sv.host);
                //System.out.println(sv.los);
                //System.out.println(zomg);
                //System.out.println(sv.dpTable.get(48));
                System.out.println(sv.hostNetwork.fileTimingString());
        }

        static void zoning(PNetwork net, Map<Integer, Vector<Integer> > ranking){
                for(Map.Entry<Integer, Vector<Integer> > name : ranking.entrySet()){
                        for(Integer node : name.getValue()){
                                net.nodes[node].zone = name.getKey();
                        }
                }
        }

        static Assoc magic(int location, Map<Integer, uselessAssoc> tSolution, PNetwork hostTree, PNetwork parasiteTree, int[] phi)
        {
                uselessAssoc mapping = tSolution.get(location);

                if(phi[location] != -1 && phi[location] == (mapping.node))
                {
                        return new Assoc(Assoc.Type.TIP, parasiteTree.nodes[location], hostTree.nodes[mapping.node], -1, 0, null, null);
                }

                Vector<Node> pchildren = parasiteTree.children(location);
                Vector<Node> hchildren = hostTree.nodes[mapping.node].children;
                
                Assoc assoc1 = magic(pchildren.get(0).name, tSolution, hostTree, parasiteTree, phi);
                Assoc assoc2 = magic(pchildren.get(1).name, tSolution, hostTree, parasiteTree, phi);
                
                Node hostNode = hostTree.nodes[mapping.node];
                
                if(mapping.type == 1)
                {
                        Node hostChild1 = null;
                        Node hostChild2 = null;
                        if(hostTree.descendant(new Edge(hostNode, hchildren.get(0)), assoc1.associate) && hostTree.descendant(new Edge(hostNode, hchildren.get(1)), assoc2.associate))
                        {
                                hostChild1 = hchildren.get(0);
                                hostChild2 = hchildren.get(1);
                        }
                        else if(hostTree.descendant(new Edge(hostNode, hchildren.get(1)), assoc1.associate) && hostTree.descendant(new Edge(hostNode, hchildren.get(0)), assoc2.associate))
                        {
                                hostChild1 = hchildren.get(1);
                                hostChild2 = hchildren.get(0);
                        }
                        else{
                                System.out.println("Trying to go from "+hostNode.name + " to " + assoc1.associate + " and " + assoc2.associate);
                                System.out.println("Well, this is unfortunate. Apparently 1 doesn't mean cospeciation.");
                                System.exit(0);
                        }

                        int cost = assoc1.cost + assoc2.cost + 2*(costs[Solver.COSPECIATION]) +
                        (hostTree.distance(new Edge(hostNode, hostChild1), assoc1.associate) +
                         hostTree.distance(new Edge(hostNode, hostChild2), assoc2.associate)) * costs[Solver.LOSS];
                        
                        return new Assoc(Assoc.Type.COSPECIATION, parasiteTree.nodes[location], hostTree.nodes[mapping.node], -1, cost, assoc1, assoc2);
                }

                Edge hostEdge = new Edge(hostNode.parents.get(0), hostNode);

                if(hostTree.descendant(hostEdge, assoc1.associate) && hostTree.descendant(hostEdge, assoc2.associate))
                {
                        int cost = 2*costs[Solver.DUPLICATION];

                    cost += assoc1.cost + assoc2.cost +
                        (hostTree.distance(hostEdge, assoc1.associate) +
                         hostTree.distance(hostEdge, assoc2.associate)) * costs[Solver.LOSS];
                        
                        return new Assoc(Assoc.Type.DUPLICATION, parasiteTree.nodes[location], hostEdge, -1, cost, assoc1, assoc2);
                }

                if(hostTree.descendant(hostEdge, assoc1.associate))
                {
                        Edge targetEdge;
                        if(assoc2.associate instanceof Node)
                                targetEdge = new Edge(((Node)assoc2.associate).parents.get(0), ((Node)assoc2.associate));
                        else
                                targetEdge = (Edge) assoc2.associate;
                        int cost = 2*costs[Solver.DUPLICATION] + costs[Solver.SWITCH];

                        cost += assoc1.cost + assoc2.cost +
                        (hostTree.distance(hostEdge, assoc1.associate) +
                         hostTree.distance(targetEdge, assoc2.associate)) * costs[Solver.LOSS];
                        
                        return new Assoc(Assoc.Type.HOST_SWITCH, parasiteTree.nodes[location], hostEdge, targetEdge, -1, cost, assoc1, assoc2);
                }

                if(hostTree.descendant(hostEdge, assoc2.associate))
                {
                        Edge targetEdge;
                        if(assoc1.associate instanceof Node)
                                targetEdge = new Edge(((Node)assoc1.associate).parents.get(0), ((Node)assoc1.associate));
                        else
                                targetEdge = (Edge) assoc1.associate;
                        int cost = 2*costs[Solver.DUPLICATION] + costs[Solver.SWITCH];

                        cost += assoc1.cost + assoc2.cost +
                        (hostTree.distance(hostEdge, assoc2.associate) +
                         hostTree.distance(targetEdge, assoc1.associate)) * costs[Solver.LOSS];
                        
                        return new Assoc(Assoc.Type.HOST_SWITCH, parasiteTree.nodes[location], hostEdge, targetEdge, -1, cost, assoc1, assoc2);
                }

                System.out.println("Something has gone horrifically wrong. This association is not acceptable");
                return null;
        }

        static Map<Integer, Vector<Integer> > parseTree(BufferedReader br) throws IOException
        {
                Map<Integer, Vector<Integer> > answer = new HashMap<Integer, Vector<Integer> >();
                String s = br.readLine();
                String[] entry = s.split("\t");
                while(entry.length == 3)
                {
                        Vector<Integer> v = new Vector<Integer>();
                        if(!"null".equals(entry[1]))
                                v.add(Integer.parseInt(entry[1]));
                        if(!"null".equals(entry[2]))
                                v.add(Integer.parseInt(entry[2]));

                        answer.put(Integer.parseInt(entry[0]), v);

                        s = br.readLine();
                        entry = s.split("\t");
                }
                return answer;
        }
        static Map<Integer, Integer> parsePhi(BufferedReader br) throws IOException
        {
                Map<Integer, Integer> answer = new HashMap<Integer, Integer>();
                String s = br.readLine();
                String[] entry = s.split("\t");
                while(entry.length == 2)
                {
                        answer.put(Integer.parseInt(entry[1]), Integer.parseInt(entry[0]));

                        s = br.readLine();
                        entry = s.split("\t");
                }
                return answer;
        }
        static Integer backwardsLookup(Map<Integer, String> map, String s)
        {
                for(Map.Entry<Integer, String> me : map.entrySet())
                        if(me.getValue().equals(s))
                                return me.getKey();
                System.out.println(s);
                return null;
        }
        static Map<Integer, uselessAssoc> parseTSolution(BufferedReader br, String[] hostNames, String[] parasiteNames) throws IOException
        {
                Map<Integer, uselessAssoc> answer = new HashMap<Integer, uselessAssoc>();

                String s = br.readLine();
                while(s.startsWith("("))
                    s = s.substring(1);
                while(s.endsWith(")"))
                    s = s.substring(0,s.length() - 1);

                String[] entry = s.split(",");

                while(entry.length == 3)
                {
                        answer.put(backwardsLookup(parasiteNames, entry[0]), new uselessAssoc(backwardsLookup(hostNames,entry[1]), Integer.parseInt(entry[2])));
                        s = br.readLine();

                        if(s == null)
                                break;

                        while(s.startsWith("("))
                            s = s.substring(1);
                        while(s.endsWith(")"))
                            s = s.substring(0,s.length() - 1);

                        entry = s.split(",");
                }
                if(entry.length > 3)
                        System.out.println(s);

                return answer;
        }
     * */
}
class uselessAssoc
{
        int type;
        int node;
        public uselessAssoc(int n, int t)
        {
                node = n;
                type = t;
        }
}
