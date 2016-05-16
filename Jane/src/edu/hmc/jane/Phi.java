package edu.hmc.jane;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Set;

/**
 *
 * @author Kevin
 */
public class Phi {
    // These store the hosts that a parasite maps to. When no multihost
    // parasites are present, singleHostPhi is used. Once a multihost parasite
    // is found we switch to using multiHostPhi.
    private int[] singleHostPhi;
    private SortedSet<Integer>[] multiHostPhi;
    private TreeSet<Integer> multihostParasites;
    public int size;                 // Number of host-parasite associations.
    private boolean hasMultihostParasites; // Are there any parasites mapped
    // to more than one host?

    public Phi(int parasiteSize) {
        singleHostPhi = new int[parasiteSize];
        Arrays.fill(singleHostPhi, -1);
        size = 0;
        hasMultihostParasites = false;
    }

    public Phi(int[] given) {
        singleHostPhi = given;
        size = 0;
        hasMultihostParasites = false;
        for (int i = 0; i < given.length; ++i) {
            if (given[i] != -1) {
                ++size;
            }
        }
    }
    
    public int[] getSingleHostPhi() {
        return singleHostPhi;
    }
    
    public SortedSet<Integer>[] getMultiHostPhi() {
        return multiHostPhi;
    }
    
    public boolean getHasMultihostParasites() {
        return hasMultihostParasites;
    }

    public final boolean addAssociation(int host, int parasite) {
        if (hasMultihostParasites) {
            if (! multiHostPhi[parasite].isEmpty()) {
                // ensure this is in our list of multihost parasites
                this.multihostParasites.add(parasite);
            }
            if (multiHostPhi[parasite].add(new Integer(host))) {
                ++size;
                return true;
            } else {
                return false;
            }
        } else {
            if (singleHostPhi[parasite] == -1) {
                singleHostPhi[parasite] = host;
                ++size;
                return true;
            } else if (singleHostPhi[parasite] == host) {
                return false;
            } else {
                // Multihost parasite found. Switch the backing array
                // to multiHostPhi.
                hasMultihostParasites = true;
                multiHostPhi = new SortedSet[singleHostPhi.length];
                for (int i = 0; i < singleHostPhi.length; ++i) {
                    multiHostPhi[i] = new TreeSet<Integer>();
                    if (singleHostPhi[i] != -1) {
                        multiHostPhi[i].add(singleHostPhi[i]);
                    }
                }
                multihostParasites = new TreeSet<Integer>();
                singleHostPhi = null;
                return addAssociation(host, parasite);
            }
        }
    }

    public boolean containsAssociation(int host, int parasite) {
        if (hasMultihostParasites) {
            return multiHostPhi[parasite].contains(new Integer(host));
        } else {
            return singleHostPhi[parasite] == host;
        }
    }

    // Only call this if the parasite is known to have 0 or 1 host.
    public int getHost(int parasite) {
        if (!hasMultihostParasites) {
            return singleHostPhi[parasite];
        } else {
            assert (multiHostPhi[parasite].size() <= 1);
            if (multiHostPhi[parasite].isEmpty()) {
                return -1;
            } else {
                return multiHostPhi[parasite].first();
            }
        }
    }

    // If hasMultihostParasites is false, it is faster to call getHost instead.
    public SortedSet<Integer> getHosts(int parasite) {
        if (hasMultihostParasites) {
            return multiHostPhi[parasite];
        } else {
            SortedSet<Integer> ans = new TreeSet<Integer>();
            if (singleHostPhi[parasite] != -1) {
                ans.add(singleHostPhi[parasite]);
            }
            return ans;
        }
    }

    public int numHosts(int parasite) {
        if (hasMultihostParasites) {
            return multiHostPhi[parasite].size();
        } else {
            if (singleHostPhi[parasite] == -1) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    public boolean hasAHost(int parasite) {
        if (!hasMultihostParasites) {
            return singleHostPhi[parasite] != -1;
        } else {
            return !multiHostPhi[parasite].isEmpty();
        }
    }

    public int size() {
        return size;
    }
    
    public int length() {
        if (hasMultihostParasites) {
            return multiHostPhi.length;
        } else {
            return singleHostPhi.length;
        }
    }

    public boolean hasMultihostParasites() {
        return hasMultihostParasites;
    }
    // creates Phi with additional (empty) entries for dummy polytomy nodes
    public Phi newWithoutPolytomies(int newEntries) {
        if (newEntries == 0) {
            return this;
        }
        Phi newPhi=new Phi(newEntries+length());
        newPhi.size = this.size;
        if (hasMultihostParasites) {
            newPhi.hasMultihostParasites=true;
            newPhi.multiHostPhi = new SortedSet[newEntries + multiHostPhi.length];
            newPhi.singleHostPhi=null;
            newPhi.multihostParasites = new TreeSet<Integer>(multihostParasites);
            for (int i = 0; i < multiHostPhi.length; i++) {
                newPhi.multiHostPhi[i] = new TreeSet<Integer>();
                newPhi.multiHostPhi[i].addAll(this.multiHostPhi[i]);
            }
            for (int i = multiHostPhi.length; i < newEntries+multiHostPhi.length; i++) {
                newPhi.multiHostPhi[i] = new TreeSet<Integer>();
            }
        } else {
            for (int i = 0; i < singleHostPhi.length; i++) {
                newPhi.singleHostPhi[i] = this.singleHostPhi[i];
            }
        }
        return newPhi;
    }
    
    @Override
    public String toString() {
        String answer = "";
        if (hasMultihostParasites) {
            answer += ("Number of nodes: " + size + "\n");
            for (int i = 0; i < size; i++) {
                answer += (i +" --> ");
                answer += multiHostPhi[i].toString();
                answer += "\n";
            }
        } else {
            answer += ("Number of nodes: " + size + "\n");
            for (int i = 0; i < size; i++) {
                answer += i + " --> " + singleHostPhi[i] +"\n";
            }
        }
        return answer;
    }
    
    public Set<Integer> getMultihostParasites() {
        if (multihostParasites == null) {
            return new TreeSet<Integer>();
        }
        return multihostParasites;
    }
}
