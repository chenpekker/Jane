/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author Kevin Modfieid by Ki Wan Gkoo
 * 
 * Modified on July 6, 2012, 3:31:00 PM
 */
public class TimeZones {

    private boolean timeZonesUsed;      // Does the data actually use time zones?
    private boolean hostRangesUsed;      // Are there ranges in the host time zones?
    private boolean parasiteRangesUsed; // Are there ranges in the parasite time zones?
    private int hostSize;
    private int parasiteSize;
    private int[] hostZoneStart;            // Each host's time zone.
    private int[] hostZoneEnd;
    private int[] parasiteZoneStart;    // Start of each parasite's time zone.
    private int[] parasiteZoneEnd;      // End of each parasite's time zone.
    private SortedSet<Integer> hostZonesUsed;     // Set of time zones used by a host.
    private SortedSet<Integer> parasiteZonesUsed; // Set of time zones used by a parasite.

    public TimeZones(int hostSize, int parasiteSize) {
        this.hostSize = hostSize;
        this.parasiteSize = parasiteSize;
        this.hostZoneStart = new int[hostSize];
        this.hostZoneEnd = new int[hostSize];
        this.parasiteZoneStart = new int[parasiteSize];
        this.parasiteZoneEnd = new int[parasiteSize];
        this.hostZonesUsed = new TreeSet<Integer>();
        this.parasiteZonesUsed = new TreeSet<Integer>();
    }

    public void setHostZone(int host, int zone) {
        assert (zone > 0);
        timeZonesUsed = true;
        hostZoneStart[host] = zone;
        hostZoneEnd[host] = zone;
        hostZonesUsed.add(zone);
    }
    
    public void setHostZone(int host, int zoneStart, int zoneEnd) {
        assert (zoneStart <= zoneEnd);
        assert (zoneStart > 0);
        timeZonesUsed = true;
        if (zoneStart != zoneEnd) {
            hostRangesUsed = true;
        }
        hostZoneStart[host] = zoneStart;
        hostZoneEnd[host] = zoneEnd;
        for (int i = zoneStart; i <= zoneEnd; ++i) {
            hostZonesUsed.add(i);
        }
    }

    public void setParasiteZone(int parasite, int zone) {
        assert (zone > 0);
        timeZonesUsed = true;
        parasiteZoneStart[parasite] = zone;
        parasiteZoneEnd[parasite] = zone;
        parasiteZonesUsed.add(zone);
    }

    public void setParasiteZone(int parasite, int zoneStart, int zoneEnd) {
        assert (zoneStart <= zoneEnd);
        assert (zoneStart > 0);
        timeZonesUsed = true;
        if (zoneStart != zoneEnd) {
            parasiteRangesUsed = true;
        }
        parasiteZoneStart[parasite] = zoneStart;
        parasiteZoneEnd[parasite] = zoneEnd;
        for (int i = zoneStart; i <= zoneEnd; ++i) {
            parasiteZonesUsed.add(i);
        }
    }

    // Some member variables aren't needed after reading so free up some space.
    public void doneReading() {
        // If only one time zone is actually used.
        if (hostZonesUsed.size() == 1 && parasiteZonesUsed.size() == 1 && hostZonesUsed.first() == parasiteZonesUsed.first()) {
            timeZonesUsed = false;
        }

        if (timeZonesUsed == false) {
            hostRangesUsed = false;
            parasiteRangesUsed = false;
            hostZoneStart = null;
            hostZoneEnd = null;
            parasiteZoneStart = null;
            parasiteZoneEnd = null;
            hostZonesUsed = null;
            parasiteZonesUsed = null;
        } else if (hostRangesUsed == false) {
            hostZoneEnd = null;
        } else if (parasiteRangesUsed == false) {
            parasiteZoneEnd = null;
        }
    }

    public boolean hostZoneAssignmentCompleted() {
        if (!timeZonesUsed) {
            return true;
        }
        for (int i = 0; i < hostSize; ++i) {
            if (hostZoneStart[i] == 0) {
                return false;
            }
            if (hostRangesUsed && hostZoneEnd[i] == 0) {
                return false;
            }
        }
        return true;
    }

    public boolean parasiteZoneAssignmentCompleted() {
        if (!timeZonesUsed) {
            return true;
        }
        for (int i = 0; i < parasiteSize; ++i) {
            if (parasiteZoneStart[i] == 0) {
                return false;
            }
            if (parasiteRangesUsed && parasiteZoneEnd[i] == 0) {
                return false;
            }
        }
        return true;
    }

    // Check for lack of gaps in the host time zones.
    public boolean hostZonesConsecutive() {
        if (!timeZonesUsed) {
            return true;
        }
        if (hostZonesUsed.size() <= 1) {
            return true;
        }

        int previousZone = hostZonesUsed.first();
        for (int currentZone : hostZonesUsed) {
            if (currentZone == hostZonesUsed.first()) {
                continue;
            }
            if (currentZone != previousZone + 1) {
                return false;
            } else {
                previousZone = currentZone;
            }
        }
        return true;
    }

    public int getHostZone(int host) {
        if (!timeZonesUsed) {
            return 1;
        }      
        //if (hostRangesUsed) {
        //    throw new UnsupportedOperationException();
        //} else {
        return hostZoneStart[host];
        //}
    }
    
    public int getHostZoneStart(int host) {
        if (!timeZonesUsed) {
            return 1;
        } else {
            return hostZoneStart[host];
        }
    }

    public int getHostZoneEnd(int host) {
        if (!timeZonesUsed) {
            return 1;
        } else if (!hostRangesUsed) {
            return hostZoneStart[host];
        } else {
            return hostZoneEnd[host];
        }
    }
    
    public int getParasiteZone(int parasite) {
        if (!timeZonesUsed) {
            return 1;
        }
        //if (parasiteRangesUsed) {
        //    throw new UnsupportedOperationException();
        //} else {
        return parasiteZoneStart[parasite];
        //}
    }

    public int getParasiteZoneStart(int parasite) {
        if (!timeZonesUsed) {
            return 1;
        } else {
            return parasiteZoneStart[parasite];
        }
    }

    public int getParasiteZoneEnd(int parasite) {
        if (!timeZonesUsed) {
            return 1;
        } else if (!parasiteRangesUsed) {
            return parasiteZoneStart[parasite];
        } else {
            return parasiteZoneEnd[parasite];
        }
    }

    public boolean areUsed() {
        return timeZonesUsed;
    }
    
    // Check if there is a time zone shared by both host and parasite.
    public boolean overlap(int parasite, int host) {
        if (!timeZonesUsed) {
            return true;
        } else if (!hostRangesUsed && !parasiteRangesUsed) {
            return (hostZoneStart[host] == parasiteZoneStart[parasite]);
        } else if (!hostRangesUsed) {
            int hostZone = hostZoneStart[host];
            return (hostZone >= parasiteZoneStart[parasite] && hostZone <= parasiteZoneEnd[parasite]);
        } else {
            int parasiteZone = parasiteZoneStart[parasite];
            return (parasiteZone >= hostZoneStart[host] && parasiteZone <= hostZoneEnd[host]);
        }
    }
    
    // Check if two hosts are in the same time zone.
    public boolean hostsInSameZone(int host1, int host2) {
        if (!timeZonesUsed) {
            return true;
        } else if (!hostRangesUsed) {
            return (hostZoneStart[host1] == hostZoneStart[host2]);
        } else {
            return ((hostZoneStart[host1] == hostZoneStart[host2]) && (hostZoneEnd[host1] == hostZoneEnd[host2]));
        }
    }

    public boolean respectHostAncestry(Tree hostTree) {
        for (int n = 0; n < hostTree.size; ++n) {
            if (n != hostTree.root && !parentTimeZoneBeforeChild(hostTree.node[n].parent, n, true)) {
                return false;
            }
        }
        return true;
    }

    public boolean respectParasiteAncestry(Tree parasiteTree) {
        for (int n = 0; n < parasiteTree.size; ++n) {
            if (n != parasiteTree.root && !parentTimeZoneBeforeChild(parasiteTree.node[n].parent, n, false)) {
                return false;
            }
        }
        return true;
    }

    // Checks if the time zones of a parent/child relationship are consistent.
    private boolean parentTimeZoneBeforeChild(int parent, int child, boolean host) {
        if (!timeZonesUsed) {
            return true;
        }
        if (host) {
            boolean zoneStartsConsistent = (hostZoneStart[parent] <= hostZoneStart[child]);
            if (!hostRangesUsed) {
                return zoneStartsConsistent;
            } else {
                return zoneStartsConsistent && (hostZoneEnd[parent] <= hostZoneEnd[child]);
            }
        } else {
            boolean zoneStartsConsistent = (parasiteZoneStart[parent] <= parasiteZoneStart[child]);
            if (!parasiteRangesUsed) {
                return zoneStartsConsistent;
            } else {
                return zoneStartsConsistent && (parasiteZoneEnd[parent] <= parasiteZoneEnd[child]);
            }
        }
    }
    
    /*
     * Creates a new timezone with accurate information regarding polytomy nodes
     */
    public TimeZones newWithoutPolytomies(int newHostEntries, int newParasiteEntries, Tree host, Tree para) {
        if (!this.timeZonesUsed) {
            return this;
        }   
        
        TimeZones newZones = new TimeZones(this.hostSize + newHostEntries, this.parasiteSize + newParasiteEntries);
        newZones.hostZonesUsed = this.hostZonesUsed;
        newZones.parasiteZonesUsed = this.parasiteZonesUsed;
        newZones.timeZonesUsed = this.timeZonesUsed;
        newZones.hostRangesUsed = this.hostRangesUsed;
        newZones.parasiteRangesUsed = this.parasiteRangesUsed;
              
        if (this.timeZonesUsed) {
            // copy host with polytomies
            for (int i = 0; i < this.hostSize; i++) {
                newZones.hostZoneStart[i] = this.hostZoneStart[i];
            }        
            for (int i = 0; i < newHostEntries; i++) {
                newZones.hostZoneStart[i] = this.hostZoneStart[host.findStartNode(i)];
            }
            
            // copy parasite with polytomies
            for (int i = 0; i < this.parasiteSize; i++) {
                newZones.parasiteZoneStart[i] = this.parasiteZoneStart[i];
            }
            for (int i = 0; i < newParasiteEntries; i++) {
                newZones.parasiteZoneStart[i] = this.parasiteZoneStart[para.findStartNode(i)];
            }
        } else {
            newZones.hostZoneStart = null;
            newZones.hostZoneEnd = null;
            newZones.parasiteZoneStart = null;
            newZones.parasiteZoneEnd = null;            
        }
        if (this.hostRangesUsed) {
            for (int i = 0; i < this.hostSize; i++) {
                newZones.hostZoneEnd[i] = this.hostZoneEnd[i];
            }         
            for (int i = 0; i < newHostEntries; i++) {
                newZones.hostZoneEnd[i] = this.hostZoneEnd[host.findStartNode(i)];
            }
        } else {
            newZones.hostZoneEnd = null;
        }
        if (this.parasiteRangesUsed) {
            for (int i = 0; i < this.parasiteSize; i++) {
                newZones.parasiteZoneEnd[i] = this.parasiteZoneEnd[i];
            }          
            for (int i = 0; i < newParasiteEntries; i++) {
                newZones.parasiteZoneEnd[i] = this.parasiteZoneEnd[para.findStartNode(i)];
            }
        } else {
            newZones.parasiteZoneEnd = null;
        }
        return newZones;
    }
    
    public int getHostSize() {     
        if (!this.timeZonesUsed)// this used to have " || !this.hostRangesUsed"
            return 0;
        return this.hostZoneStart.length;
    }
    
    public int getParaSize() {
        if (!this.timeZonesUsed || !this.parasiteRangesUsed)
            return 0;
        return this.parasiteZoneStart.length;
    }
}
