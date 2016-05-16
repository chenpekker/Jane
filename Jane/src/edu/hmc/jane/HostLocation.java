/*
 * HostLocation class is used mainly by SolutionViewer (new version) to refer
 * to a position on the host tree, which can be either a node or an edge.
 */
package edu.hmc.jane;

public class HostLocation {

    public final int ID;          // index of the node or edge
    public final int time;        // time of that location - unique for a node
    public final boolean isNode;  // store whether this object is a node or edge
    public final int nodeID;
    
    // CONSTRUCTOR
    public HostLocation(int ID, int time, boolean isNode, int nodeID) {
        this.ID = ID;
        this.time = time;
        this.isNode = isNode;
        this.nodeID = nodeID;
    }

    @Override
    public String toString() {
        return "host " + (isNode ? "node " : "edge ") + nodeID + " at time " + time;
    }

    public String userString(Tree hostTree) {
        String selfName = hostTree.node[ID].name;
        if(isNode) return selfName;
        String parentName = (hostTree.root == ID ? "Dummy root" : hostTree.node[hostTree.node[ID].parent].name);
        return "(" + parentName + ", " + selfName + ")";
    }

    @Override
    public boolean equals(Object other) {
        assert (other instanceof HostLocation);
        return (this.ID == ((HostLocation) other).ID
                && this.time == ((HostLocation) other).time
                && this.isNode == ((HostLocation) other).isNode)
                && this.nodeID == ((HostLocation) other).nodeID;
    }

    @Override
    public int hashCode() {
        return ((ID << 12) | (time << 1) | (isNode ? 1 : 0));
    }

    // Simply a pair of host location
    public static class HostLocationPair {
        public final HostLocation l, r;
        // CONSTRUCTOR
        public HostLocationPair(HostLocation l, HostLocation r) {
            this.l = l;
            this.r = r;
        }
    }
}
