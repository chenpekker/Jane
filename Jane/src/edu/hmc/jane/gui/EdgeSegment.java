/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.gui;

import edu.hmc.jane.gui.light.LightPanel;
import edu.hmc.jane.solving.SolutionViewerInfo;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

/**
 *
 * @author ben
 */
//each edge segment is a possible alternate location, so each should be an object with an action listener
public class EdgeSegment extends LightPanel implements MouseListener {

    int time; //time slice in which this segment lives; range: [0,n-1]
    int numParasites; //number of parasite edges that live on this segment in the current mapping
    int numParasiteEvents; //number of parasite nodes that live on this segment
    boolean active = false;
    Color color;
    DrawingObjects panel;
    ArrayList<GraphicsShape> shapes;

    public EdgeSegment(int t, DrawingObjects panel) {
        super();
        panel.add(this);
        setOpaque(false);
        this.panel = panel;
        this.time = t;
        this.numParasites = 0;
        this.numParasiteEvents = 0;
        color = panel.hostEdgeColor;
        shapes = new ArrayList<GraphicsShape>();
    }

    void drawSegment(DrawingObjects panel, int vertPosition, SolutionViewerInfo info, int[] adjustedHostTimes) {
        int start = timeToPosition(this.time + 1, panel, info, adjustedHostTimes);
        int end = timeToPosition(this.time, panel, info, adjustedHostTimes);
        this.numParasiteEvents = adjustedHostTimes[this.time+1] - adjustedHostTimes[this.time];
        this.setBounds(end + HostNode.lineWidth / 2, vertPosition - HostNode.highlightWidth - HostNode.lineWidth / 2, 
                start - end, 2 * HostNode.highlightWidth + HostNode.lineWidth);
        GraphicsShape segment = new GraphicsShape(start, vertPosition, end, vertPosition, "Line", 'H');
        shapes.add(segment);
    }

    int timeToPosition(int t, DrawingObjects panel, SolutionViewerInfo info, int[] adjustedHostTimes) {
        return ((panel.getWidth() - panel.tipNameWidth) * adjustedHostTimes[t]) / (panel.totalTime);
    }

    void setColor(Color c) {
        color = c;
    }

    //return pixel position based upon its "rank" within this timeslice
    int getX(int XOffset) {
        return this.getX() + (this.getWidth() * XOffset)/numParasiteEvents;
    }

    //return pixel position based upon its "rank" on the EdgeSegment
    int getY(int YOffset) {
        return this.getY() + HostNode.highlightWidth-YOffset*panel.edgeSpacing;
    }

    @Override
    public void paintOverlay(Graphics2D g) {
        if (this.active) {
            g.setColor(Color.black);
            g.fillRect(0, 0, this.getWidth(), 2 * HostNode.highlightWidth + HostNode.lineWidth);
        }
        g.setColor(color);
        g.fillRect(0, HostNode.highlightWidth, this.getWidth(), HostNode.lineWidth);
        super.paintOverlay(g);
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
}
