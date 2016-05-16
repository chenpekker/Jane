/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.gui;

import edu.hmc.jane.HostLocation;
import edu.hmc.jane.gui.light.LightPanel;
import edu.hmc.jane.solving.Embedding;
import edu.hmc.jane.solving.SolutionViewerInfo;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JOptionPane;

/**
 *
 * @author bcousins
 */
public class DrawingObjects extends LightPanel implements MouseMotionListener {

    int solutionCost;
    int tipNameWidth; //current space for tip names
    int fullWidth; //space needed for tip names
    boolean parasitesMapped; //does the P tree need to be remapped to H? (embedding changed)
    int edgeSpacing; //amount of space to put between H and P edges (update 7/5/12: this doesn't seem to be right but I can't tell what it actually is)
    HostNode hRoot; //root of host tree
    ParasiteNode pRoot; //root of parasite tree
    SolutionViewerInfo info;
    HostNode[] hostNodes; //have array of HostNodes for easy access
    ArrayList<ParasiteNode> parasiteNodes;
    int[] adjustedHostTimes; //adjusted to allow more space for parasite events
    Embedding embedding;
    public int totalTime; // ... =#(HostNodes-HostTips) + #(ParasiteEvents)
    int numParasites[]; //number of parasites in each timeslice
    int firstOpenSlot[]; //keyed on timeslice, holds next open slot for HS and Duplication
    int middleOpenSlotLeft[]; //next open left slot for loss and ftd events
    int middleOpenSlotRight[]; //next open right slot for loss and ftd events
    int cospeciations[]; //next open slot for cospeciations
    SolutionViewer frame; //reference to parent
    int numTipSlots;
    boolean supportIsOn;
    int supportType = 0;
    boolean modified = false;
    ParasiteNode highlighted = null;
    ParasiteNode dragging = null;
    HostLocation oldLoc = null;
    final static Color hostPolytomyColor = new Color(145, 0, 194);
    final static Color parasitePolytomyColor = new Color(218, 107, 255);
    final static Color equalSolutions = Color.ORANGE;
    final static Color betterSolutions = Color.GREEN;
    final static Color worseSolutions = Color.RED;
    final static Color noSolutions = Color.GRAY;
    final static Color parasiteEdgeColor = Color.blue;
    final static Color hostEdgeColor = Color.BLACK;
    final static Color parasiteNameColor = Color.BLACK;
    final static Color highlightNodeColor = Color.WHITE;
    
    public boolean highlightParasitePolytomy;
    public boolean highlightHostPolytomy;

    public DrawingObjects(SolutionViewerInfo info, SolutionViewer frame) {
        super();
        solutionCost = 0;
        tipNameWidth = 100;
        fullWidth = 100;
        parasitesMapped = false;
        this.frame = frame;
        this.embedding = new Embedding(info);
        this.info = info;
        this.adjustedHostTimes = new int[info.hostTree.numTips + 1];
        this.hostNodes = new HostNode[info.hostTree.size];
        this.parasiteNodes = new ArrayList<ParasiteNode>(info.phi.size());
        this.hRoot = new HostNode(info.hostTree.root, null, info, hostNodes, this);
        this.pRoot = new ParasiteNode(info.parasiteTree.root, null, embedding, parasiteNodes, this, embedding.parasitePosition, hostNodes, info.hostTiming, false, null);
        this.numParasites = new int[info.hostTree.numTips];
        this.firstOpenSlot = new int[info.hostTree.numTips];
        this.middleOpenSlotLeft = new int[info.hostTree.numTips];
        this.middleOpenSlotRight = new int[info.hostTree.numTips];
        this.cospeciations = new int[info.hostTree.numTips];
        this.numTipSlots = info.hostTree.numTips + info.phi.size() + 4;
        this.highlightHostPolytomy = true;
        this.highlightParasitePolytomy = true;
        
        resolveHostTipSpacing(); //static, doesn't change as parasite tree is moved around
        tipNameWidth = fullWidth + 12;
        supportIsOn = false;
    }

    //want time slices with a lot of parasite events to be larger
    //also, initialize slots for events (for every timeslice)
    void resolveHostTiming() {

        for (int i = 0; i < info.hostTree.numTips + 1; i++) {
            adjustedHostTimes[i] = 0;
        }

        for (int i = 0; i < info.hostTree.numTips; i++) {
            cospeciations[i] = 0;
        }

        //count number of events in each timeslice
        for (HostLocation hl : embedding.parasitePosition) {
            if (hl.time < info.hostTree.numTips) {
                int time = hl.time;
                if (time != info.hostTiming.timeOfNode(hl.ID)) {
                    time = hl.time + 1;
                } else {
                    cospeciations[time - 1]++;
                }
                adjustedHostTimes[time]++;
            }
        }

        // Want (DUP/HS) -> (LOSS/FTD) -> (COSP) for each timeslice. The counter
        // for DUP/HS starts low and will count up with events. There are two
        // counters for LOSS/FTD: one counting up and one counting down.
        for (int i = 0; i < info.hostTree.numTips; i++) {
            firstOpenSlot[i] = 0;
            middleOpenSlotLeft[i] = adjustedHostTimes[i + 1] - cospeciations[i];
        }
        for (int i = 0; i < info.hostTree.size; i++) {
            adjustedHostTimes[info.hostTiming.timeOfNode(i)] += embedding.lossOrFtdAtHost[i];
        }
        // The COSP counter starts high and will count down with events.
        for (int i = 0; i < info.hostTree.numTips; i++) {
            middleOpenSlotRight[i] = adjustedHostTimes[i + 1] + 1 - cospeciations[i];
            cospeciations[i] = adjustedHostTimes[i + 1] + 1;
        }
        //compute suffix sums to get position of host nodes
        //after mapping parasite tree onto host tree
        for (int i = 1; i < info.hostTree.numTips + 1; i++) {
            adjustedHostTimes[i] += adjustedHostTimes[i - 1] + 1;
        }
        totalTime = adjustedHostTimes[info.hostTree.numTips];
    }

    void turnOnTips() {
        tipNameWidth = fullWidth + 12;
        repaint();
    }

    void turnOffTips() {
        tipNameWidth = 10;
        // TODO When tips are changed, need to redraw eps file.
        repaint();
    }

    void turnOnSupport() {
        if (modified) {
            JOptionPane.showMessageDialog(frame, "Support values are not available for modified solutions.", "", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JProgressDialog.runTask("Computing Support", "Computing support values (This may take a few minutes.)", frame,
                        new Runnable() {

                            public void run() {
                                frame.updateSupportValues();
                                supportIsOn = true;
                                parasiteNodes.get(0).drawEventSupport(null, parasiteNodes.get(0).event);
                                repaint();
                            }
                        });
    }

    void turnOffSupport() {
        supportIsOn = false;
        parasiteNodes.get(0).drawEventSupport(null, parasiteNodes.get(0).event);
        repaint();
    }

    //for each timeslice counter, return the open slot
    //and move counter to the next slot
    int getCospSlot(int timeslice) {
        cospeciations[timeslice]--;
        return cospeciations[timeslice];
    }

    // When drawing the left child, get time slots from the right.
    int getMiddleOpenSlot(int timeslice, boolean wasLeftChild) {
        if (wasLeftChild) {
            return getMiddleOpenSlotRight(timeslice);
        } else {
            return getMiddleOpenSlotLeft(timeslice);
        }
    }

    private int getMiddleOpenSlotLeft(int timeslice) {
        middleOpenSlotLeft[timeslice]++;
        return middleOpenSlotLeft[timeslice];
    }

    private int getMiddleOpenSlotRight(int timeslice) {
        middleOpenSlotRight[timeslice]--;
        return middleOpenSlotRight[timeslice];
    }

    int getFirstOpenSlot(int timeslice) {
        firstOpenSlot[timeslice]++;
        return firstOpenSlot[timeslice];
    }

    // Return the open slot without moving the counter. 
    int peekMiddleOpenSlot(int timeslice, boolean wasLeftChild) {
        if (wasLeftChild) {
            return middleOpenSlotRight[timeslice];
        } else {
            return middleOpenSlotLeft[timeslice];
        }
    }

    //space out the host tips according to how many parasite tips are mapped to it
    void resolveHostTipSpacing() {
        for (int e_P : info.parasiteTree.getTips()) {
            if (embedding.info.phi.hasMultihostParasites()) {
                for (int e_H : embedding.info.phi.getHosts(e_P)) {
                    hostNodes[e_H].numMappedTips++;
                }
            } else {
                hostNodes[embedding.info.phi.getHost(e_P)].numMappedTips++;
            }
        }
    }

    //parasite node locations have been set, now just connect the dots
    void resolveParasitePositions(Graphics g) {
        clear();
        g.setColor(DrawingObjects.parasiteEdgeColor);
        ParasiteNode root = parasiteNodes.get(0);
        // Draw all events occurring before the root node.
        root.drawBeforeRoot(g);
        //initialize the root
        //even though position has been set for need, need the counters to be correct
        root.hostSegment.numParasites = 1;
        root.XOffset = this.getFirstOpenSlot(root.hostSegment.time);
        //draw the dummy edge out of the root
        double x1 = root.getX() + 5;
        double y1 = root.getY() + 5;
        double x2 = root.getX() - 10;
        g.drawLine((int) x1, (int) y1, (int) x2, (int) y1);
        // The parasite root will connect the dots between it and its children
        // and then recurse depth-first.
        root.drawParasite(g);
    }

    void setParasitePixelLocations() {
        for (ParasiteNode pNode : parasiteNodes) {
            pNode.setLocation(); //assign each node an x and y coordinate
            if (pNode.isTip) {
                pNode.setTipNamePosition();
            }
        }

        parasiteNodes.get(0).drawEventSupport(null, parasiteNodes.get(0).event);
        
    }

    void drawHostNames() {
        for (HostNode hn : hostNodes) {
            if (hn.isTip) {
                hn.setTipNamePosition();
            }
        }
    }

    //parasite tree has been moved around, so recompute some information
    void remapParasites() {
        //the tree looks much prettier if the parasite nodes are sorted, and
        //then drawn in that sorted order. the comparison function is in
        //ParasiteNode.java. However, because failure to diverge events require
        //the tree to instead be drawn depth-first, we lose this benefit.
        Collections.sort(parasiteNodes);

        int i = 0;

        //give each ParasiteNode its index in the sorted array
        for (ParasiteNode pNode : parasiteNodes) {
            pNode.setRank(i);
            i++;
        }

        for (ParasiteNode pNode : parasiteNodes) {
            pNode.mapHoriz();
            if (!pNode.isTip) {
                pNode.setColor();
            } else if (embedding.info.phi.hasMultihostParasites()) {
                // Efficiency-wise, it would be nice to avoid remapping the
                // copies of every non-tip parasite.
                pNode.remapCopies();
            }
        }
        clear();
        ParasiteNode root = parasiteNodes.get(0);
        // Position events occurring before the root node.
        root.positionBeforeRoot();
        // Position the root node.
        root.YOffset = 1;
        root.hostSegment.numParasites = 1;
        root.XOffset = this.getFirstOpenSlot(root.hostSegment.time);
        // Position the parasite root which will recurse depth-first and
        // position its children.
        root.positionNode();
    }

    void clear() {
        for (HostNode hn : hostNodes) {
            for (EdgeSegment es : hn.segments) {
                es.numParasites = 0;
            }
        }
    }

    @Override
    public void paint(Graphics2D g) {
        updateChildPositions();
        colorPolytomyEdges();
        super.paint(g);
    }

    @Override
    public void paintBackground(Graphics2D g) {
        super.paintOverlay(g);

        frame.setTitle("Solution Number: " + frame.solutionNumber + "     Solution Cost: " + embedding.currentCost);
        // we need the solution cost to pass it on to the solution viewer, which
        // saves it to an image file if the user asks for it.
        solutionCost = embedding.currentCost;
        resolveHostTiming();

        g.setColor(this.hostEdgeColor);
        resolveParasitePositions(g);
        //resolveHostTiming() must be called before every call to resolveParasitePositions()
    }

    private void updateChildPositions() {

        this.edgeSpacing = getHeight() * Math.min(20, frame.getContentPane().getHeight() / (numTipSlots)) / frame.getContentPane().getHeight();
        this.edgeSpacing = (int) Math.round(((double) this.edgeSpacing) * 4d / 5d);

        //now, get all of the necessary information from the P->H mapping
        //place the P root at its optimal location, then resolve the rest recursively
        resolveHostTiming();

        hRoot.drawNode(1, info, adjustedHostTimes); //recursively position the host tree

        if (!parasitesMapped) {
            remapParasites();
            parasitesMapped = true;
        }

        setParasitePixelLocations();

        drawHostNames();
    }

    public void mouseDragged(MouseEvent e) {
        if (dragging != null) {
            dragging.setLocation(e.getX() - 6, e.getY() - 6);
            repaint();
        }
    }

    //if the user starts dragging, moves the mouse outside the screen, releases the mouse button
    //while outside of the screen, then moves it back in, we get issues unless we
    //do this
    public void mouseMoved(MouseEvent e) {
        if (dragging != null) {
            stopDrag();
        }
    }

    void stopDrag() {      
        for (HostNode hn : hostNodes) {
            for (EdgeSegment es : hn.segments) {
                es.setColor(hostEdgeColor);
                if (es.active) {
                    es.active = false;
                    HostLocation newLoc = new HostLocation(hn.index, es.time, false, hn.nodeID);
                    if(!modified && (oldLoc.time != es.time || oldLoc.nodeID != hn.nodeID) ){ //show if position changed and not already modified
                        JOptionPane.showMessageDialog(frame, "Support values are not available for modified solutions.", "", JOptionPane.INFORMATION_MESSAGE);
                        modified = true;
                        frame.updateSupportValues();
                    }
                    embedding.move(dragging.index, newLoc);
                    
                    parasitesMapped = false;
                }
                es.removeMouseListener(es);
            }
            if (!hn.isTip) {
                hn.setColor(hostEdgeColor);
                if (hn.active) {
                    hn.active = false;
                    HostLocation newLoc = new HostLocation(hn.index, embedding.info.hostTiming.timeOfNode(hn.index), true, hn.nodeID);
                    if(!modified && oldLoc.nodeID != hn.nodeID){ //show if position changed and not already modified
                        JOptionPane.showMessageDialog(frame, "Support values are not available for modified solutions.", "", JOptionPane.INFORMATION_MESSAGE);
                        modified = true;
                        frame.updateSupportValues();
                    }
                    embedding.move(dragging.index, newLoc);
                    
                    parasitesMapped = false;
                }
                hn.removeMouseListener(hn);
            }
        }
        
        
        
        // TODO: Should this be here without an if statement?
        
        repaint();
        //de-activate the host edge listeners
        //if the node is on a host edge, update the tree
        //otherwise, put the parasite back in its previous location

        highlighted = null;
        dragging = null;
        removeMouseMotionListener(this);
    }

    void colorPolytomyEdges() {
        // does nothing if we are dragging a node
        if (dragging != null) {
            return;
        }
        for (HostNode hn : hostNodes) {
            for (EdgeSegment es : hn.segments) {
                if (this.highlightHostPolytomy && info.hostTree.isPolytomyEdge(hn.index)) {
                    es.setColor(hostPolytomyColor);
                } else if (!this.highlightHostPolytomy && info.hostTree.isPolytomyEdge(hn.index)) {
                    es.setColor(hostEdgeColor);
                }
            }
            if (this.highlightHostPolytomy && info.hostTree.isInPolytomy(hn.index)) {
                hn.setColor(hostPolytomyColor);
            } else if (!this.highlightHostPolytomy && info.hostTree.isInPolytomy(hn.index)) {
                hn.setColor(hostEdgeColor);
            }
        }
    }

    void startDrag(ParasiteNode p) {
        // POLY: add support for polytomies
        dragging = p;
        oldLoc = p.getHostLocation(); //store location to check if placed at different host location
        for (HostNode hn : hostNodes) {
            for (EdgeSegment es : hn.segments) {
                int cost = embedding.costIfMove(p.index, new HostLocation(hn.index, es.time, false, hn.nodeID));
                if (!embedding.costModel.isInfinity(cost)) {
                    if (cost > embedding.currentCost) {
                        es.setColor(DrawingObjects.worseSolutions);
                        es.addMouseListener(es);
                    } else if (cost == embedding.currentCost) {
                        es.setColor(DrawingObjects.equalSolutions);
                        es.addMouseListener(es);
                    } else {
                        es.setColor(DrawingObjects.betterSolutions);
                        es.addMouseListener(es);
                    }
                } else {
                    es.setColor(noSolutions);
                }
            }
            if (!hn.isTip) {
                int cost = this.embedding.costIfMove(p.index, new HostLocation(hn.index, embedding.info.hostTiming.timeOfNode(hn.index), true, hn.nodeID));
                if (!embedding.costModel.isInfinity(cost)) {
                    if (cost > embedding.currentCost) {
                        hn.setColor(DrawingObjects.worseSolutions);
                        hn.addMouseListener(hn);
                    } else if (cost == embedding.currentCost) {
                        hn.setColor(DrawingObjects.equalSolutions);
                        hn.addMouseListener(hn);
                    } else {
                        hn.setColor(DrawingObjects.betterSolutions);
                        hn.addMouseListener(hn);
                    }
                } else {
                    hn.setColor(noSolutions);
                }
            }
        }
        repaint();
        //activate all the host edge listeners

        addMouseMotionListener(this);
    }
}
