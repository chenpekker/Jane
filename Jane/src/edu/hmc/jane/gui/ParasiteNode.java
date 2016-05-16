/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.gui;

import edu.hmc.jane.CostModel;
import edu.hmc.jane.HostLocation;
import edu.hmc.jane.TreeSpecs;
import edu.hmc.jane.gui.SolutionViewer.SupportOptions;
import edu.hmc.jane.gui.light.LightLabel;
import edu.hmc.jane.gui.light.LightPanel;
import edu.hmc.jane.solving.Embedding;
import edu.hmc.jane.solving.SolutionViewerInfo.EventInfo;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author ben
 */
public class ParasiteNode extends LightPanel implements MouseListener, Comparable<Object> {

    int index;
    ParasiteNode Rchild, Lchild, Parent;
    boolean isTip = true;
    HostLocation[] parasitePosition;
    int time;
    boolean isCopy;      // Copies are made of multihost ParasiteNode tips, one
    // for each host after the first.
    HostLocation copyPosition; // Store HostLocation for copies since it
    // doesn't appear in parasitePosition[]
    LinkedList<ParasiteNode> copies = null;
    EdgeSegment hostSegment;
    Embedding embedding;
    int rank;
    boolean beingDrawn;
    int XOffset, YOffset;
    LightLabel tipName;
    HostNode[] hostNodes;
    TreeSpecs hostTiming;
    Color color;
    private char eventType; //for showing event type on "node hovering"
    boolean LchildChosenFirst;
    DrawingObjects panel;
    boolean isRoot = false;
    boolean isHS = false;
    EventInfo event;
    boolean hasLoss = false; // Denotes a parent whose child comes  after loss
    boolean isLoss = false;  // Denotes that the node comes after a loss
    int lossX = 0;           // x position of thechild after a loss
    int lossY = 0;           // y position of the child after a loss

    
    public ParasiteNode(int index, ParasiteNode Parent, Embedding embedding, ArrayList<ParasiteNode> parasiteNodes, DrawingObjects panel,
            HostLocation[] parasitePosition, HostNode[] hostNodes, TreeSpecs hostTiming, boolean isCopy, HostLocation copyPosition) {
        super();
        this.isCopy = isCopy;
        this.copyPosition = copyPosition;
        this.panel = panel;
        this.color = Color.black;
        this.hostNodes = hostNodes;
        this.hostTiming = hostTiming;
        this.setSize(11, 11);
        setOpaque(false);
        this.index = index;
        this.Parent = Parent;
        this.parasitePosition = parasitePosition;
        parasiteNodes.add(this);
        this.embedding = embedding;
        //recursively call constructor on children
        if (embedding.info.parasiteTree.node[index].Rchild != -1) {
            this.isTip = false;
            this.Rchild = new ParasiteNode(embedding.info.parasiteTree.node[index].Rchild, this, embedding, parasiteNodes, panel, parasitePosition, hostNodes, hostTiming, false, null);
        }
        if (embedding.info.parasiteTree.node[index].Lchild != -1) {
            this.isTip = false;
            this.Lchild = new ParasiteNode(embedding.info.parasiteTree.node[index].Lchild, this, embedding, parasiteNodes, panel, parasitePosition, hostNodes, hostTiming, false, null);
        }

        panel.add(this);
        if (!this.isTip) {
            addMouseListener(this);
        }

        if (this.isTip) {
            color = DrawingObjects.parasiteNameColor;
            tipName = new LightLabel(embedding.info.parasiteTree.node[index].name);
            tipName.setOpaque(false);
            if (tipName.getPreferredSize().width > panel.fullWidth) {
                panel.fullWidth = tipName.getPreferredSize().width;
            }
            panel.add(tipName);

            // Make copies for each other infected host.
            // parasitePosition[index] can be used to find a copy's original host.
            if (!this.isCopy) {
                this.copies = new LinkedList<ParasiteNode>();
                for (int e_H : embedding.info.phi.getHosts(index)) {
                    if (e_H != parasitePosition[index].ID) {
                        HostLocation host = new HostLocation(e_H, embedding.info.hostTiming.tipTime, true, embedding.info.hostTree.getNodeID(e_H));
                        this.copies.add(new ParasiteNode(index, this, embedding, parasiteNodes, panel, parasitePosition, hostNodes, hostTiming, true, host));
                    }
                }
            }
        }
        if (this.Parent == null) {
            this.isRoot = true;
        }
        eventType = '?';
        // create event tree for support values
        this.event = embedding.info.getEventTree(index, parasitePosition[index]);
    }

    // Ensure that a parasite and of its copies all map to different hosts.
    void remapCopies() {
        if (this.copies == null) {
            return;
        }
        Iterator hostIter = embedding.info.phi.getHosts(index).iterator();

        for (ParasiteNode copy : copies) {
            int e_H = (Integer) hostIter.next();
            if (e_H == parasitePosition[index].ID) {
                e_H = (Integer) hostIter.next();
            }
            if (copy.copyPosition.ID != e_H) {
                copy.copyPosition = new HostLocation(e_H, embedding.info.hostTiming.tipTime, true, embedding.info.hostTree.getNodeID(e_H));
            }
        }
    }

    //rank within sorted array of ParasiteNodes (comparison fuction below)
    void setRank(int r) {
        this.rank = r;
    }

    void setTipNamePosition() {
        // This, as the name might suggest, sets the positions for the labels on
        // the tips, for the parasites. Some variables are here to store calculations
        // and make code a bit simpler.  
        // these are for the graphics panel.
        int x = this.getX() + this.getWidth() + 3;
        int y = this.getY() + (this.getHeight() - panel.edgeSpacing) / 2 + 1;
        tipName.setBounds((int) x, y, panel.tipNameWidth, panel.edgeSpacing - 2);
    }


    public void setSupportText(EventInfo currEvent){
        if(currEvent.supportLabel == null) {
            return;
        }
        if(panel.supportIsOn && currEvent.eventType != CostModel.TIP && !panel.modified){
            if(panel.supportType == SupportOptions.DISTINCT){
                if(currEvent.numSolns == 0) {
                    currEvent.supportLabel.setText("<1");
                } else {
                    currEvent.supportLabel.setText("" + currEvent.numSolns);
                }
            } else { // should never hit this case but just incase clear text
                currEvent.supportLabel.setText("");
            }
        } else {
            currEvent.supportLabel.setText("");
        }
    }

    @Override
    public void paintOverlay(Graphics2D g) {
        // These paint the nodes originally and when they are highlighted.  
        g.setColor(color);
        if (panel.highlighted == this) {
            g.fillOval(1, 1, 9, 9);
        } else {
            g.fillOval(3, 3, 5, 5);
        }

        if (this.getHostLocation().isNode && !isTip) {
            g.setColor(DrawingObjects.highlightNodeColor);
            if (panel.highlighted == this) {
                g.fillOval(2, 2, 7, 7);
            } else {
                g.fillOval(4, 4, 3, 3);
            }
        }

        super.paintOverlay(g);
    }

    //change color based upon existence & cost of other solutions
    void setColor() {
        int colorMode = embedding.colorMode(this.index);
        switch (colorMode) {
            case 1:
                color = DrawingObjects.equalSolutions;
                break;
            case 2:
                color = DrawingObjects.betterSolutions;
                break;
            default:
                color = DrawingObjects.worseSolutions;
                break;
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) { //double click, move to optimal location
            embedding.move(this.index, embedding.getOptimalPosition(this.index));
            panel.parasitesMapped = false;
            this.getParent().repaint();
        }
    }

    //start dragging the node and activate all possible locations,
    //with appropriate colors
    public void mousePressed(MouseEvent e) {
        if (panel.dragging == null && panel.highlighted == this && !e.isControlDown()) {
            panel.startDrag(this);
        }
    }

    //check to see if node is on a new location for the H tree.
    //if so, then tell Embedding.java and remap the P tree accordingly
    public void mouseReleased(MouseEvent e) {
        if (panel.dragging == this) {
            panel.stopDrag();
        }
    }

    public void mouseEntered(MouseEvent e) {
        if (panel.dragging == null && panel.highlighted == null) {
            panel.highlighted = this;
            repaint();
        }
    }

    public void mouseExited(MouseEvent e) {
        if (panel.highlighted == this) {
            panel.highlighted = null;
            repaint();
        }
    }

    // This function will return true if this is a descendant of potentialParent
    // or if this and potentialParent are (copies of) the same ParasiteNode.
    boolean isDescendant(ParasiteNode potentialParent) {
        if (this.index == potentialParent.index) {
            return true;
        }
        if (this.Parent == null) {
            return false;
        }
        return Parent.isDescendant(potentialParent);
    }

    //this function is used to sort the parasite tree based on the time the node
    //is mapped to on the host tree. ties are broken if one node is an ancestor of the other
    public int compareTo(Object o) {
        ParasiteNode temp = (ParasiteNode) o;
        HostLocation thisPosition = this.getHostLocation();
        HostLocation tempPosition = temp.getHostLocation();

        int thisTime = thisPosition.time;
        if (thisPosition.isNode) {
            thisTime--;
        }

        int tempTime = tempPosition.time;
        if (tempPosition.isNode) {
            tempTime--;
        }

        if (thisTime < tempTime) {//less than
            return -1;
        } else if (thisTime > tempTime) {//greater than
            return 1;
        } else {//equal
            if (thisPosition.isNode && !tempPosition.isNode) {
                return 1;
            } else if (!thisPosition.isNode && tempPosition.isNode) {
                return -1;
            }
            //now need to differentiate between parasite nodes that are mapped to
            //the same HostLocation or to tips at the same time.

            //map ancestors higher on the tree
            if (this.isDescendant(temp)) {
                return 1;
            } else if (temp.isDescendant(this)) {
                return -1;
            }

            //it doesn't really matter, but say the parasite with smaller index comes first

            if (this.index < temp.index) {
                return -1;
            }
            return 1;
        }
    }

    //give each P node a "rank" within a timeslice
    void mapHoriz() {
        if (!this.isCopy) {
            HostLocation location = this.getHostLocation();
            int time = hostTiming.timeOfNode(location.ID) - location.time - 1;
            if (time == -1) {
                time = 0;
            }
            this.hostSegment = hostNodes[location.ID].segments[time];
        } else {
            this.hostSegment = hostNodes[copyPosition.ID].segments[0];
        }
    }

    //set the pixel location of a parasite tree based upon XOffset and YOffset
    //XOffset->rank within timeslice
    //YOffset->rank on EdgeSegment
    void setLocation() {
        if (panel.dragging != this && (hostSegment.getX(XOffset) - 5 != this.getX() || hostSegment.getY(YOffset) - 5 != this.getY())) {
            this.setLocation(hostSegment.getX(XOffset) - 5, hostSegment.getY(YOffset) - 5);
        }
    }

    // Determine whether to draw Lchild or Rchild first. Prevents some
    // criss-crossing of parasite edges and ensures that children of host
    // switches not switching hosts are drawn first.
    ParasiteNode selectChild(int c) {

        if (c == 0) {
            HostNode currentHN = hostNodes[this.getHostLocation().ID];
            HostNode LchildHN = hostNodes[Lchild.getHostLocation().ID];
            HostNode RchildHN = hostNodes[Rchild.getHostLocation().ID];
            if (LchildHN == RchildHN) {
                if (this.Lchild.rank < Rchild.rank) {
                    LchildChosenFirst = true;
                } else {
                    LchildChosenFirst = false;
                }
            } else if (currentHN.isTip) {
                //Here P is on a H tip and its "sibling" is not, so a HS
                //event occurred. Draw the child not switching hosts first.
                if (LchildHN == currentHN) {
                    LchildChosenFirst = true;
                } else {
                    LchildChosenFirst = false;
                }
            } else {
                boolean done;
                do {
                    //travel down the tree until the children are on different subtrees,
                    //which will determine which one gets drawn first
                    boolean LPisChildLH = LchildHN.isDescendant(currentHN.Lchild);
                    boolean RPisChildLH = RchildHN.isDescendant(currentHN.Lchild);
                    if (LchildHN.isDescendant(currentHN) && !RchildHN.isDescendant(currentHN)) {
                        //host switch, draw child not switching hosts first
                        LchildChosenFirst = true;
                        done = true;
                    } else if (!LchildHN.isDescendant(currentHN) && RchildHN.isDescendant(currentHN)) {
                        //host switch, draw child not switching hosts first
                        LchildChosenFirst = false;
                        done = true;
                    } else if (LchildHN == currentHN) {
                        if (RPisChildLH) {
                            LchildChosenFirst = true;
                        } else {
                            LchildChosenFirst = false;
                        }
                        done = true;
                    } else if (RchildHN == currentHN) {
                        if (LPisChildLH) {
                            LchildChosenFirst = false;
                        } else {
                            LchildChosenFirst = true;
                        }
                        done = true;
                    } else if (LPisChildLH && !RPisChildLH) {
                        LchildChosenFirst = false;
                        done = true;
                    } else if (!LPisChildLH && RPisChildLH) {
                        LchildChosenFirst = true;
                        done = true;
                    } else if (LPisChildLH && RPisChildLH) {
                        done = false;
                        currentHN = currentHN.Lchild;
                    } else {
                        done = false;
                        currentHN = currentHN.Rchild;
                    }
                } while (!done);
            }
            if (LchildChosenFirst) {
                return Lchild;
            }
            return Rchild;
        }
        if (LchildChosenFirst) {
            return Rchild;
        }
        return Lchild;
    }

    //get the XOffset for the ParasiteNode
    void placeChildX(ParasiteNode child) {
        if (child == null) {
            return;
        }
        if (child.isTip) {
            child.hostSegment = hostNodes[child.getHostLocation().ID].segments[0];
            child.XOffset = child.hostSegment.numParasiteEvents;
        } else {
            if (child.getHostLocation().isNode) {
                child.XOffset = panel.getCospSlot(child.hostSegment.time);
            } else {
                child.XOffset = panel.getFirstOpenSlot(child.hostSegment.time);
            }
        }
    }

    //wrapper class to contain variables for drawing the P tree
    private class currData {

        int currParID;
        int currX;
        int currTime;
        int currID;
        EdgeSegment currSegment;
        boolean childPlacedWithParent;
        Point lossPoint;
        boolean firstTime;
        boolean doneDrawing;
        int lossX;
        int lossY;

        public currData() {
            lossPoint = new Point();
        }
    }

    // Draws duplications in the parasite tree.  All duplication events that
    // are drawn are also saved in the shapes arraylist.
    void drawDuplication(Graphics g, currData d, HostLocation location, int childIndex) {
        // true if childIndex's parent is in a polytomy without the child.
        boolean onlyParentPolytomy = false;
        // true if childIndex's parent and childIndex are in the same polytomy
        boolean bothSamePolytomy = false;
        int parent = hostTiming.parasiteTree.getParent(childIndex);
        if (panel.highlightParasitePolytomy && panel.dragging == null && hostTiming.parasiteTree.isInPolytomy(parent)
                & hostTiming.parasiteTree.samePolytomy(parent, childIndex)) {
            bothSamePolytomy = true;
            g.setColor(DrawingObjects.parasitePolytomyColor);
        }

        if (panel.highlightParasitePolytomy && panel.dragging == null && hostTiming.parasiteTree.isInPolytomy(parent)
                & !hostTiming.parasiteTree.samePolytomy(parent, childIndex)) {
            onlyParentPolytomy = true;
        }

        // Lots of lines are going to be drawn, so variables to hold them are
        // initialized here.  The averages, widths, and heights are only relevant
        // to shapes.
        double x1;
        double y1;
        double x2;
        double y2;
        if (d.childPlacedWithParent) {
            y1 = d.currSegment.getY(d.currSegment.numParasites + 1);
            this.hostSegment.numParasites++;
        } else {
            y1 = d.currSegment.getY(YOffset);
        }
        if (!d.firstTime) { //draw loss event
            drawLoss(g, d, (int) y1);
        }
        d.childPlacedWithParent = true;
        if (this.hostSegment != d.currSegment) { //draw lines up to the parent's EdgeSegment
            // storing the points now.
            x1 = d.currSegment.getX(d.currX);
            x2 = d.currSegment.getX();
            final double lastX = d.currSegment.getX();
            // Draws LAST part of branch from a child to the next node/tip.
            g.drawLine((int) x1, (int) y1, (int) x2, (int) y1);
            d.currSegment.numParasites++;
            for (int i = d.currTime - 1; i > location.time; i--) {
                //add left child on all segments except parent's
                d.currSegment = hostNodes[d.currID].segments[hostTiming.timeOfNode(d.currID) - i - 1];
                // saving more points
                x1 = d.currSegment.getX() + d.currSegment.getWidth();
                x2 = d.currSegment.getX();
                // Draws PART of line from HOST SWITCH NODE to NEXT NODE/TIP
                // if it's REALLY LONG (multiple segments.
                // (Non-host-switching path)
                g.drawLine((int) x1, (int) y1, (int) x2, (int) y1);
                d.currSegment.numParasites++;
            }
            //now connect the child to the parent
            x1 = hostSegment.getX() + hostSegment.getWidth();
            x2 = hostSegment.getX(XOffset);
            y2 = hostSegment.getY(YOffset);
            final double firstX = x1;
            // Draws FIRST PART of line going DIRECTLY from a HOST SWITCH NODE
            // to a TIP (on different edge segments, multiple line pieces needed)
            // (Non-host-switching path).

            g.drawLine((int) x1, (int) y1, (int) x2, (int) y1);
            if (onlyParentPolytomy) {
                g.setColor(DrawingObjects.parasitePolytomyColor);
            }
            g.drawLine((int) x2, (int) y1, (int) x2, (int) y2);
            if (onlyParentPolytomy) {
                g.setColor(DrawingObjects.parasiteEdgeColor);
            }
        } else {//on the same EdgeSegment, just connect the dots
            x1 = this.hostSegment.getX(d.currX);
            x2 = this.hostSegment.getX(XOffset);
            y2 = this.hostSegment.getY(YOffset);
            // Draws line from a HOST SWITCH NODE to a TIP (same edge segment,
            // only one line piece needed) (Non- host-switching path).
            g.drawLine((int) x1, (int) y1, (int) x2, (int) y1);
            if (onlyParentPolytomy) {
                g.setColor(DrawingObjects.parasitePolytomyColor);
            }
            g.drawLine((int) x2, (int) y1, (int) x2, (int) y2);
            if (onlyParentPolytomy) {
                g.setColor(DrawingObjects.parasiteEdgeColor);
            }
        }
        if (bothSamePolytomy) {
            g.setColor(DrawingObjects.parasiteEdgeColor);
        }
    }

    void drawHostSwitch(Graphics g, currData d, HostLocation location, int childIndex) {
        // true if childIndex's parent is in a polytomy without the child.
        boolean onlyParentPolytomy = false;
        // true if childIndex's parent and childIndex are in the same polytomy
        boolean bothSamePolytomy = false;
        int parent = hostTiming.parasiteTree.getParent(childIndex);
        if (panel.highlightParasitePolytomy && panel.dragging == null && hostTiming.parasiteTree.isInPolytomy(parent)
                & hostTiming.parasiteTree.samePolytomy(parent, childIndex)) {
            bothSamePolytomy = true;
            g.setColor(DrawingObjects.parasitePolytomyColor);
        }
        if (panel.highlightParasitePolytomy && panel.dragging == null && hostTiming.parasiteTree.isInPolytomy(parent)
                & !hostTiming.parasiteTree.samePolytomy(parent, childIndex)) {
            onlyParentPolytomy = true;
        }

        //d.currSegment.numParasites++;
        this.isHS = true;
        HostNode tempHN = hostNodes[d.currID];
        double x1;
        double y1 = tempHN.segments[0].getY(tempHN.segments[0].numParasites + 1);
        double x2;

        if (!d.firstTime) { //draw loss event
            drawLoss(g, d, (int) y1);
        }

        EdgeSegment tempES = hostNodes[d.currID].segments[hostTiming.timeOfNode(d.currID) - location.time - 1];
        tempES.numParasites++;
        if (location.time != d.currTime) { //draw lines up to the time of the parent
            x1 = d.currSegment.getX(d.currX);
            x2 = d.currSegment.getX();
            // Draws last part of HOST-SWITCHING PATH from HOST-SWITCHING NODE
            // to next node/tip.
            final double lastX = d.currSegment.getX();
            g.drawLine((int) x1, (int) y1, (int) x2, (int) y1);
            d.currSegment.numParasites++;

            for (int i = d.currTime - 1; i > location.time; i--) {
                d.currSegment = hostNodes[d.currID].segments[hostTiming.timeOfNode(d.currID) - i - 1];
                x1 = d.currSegment.getX() + d.currSegment.getWidth();
                x2 = d.currSegment.getX();
                // Draws parts of HOST-SWITCHING PATH from HOST-SWITCHING NODE
                // to next node/tip.
                g.drawLine((int) x1, (int) y1, (int) x2, (int) y1);
                d.currSegment.numParasites++;
            }

            x1 = hostSegment.getX() + hostSegment.getWidth();
            x2 = hostSegment.getX(XOffset);
            final double firstX = d.currSegment.getX(XOffset);
            // Draws FIRST horizontal segment of HOST-SWITCHING PATH from 
            // HS node to next node/tip.
            g.drawLine((int) x1, (int) y1, (int) x2, (int) y1);
        } else {
            x1 = d.currSegment.getX(d.currX);
            x2 = hostSegment.getX(XOffset);
            // If it fits in one line segment, draws the horizontal part of the
            // HOST-SWITCHING PATH from the HOST-SWITCHING NODE to the next
            // node/tip.
            g.drawLine((int) x1, (int) y1, (int) x2, (int) y1);

        }
        if (onlyParentPolytomy) {
            g.setColor(DrawingObjects.parasitePolytomyColor);
        }
        drawHSArrow(g, y1);
        if (onlyParentPolytomy || bothSamePolytomy) {
            g.setColor(DrawingObjects.parasiteEdgeColor);
        }
    }

    void drawCospLoss(Graphics g, currData d, HostLocation location, boolean secondHalfOfFtd, int childIndex) {
        // true if childIndex's parent is in a polytomy without the child.
        boolean onlyParentPolytomy = false;
        // true if childIndex's parent and childIndex are in the same polytomy
        boolean bothSamePolytomy = false;
        if (childIndex != 0) {
            // this function is called with childIndex=0 for the before-the-root drawing.
            int parent = hostTiming.parasiteTree.getParent(childIndex);
            if (panel.highlightParasitePolytomy && panel.dragging == null && hostTiming.parasiteTree.isInPolytomy(parent)
                    & hostTiming.parasiteTree.samePolytomy(parent, childIndex)) {
                bothSamePolytomy = true;
                g.setColor(DrawingObjects.parasitePolytomyColor);
            }
            if (panel.highlightParasitePolytomy && panel.dragging == null && hostTiming.parasiteTree.isInPolytomy(parent)
                    & !hostTiming.parasiteTree.samePolytomy(parent, childIndex)) {
                onlyParentPolytomy = true;
            }
        }

        HostNode tempHN = hostNodes[d.currID];
        EdgeSegment tempES = tempHN.segments[0];
        int x1;
        int y1 = tempES.getY(tempES.numParasites + 1);
        int x2;
        int y2;

        if (!d.firstTime) {
            drawLoss(g, d, (int) y1);
        }
        boolean startDrawing = false;
        d.currSegment.numParasites++;
        // Gets the xPos of each edgesegment between where we are and where 
        // we want to be and uses that to draw a line to the next node.

        for (EdgeSegment temp : tempHN.segments) {
            if (startDrawing == true) {
                x1 = temp.getX() + temp.getWidth();
                x2 = temp.getX();
                // Draws segments of line from COSP NODE to NEXT NODE/TIP.
                g.drawLine(x1, y1, x2, y1);
                temp.numParasites++;
            }
            if (temp == d.currSegment) {
                startDrawing = true;
            }
        }

        if (d.currID != embedding.parasiteRootEdgeStart.ID) {
            x1 = d.currSegment.getX(d.currX);
            x2 = d.currSegment.getX();
            // Draws LAST LINE from COSP NODE to TIP or HOST SWITCH NODE.  
            // This is a horizontal line that can be of varying length.
            g.drawLine(x1, y1, x2, y1);
        } else { // Draw the dummy edge out of the root
            x1 = d.currSegment.getX(d.currX);
            x2 = d.currSegment.getX(d.currX) - 10;
            // TODO UNKNOWN
            g.drawLine((int) x1, (int) y1, (int) x2, (int) y1);

            d.doneDrawing = true;
            return;
        }

        //move up the tree one step, and
        //update all variables to first EdgeSegment
        //of parent HostNode
        boolean wasLeftChild = (hostNodes[d.currID].Parent.Lchild == hostNodes[d.currID]);
        d.currID = hostNodes[d.currID].Parent.index;
        d.currTime = hostTiming.timeOfNode(d.currID) - 1;
        d.currSegment = hostNodes[d.currID].segments[0];

        if (location.ID == d.currID && location.isNode && embedding.info.parasiteTree.root != d.currParID) { //cospeciation event
            x1 = tempHN.segments[tempHN.numSegments - 1].getX();
            x2 = hostNodes[location.ID].segments[0].getX(XOffset);
            y2 = hostNodes[location.ID].segments[0].getY(YOffset);
            // Draws the HORIZONTAL and VERTICAL lines DIRECTLY AFTER a 
            // COSPECIATION NODE.  These lines join the node to the path that
            // leads to the NEXT NODE or TIP.
            g.drawLine((int) x1, (int) y1, (int) x2, (int) y1);
            if (onlyParentPolytomy)
                g.setColor(DrawingObjects.parasitePolytomyColor);
            g.drawLine((int) x2, (int) y1, (int) x2, (int) y2);
            if (onlyParentPolytomy)
                g.setColor(DrawingObjects.parasiteEdgeColor);
            d.doneDrawing = true;
        } else {
            // Don't get a new X when drawing the second half of an ftd event.
            if (secondHalfOfFtd) {
                d.currX = panel.peekMiddleOpenSlot(d.currTime, !wasLeftChild);
            } else {
                d.currX = panel.getMiddleOpenSlot(d.currTime, wasLeftChild);
            }

            //store information to draw loss event in next pass through loop
            d.lossPoint.x = tempHN.segments[tempHN.numSegments - 1].getX();
            d.lossPoint.y = (int) y1;
        }
        g.setColor(DrawingObjects.parasiteEdgeColor);
    }

    void drawLoss(Graphics g, currData d, double Y) {
        // See if it is actually a failure to diverge event.
        if (embedding.info.phi.hasMultihostParasites()) {
            for (int e_PTip : embedding.info.needsFTD[d.currID]) {
                if (embedding.info.parasiteTree.descendant(d.currParID, e_PTip)) {
                    drawFtd(g, d, Y, false);
                    return;
                }
            }
        }

        // storing variables for convenience
        int x1 = d.lossPoint.x;
        int y1 = d.lossPoint.y;

        // Set variables for EPS writer
        hasLoss = true;
        lossX = x1;
        lossY = y1;

        int x2 = d.currSegment.getX(d.currX);
        // Draws the FIRST PART of the horizontal path AFTER the actual loss
        // symbol is drawn.
        g.drawLine(x1, y1, x2, y1);

        int x = d.currSegment.getX(d.currX);
        double max = Math.max(d.lossPoint.y, Y), min = Math.min(d.lossPoint.y, Y);
        // This loop draws the rectangles that form the loss symbol.
        for (int i = (int) min; i <= (int) max; i += 10) {
            double height = (Math.min(max - i, 5));
            // Draws the actual loss symbol (dashed rectangles).
            g.fillRect(x, i, 2, (int) height);
        }
    }

    void drawFtd(Graphics g, currData d, double Y, boolean topHalf) {
        double x1 = d.lossPoint.x;
        double y1 = d.lossPoint.y;
        double x2 = d.currSegment.getX(d.currX);
        double y2;
        // TODO UNKNOWN FTD
        g.drawLine((int) x1, (int) y1, (int) x2, (int) y1);

        int x = d.currSegment.getX(d.currX);
        double max = Math.max(d.lossPoint.y, Y), min = Math.min(d.lossPoint.y, Y);

        // dashed
        // for (int i = min; i <= max; i += 10) {
        //     g.drawLine(x+3, i, x+3, Math.min(max, i + 4));
        // }

        // jagged
        // current method to draw ftd lines.
        boolean swap = false;       
        for (int i = (int) min; i <= (int) max; i += 3) {
            if (!swap) {
                y2 = Math.min(max, i + 3);
                // Draws one type of dash in the FTD symbol.
                g.drawLine(x, i, x+3, (int)y2);

            } else {
                y2 = Math.min(max, i + 3);
                // Draws the other type of dash in the FTD symbol.
                g.drawLine((int) x + 3, i, (int) x, (int) y2);

            }
            swap = !swap;
        }

        // hashed
        //  for (int i = min; i <= max; i += 5) {
        //      g.drawLine(x, i, x+3, Math.min(max, i + 3));
        //  }
    }

    void drawHSArrow(Graphics g, double Y) {
        int x1 = hostSegment.getX(XOffset);
        int y1 = hostSegment.getY(YOffset);
        // Draws the FIRST PART of a host-switch-node-to-tip path (The part 
        // with the arrow on it.
        g.drawLine(x1, (int) Y, x1, y1);
        
        int sizeModifier = Math.max(80, (4*(embedding.info.hostTree.numTips + embedding.info.phi.size())));

        //int space = panel.getWidth() / (4*(embedding.info.hostTree.numTips + embedding.info.phi.size()));
        int space = panel.getWidth() / (sizeModifier);
        int direction;
        if (Y > y1) {
            direction = -1;
        } else {
            direction = 1;
        }
        double y = (Y + y1) / 2;
        double xA = x1 - space / 2;
        double xB = x1 + space / 2;
        double y2 = (Y + y1) / 2 + direction * space;
        // Draws the actual arrow on the first part of the path.
        g.drawLine((int) x1, (int) y, (int) xA, (int) y2);
        g.drawLine((int) x1, (int) y, (int) xB, (int) y2);
    }

    // Draw the edge before the parasite root
    void drawBeforeRoot(Graphics g) {
        HostLocation rootStartLocation = embedding.parasiteRootEdgeStart; // get the root edge's starting position
        currData d = new currData();
        HostLocation rootNodeLocation = this.getHostLocation();

        if (rootNodeLocation == rootStartLocation) {
            return;
        }

        d.currParID = this.index;
        d.currTime = this.hostSegment.time;
        d.currID = rootNodeLocation.ID;
        d.currX = this.XOffset;
        d.currSegment = this.hostSegment;
        d.firstTime = true;
        d.doneDrawing = false;

        while (!d.doneDrawing) {
            drawCospLoss(g, d, rootStartLocation, false, 0);
            d.firstTime = false;
        }
    }

    
    // Places support values for each event
    public void drawEventSupport(currData d,  EventInfo currEvent){
        if(currEvent == null){
            return;
        }
        d = new currData();
        ParasiteNode children[] = new ParasiteNode[]{Lchild, Rchild};
        HostLocation location = getHostLocation();
        int labelHS = -1;
        for (ParasiteNode child : children) {
            if (child == null) {
                continue;
            }

            // set up currData
            HostLocation childLocation = child.getHostLocation();
            d.currParID = child.index;
            d.currTime = child.hostSegment.time;
            d.currID = childLocation.ID;
            d.currX = child.XOffset;
            d.currSegment = child.hostSegment;
            d.firstTime = true;
            d.doneDrawing = false;

            if (currEvent.eventType == CostModel.DUPLICATION) {
                // if loss follows check event children
                EventInfo nextEvent;
                if (child == Lchild) {
                    nextEvent = currEvent.subtree1;
                } else {
                    nextEvent = currEvent.subtree2;
                }
                if (nextEvent.eventLoc.ID == location.ID) {
                    labelHS = this.hostSegment.getX(XOffset);
                }
                child.drawEventSupport(d, nextEvent);
                
            } else if (currEvent.eventType == CostModel.HOST_SWITCH) {
                // if loss follows check event children
                EventInfo nextEvent;
                if (child == Lchild) {
                    nextEvent = currEvent.subtree1;
                } else {
                    nextEvent = currEvent.subtree2;
                }
                // parasite statys on host
                if (d.currID == location.ID) {
                    labelHS = this.hostSegment.getX(XOffset);
                } else if (nextEvent.eventLoc.ID == location.ID) {
                    labelHS = this.hostSegment.getX(XOffset);
                }
            }
        }
        
        if(currEvent.eventType != CostModel.HOST_SWITCH || parasitePosition[index].nodeID == currEvent.eventLoc.nodeID){
            setSupportText(currEvent);
            int labelY = hostNodes[currEvent.eventLoc.ID].segments[0].getY(YOffset);
            HostNode tempNode = hostNodes[currEvent.eventLoc.ID];
            int labelX = tempNode.segments[0].getX(XOffset);
            
            // label position set above
            if(labelHS != -1) {
                labelX = labelHS;
            }

            currEvent.supportLabel.setTextSize((int)(this.getHeight()*panel.frame.getScale()*.75));
            currEvent.supportLabel.setBounds(labelX, labelY, currEvent.supportLabel.getPreferredSize().width, currEvent.supportLabel.getPreferredSize().height);

            panel.add(currEvent.supportLabel);
            
            // draw loss label
            if (currEvent.eventType == CostModel.LOSS) {

                labelY = hostNodes[currEvent.eventLoc.ID].segments[0].getY(YOffset);
                // correct loss placement for all but cospec & loss this is messed up when the first edge doesn't have the event on the far right
                labelX = tempNode.segments[0].getX(tempNode.segments[0].numParasiteEvents - tempNode.segments[0].numParasites + YOffset - 1); 

                currEvent.supportLabel.setBounds(labelX, labelY, currEvent.supportLabel.getPreferredSize().width, currEvent.supportLabel.getPreferredSize().height);
                drawEventSupport(d, currEvent.subtree1); 
                return;
            }

            if (currEvent.eventType == CostModel.FAILURE_TO_DIVERGE) {
                labelY = hostNodes[currEvent.eventLoc.ID].segments[0].getY(YOffset);
                labelX = tempNode.segments[0].getX(tempNode.segments[0].numParasiteEvents - tempNode.segments[0].numParasites + YOffset - 1);
                    
                currEvent.supportLabel.setBounds(labelX, labelY, currEvent.supportLabel.getPreferredSize().width, currEvent.supportLabel.getPreferredSize().height);
                drawEventSupport(d, currEvent.subtree1);
                drawEventSupport(d, currEvent.subtree2);
                return;

            } else if (currEvent.eventType == CostModel.DUPLICATION) {
                return; // children were handled above
            }
        } else if (currEvent.eventType == CostModel.HOST_SWITCH && parasitePosition[index].nodeID != currEvent.eventLoc.nodeID) {
            if(Lchild != null){
                Lchild.drawEventSupport(d, currEvent.subtree1);
            }
             
            return;
        }

        // Child choosing has already been computed, in positionNode.
        ParasiteNode firstChildToDraw;
        ParasiteNode secondChildToDraw;
        EventInfo firstSubtree;
        EventInfo secondSubtree;
        if (LchildChosenFirst) {
            firstChildToDraw = Lchild;
            secondChildToDraw = Rchild;
            firstSubtree = currEvent.subtree1;
            secondSubtree = currEvent.subtree2;
        } else {
            firstChildToDraw = Rchild;
            secondChildToDraw = Lchild;
            firstSubtree = currEvent.subtree2;
            secondSubtree = currEvent.subtree1;
        }
        if (firstChildToDraw != null) {
            firstChildToDraw.drawEventSupport(d, firstSubtree);
        }
        if (secondChildToDraw != null) {
            secondChildToDraw.drawEventSupport(d, secondSubtree);
        }
    }

    // Connect the dots from this ParasiteNode to its children and then
    // recurse on the children. This recursion must be depth first or else
    // failure to diverge events may not align correctly when drawn.
    void drawParasite(Graphics g) {
        HostLocation location = this.getHostLocation(); //get the parent's location on the host tree
        int tParent = location.time;
        currData d = new currData();
        d.childPlacedWithParent = false; //for duplication events and drawing the 2 children differently
        if (location.isNode) { //map the time to a time "slice"
            tParent--;
        }

        // Child choosing has already been computed, in positionNode.
        ParasiteNode firstChildToDraw;
        ParasiteNode secondChildToDraw;
        if (LchildChosenFirst) {
            firstChildToDraw = Lchild;
            secondChildToDraw = Rchild;
        } else {
            firstChildToDraw = Rchild;
            secondChildToDraw = Lchild;
        }

        ParasiteNode children[] = new ParasiteNode[]{firstChildToDraw, secondChildToDraw};

        for (ParasiteNode child : children) {
            placeChildX(child); //determine the horizontal position
            HostLocation childLocation = child.getHostLocation();
            d.currParID = child.index;
            d.currTime = child.hostSegment.time;
            d.currID = childLocation.ID;
            d.currX = child.XOffset;
            d.currSegment = child.hostSegment;
            d.firstTime = true;
            d.doneDrawing = false;

            while (!d.doneDrawing) {
                if (location.ID == d.currID) { //duplication
                    drawDuplication(g, d, location, child.index);
                    d.doneDrawing = true;
                } else if (hostTiming.timeOfNode(hostNodes[location.ID].index) != 1 && tParent >= hostTiming.timeOfNode(hostNodes[d.currID].Parent.index)) {
                    drawHostSwitch(g, d, location, child.index);
                    d.doneDrawing = true;
                } else { //if parent is high up in the tree, just draw a line and then move up
                    drawCospLoss(g, d, location, false, child.index);
                }
                d.firstTime = false;
            }

            // Draw any copies.
            if (child.copies != null && !child.copies.isEmpty()) {
                LinkedList<Integer> ftdPositionsClone = (LinkedList<Integer>) embedding.info.ftdPositions[child.index].clone();
                drawCopies(g, child.copies, ftdPositionsClone);
            }

            // Recursively draw the children parasites in depth first order.
            if (!child.isTip) {
                child.drawParasite(g);
            }
        }
    }

    // Draw ParasiteNode copies from their tip locations back to the parasite tree.
    // ASSUMPTION: copies and ftdLocations are not null and are the same size.
    private void drawCopies(Graphics g, LinkedList<ParasiteNode> copies, LinkedList<Integer> ftdLocations) {
        HostLocation location = this.getHostLocation(); //get the parent's location on the host tree
        assert (copies.size() == ftdLocations.size());

        for (ParasiteNode copy : copies) {
            currData d = new currData();
            d.childPlacedWithParent = false; //for duplication events and drawing the 2 children differently

            placeChildX(copy); //determine the horizontal position
            HostLocation copyLocation = copy.getHostLocation();
            d.currParID = copy.index;
            d.currTime = copy.hostSegment.time;
            d.currID = copyLocation.ID;
            d.currX = copy.XOffset;
            d.currSegment = copy.hostSegment;
            d.firstTime = true;
            d.doneDrawing = false;

            while (!d.doneDrawing) {
                boolean secondHalfOfFtd = false;
                // Has a failure to diverge event already been used by a copy?
                if (!ftdLocations.contains(hostNodes[d.currID].Parent.index) && embedding.info.needsFTD[hostNodes[d.currID].Parent.index].contains(copy.index)) {
                    secondHalfOfFtd = true;
                } else if (embedding.info.hostTree.descendant(hostNodes[d.currID].Parent.index, parasitePosition[copy.index].ID)) { // Has a failure to diverge event already been used by the original?
                    secondHalfOfFtd = true;
                }

                drawCospLoss(g, d, location, secondHalfOfFtd, copy.index);
                d.firstTime = false;

                if (ftdLocations.contains(d.currID)) {
                    // Connect to the ftd with a loss and move onto the next copy.
                    HostNode tempHN = hostNodes[d.currID];
                    EdgeSegment tempES = tempHN.segments[0];
                    int Y;
                    if (secondHalfOfFtd) {
                        Y = tempES.getY(tempES.numParasites);
                    } else {
                        Y = tempES.getY(tempES.numParasites + 1);
                    }
                    drawFtd(g, d, Y, true);
                    // Cast to Integer to avoid confusion with remove(int)
                    ftdLocations.remove((Integer) d.currID);
                    d.doneDrawing = true;
                }
            }
        }
    }

    // Update the number of parasites along edges occupied by the parasite root edge.
    void positionBeforeRoot() {
        HostLocation rootStartLocation = embedding.parasiteRootEdgeStart; //get the parent's location on the host tree

        HostLocation rootNodeLocation = this.getHostLocation();
        int currTime = this.hostSegment.time;
        int currID = rootNodeLocation.ID;
        int currX = this.XOffset;
        boolean firstTime = true;
        EdgeSegment currSegment = this.hostSegment;
        HostNode tempHN;
        boolean donePlacingNode = false;

        while (!donePlacingNode) {
            tempHN = hostNodes[currID];

            if (!firstTime) {
                currSegment.numParasites++;
            }
            if (currID == rootStartLocation.ID) {
                donePlacingNode = true;
                continue;
            } else {
                boolean startDrawing = false;
                for (EdgeSegment temp : tempHN.segments) {
                    if (startDrawing == true) {
                        temp.numParasites++;
                    }
                    if (temp == currSegment) {
                        startDrawing = true;
                    }
                }
            }

            //move up the tree one step, and
            //update all variables to first EdgeSegment
            //of parent HostNode
            boolean wasLeftChild = (hostNodes[currID].Parent.Lchild == hostNodes[currID]);
            currID = hostNodes[currID].Parent.index;
            currTime = hostTiming.timeOfNode(currID) - 1;
            currX = panel.getMiddleOpenSlot(currTime, wasLeftChild);
            currSegment = hostNodes[currID].segments[0];

            firstTime = false;
        }
    }

    // Position a ParasiteNode's children and then recurse. This recursion must
    // be depth first or else failure to diverge events may not align correctly.
    void positionNode() {
        HostLocation location = this.getHostLocation(); //get the parent's location on the host tree
        int tParent = location.time;
        boolean childPlacedWithParent = false; //for duplication events and drawing the 2 children differently
        if (location.isNode) { //map the time to a time "slice"
            tParent--;
        }

        setEventType('d'); //initialize to duplication, and change to HS if necessary
        //this is only for ToolTips, not any drawing of the tree

        // Decide the order in which to draw the children. Then position each
        // child, including any copies.
        ParasiteNode firstChildToPosition = selectChild(0);
        ParasiteNode secondChildToPosition = selectChild(1);
        ParasiteNode[] children = new ParasiteNode[]{firstChildToPosition, secondChildToPosition};

        for (ParasiteNode child : children) {
            placeChildX(child); //determine the horizontal position

            HostLocation childLocation = child.getHostLocation();
            int currTime = child.hostSegment.time;
            int currID = childLocation.ID;
            int currX = child.XOffset;
            boolean firstTime = true;
            EdgeSegment currSegment = child.hostSegment;
            boolean donePlacingNode = false;

            while (!donePlacingNode) {
                //if parent is on a higher edge segment for the same host node
                if (location.ID == currID) { //duplication
                    if (childPlacedWithParent) {
                        if (firstTime) { //set the position of the child
                            child.YOffset = currSegment.numParasites + 1;
                            // Look right of the event for segments with more parasites.
                            for (EdgeSegment temp : hostNodes[currID].segments) {
                                if (temp == currSegment) {
                                    break;
                                }
                                if (temp.numParasites >= child.YOffset) {
                                    child.YOffset = temp.numParasites + 1;
                                }
                            }
                        }
                        this.hostSegment.numParasites++;
                    } else {
                        if (firstTime) {
                            child.YOffset = YOffset;
                        }
                    }

                    childPlacedWithParent = true;
                    if (this.hostSegment != currSegment) {
                        currSegment.numParasites++;
                        for (int i = currTime - 1; i > location.time; i--) {
                            //add child on all segments except parent's
                            currSegment = hostNodes[currID].segments[hostTiming.timeOfNode(currID) - i - 1];
                            currSegment.numParasites++;
                        }
                    }
                    donePlacingNode = true;
                } //if a host switch event is going to occur soon
                else if (hostTiming.timeOfNode(hostNodes[location.ID].index) != 1 && tParent >= hostTiming.timeOfNode(hostNodes[currID].Parent.index)) {
                    setEventType('h');
                    EdgeSegment landingES = hostNodes[currID].segments[hostTiming.timeOfNode(currID) - location.time - 1];
                    landingES.numParasites++;
                    if (firstTime) {
                        child.YOffset = landingES.numParasites;
                        // Look right of the event for segments with more parasites.
                        for (EdgeSegment temp : hostNodes[currID].segments) {
                            if (temp == landingES) {
                                break;
                            }
                            if (temp.numParasites >= child.YOffset) {
                                child.YOffset = temp.numParasites + 1;
                            }
                        }
                    }
                    if (location.time != currTime) {
                        currSegment.numParasites++;
                        for (int i = currTime - 1; i > location.time; i--) {
                            currSegment = hostNodes[currID].segments[hostTiming.timeOfNode(currID) - i - 1];
                            currSegment.numParasites++;
                        }
                    }

                    donePlacingNode = true;
                } //if parent is high up in the tree, do stuff, then move up the tree
                else { //loss, failure to diverge, or cospeciation
                    HostNode tempHN = hostNodes[currID];
                    currSegment.numParasites++;
                    if (firstTime) {
                        child.YOffset = currSegment.numParasites;
                        // Look right of the event for segments with more parasites.
                        for (EdgeSegment temp : tempHN.segments) {
                            if (temp == currSegment) {
                                break;
                            }
                            if (temp.numParasites >= child.YOffset) {
                                child.YOffset = temp.numParasites + 1;
                            }
                        }
                    }
                    boolean startDrawing = false;
                    for (EdgeSegment temp : tempHN.segments) {
                        if (startDrawing == true) {
                            temp.numParasites++;
                        }
                        if (temp == currSegment) {
                            startDrawing = true;
                        }
                    }
                    //move up the tree one step, and
                    //update all variables to first EdgeSegment
                    //of parent HostNode
                    boolean wasLeftChild = (hostNodes[currID].Parent.Lchild == hostNodes[currID]);
                    currID = hostNodes[currID].Parent.index;
                    currTime = hostTiming.timeOfNode(currID) - 1;
                    currX = panel.getMiddleOpenSlot(currTime, wasLeftChild);
                    currSegment = hostNodes[currID].segments[0];

                    if (location.ID == currID && location.isNode) { //cospeciation event
                        donePlacingNode = true;
                    }
                }
                firstTime = false;
            }

            // Position any copies.
            if (child.copies != null && !child.copies.isEmpty()) {
                LinkedList<Integer> ftdPositionsClone = (LinkedList<Integer>) embedding.info.ftdPositions[child.index].clone();
                positionCopies(child.copies, ftdPositionsClone);
            }

            // Recursively draw the children parasites in depth first order.
            if (!child.isTip) {
                child.positionNode();
            }
        }
        updateToolTip();
    }

    // Position ParasiteNode copies in the display
    // ASSUMPTION: copies and ftdLocations are not null and are the same size.
    private void positionCopies(LinkedList<ParasiteNode> copies, LinkedList<Integer> ftdLocations) {
        assert (copies.size() == ftdLocations.size());

        for (ParasiteNode copy : copies) {
            placeChildX(copy); //determine the horizontal position

            HostLocation copyLocation = copy.getHostLocation();
            int currTime = copy.hostSegment.time;
            int currID = copyLocation.ID;
            int currX = copy.XOffset;
            boolean firstTime = true;
            EdgeSegment currSegment = copy.hostSegment;
            EdgeSegment tempES;
            HostNode tempHN;

            while (true) {
                tempHN = hostNodes[currID];
                tempES = tempHN.segments[0];
                if (firstTime) {
                    copy.YOffset = tempES.numParasites + 1;
                }
                currSegment.numParasites++;
                boolean startDrawing = false;
                for (EdgeSegment temp : tempHN.segments) {
                    if (startDrawing == true) {
                        temp.numParasites++;
                    }
                    if (temp == currSegment) {
                        startDrawing = true;
                    }
                }

                //move up the tree one step, and
                //update all variables to first EdgeSegment
                //of parent HostNode
                boolean wasLeftChild = (hostNodes[currID].Parent.Lchild == hostNodes[currID]);
                currID = hostNodes[currID].Parent.index;
                currTime = hostTiming.timeOfNode(currID) - 1;
                currX = panel.getMiddleOpenSlot(currTime, wasLeftChild);
                currSegment = hostNodes[currID].segments[0];

                // Stop if we reach a new failure to diverge event.
                if (ftdLocations.remove((Integer) currID)) {
                    break;
                }

                firstTime = false;
            }
        }
    }

    HostLocation getHostLocation() {
        if (this.isCopy) {
            return this.copyPosition;
        } else {
            return parasitePosition[this.index];
        }
    }

    @Override
    public DrawingObjects getParent() {
        return (DrawingObjects) super.getParent();
    }

    /**
     * @return the eventType
     */
    protected char getEventType() {
        return eventType;
    }

    /**
     * @param eventType the eventType to set
     */
    protected void setEventType(char eventType) {
        this.eventType = eventType;
    }

    private void updateToolTip() {
        if (!isTip) {
            if (this.getHostLocation().isNode) {
                this.setToolTipText("Cospeciation");
            } else if (eventType == 'd') {
                this.setToolTipText("Duplication");
            } else if (eventType == 'h') {
                this.setToolTipText("Duplication + Host Switch");
            } else {
                System.err.println("Error, undefined event!!!!!");
            }
        }
    }
}
