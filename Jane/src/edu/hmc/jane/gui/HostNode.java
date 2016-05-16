/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.gui;

import edu.hmc.jane.gui.light.LightLabel;
import edu.hmc.jane.gui.light.LightPanel;
import edu.hmc.jane.solving.SolutionViewerInfo;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 *
 * @author bcousins
 */
public class HostNode extends LightPanel implements MouseListener {

    static final int lineWidth = 2; //pixel width of a line in the host tree
    static final int highlightWidth = 3; //when a host edge is highlighted
    int index; //index of node in nodes
    int nodeID; // Node id based on postorder traversal
    int horizPosition;
    int vertPosition; //pixel position of this node
    boolean isTip = true;
    int numMappedTips; //number of parasite tips mapped, only matters for host tips
    EdgeSegment[] segments; //segmented line emanating from this host node
    int numSegments;
    int numParasiteEvents; //number of losses, cosp, etc. that happen 
    HostNode Lchild = null;
    HostNode Rchild = null;
    HostNode Parent = null;
    DrawingObjects panel; //reference to parent
    boolean active = false;
    Color color;
    LightLabel tipName;

    //only need to call constructor on root of host tree
    public HostNode(int index, HostNode Parent, SolutionViewerInfo info, HostNode[] hostNodes, DrawingObjects panel) {
        super();
        this.setOpaque(false);
        this.panel = panel;
        if (!info.hostTree.isTip(index)) {
            panel.add(this);
        }
        this.color = DrawingObjects.hostEdgeColor;
        this.index = index;
        this.nodeID = info.hostTree.getNodeID(index);
        this.Parent = Parent;
        this.numMappedTips = 0;
        //segment all time slices between this node and its parent
        if (this.Parent != null) {
            numSegments = info.hostTiming.timeOfNode(this.index) - info.hostTiming.timeOfNode(Parent.index);
        } else {
            numSegments = 1;
        }
        segments = new EdgeSegment[numSegments];
        for (int i = 0; i < numSegments; i++) {
            segments[i] = new EdgeSegment(info.hostTiming.timeOfNode(this.index) - i - 1, panel);
        }
        hostNodes[index] = this;
        this.numParasiteEvents = 0;
        //recursively call constructor on children
        if (info.hostTree.node[index].Lchild != -1) {
            isTip = false;
            this.Lchild = new HostNode(info.hostTree.node[index].Lchild, this, info, hostNodes, panel);
        }
        if (info.hostTree.node[index].Rchild != -1) {
            isTip = false;
            this.Rchild = new HostNode(info.hostTree.node[index].Rchild, this, info, hostNodes, panel);
        }
        if (isTip) {
            tipName = new LightLabel(info.hostTree.node[index].name);
            tipName.setOpaque(false);
            if (tipName.getPreferredSize().width > panel.fullWidth) {
                panel.fullWidth = tipName.getPreferredSize().width;
            }
            panel.add(tipName);
        }
    }

    //called if this node is a tip
    void setTipNamePosition() {
        if (panel.tipNameWidth != 10) {
            tipName.setBounds(horizPosition + 5, vertPosition - panel.edgeSpacing / 2 + 1, panel.tipNameWidth, panel.edgeSpacing - 2);
        } else {
            tipName.setBounds(-1, -1, 0, 0);
        }
    }

    @Override
    public void paintOverlay(Graphics2D g) {
        if (this.active) {
            g.setColor(DrawingObjects.hostEdgeColor);
            g.fillRect(0, 0, 2 * highlightWidth + lineWidth, this.getHeight());
        }
        g.setColor(this.color);
        g.fillRect(highlightWidth, 0, lineWidth, this.getHeight());
        super.paintOverlay(g);
    }

    //change color when dragging a parsite node
    void setColor(Color c) {
        this.color = c;
    }

    //as with constructor, only call Draw with root of tree (recursively 
    //resolves all locations based upon children, base case->tips)
    int drawNode(int nextTipLocation, SolutionViewerInfo info, int[] adjustedHostTimes) {
        if (isTip) {
            horizPosition = panel.getWidth() - panel.tipNameWidth;
            nextTipLocation += this.numMappedTips;
            vertPosition = ((nextTipLocation + 1) * panel.getHeight()) / (panel.numTipSlots);
            nextTipLocation++;
        } else {
            if (Lchild != null) {
                nextTipLocation = Lchild.drawNode(nextTipLocation, info, adjustedHostTimes);
            }
            if (Rchild != null) {
                nextTipLocation = Rchild.drawNode(nextTipLocation, info, adjustedHostTimes);
            }
            vertPosition = (Lchild.vertPosition + Rchild.vertPosition) / 2;
            horizPosition = timeToPosition(index, info, panel, adjustedHostTimes);
            this.setBounds(horizPosition - highlightWidth - lineWidth / 2, Lchild.vertPosition - lineWidth / 2, 2 * highlightWidth + lineWidth, (Rchild.vertPosition - Lchild.vertPosition) + lineWidth);
        }

        for (int i = 0; i < this.numSegments; i++) {
            this.segments[i].drawSegment(panel, vertPosition, info, adjustedHostTimes);
        }

        return nextTipLocation;
        //draw this node, based upon its children
        //if this node is a tip, place in next open location
        //if not a tip, draw an edge through the node
        //for all nodes, draw the edge emanating from this node (the edge is made up of a bunch of segments)
    }

    //convert a host tree time into a pixel position based on the size of the current window and parasite mapping
    int timeToPosition(int index, SolutionViewerInfo info, DrawingObjects panel, int[] adjustedHostTimes) {
        return ((panel.getWidth() - panel.tipNameWidth) * adjustedHostTimes[info.hostTiming.timeOfNode(index)]) / (panel.totalTime);
    }

    //request if a HostNode is a parent of another
    boolean isDescendant(HostNode potentialAncestor) {
        if (this.index == potentialAncestor.index) {
            return true;
        }
        if (this.Parent == null) {
            return false;
        }
        return Parent.isDescendant(potentialAncestor);
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        this.active = true;
        repaint();
    }

    public void mouseExited(MouseEvent e) {
        this.active = false;
        repaint();
    }

    public int numEvents(int start, int end) {
        int numEvents = 0;
        for (int i = start; i < end; i++) {
            numEvents = numEvents + this.segments[i].numParasiteEvents;
        }
        return numEvents;
    }
}
