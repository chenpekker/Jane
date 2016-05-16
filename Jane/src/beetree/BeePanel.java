/**
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package beetree;

import edu.hmc.jane.ProblemInstance;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.SortedSet;
import java.awt.geom.Line2D;
import java.io.*;
import edu.hmc.jane.Tree;
import edu.hmc.jane.Node;

/**
 *
 * @author beametitiri Modified by Nicole Wein
 */
public class BeePanel extends JPanel implements MouseListener,
                                                MouseMotionListener {
    /**
     *
     */
    public final static int CLEAR_STATE = 0;
    /**
     *
     */
    public final static int NODE_STATE = 1;
    /**
     *
     */
    public final static int ERASE_STATE = 2;
    /**
     *
     */
    public final static int LINK_STATE = 3;
    /**
     *
     */
    public final static int LABEL_STATE = 4;
    /**
     *
     */
    public final static int TIME_STATE = 5;
    /**
     *
     */
    public final static int REGION_STATE = 6;
    /**
     * 
     */
    public final static int MOVE_STATE = 7;
    
    private BeeTree frame = null;
    private DrawTree host = null;
    private DrawTree parasite = null;
    private int state = -1;
    private static int radius = 5;  // pixel radius of nodes
    private boolean linking = false;
    private boolean dragging = false;
    private DrawTree.Node link1;
    private ArrayList<DrawTree.Node> links = new ArrayList<DrawTree.Node>();
    private ArrayList<Float> timeBorders = new ArrayList<Float>();
    private ArrayList<ArrayList<Double>> regionCosts
        = new ArrayList<ArrayList<Double>>();
    private int prevWidth = 0; // width before resize
    private int panelDisp = 30; // panel displacement due to descriptions of
                                // states and region key
    private DrawTree.Node grabbedNode;
    private DrawTree.Node grabbedArrow;
    private int arrowHead; // position of arrow head when dragging arrow
    private float grabbedBorder = -1;
    private int grabbedLeaf = -1; // for dragging leaf axis - 0 for host,
                                  //                          1 for parasite
    private DrawTree.Node grabbedBranch; 
    private int borderIndex = -1; // index of grabbedBorder
    private int dragX = -1; // x position of link or box when dragging
    private int dragY = -1; // y position of link or box when dragging
    private int clickedX = -1;
    private int clickedY = -1;
    private int dragLeaves = -1; // x position of leaf axis when dragging
    static final int INFINITE_DISTANCE = 410338673;
    private boolean saved = false;
    private boolean quitted = false;

    /**
     *
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public BeePanel() {
        this.setBackground(Color.WHITE);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    /**
     *
     * @param f
     */
    public void setFrame(BeeTree f) {
        frame = f;
        frame.addWindowListener(new WindowAdapter() { 
            public void windowClosing(WindowEvent e) { 
                if (!quitted)
                    quit();
            }
        }); 
    }
    
    /*
     * prompts user to save before quitting
     */
    public void quit() {
        if (!saved
                && (host.getNodes().size() > 1
                       || parasite.getNodes().size() > 1)) {
            int result = JOptionPane.showOptionDialog(
                            null, "Would you like to save?", "",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.PLAIN_MESSAGE, null,
                            new String[]{"Save", "Don't Save", "Cancel"}, "");
            
            if (result == 1) {
                frame.dispose();
                quitted = true;
            } else if (result == 0) {
                frame.save();
                frame.dispose();
                quitted = true;
            }
        } else {
            frame.dispose();
            quitted = true;
        }
    }

    /**
     * State affects which buttons are depressed. Depressed buttons are darkly
     * shaded
     *
     * @param newstate
     */
    public void setState(int newstate) {
        // Unhighlight old state's button
        if (state == CLEAR_STATE) {
            frame.releaseClear();
        } else if (state == NODE_STATE) {
            frame.releaseNode();
        } else if (state == ERASE_STATE) {
            frame.releaseErase();
        } else if (state == LINK_STATE) {
            frame.releaseLink();
        } else if (state == LABEL_STATE) {
            frame.releaseLabel();
        } else if (state == TIME_STATE) {
            frame.releaseTime();
        } else if (state == REGION_STATE) {
            frame.releaseRegion();
        } else if (state == MOVE_STATE) {
            frame.releaseMove();
        }

        // Change state
        state = newstate;

        // Highlight new state's button
        if (state == CLEAR_STATE)
            frame.pressClear();
        else if (state == NODE_STATE)
            frame.pressNode();
        else if (state == ERASE_STATE)
            frame.pressErase();
        else if (state == LINK_STATE)
            frame.pressLink();
        else if (state == LABEL_STATE)
            frame.pressLabel();
        else if (state == TIME_STATE)
            frame.pressTime();
        else if (state == REGION_STATE)
            frame.pressRegion();
        else if (state == MOVE_STATE)
            frame.pressMove();

        redoPanel();
    }

    public DrawTree getHost() {
        return host;
    }

    public DrawTree getParasite() {
        return parasite;
    }
    
    public boolean getSaved() {
        return saved;
    }
    
    public void setSaved(boolean b) {
        saved = b;
    }

    /**
     * clears host tree after warning
     */
    public void clearHost() {
        int answer = 0;
        if (host != null && (host.getNodes().size() > 1 || !allZero())) {
            answer = JOptionPane.showConfirmDialog(null,
                    "Are you sure you want to clear this tree?",
                    "Confirm clear", JOptionPane.YES_NO_OPTION);
        }
        
        if (answer == JOptionPane.YES_OPTION) {
            host = new DrawTree(0, 0, getWidth() / 2 - 5, getHeight(),
                                DrawTree.HOST_TYPE);
            
            if (timeBorders.isEmpty())
                panelDisp = 30;
            else
                panelDisp = 60;
            
            links.clear();
            
            for (DrawTree.Node node : host.getNodes())
                node.getLink().clear();
            
            if (parasite.getNodes().size()==1)
                timeBorders.clear();
            
            saved = false;
        }
    }

    /**
     * clears parasite tree after warning
     */
    public void clearParasite() {
        int answer = 0;
        
        if (parasite != null && parasite.getNodes().size() > 1) {
            answer = JOptionPane.showConfirmDialog(
                         null,
                         "Are you sure you want to clear this tree?",
                         "Confirm clear", JOptionPane.YES_NO_OPTION);
        }
        
        if (answer == JOptionPane.YES_OPTION) {
            parasite = new DrawTree(getWidth() / 2 + 5, 0, getWidth() / 2 - 5,
                                    getHeight(), DrawTree.PARASITE_TYPE);
            links.clear();
            for (DrawTree.Node node : parasite.getNodes())
                node.getLink().clear();
            if (host.getNodes().size() == 1)
                timeBorders.clear();
            saved = false;
        }
    }

    /**
     * Used to add roots, add children, erase nodes, and label nodes.
     *
     * @param e
     */
    public void mouseClicked(MouseEvent e) {
        /**
         * Determines the action to be taken upon a mouse click based on the
         * state of the panel
         */    
        int x = e.getX();
        int y = e.getY() - panelDisp;

        if (state == CLEAR_STATE) {
            // If clicking on left half
            if (x < getWidth() / 2)
                clearHost();
            else if (x > getWidth() / 2)
                clearParasite();
            redoPanel();
        } else if (state == NODE_STATE) {
            addChildNode(x, y);
        } else if (state == ERASE_STATE) {
            removeNode(x, y);
            removeLink(x, y);
        } else if (state == LABEL_STATE) {
            labelNode(x, y);
        } else if (state == LINK_STATE) {
            processLink(x, y);
        } 
    }

    /**
     * Used for linking and editing timings
     *
     * @param e
     */
    public void mousePressed(MouseEvent e) {
        /**
         * If we are in link mode, pass coordinates to link processor to start
         * making a link between nodes
         */
        int x = e.getX();
        int y = e.getY() - panelDisp;

        if (state == LINK_STATE) {
            if (findNode(x, y) != null)
                processLink(x, y);
        }
        
        if (state == MOVE_STATE) {
            if (host.getNodes().size() != 1 && findNode(x, y) == null) {
                int hostLeafX = host.getLeaves().get(0).getX();
                if (x < hostLeafX + 5 && x > hostLeafX - 5 && y > -30)
                    grabbedLeaf = 0;
            }
            if (parasite.getNodes().size() != 1 && findNode(x, y) == null) {
                int paraLeafX = parasite.getLeaves().get(0).getX();
                if (x < paraLeafX + 5 && x > paraLeafX - 5 && y > -30)
                    grabbedLeaf = 1;
            }
            if (host.findBranch(x, y) != null)
                grabbedBranch = host.findBranch(x, y);
            if (parasite.findBranch(x, y) != null)
                grabbedBranch = parasite.findBranch(x, y);    
        }

        if (state == TIME_STATE) {
            DrawTree.Node pressedNode = findNode(x, y);
            if (pressedNode != null) {
                if (pressedNode.getType() != DrawTree.LEAF_TYPE)
                    grabbedNode = pressedNode;
            }

            DrawTree.Node pressedArrow = findArrow(x, y);
            if (pressedArrow != null) {
                grabbedArrow = pressedArrow;
                arrowHead = x;
            }

            int pressedBorder = findBorder(x, y);
            if (pressedBorder != -1)
                grabbedBorder = pressedBorder;
        }
        
        clickedX = x;
        clickedY = e.getY();
        redoPanel();
    }

    /**
     * Releasing finishes the linking between two nodes
     *
     * @param e
     */
    public void mouseReleased(MouseEvent e) {
        /**
         * If we are in link mode, pass coordinates to link processor
         */
        dragging = false;
        int x = e.getX();
        int y = e.getY() - panelDisp;

        if (state == LINK_STATE) {
            if (findNode(x, y) != null)
                processLink(x, y);
        }
        
        if (state == MOVE_STATE)
            processMove(x, y);

        if (state == REGION_STATE && dragX != -1)
            processRegion();

        grabbedNode = null;
        grabbedArrow = null;
        grabbedBorder = -1;
        dragX = -1;
        dragY = -1;
        clickedX = -1;
        clickedY = -1;
        redoPanel();
    }

    /**
     * Dragging nodes, time zone borders, or node arrows
     */
    public void mouseDragged(MouseEvent e) {
        dragging = true;
        int x = e.getX();
        int y = e.getY() - panelDisp;

        if (state == TIME_STATE && !timeBorders.isEmpty()) {
            if (grabbedNode != null)
                dragNode(x, y);
            else if (grabbedArrow != null)
                dragArrow(x, y);
            else if (grabbedBorder > 0)
                dragBorder(x, y);
            Collections.sort(timeBorders);
        }
        
        if (state == MOVE_STATE) {
            if (grabbedLeaf == 0 && x < getWidth() / 2 - 20 
                    && (timeBorders.isEmpty() || x > timeBorders.get(timeBorders.size() / 2 - 1) + 15) 
                    && x > host.getLeafClosest() + 15) {
                for (DrawTree.Node leaf : host.getLeaves()) {
                    leaf.setX(x);
                    dragLeaves = x;
                }
            } else if (grabbedLeaf == 1 && x > getWidth() / 2 + 20 
                    && (timeBorders.isEmpty() || x < timeBorders.get(timeBorders.size()/2)-15) 
                    && x < parasite.getLeafClosest()-15) {
                for (DrawTree.Node leaf : parasite.getLeaves()) {
                    leaf.setX(x);
                    dragLeaves = x;
                }
            } else if (grabbedBranch != null) {
                int lowBound = 0;
                int highBound = getHeight()-panelDisp;
                if (grabbedBranch.getParent().getParent() != null) {
                    if (grabbedBranch.getTreeType() == DrawTree.HOST_TYPE) {
                        DrawTree.Node lowBoundNode = host.findClosestLeaf(grabbedBranch.getParent());
                        if (lowBoundNode != null)
                            lowBound = lowBoundNode.getY();
                        DrawTree.Node highBoundNode = host.findClosestLeafBelow(grabbedBranch.getParent());
                        if (highBoundNode != null)
                            highBound = highBoundNode.getY();
                    }
                    else {
                        DrawTree.Node lowBoundNode = parasite.findClosestLeaf(grabbedBranch.getParent());
                        if (lowBoundNode != null)
                            lowBound = lowBoundNode.getY();
                        DrawTree.Node highBoundNode = parasite.findClosestLeafBelow(grabbedBranch.getParent());
                        if (highBoundNode != null)
                            highBound = highBoundNode.getY();
                    }
                }
                if (y > lowBound && y < highBound) {
                    int prevY = grabbedBranch.getY();
                    grabbedBranch.setY(y);
                    int diff = y - prevY;
                    grabbedBranch.moveSubtree(diff, getHeight() - panelDisp);
                }
            }
        }

        dragX = x;
        dragY = e.getY();

        redoPanel();
    }

    /*
     * node dragging (to change time zone)
     */
    private void dragNode(int x, int y) {
        // dragging non-leaf host node (not past its parent or children)
        for (DrawTree.Node node : host.getNodes()) {
        }
        if (x > 0 && x < getWidth() / 2 - 20
                && (grabbedNode.getParent() == null || x > grabbedNode.getParent().getX())
                && (grabbedNode.getChildren().isEmpty() || x < grabbedNode.getClosest().getX())) {
            saved = false;
            grabbedNode.setX(x);

            // Zone 1
            if (x < timeBorders.get(0)) {
                grabbedNode.setMinTime(1);
                grabbedNode.setMaxTime(1);
            } 
            
            // Last zone
            else if (x > timeBorders.get(timeBorders.size() / 2 - 1)) {
                grabbedNode.setMinTime(timeBorders.size() / 2 + 1);
                grabbedNode.setMaxTime(timeBorders.size() / 2 + 1);
            } else { // Any middle zone
                for (int i = 0; i < timeBorders.size() - 1; i++) {
                    if (x > timeBorders.get(i) && x < timeBorders.get(i + 1)) {
                        grabbedNode.setMinTime(i + 2);
                        grabbedNode.setMaxTime(i + 2);
                    }
                }
            }
        } 
        
        // dragging non-leaf parasite node (not past its parent or children)
        else if (x > getWidth() / 2 + 20 && x < getWidth() 
                && (grabbedNode.getParent() == null || (grabbedNode.getParent().getMaxTime() == 1 || x < grabbedNode.getParent().getX()))
                && (grabbedNode.getChildren().isEmpty() || minChildTime(grabbedNode) == timeBorders.size() + 1 || x > grabbedNode.getClosest().getX())) {
            grabbedNode.setX(x);

            //zone 1
            if (x > getWidth() - timeBorders.get(0)) {
                grabbedNode.setMinTime(1);
                grabbedNode.setMaxTime(1);
            } 
            
            //last zone
            else if (x < timeBorders.get(timeBorders.size() / 2)) {
                grabbedNode.setMinTime(timeBorders.size() / 2 + 1);
                grabbedNode.setMaxTime(timeBorders.size() / 2 + 1);
            } 
            
            //any middle zone
            else {
                for (int i = timeBorders.size() / 2; i < timeBorders.size() - 1; i++) {
                    if (x > timeBorders.get(i) && x < timeBorders.get(i + 1)) {
                        grabbedNode.setMinTime(timeBorders.size() - i);
                        grabbedNode.setMaxTime(timeBorders.size() - i);
                    }
                }
            }
        }
    }

    /*
     * arrow dragging
     */
    private void dragArrow(int x, int y) {
        //dragging host node arrow
        saved = false;
        if (x < getWidth() / 2 - 20 && grabbedArrow.getTreeType() == DrawTree.HOST_TYPE) {
            boolean inBounds = true;
            for (DrawTree.Node child : grabbedArrow.getChildren()) {
                if (x > timeBorders.get(child.getMaxTime() - 1) - 15)
                    inBounds = false;
            }

            //dragging right arrow to allowed spot
            if (x > grabbedArrow.getX() + 10 && inBounds) {
                arrowHead = x;

                //zone 1
                if (x < timeBorders.get(0))
                    grabbedArrow.setMaxTime(1);

                //last zone
                if (x > timeBorders.get(timeBorders.size() / 2 - 1)) {
                    grabbedArrow.setMaxTime(timeBorders.size() / 2 + 1);
                } //any middle zone
                else {
                    for (int i = 0; i < timeBorders.size() / 2 - 1; i++) {
                        if (x > timeBorders.get(i) && x < timeBorders.get(i + 1))
                            grabbedArrow.setMaxTime(i + 2);
                    }
                }
            } //dragging left arrow to allowed spot
            else if (x < grabbedArrow.getX() - 10
                    && x > 15
                    && (grabbedArrow.getParent() == null || grabbedArrow.getParent().getMinTime() == 1
                    || x > timeBorders.get(grabbedArrow.getParent().getMinTime() - 2) + 15)) {
                arrowHead = x;
                
                //zone 1
                if (x < timeBorders.get(0)) {
                    grabbedArrow.setMinTime(1);
                }

                //last zone
                if (x > timeBorders.get(timeBorders.size() / 2 - 1)) {
                    grabbedArrow.setMinTime(timeBorders.size() / 2 + 1);
                } //any middle zone
                else {
                    for (int i = 0; i < timeBorders.size() / 2 - 1; i++) {
                        if (x > timeBorders.get(i) && x < timeBorders.get(i + 1)) {
                            grabbedArrow.setMinTime(i + 2);
                        }
                    }
                }
            }
        } //dragging parasite node arrow
        else if (x > getWidth() / 2 + 20 && grabbedArrow.getTreeType() == DrawTree.PARASITE_TYPE) {
            boolean inBounds = true;
            for (DrawTree.Node child : grabbedArrow.getChildren()) {
                if (x < timeBorders.get(timeBorders.size() - child.getMaxTime()) + 15)
                    inBounds = false;
            }

            //dragging left arrow to allowed spot
            if (x < grabbedArrow.getX() - 10 && inBounds) {
                arrowHead = x;

                //zone 1
                if (x > timeBorders.get(timeBorders.size() - 1)) {
                    grabbedArrow.setMaxTime(1);
                }

                //last zone
                if (x < timeBorders.get(timeBorders.size() / 2)) {
                    grabbedArrow.setMaxTime(timeBorders.size() / 2 + 1);
                } 
                //any middle zone
                else {
                    for (int i = 0; i < timeBorders.size() / 2 - 1; i++) {
                        if (x < timeBorders.get(timeBorders.size() - i - 1) && x > timeBorders.get(timeBorders.size() - i - 2)) {
                            grabbedArrow.setMaxTime(i + 2);
                        }
                    }
                }
            } 
            //dragging right arrow to allowed spot
            else if (x > grabbedArrow.getX() + 10
                    && x < getWidth() - 15
                    && (grabbedArrow.getParent() == null || grabbedArrow.getParent().getMinTime() == 1
                    || x < timeBorders.get(timeBorders.size() - grabbedArrow.getParent().getMinTime() + 1) - 15)) {
                arrowHead = x;
                
                //zone 1
                if (x > timeBorders.get(timeBorders.size() - 1)) {
                    grabbedArrow.setMinTime(1);
                }

                //last zone
                if (x < timeBorders.get(timeBorders.size() / 2 - 1)) {
                    grabbedArrow.setMinTime(timeBorders.size() / 2 + 1);
                } 
                
                //any middle zone
                else {
                    for (int i = 0; i < timeBorders.size() / 2 - 1; i++) {
                        if (x < timeBorders.get(timeBorders.size() - i - 1) && x > timeBorders.get(timeBorders.size() - i - 2)) {
                            grabbedArrow.setMinTime(i + 2);
                        }
                    }
                }
            }
        }
    }

    /*
     * time zone border dragging
     */
    private void dragBorder(int x, int y) {
        // dragging host time zone border (not past adjacent time zone borders or midline)
        saved = false;
        if (findBorder(x, y) != -1 && findBorder(x, y) < getWidth() / 2) {
            grabbedBorder = findBorder(x, y);
            borderIndex = timeBorders.indexOf(grabbedBorder);
            int leafBarrier = getWidth()/2-45;
            if (!host.getLeaves().isEmpty())
                leafBarrier = host.getLeaves().get(0).getX()-15;
            if ((borderIndex == 0 || x > timeBorders.get(borderIndex - 1) + 60)
                    && (borderIndex == timeBorders.size() / 2 - 1 || x < timeBorders.get(borderIndex + 1) - 60)
                    && x < leafBarrier
                    && x > 45) {
                timeBorders.set(borderIndex, (float) x);
                for (DrawTree.Node node : host.getNodes()) {
                    //if time zone border moved to the right past a node, change node's time zone
                    if (node.getMinTime() == borderIndex + 2 && node.getMaxTime() == borderIndex + 2 && x > node.getX()) {
                        node.setMinTime(borderIndex + 1);
                        node.setMaxTime(borderIndex + 1);
                    }

                    //if time zone border moved to the left past a node, change node's time zone
                    if (node.getMinTime() == borderIndex + 1 && node.getMaxTime() == borderIndex + 1 && x < node.getX()) {
                        node.setMinTime(borderIndex + 2);
                        node.setMaxTime(borderIndex + 2);
                    }
                }
            }
        } 
        // dragging parasite time zone border (not past adjacent time zone borders or midline)
        else if (grabbedBorder > getWidth() / 2 && findBorder(x, y) != -1) {
            grabbedBorder = findBorder(x, y);
            borderIndex = timeBorders.indexOf(grabbedBorder);
            int leafBarrier = getWidth()/2+45;
            if (!parasite.getLeaves().isEmpty()) {
                leafBarrier = parasite.getLeaves().get(0).getX()+15;
            }
            if ((borderIndex == timeBorders.size() / 2 || x > timeBorders.get(borderIndex - 1) + 60)
                    && (borderIndex == timeBorders.size() - 1 || x < timeBorders.get(borderIndex + 1) - 60)
                    && x > leafBarrier
                    && x < getWidth() - 45) {
                timeBorders.set(borderIndex, (float) x);
            }
            for (DrawTree.Node node : parasite.getNodes()) {
                //if time zone border moved to the right past a node, change node's time zone
                if (node.getMinTime() == timeBorders.size() - borderIndex + 1 && node.getMaxTime() == timeBorders.size() - borderIndex + 1 && x < node.getX()) {
                    node.setMinTime(timeBorders.size() - borderIndex);
                    node.setMaxTime(timeBorders.size() - borderIndex);
                }

                //if time zone border moved to the left past a node, change node's time zone
                if (node.getMinTime() == timeBorders.size() - borderIndex && node.getMaxTime() == timeBorders.size() - borderIndex && x > node.getX()) {
                    node.setMinTime(timeBorders.size() - borderIndex + 1);
                    node.setMaxTime(timeBorders.size() - borderIndex + 1);
                }
            }
        }
    }

    /**
     *
     * @param e
     */
    public void mouseMoved(MouseEvent e) {
        /*
         * sets cursor to hand if hovering over place to click
         */
        int x = e.getX();
        int y = e.getY() - panelDisp;

        if (state == NODE_STATE) {
            if (findNode(x, y) != null) {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                this.setCursor(Cursor.getDefaultCursor());
            }
        }

        if (state == ERASE_STATE) {
            if (findNode(x, y) != null && findNode(x, y) != host.getRoot() && findNode(x, y) != parasite.getRoot()) {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else if (findLink(x, y) != null) {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else if (findBorder(x, y) != -1) {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                this.setCursor(Cursor.getDefaultCursor());
            }
        }

        if (state == LABEL_STATE) {
            if (findNode(x, y) != null) {
                DrawTree.Node node = findNode(x, y);
                if (node.getType() == DrawTree.LEAF_TYPE) {
                    this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    this.setCursor(Cursor.getDefaultCursor());
                }
            } else {
                this.setCursor(Cursor.getDefaultCursor());
            }
        }

        if (state == LINK_STATE) {
            if (findNode(x, y) != null) {
                DrawTree.Node node = findNode(x, y);
                if (node.getType() == DrawTree.LEAF_TYPE) {
                    if (linking) {
                        if (link1.getX() < getWidth() / 2) //can only link to node on parasite tree
                        {
                            if (x > getWidth() / 2) {
                                this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            } else {
                                this.setCursor(Cursor.getDefaultCursor());
                            }
                        } else //can only link to node on host tree
                        {
                            if (x < getWidth() / 2) {
                                this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            } else {
                                this.setCursor(Cursor.getDefaultCursor());
                            }
                        }
                    } else //not linking
                    {
                        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }
                } else //not leaf
                {
                    this.setCursor(Cursor.getDefaultCursor());
                }
            }else // not node
            {
                this.setCursor(Cursor.getDefaultCursor());
            } 
            redoPanel();
        }
         
        if (state == MOVE_STATE) {
            int hostLeafX=0;
            if (host.getNodes().size() != 1) {
                hostLeafX = host.getLeaves().get(0).getX();
                if (x < hostLeafX + 5 && x > hostLeafX - 5 && y > -30) 
                {
                    this.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)); //on host leaf axis
                    grabbedLeaf = 0;
                    dragLeaves = hostLeafX;
                    redoPanel();
                }
                else {
                    if (host.findBranch(x, y) != null) {
                        this.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)); //on host branch
                    }
                    else {
                        this.setCursor(Cursor.getDefaultCursor());
                        grabbedBranch = null;
                    }
                    grabbedLeaf = -1;
                    redoPanel();
                }
            }
            if (parasite.getNodes().size() != 1) {
                int paraLeafX = parasite.getLeaves().get(0).getX();
                if (x < paraLeafX + 5 && x > paraLeafX - 5 && y > -30) 
                {
                    this.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)); //parasite leaf axis
                    grabbedLeaf = 1;
                    dragLeaves = paraLeafX;
                    redoPanel();
                }
                else if (!(x < hostLeafX + 5 && x > hostLeafX - 5 && y > -30) && host.findBranch(x, y) == null) {
                    if (parasite.findBranch(x, y) != null) {
                        this.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)); //on parasite branch
                    }
                    else {
                        this.setCursor(Cursor.getDefaultCursor());
                        grabbedBranch = null;
                    }
                    grabbedLeaf = -1;
                    redoPanel();
                }
            }
            else if (!(x < hostLeafX + 5 && x > hostLeafX - 5 && y > -30) && host.findBranch(x, y) == null) {
                this.setCursor(Cursor.getDefaultCursor());
            }
        }

        if (state == TIME_STATE) {
            if (findNode(x, y) != null) {
                DrawTree.Node node = findNode(x, y);
                if (node.getType() != DrawTree.LEAF_TYPE) {
                    this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } 
                else {
                    this.setCursor(Cursor.getDefaultCursor());
                }
            } 
            else if (findBorder(x, y) != -1) {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } 
            else if (findArrow(x, y) != null) {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } 
            else {
                this.setCursor(Cursor.getDefaultCursor());
            }
        }

        if (state == CLEAR_STATE) {
            if (y > 0) {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } 
            else {
                this.setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    /**
     *
     * @param e
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     *
     * @param e
     */
    public void mouseExited(MouseEvent e) {
    }

    /*
     * returns minimum time zone of children of node
     */
    private int minChildTime(DrawTree.Node node) {
        int minTime = 9999;
        for (DrawTree.Node child : node.getChildren()) {
            int childTime = child.getMinTime();
            if (childTime < minTime) {
                minTime = childTime;
            }
        }
        return minTime;
    }

    /*
     * labels node (only leaves can have labels)
     */
    private void labelNode(int x, int y) {
        if (findNode(x, y) != null) {
            DrawTree.Node labelee = findNode(x, y);
            if (labelee.getType() == DrawTree.LEAF_TYPE) {
                boolean validInput = false;
                while (validInput == false) {
                    String labelStr = JOptionPane.showInputDialog("Enter Label: ");
                    try {
                        Integer.parseInt(labelStr);
                        JOptionPane.showConfirmDialog(null, "Integer labels are not permitted", "Integer warning", JOptionPane.DEFAULT_OPTION);
                    } catch (NumberFormatException nfe) {
                        if (labelStr != null) { 
                            labelee.setLabel(labelStr);
                            saved = false;
                        }
                        validInput = true;
                    }
                }
            }
            redoPanel();
        }

    }

    /*
     * Checks for the existence of a node at the coordinates given. If in
     * linking state, then link appropriately.
     */
    private void processLink(int x, int y) {
        /**
         * If we have not started a link, check for node at given position and
         * start a link if one is there. If we have started a link, check for a
         * node at the given position and complete the link if one's there,
         * otherwise stop linking
         */
        DrawTree.Node clicked = findNode(x, y);

        if (!linking) {
            if (clicked != null && clicked.getType() == DrawTree.LEAF_TYPE) {
                linking = true;
                link1 = clicked;
            }
        } 
        else {
            if (clicked != null && clicked.getType() == DrawTree.LEAF_TYPE
                    && getSide(clicked.getX()) != getSide(link1.getX())) {
                for (DrawTree.Node linkedNode : link1.getLink()) {
                    if (linkedNode == clicked) { //trying to draw already present link
                        link1 = null;
                        linking = false;
                        redoPanel();
                        return;
                    }
                }

                saved = false;
                link1.addLink(clicked);
                clicked.addLink(link1);
                boolean contains = false;
                if (link1.getTreeType() == DrawTree.HOST_TYPE) {
                    for (DrawTree.Node linkedNode : links) {
                        if (link1 == linkedNode) {
                            contains = true;
                        }
                    }
                    if (!contains) {
                        links.add(link1);
                    }
                } else {
                    for (DrawTree.Node linkedNode : links) {
                        if (clicked == linkedNode) {
                            contains = true;
                        }
                    }
                    if (!contains) {
                        links.add(clicked);
                    }
                }
            }

            linking = false;
            link1 = null;
            redoPanel();
        }

    }

    /*
     * user sets region of selected nodes
     */
    private void processRegion() {
        ArrayList<DrawTree.Node> selected = new ArrayList<DrawTree.Node>();
        for (DrawTree.Node node : host.getNodes()) {
            int nodeY = node.getY() + panelDisp;
            if (((node.getX()+radius > clickedX && node.getX()-radius < dragX) || (node.getX()-radius < clickedX && node.getX()+radius > dragX))
                    && ((nodeY+radius > clickedY && nodeY-radius < dragY) || (nodeY-radius < clickedY && nodeY+radius > dragY))) {
                selected.add(node);
            }
        }

        if (!selected.isEmpty()) {
            
            int regNum = 0;
            boolean validInput = false;
            while (validInput == false) {
                JPanel regPanel = new JPanel();
                regPanel.setLayout(new BoxLayout(regPanel, BoxLayout.Y_AXIS));
                regPanel.add(new JLabel("Region Number:"));
                JTextField regionNum = new JTextField();
                regPanel.add(regionNum, Component.LEFT_ALIGNMENT);
                JLabel note = new JLabel("Note: either all host nodes must be assigned to nonzero");
                regPanel.add(note);
                JLabel noteln = new JLabel("regions or all must be assigned to region 0 (default setting).");
                regPanel.add(noteln);

                int result = JOptionPane.showConfirmDialog(null, regPanel, "", JOptionPane.OK_CANCEL_OPTION);

                if (result == JOptionPane.OK_OPTION) {
                    if (regionNum.getText().trim().length() > 0) {
                        try {
                            saved = false;
                            String rNum = regionNum.getText();
                            regNum = Integer.parseInt(rNum);

                            if (regNum < 0) {
                                JOptionPane.showConfirmDialog(null, "Enter a nonnegative integer", "Invalid integer warning", JOptionPane.DEFAULT_OPTION);
                            } else {
                                validInput = true;
                            }
                        } catch (NumberFormatException nfe) {
                            JOptionPane.showConfirmDialog(null, "Enter an integer", "Non-integer warning", JOptionPane.DEFAULT_OPTION);
                        }
                    } else {
                        return;
                    }
                } else {
                    return;
                }

                for (DrawTree.Node select : selected) {
                    select.setRegion(regNum);
                }
                if (allZero())
                {
                    if (timeBorders.isEmpty())
                        panelDisp = 30;
                    else
                        panelDisp = 60;  
                }
            }
            if (allZero()) {
                frame.getChangeTimeButton().setVisible(false);
                frame.getChangeTimeButton().setEnabled(false);
            }
            redoPanel();
        }
    }
    
    /*
     * sets position of moved leaves and branches
     */
    private void processMove(int x, int y) {
        int hostLeafX=-5;
        int paraLeafX=-5;
        if (host.getNodes().size() != 1)
            hostLeafX = host.getLeaves().get(0).getX();
        if (parasite.getNodes().size() != 1)
            paraLeafX = parasite.getLeaves().get(0).getX();
        
        if (x < hostLeafX + 5 && x > hostLeafX - 5 && y > -30 && grabbedBranch == null) {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
            grabbedLeaf = 0;
            dragLeaves = hostLeafX;
        }
   
        if (x < paraLeafX + 5 && x > paraLeafX - 5 && y > -30 && grabbedBranch == null) {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
            grabbedLeaf = 1;
            dragLeaves = paraLeafX;
        }

        if (grabbedLeaf == -1) {
            if ((grabbedBranch != null || host.findBranch(x, y) != null) && getSide(grabbedBranch.getX()) == DrawTree.HOST_TYPE) {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                host.redoLeafList(grabbedBranch);
                host.redoNodeList(grabbedBranch);
                Collections.sort(grabbedBranch.getParent().getChildren());
            }
            if ((grabbedBranch != null || parasite.findBranch(x, y) != null) && getSide(grabbedBranch.getX()) == DrawTree.PARASITE_TYPE) {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                parasite.redoLeafList(grabbedBranch);
                parasite.redoNodeList(grabbedBranch);
                Collections.sort(grabbedBranch.getParent().getChildren());
            } else {
                this.setCursor(Cursor.getDefaultCursor()); 
            }
        }
        
        else if (!(x < hostLeafX + 5 && x > hostLeafX - 5 && y > -30) && !(x < paraLeafX + 5 && x > paraLeafX - 5 && y > -30)) {
            this.setCursor(Cursor.getDefaultCursor()); 
            grabbedLeaf = -1;
        }
    }

    /*
     * if the given point is contained by a node, a child will be added to that
     * node, otherwise nothing will be done
     */
    private void addChildNode(int x, int y) {

        DrawTree.Node clicked = findNode(x, y);

        if (clicked != null) {
            saved = false;
            if (!clicked.getLink().isEmpty()) {
                for (DrawTree.Node linkedNode : clicked.getLink()) {
                    linkedNode.getLink().remove(clicked);
                    if (!linkedNode.getLink().isEmpty()) {
                        links.remove(linkedNode);
                    }
                }
                links.remove(clicked);
                clicked.getLink().clear();
            }
            int side = getSide(x);

            DrawTree.Node newkid;

            if (side == DrawTree.HOST_TYPE) {
                host.addChild(clicked);
                newkid = clicked.getLastChild();
                setLeafTime(newkid);
                if (clicked.getChildren().size() == 1) {
                    host.addChild(clicked);
                }
                newkid = clicked.getLastChild();
                setLeafTime(newkid);
            } else {
                parasite.addChild(clicked);
                newkid = clicked.getLastChild();
                setLeafTime(newkid);
                if (clicked.getChildren().size() == 1) {
                    parasite.addChild(clicked);
                }
                newkid = clicked.getLastChild();
                setLeafTime(newkid);
            }
            redoPanel();
        }
    }

    /*
     * sets leaf to be in highest time zone
     */
    private void setLeafTime(DrawTree.Node leaf) {
        saved = false;
        int leafTime = 1;
        if (!timeBorders.isEmpty())
            leafTime = timeBorders.size() / 2 + 1;
        leaf.setMinTime(leafTime);
        leaf.setMaxTime(leafTime);
    }

    private void removeNode(int x, int y) {
        /**
         * Removes the node at the given point (if there is one) and all its
         * children
         */
        DrawTree.Node clicked = findNode(x, y);
        if (clicked != null && clicked != host.getRoot() && clicked != parasite.getRoot()) {
            saved = false;
            removeChildLinks(clicked);

            int side = getSide(x);
            if (side == DrawTree.HOST_TYPE)
                host.removeChild(clicked);
            else
                parasite.removeChild(clicked);

            if (allZero()) {
                if (timeBorders.isEmpty())
                    panelDisp = 30;
                else
                    panelDisp = 60;
            }

            redoPanel();
        }
    }

    private void removeChildLinks(DrawTree.Node node) {
        /*
         * removes all links in node's subtree
         */
        if (node.getParent().getChildren().size() == 2) {
            DrawTree.Node otherChild;
            if (node.getParent().getChildren().get(0) == node)
                otherChild = node.getParent().getChildren().get(1);
            else
                otherChild = node.getParent().getChildren().get(0);
            if (otherChild.getType() == DrawTree.LEAF_TYPE && otherChild.getLink() != null) {
                links.remove(otherChild);
                for (DrawTree.Node linkedNode : otherChild.getLink()) {
                    linkedNode.getLink().remove(otherChild);
                    if (linkedNode.getLink().isEmpty())
                        links.remove(linkedNode);
                }
                otherChild.getLink().clear();
            }
        }

        if (node.getChildren().isEmpty()) {
            if (node.getLink() != null) {
                links.remove(node);
                for (DrawTree.Node linkedNode : node.getLink()) {
                    linkedNode.getLink().remove(node);
                    if (linkedNode.getLink().isEmpty())
                        links.remove(linkedNode);
                }
                node.getLink().clear();
            }
        } else {
            for (DrawTree.Node child : node.getChildren()) {
                removeChildLinks(child);
            }
        }
    }

    private void removeLink(int x, int y) {
        /*
         * removes link at given point (if there is one)
         */
        if (findLink(x, y) != null) {
            ArrayList<DrawTree.Node> clickedLink = findLink(x, y);
            if (!clickedLink.isEmpty()) {
                saved = false;
                DrawTree.Node leaf = clickedLink.get(0);
                DrawTree.Node leafLink = clickedLink.get(1);
                leaf.getLink().remove(leafLink);
                leafLink.getLink().remove(leaf);
                if (leaf.getLink().isEmpty()) {
                    links.remove(leaf);
                }
                if (leafLink.getLink().isEmpty()) {
                    links.remove(leafLink);
                }
            }
            redoPanel();
        }
    }
    
    public void timeRegButtonClicked() {
        /*
         * in time state, button allows user to change number of time zones. 
         * in region state, button allows user to specify region switching costs
         */
        if (state == TIME_STATE) {
            changeNumberTimeZones();
        } else if (state == REGION_STATE) {
            editCosts();
        }
    }

    private void editCosts() {
        /*
         * user inputs region costs
         */
        boolean validInput = false;
        while (validInput == false) {
            JPanel costPanel = new JPanel();
            costPanel.setLayout(new BoxLayout(costPanel, BoxLayout.Y_AXIS));
            costPanel.add(new JLabel("From region: "));
            JTextField startReg = new JTextField();
            costPanel.add(startReg);
            costPanel.add(new JLabel("to region: "));
            JTextField endReg = new JTextField();
            costPanel.add(endReg);
            costPanel.add(new JLabel("Cost: "));
            JTextField cost = new JTextField();
            costPanel.add(cost);
            costPanel.add(new JLabel("Note: All costs are added to Jane's host switch cost except"));
            costPanel.add(new JLabel("a cost of i means host switch not allowed."));

            int result = JOptionPane.showOptionDialog(null, costPanel, "", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[]{"Finish", "Add Another", "Cancel"}, "");

            int startInt;
            int endInt;
            double startNum;
            double endNum;
            double costNum;
            String costStr = "1";
            if (result == 0 || result == 1) {
                try {
                    String startStr = startReg.getText();
                    String endStr = endReg.getText();
                    
                    
                    if (!startStr.equals("") || !endStr.equals("") && !costStr.equals("")) {
                        
                        startInt = Integer.parseInt(startStr);
                        if (hasRegion(startInt)) {
                            startNum = (double) startInt;
                        } else {
                            startNum = Double.valueOf("s").doubleValue();
                        }

                        endInt = Integer.parseInt(endStr);
                        if (hasRegion(endInt)) {
                            endNum = (double) endInt;
                        } else {
                            endNum = Double.valueOf("s").doubleValue();
                        }
                                                
                        costStr = cost.getText();
                        if (!costStr.equals("0")) {
                            if (costStr.equals("i")) {
                                costNum = Double.POSITIVE_INFINITY;
                            } else {
                                costNum = Double.valueOf(costStr).doubleValue();
                            }

                            ArrayList<Double> switchCost = new ArrayList<Double>();
                            switchCost.add(startNum);
                            switchCost.add(endNum);
                            switchCost.add(costNum);

                            for (int i = 0; i < regionCosts.size(); i++) {
                                if (regionCosts.get(i).get(0) == startNum && regionCosts.get(i).get(1) == endNum)
                                    regionCosts.remove(i);
                            }
                            saved = false;
                            regionCosts.add(switchCost);
                        } else {
                            for (int i = 0; i < regionCosts.size(); i++) {
                                if (regionCosts.get(i).get(0) == startNum && regionCosts.get(i).get(1) == endNum)
                                    regionCosts.remove(i);
                            }
                        }
                    }

                    if (result == 0)
                        validInput = true;
                } catch (NumberFormatException nfe) {
                    JOptionPane.showConfirmDialog(null, "Enter region numbers in first two fields and an integer or 'i' in last field", "Invalid input warning", JOptionPane.DEFAULT_OPTION);
                }
              
                redoPanel();
            }
            else {
                return;
            }
        }
    }

    private ArrayList<DrawTree.Node> findLink(int x, int y) {
        /*
         * returns ArrayList of Tree.Nodes connected by clicked link
         */
        for (DrawTree.Node leaf : links) {

            float x1 = leaf.getX();
            float y1 = leaf.getY();
            for (DrawTree.Node leafLink : leaf.getLink()) {
                float x2 = leafLink.getX();
                float y2 = leafLink.getY();

                if ((x > getWidth() / 2 - 20 && x < getWidth() / 2 + 20)) {
                    float slope = (y2 - y1) / (x2 - x1);
                    float linky = y1 + slope * (x - x1);
                    if (y > linky - 15 && y < linky + 15) {
                        ArrayList<DrawTree.Node> clickedLink = new ArrayList<DrawTree.Node>();
                        clickedLink.add(leaf);
                        clickedLink.add(leafLink);
                        return clickedLink;
                    }
                }
            }
        }
        return null;
    }

    private int getSide(int x) {
        /**
         * Given an x coordinate, returns whether it is on the host or parasite
         * side
         */
        if (x == getWidth() / 2) {
            return -1;
        }
        if (x < getWidth() / 2) {
            return DrawTree.HOST_TYPE;
        } else {
            return DrawTree.PARASITE_TYPE;
        }
    }

    private DrawTree.Node findNode(int x, int y) {
        /**
         * Returns the node whose bounds contain the given point
         */
        DrawTree.Node clicked = null;
        DrawTree tree;
        int side = getSide(x);

        if (side == DrawTree.HOST_TYPE) {
            tree = host;
        } else if (side == DrawTree.PARASITE_TYPE) {
            tree = parasite;
        } else {
            return null;
        }

        if (tree != null) {
            for (int i = 0; i < tree.getNodes().size(); i++) {
                clicked = tree.getNodes().get(i);
                if (x < (clicked.getX() + 2.0 * radius) && x > (clicked.getX() - 2.0 * radius)
                        && y < (clicked.getY() + 2.0 * radius) && y > (clicked.getY() - 2.0 * radius)) {
                    return clicked;
                }
            }
        }

        return null;
    }

    private int findBorder(int x, int y) {
        /**
         * returns x-coordinate of time zone border whose bounds contain given x
         */
        for (float timeBorder : timeBorders) {
            if (x < timeBorder + 15 && x > timeBorder - 15 && y > -30) {
                return (int) Math.round(timeBorder);
            }
        }
        return -1;
    }

    private DrawTree.Node findArrow(int x, int y) {
        /**
         * returns node whose arrow contains given point
         */
        //host tree arrows
        for (DrawTree.Node node : host.getNodes()) {
            //node in only one time zone
            if ((node.getMinTime() == node.getMaxTime())
                    && ((x >= (node.getX() + 10) && x < (node.getX() + 20))
                    || (x <= (node.getX() - 10) && x > (node.getX() - 20)))
                    && (y < (node.getY() + 10) && y > (node.getY() - 10))) {
                return node;
            } //range of time zones
            else if (node.getMinTime() != node.getMaxTime()) {
                int minY = node.getY() - 10;
                int maxY = node.getY() + 10;

                //left arrow extends to root AND right arrow extends to leaves
                if (node.getMinTime() == 1 && node.getMaxTime() == timeBorders.size() / 2 + 1) {
                    if (((x > 10 && x < 25)
                            || (x < getWidth() / 2 - 15 && x > getWidth() / 2 - 30))
                            && (y < maxY && y > minY)) {
                        return node;
                    }
                } else {
                    //left arrow extends to root
                    if (node.getMinTime() == 1
                            && ((x > 10 && x < 25)
                            || (x <= timeBorders.get(node.getMaxTime() - 1) - 15 && x > timeBorders.get(node.getMaxTime() - 1) - 30))
                            && (y < maxY && y > minY)) {
                        return node;
                    }

                    //right arrow extends to leaves
                    if (node.getMaxTime() == timeBorders.size() / 2 + 1
                            && ((x < getWidth() / 2 - 15 && x > getWidth() / 2 - 30)
                            || (x >= timeBorders.get(node.getMinTime() - 2) + 15 && x < timeBorders.get(node.getMinTime() - 2) + 30))
                            && (y < maxY && y > minY)) {
                        return node;
                    }

                    //arrows span only internal time zones
                    if (node.getMinTime() != 1 && node.getMaxTime() != timeBorders.size() / 2 + 1) {
                        if (((x <= timeBorders.get(node.getMaxTime() - 1) - 15 && x > timeBorders.get(node.getMaxTime() - 1) - 30)
                                || (x >= timeBorders.get(node.getMinTime() - 2) + 15 && x < timeBorders.get(node.getMinTime() - 2) + 30))
                                && (y < maxY && y > minY)) {
                            return node;
                        }
                    }
                }
            }
        }

        //parasite tree arrows
        for (DrawTree.Node node : parasite.getNodes()) {
            //node in only one time zone
            if ((node.getMinTime() == node.getMaxTime())
                    && ((x >= (node.getX() + 10) && x < (node.getX() + 20))
                    || (x <= (node.getX() - 10) && x > (node.getX() - 20)))
                    && (y < (node.getY() + 10) && y > (node.getY() - 10))) {
                return node;
            } else if (node.getMinTime() != node.getMaxTime()) { //range of time zones
                int minY = node.getY() - 10;
                int maxY = node.getY() + 10;
                //right arrow extends to root AND left arrow extends to leaves
                if (node.getMinTime() == 1 && node.getMaxTime() == timeBorders.size() / 2 + 1) {
                    if (((x < getWidth() - 10 && x > getWidth() - 25)
                            || (x > getWidth() / 2 + 15 && x < getWidth() / 2 + 30))
                            && (y < maxY && y > minY)) {
                        return node;
                    }
                } else {
                    //right arrow extends to root
                    if (node.getMinTime() == 1
                            && ((x < getWidth() - 10 && x > getWidth() - 25)
                            || (x >= timeBorders.get(timeBorders.size() - node.getMaxTime()) + 15 && x < timeBorders.get(timeBorders.size() - node.getMaxTime()) + 30))
                            && (y < maxY && y > minY)) {
                        return node;
                    }

                    //left arrow extends to leaves
                    if (node.getMaxTime() == timeBorders.size() / 2 + 1
                            && ((x > getWidth() / 2 + 15 && x < getWidth() / 2 + 30)
                            || (x <= timeBorders.get(timeBorders.size() - node.getMinTime() + 1) - 15 && x > timeBorders.get(timeBorders.size() - node.getMinTime() + 1) - 30))
                            && (y < maxY && y > minY)) {
                        return node;
                    }

                    //arrows span only internal time zones
                    if (node.getMinTime() != 1 && node.getMaxTime() != timeBorders.size() / 2 + 1) {
                        if (((x >= timeBorders.get(timeBorders.size() - node.getMaxTime()) + 15 && x < timeBorders.get(timeBorders.size() - node.getMaxTime()) + 30)
                                || (x <= timeBorders.get(timeBorders.size() - node.getMinTime() + 1) - 15 && x > timeBorders.get(timeBorders.size() - node.getMinTime() + 1) - 30))
                                && (y < maxY && y > minY)) {
                            return node;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     *
     * @param g
     */
    @Override
    protected void paintComponent(Graphics g) {
        /**
         * Overrides JComponent's paintComponent function to paint trees
         */
        super.paintComponent(g);
                
        if (host == null)
            host = new DrawTree(0, 0, getWidth() / 2 - 5, getHeight(), DrawTree.HOST_TYPE);

        if (parasite == null)
            parasite = new DrawTree(0, 0, getWidth() / 2 - 5, getHeight(), DrawTree.PARASITE_TYPE);
        
        //draws background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.BLACK);
        g.drawLine(0, 30, getWidth(), 30);
        
        //Note: If the Design window of BeeTree.java shows a null pointer exception, it will still run. 
        // If you need to edit it, comment out these next 6 lines of code. The exception is thrown 
        // because the save button is referenced before it is created.
        if (saved)
            frame.getSaveButton().setEnabled(false);
        else
            frame.getSaveButton().setEnabled(true);

        //sets Y coordinate of where trees start
        if (timeBorders.isEmpty())
            panelDisp = 30;
        else
            panelDisp = 60;

        //writes state description
        if (state == NODE_STATE) {
            g.drawString("Add Child Mode - click any node", 20, 20);
        }
        if (state == LABEL_STATE) {
            g.drawString("Label Mode - click any tip", 20, 20);
        }
        if (state == LINK_STATE) {
            g.drawString("Link Mode - drag between two tips or click two tips in succession to form link", 20, 20);
        }
        if (state == TIME_STATE) {
            g.drawString("Time Zone Mode - drag any time zone border or non-tip node to set time zone or drag arrows beside nodes to set range of time zones", 20, 20);
        }
        if (state == REGION_STATE) {
            g.drawString("Region Mode - select host tree nodes to specify region", 20, 20); 
        }
        if (state == ERASE_STATE) {
            g.drawString("Erase Mode - click any node or link ", 20, 20);
        }
        if (state == CLEAR_STATE) {
            g.drawString("Clear Mode - click either tree", 20, 20);
        }
        if (state == MOVE_STATE) {
            g.drawString("Move Mode - drag tip axis to move tips horizontally or drag branch to move nodes vertically", 20, 20);
        }

        //draws region keys
        if (maxRegion() != 0) {
            drawRegKey(g);
            drawRegCostKey(g);
            int lineX=panelDisp;
            if (!timeBorders.isEmpty())
                lineX = panelDisp-30;
            g.drawLine(0, lineX, getWidth(), lineX);
        }
        int lineX=panelDisp;
        if (!timeBorders.isEmpty())
            lineX = panelDisp-30;
        g.drawLine(getWidth() / 2, lineX, getWidth() / 2, getHeight());

        // sets coordinates of nodes and time zone borders before drawing
        if (!dragging) {
            scaleHostX();
            scaleHostY();
            scaleParasiteX();
            scaleParasiteY();
            scaleTimeBorders();
        }

        //draws leaf axis
        if (grabbedLeaf > -1) {
            Graphics2D g2d = (Graphics2D) g;
            float dash[] = {20.0f, 20.0f};
            BasicStroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, dash, 25.0f);
            g2d.setStroke(dashed);
            g.drawLine(dragLeaves, getHeight(), dragLeaves, panelDisp);
            g2d.setStroke(new BasicStroke());
        }
        //draws linking between trees
        drawLinks(g);

        //draws host tree
        paintHelper(g, host.getRoot(), 0);

        //draws parasite tree 
        paintHelper(g, parasite.getRoot(), getWidth());

        g.setColor(Color.RED);
        // draws time zone arrows
        drawArrows(g);
        //draws time zone borders
        drawZones(g);

        //draws selection box for selecting nodes in region mode
        g.setColor(Color.gray);
        drawBox(g);
    }

    /*
     * Sets x-coordinates of host tree nodes(supports window resize)
     */
    private void scaleHostX() {
        if (prevWidth == 0) {
            prevWidth = getWidth();
        }
        if (host != null) {
            //sets leaf position
            if (!host.getLeaves().isEmpty()) {
                int leafDist = prevWidth/2-host.getLeaves().get(0).getX();
                for (DrawTree.Node leaf : host.getLeaves()) {
                    if (leaf.getX() == host.getRoot().getX()) {
                        leaf.setX(getWidth()/2 - 20);
                    }
                    else {
                        leaf.setX(getWidth()/2 - leafDist);
                    }
                }
            }
            
            //corrects if node's max time < parent's max time or node's min time > parent's min time
            for (DrawTree.Node node : host.getNodes()) {
                if (node.getParent() != null) {
                    DrawTree.Node parent = node.getParent();
                    if (node.getMaxTime() < parent.getMaxTime()) {
                        int newMaxTime = parent.getMaxTime();
                        node.setMaxTime(newMaxTime);
                        node.setMinTime(newMaxTime);
                    }
                    if (node.getMinTime() < parent.getMinTime()) {
                        int newMinTime = node.getMinTime();
                        parent.setMinTime(newMinTime);
                        parent.setMaxTime(newMinTime);
                    }
                }
            }         
            
            //sets position of non-leaf nodes
            for (int depth=0; depth<host.getMaxdepth()+1; depth++) {
                ArrayList<DrawTree.Node> nodesAtDepth = getNodesAtDepth(host, depth);
                for (DrawTree.Node node : nodesAtDepth) {
                    if (node.getType() != DrawTree.LEAF_TYPE) {
                        int depthSubtree = maxDepthSubtree(node);
                        int avTime = (node.getMinTime() + node.getMaxTime()) / 2; //node's average time zone (rounded)
                        int highBound; //x-coord of time zone border, child, or panel divide that bounds node on right

                        if (timeBorders.isEmpty() || avTime == timeBorders.size() / 2 + 1) {
                            if (host.getLeaves().isEmpty()) {
                                highBound = getWidth()/2-20;
                            } else {
                                highBound = host.getLeaves().get(0).getX();
                            }
                        } else {
                            highBound = (int) Math.round(timeBorders.get(avTime - 1));
                        }

                        int lowBound=0; //x-coord of time zone border, parent, or panel edge that bounds node on left
                        int childDist=0; //distance from lowBound to node
                        int avParTime=0;
                        
                        if (node != host.getRoot()) {
                            avParTime = (node.getParent().getMinTime() + node.getParent().getMaxTime()) / 2; //parent's average time zone (rounded)

                            if (avTime == avParTime) {
                                lowBound = node.getParent().getX();
                                //bound by parent and midline
                                if (timeBorders.isEmpty() || avTime == timeBorders.size() / 2 + 1) {
                                    childDist = ((highBound - lowBound) / (depthSubtree + 1));
                                } else { //bound by parent and time zone line
                                    childDist = 2 * (highBound - lowBound) / (2 * depthSubtree + 3);
                                }
                            }
                        }
                        
                        if (node == host.getRoot() || avTime != avParTime) {
                            if (avTime == 1) {
                                lowBound = 15;
                            } else {
                                lowBound = (int) Math.round(timeBorders.get(avTime - 2));
                            }
                            
                            //bound by time zone line (or root) and midline
                            if (avTime == timeBorders.size() / 2 + 1) {
                                childDist = (highBound - lowBound) / 3;
                            } else { //bound by two time zone lines
                                childDist = (highBound - lowBound) / (2 * depthSubtree + 2);
                            }
                        }
                        node.setX(lowBound + childDist);

                    }
                    if (node == host.getRoot()) {
                       if (node.getMinTime() == 1 && node.getMaxTime() == 1) {
                           if (state == TIME_STATE && !timeBorders.isEmpty()) {
                               node.setX(20);
                           } else {
                               node.setX(10);
                           }
                        }
                    }
                }
            }
        }
    }

    /*
     * sets y-coordinates of host nodes (supports window resize)
     */
    private void scaleHostY() {
        if (host != null) {
            host.getRoot().setY((getHeight()-panelDisp)/2);
            if (host.getLeaves().size() != 0) {
                int leafSep = (getHeight() - panelDisp) / host.getLeaves().size();
                for (int i = 0; i < host.getLeaves().size(); i++) {
                    host.getLeaves().get(i).setY(leafSep / 2 + leafSep * i);
                }
                int maxDepth = host.getMaxdepth();
                for (int i = maxDepth - 1; i > -1; i--) {
                    for (DrawTree.Node node : host.getNodes()) {
                        if (node.getDepth() == i) {
                            int maxChild=0;

                            //if node's arrows could intersect another node
                            if (node.getChildren().size() != 0) {
                                maxChild = node.getChildren().size() - 1;   
                                node.setY((node.getChildren().get(0).getY() + node.getChildren().get(maxChild).getY()) / 2);
                            }
                            if (node.getChildren().size() % 2 == 1 && node.getChildren().size() != 1
                                    && (node.getMinTime() != node.getMaxTime() || sameYChild(host, node))) {
                                maxChild = node.getChildren().size() - 2;
                                node.setY((node.getChildren().get(0).getY() + node.getChildren().get(maxChild).getY()) / 2);
                            }   
                        }
                    }
                }
            }
        }
    }

    /*
     * sets x-coordinates of parasite nodes (supports window resize)
     */
    private void scaleParasiteX() {
        if (parasite != null) {
            //sets position of leaves
            if (!parasite.getLeaves().isEmpty()) {
                int leafDist = parasite.getLeaves().get(0).getX()-prevWidth/2;
                for (DrawTree.Node leaf : parasite.getLeaves()) {
                    if (leaf.getX() == parasite.getRoot().getX()) {
                        leaf.setX(getWidth()/2 + 20);
                    }
                    else {
                        leaf.setX(getWidth()/2 + leafDist);
                    }
                }
            }
            
            //corrects if node's max time < parent's max time or node's min time > parent's min time
            for (DrawTree.Node node : parasite.getNodes()) {
                if (node.getParent() != null) {
                    DrawTree.Node parent = node.getParent();
                    if (node.getMaxTime() < parent.getMaxTime()) {
                        int newMaxTime = parent.getMaxTime();
                        node.setMaxTime(newMaxTime);
                        node.setMinTime(newMaxTime);
                    }
                    if (node.getMinTime() < parent.getMinTime()) {
                        int newMinTime = node.getMinTime();
                        parent.setMinTime(newMinTime);
                        parent.setMaxTime(newMinTime);
                    }
                }
            } 
            
            //sets position of non-leaf nodes
            for (int depth=0; depth<parasite.getMaxdepth()+1; depth++)
            {
                ArrayList<DrawTree.Node> nodesAtDepth = getNodesAtDepth(parasite, depth);
                for (DrawTree.Node node : nodesAtDepth)
                {

                    if (node.getType() != DrawTree.LEAF_TYPE)
                    {
                        int depthSubtree = maxDepthSubtree(node);
                        int avTime = (node.getMinTime() + node.getMaxTime()) / 2; //node's average time zone (rounded)
                        int highBound; //x-coord of time zone border, child, or panel divide that bounds node on right

                        if (timeBorders.isEmpty() || avTime == timeBorders.size() / 2 + 1) {
                            if (parasite.getLeaves().isEmpty()) {
                                highBound = getWidth()/2+20;
                            } else {
                                highBound = parasite.getLeaves().get(0).getX();
                            }
                        } else {
                            highBound = (int) Math.round(timeBorders.get(timeBorders.size() - avTime));
                        }

                        int lowBound=0; //x-coord of time zone border, parent, or panel edge that bounds node on left
                        int childDist=0; //distance from lowBound to node
                        int avParTime=0;
                        
                        if (node != parasite.getRoot()) {
                            avParTime = (node.getParent().getMinTime() + node.getParent().getMaxTime()) / 2; //parent's average time zone (rounded)
                            if (avTime == avParTime) {
                                lowBound = node.getParent().getX();
                                //bound by parent and midline
                                if (timeBorders.isEmpty() || avTime == timeBorders.size() / 2 + 1) {
                                    childDist = ((lowBound - highBound) / (depthSubtree + 1));
                                } 
                                //bound by parent and time zone line
                                else {
                                    childDist = 2 * (lowBound - highBound) / (2 * depthSubtree + 3);
                                }
                            }
                        }
                        
                        if (node == parasite.getRoot() || avTime != avParTime) {
                            if (avTime == 1) {
                                lowBound = getWidth()-15;
                            } 
                            else {
                                lowBound = (int) Math.round(timeBorders.get(timeBorders.size() - avTime + 1));
                            }
                            //bound by time zone line (or root) and midline
                            if (avTime == timeBorders.size() / 2 + 1) {
                                childDist = (lowBound - highBound) / 3;
                            } 
                            //bound by two time zone lines
                            else {
                                childDist = (lowBound - highBound) / (2 * depthSubtree + 2);
                            }
                        }
                        node.setX(lowBound - childDist);

                    }
                    
                    if (node == parasite.getRoot()) {
                        if (node.getMinTime() == 1 && node.getMaxTime() == 1) {
                            if (state == TIME_STATE && !timeBorders.isEmpty()) {
                                node.setX(getWidth()-20);
                            }
                            else {
                                node.setX(getWidth()-10);
                            }
                        }
                    }
                }
            }
        }
    }

    /*
     * sets y-coordinates of parasite nodes (supports window resize
     */
    private void scaleParasiteY() {
        if (parasite != null) {
            parasite.getRoot().setY((getHeight()-panelDisp)/2);
            if (parasite.getLeaves().size() != 0) {
                int leafSep = (getHeight() - panelDisp) / parasite.getLeaves().size();
                for (int i = 0; i < parasite.getLeaves().size(); i++) {
                    parasite.getLeaves().get(i).setY(leafSep / 2 + leafSep * i);
                }
                int maxDepth = parasite.getMaxdepth();
                for (int i = maxDepth - 1; i > -1; i--) {
                    for (DrawTree.Node node : parasite.getNodes()) {
                        if (node.getDepth() == i) {
                            int maxChild;

                            //if node's arrows could intersect another node
                            if (node.getChildren().size() != 0) {
                                maxChild = node.getChildren().size() - 1;   
                                node.setY((node.getChildren().get(0).getY() + node.getChildren().get(maxChild).getY()) / 2);
                            }
                            if (node.getChildren().size() % 2 == 1 && node.getChildren().size() != 1
                                    && (node.getMinTime() != node.getMaxTime() || sameYChild(parasite, node))) {
                                maxChild = node.getChildren().size() - 2;
                                node.setY((node.getChildren().get(0).getY() + node.getChildren().get(maxChild).getY()) / 2);
                            }   
                        }
                    }
                }
            }
        }
    }

    /*
     * sets x-coordinates of time zone borders (supports window resize)
     */
    private void scaleTimeBorders() {
        if (!timeBorders.isEmpty()) {
            float scale = (float) getWidth() / (float) prevWidth;
            float hostScale = scale;
            float paraScale = scale;
            if (!host.getLeaves().isEmpty()) {
                int hostLeafX = host.getLeaves().get(0).getX();
                hostScale = (float)hostLeafX/(float)(prevWidth/2-(getWidth()/2-hostLeafX));
            }
            if (!parasite.getLeaves().isEmpty()) {
                float paraLeafX = (float)parasite.getLeaves().get(0).getX();
                float paraLeafDist = getWidth()-paraLeafX;
                float prevParaLeafDist = (float)prevWidth/2-(paraLeafX-(float)getWidth()/2);
                paraScale = (float)paraLeafDist/prevParaLeafDist;
            }
            ArrayList<Float> newTimeBorders = new ArrayList<Float>();

            for (float timeBorder : timeBorders) {
                float newTimeBorder;
                if (timeBorder<getWidth()/2) {
                    newTimeBorder = (timeBorder * hostScale);
                }
                else {
                    newTimeBorder = getWidth()-(prevWidth-timeBorder)*(paraScale);
                    float oldTimeDist = prevWidth-timeBorder;
                    float timeDist = getWidth()-newTimeBorder;
                }
                newTimeBorders.add(newTimeBorder);
            }
            Collections.sort(timeBorders);
            timeBorders = newTimeBorders;
        }
        prevWidth = getWidth();
    }

    /*
     * Returns maximum depth of subtree with given root considering ONLY nodes
     * in same time zone
     */
    private int maxDepthSubtree(DrawTree.Node root) {
        if (root.getChildren().isEmpty()) {
            return 0;
        } else {
            int maxDepth = 0;
            boolean timeChildren = false; // true if root has child in same time zone
            for (DrawTree.Node c : root.getChildren()) {
                if (root.getMinTime() == c.getMinTime() && root.getMaxTime() == c.getMaxTime()) {
                    timeChildren = true;
                    int maxDepthC = maxDepthSubtree(c);
                    if (maxDepthC > maxDepth) {
                        maxDepth = maxDepthC;
                    }
                }
            }
            if (timeChildren) {
                return maxDepth + 1;
            } else {
                return 0;
            }
        }
    }
    
    /*
     * returns arraylist of nodes at specified depth
     */
    private ArrayList<DrawTree.Node> getNodesAtDepth(DrawTree tree, int depth)
    {
        ArrayList<DrawTree.Node> nodesAtDepth = new ArrayList<DrawTree.Node>();
        for (DrawTree.Node current : tree.getNodes())
        {
            if (current.getDepth() == depth) {
                nodesAtDepth.add(current);
            }
        }
        return nodesAtDepth;
    }

    /*
     * returns true if node has a descendant with the same y coordinate and the child
     * covers a range of time zones (so that arrows do not intersect node
     */
    private boolean sameYChild(DrawTree tree, DrawTree.Node node) {
        for (DrawTree.Node current : tree.getNodes()) {
            if (current.getDepth() > node.getDepth() && current.getY() == node.getY() 
                    && (current.getMinTime() != current.getMaxTime() || (current.getType() == DrawTree.LEAF_TYPE && node.getMinTime() != node.getMaxTime()))) {
                return true;
            }
        }
        return false;
    }

    /*
     * returns maximum power of two less than or equal to x
     */
    private int roundPowerTwo(int x) {
        if (x == 1) {
            return 1;
        } else {
            return roundPowerTwo(x / 2) * 2;
        }
    }

    /*
     * true if all host nodes in region 0
     */
    private boolean allZero() {
        for (DrawTree.Node node : host.getNodes()) {
            if (node.getRegion() != 0) {
                return false;
            }
        }
        return true;
    }
    
    public boolean getAllZero() {
        return allZero();
    }
    
    /*
     * true if all host nodes assigned to nonzero region
     */
    private boolean allAssigned() {
        for (DrawTree.Node node : host.getNodes()) {
            if (node.getRegion() == 0)
                return false;
        }
        return true;
    }
    
    public boolean getAllAssigned() {
        return allAssigned();
    }

    /*
     * true if there is at least one node in the given region
     */
    private boolean hasRegion(int reg) {
        for (DrawTree.Node node : host.getNodes()) {
            if (node.getRegion() == reg && reg != 0)
                return true;
        }
        return false;
    }

    /*
     * returns maximum region number;
     */
    private int maxRegion() {
        int max = 0;
        for (DrawTree.Node node : host.getNodes()) {
            if (node.getRegion() > max)
                max = node.getRegion();
        }
        return max;
    }
    
    /*
     * returns user input number of time zones
     */
    private int setNumTimeZones() {
        int numTimeZones = 0;
        boolean validInput = false;
        while (validInput == false) {
            JPanel timePanel = new JPanel();
            timePanel.setLayout(new BoxLayout(timePanel, BoxLayout.Y_AXIS));
            timePanel.add(new JLabel("Number of Time Zones:"));
            JTextField timeNum = new JTextField();
            timePanel.add(timeNum, Component.LEFT_ALIGNMENT);
            JLabel note = new JLabel("Note: the default value is 1");
            timePanel.add(note);
            JLabel noteln = new JLabel("\t");
            timePanel.add(noteln);
            int result = JOptionPane.showConfirmDialog(null, timePanel, "", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                saved = false;
                if (timeNum.getText() != "") {
                    try {
                        numTimeZones = Integer.parseInt(timeNum.getText());

                        if (numTimeZones <= 0) {
                            JOptionPane.showConfirmDialog(null, "Enter a positive integer", "Invalid integer warning", JOptionPane.DEFAULT_OPTION);
                        } 
                        else {
                            validInput = true;
                            saved = false;
                        }
                    } catch (NumberFormatException nfe) {
                        JOptionPane.showConfirmDialog(null, "Enter an integer", "Non-integer warning", JOptionPane.DEFAULT_OPTION);
                    }
                }
            } else {
                break;
            }
        }
        if (numTimeZones > 1) {
            timeBorders.clear();
            int numTimeBorders = 2 * (numTimeZones - 1);
            float timeBorderX;
            //places time zone lines so that leaves are always in last time zone
            int timeWidthHost = getWidth()/2;
            if (host.getNodes().size() > 1) {
                if (timeWidthHost*(numTimeZones-1)/numTimeZones > host.getLeaves().get(0).getX()-15) {
                    timeWidthHost = ((host.getLeaves().get(0).getX()-15)*(numTimeZones)/(numTimeZones-1));
                }
            }
            int timeWidthPara = getWidth()/2;
            if (parasite.getNodes().size() > 1) {
                if (timeWidthPara/numTimeZones+getWidth()/2 < parasite.getLeaves().get(0).getX()+15) {
                    timeWidthPara = (getWidth()-parasite.getLeaves().get(0).getX()-15)*numTimeZones/(numTimeZones-1);
                }
            }
            for (int i = 1; i < numTimeZones; i++) {
                timeBorderX = i * (timeWidthHost / numTimeZones);
                timeBorders.add(timeBorderX);
                timeBorderX = getWidth() - (i * (timeWidthPara / numTimeZones));
                timeBorders.add(timeBorderX);
            }
            Collections.sort(timeBorders);
        }
        return numTimeZones;
    }

    /*
     * allows user to input number of time zones first time time zone button is
     * clicked and intial locations of time zone borders
     */
    public void timeZoneInit() {
        if (timeBorders.isEmpty()) {
            int numTimeZones = setNumTimeZones();
            
            //sets time zones of nodes
            if (numTimeZones > 1) {
                for (DrawTree.Node node : host.getNodes()) {
                    for (int i = 0; i < timeBorders.size(); i++) {
                        if (i == timeBorders.size() - 1) {
                            if (node.getX() >= timeBorders.get(i)) {
                                node.setMinTime(i + 2);
                                node.setMaxTime(i + 2);
                                break;
                            }
                        }
                        if (node.getX() >= timeBorders.get(i) && node.getX() < timeBorders.get(i + 1)) {
                            node.setMinTime(i + 2);
                            node.setMaxTime(i + 2);
                            break;
                        }
                    }
                }
                for (DrawTree.Node node : parasite.getNodes()) {
                    if (node.getX() < timeBorders.get(0) + getWidth() / 2) {
                        node.setMinTime(numTimeZones);
                        node.setMaxTime(numTimeZones);
                    }
                    for (int i = 0; i < numTimeZones - 2; i++) {
                        if (node.getX() >= timeBorders.get(i) + getWidth() / 2 && node.getX() < timeBorders.get(i + 1) + getWidth() / 2) {
                            node.setMinTime(numTimeZones - 1 - i);
                            node.setMaxTime(numTimeZones - 1 - i);
                            break;
                        }
                    }
                }
                redoPanel();
            }
        }
    }
    
    /*
     * allows user to change number of time zones
     */
    public void changeNumberTimeZones() {
        int oldNumTimeZones = timeBorders.size()/2+1;
        if (oldNumTimeZones == 1) {
            timeZoneInit();
        }
        else {
            int numTimeZones = setNumTimeZones();
            if (numTimeZones > 1) {
                if (oldNumTimeZones>numTimeZones) {
                    for (int treeType = 0; treeType < 2; treeType++) 
                    {
                        DrawTree tree;
                        if (treeType == 0) {
                            tree = host;
                        }
                        else {
                            tree = parasite;
                        }
                        for (DrawTree.Node node : tree.getNodes()) {
                            if (node.getMinTime() > numTimeZones) {
                                node.setMinTime(numTimeZones);
                            }            
                            if (node.getMaxTime() > numTimeZones) {
                                node.setMaxTime(numTimeZones);
                            }     
                        }
                    }
                }
                else if (oldNumTimeZones<numTimeZones) {
                    for (int treeType = 0; treeType < 2; treeType++) {
                        DrawTree tree;
                        if (treeType == 0) {
                            tree = host;
                        }
                        else {
                            tree = parasite;
                        }
                        for (DrawTree.Node leaf : tree.getLeaves()) {
                            leaf.setMinTime(numTimeZones);
                            leaf.setMaxTime(numTimeZones);
                        }
                    }
                }
            }
            else if (numTimeZones == 1) {
                for (DrawTree.Node node : host.getNodes()) {
                    node.setMinTime(1);
                    node.setMaxTime(1);
                }
                for (DrawTree.Node node : parasite.getNodes()) {
                    node.setMinTime(1);
                    node.setMaxTime(1);
                }
                timeBorders.clear();
                panelDisp-=30;
            }
            redoPanel();
        }
    }

    /*
     * draws region key
     */
    private void drawRegKey(Graphics g) {
        
        //deletes elements from regionCosts whose regions do not exist
        ArrayList<ArrayList<Double>> removeL = new ArrayList<ArrayList<Double>>();
        for (int i = 0; i < regionCosts.size(); i++) {
            int startInt = regionCosts.get(i).get(0).intValue();
            int endInt = regionCosts.get(i).get(1).intValue();
            if (!hasRegion(startInt) || !hasRegion(endInt)) {
                removeL.add(regionCosts.get(i));
            }
        }
        for (int i = 0; i < removeL.size(); i++) {
            regionCosts.remove(removeL.get(i));
        }
        
        int add=0;
        if (!regionCosts.isEmpty()) {
            add = 5;
        }
        g.drawString("Region Key:", 20, 50+add);
        int maxRegion = maxRegion();
        int maxRow = maxRegion / 7 + 1;
        if (timeBorders.isEmpty())
            panelDisp = 30 * (maxRow + 1)+add;
        else
            panelDisp = 30 * (maxRow + 2)+add;
        for (int i = 1; i < maxRegion + 1; i++) {
            if (hasRegion(i)) {
                int row = i / 7 + 1;
                setColor(i, g);
                g.fillOval(50 + 70 * (i % 7), 30 * row + 11+add, 2 * radius, 2 * radius);
                g.setColor(Color.BLACK);
                g.drawString(" = " + i, 65 + 70 * (i % 7), 30 * row + 20+add);
            }
        }
    }

    /*
     * draws region cost key
     */
    private void drawRegCostKey(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        if (state == REGION_STATE && regionCosts.isEmpty()) {
            if (!allZero()) {
                frame.getChangeTimeButton().setVisible(true);
                frame.getChangeTimeButton().setEnabled(true);
                frame.getChangeTimeButton().setText("Edit Region Switching Costs");
            }
        } 
        else if (!regionCosts.isEmpty()) {
            g.drawString("Region Switching Costs: ", getWidth() / 2 + 20, 55);
            for (int i = 0; i < regionCosts.size(); i++) {
                int row = (i + 1) / 4 + 1;
                int rowTop = 30 + 40 * (row - 1);

                //writes region numbers
                int startInt = regionCosts.get(i).get(0).intValue();
                int endInt = regionCosts.get(i).get(1).intValue();
                String start = Integer.toString(startInt);
                String end = Integer.toString(endInt);
                int startX = getWidth() / 2 + 80 + 120 * ((i + 1) % 4);
                int endX = getWidth() / 2 + 140 + 120 * ((i + 1) % 4);
                g.drawString(start, startX, rowTop + 21);
                g.drawString(end, endX, rowTop + 21);

                //draw arrow
                g.setColor(Color.BLACK);
                g.drawLine(startX + 9, rowTop + 28, endX - 4, rowTop + 28);
                g.drawLine(endX - 4, rowTop + 28, endX - 9, rowTop + 23);
                g.drawLine(endX - 4, rowTop + 28, endX - 9, rowTop + 33);

                //writes cost 
                Double costD = regionCosts.get(i).get(2);
                if (costD == Double.POSITIVE_INFINITY) {
                    g.setColor(Color.RED);
                    BasicStroke thick = new BasicStroke(3.0f);
                    g2d.setStroke(thick);
                    g.drawLine((startX + endX) / 2 - 2, rowTop + 23, (startX + endX) / 2 + 8, rowTop + 33);
                    g.drawLine((startX + endX) / 2 - 2, rowTop + 33, (startX + endX) / 2 + 8, rowTop + 23);
                    g2d.setStroke(new BasicStroke());
                } else {
                    String costStr = Integer.toString(costD.intValue());
                    java.awt.geom.Rectangle2D rect = g.getFontMetrics().getStringBounds(costStr, g);
                    int textWidth = (int) (rect.getWidth());
                    int costX = (startX + endX) / 2 - textWidth / 2 + 3;
                    g.drawString(costStr, costX, rowTop + 25);
                }

                //draw nodes
                setColor(startInt, g);
                g.fillOval(startX - 1, rowTop + 23, 2 * radius, 2 * radius);
                setColor(Integer.parseInt(end), g);
                g.fillOval(endX - 1, rowTop + 23, 2 * radius, 2 * radius);
                g.setColor(Color.BLACK);

                if (timeBorders.isEmpty()) {
                    if (panelDisp < rowTop + 40) {
                        panelDisp = rowTop + 40;
                    }
                } else {
                    if (panelDisp < rowTop + 70) {
                        panelDisp = rowTop + 70;
                    }
                }

            }
        }
    }

    /*
     * draws time zone arrows
     */
    private void drawArrows(Graphics g) {
        for (DrawTree.Node node : host.getNodes()) {
            if (node.getType() != DrawTree.LEAF_TYPE && node.getMinTime() != node.getMaxTime() && grabbedArrow != node) {
                //dragging time zone border near node from left
                if (dragging && grabbedBorder > 0
                        && grabbedBorder < node.getX() && node.getX() - grabbedBorder < 35
                        && (node.getMinTime() == borderIndex + 2)) {
                    int nodeX = node.getX();
                    int nodeY = node.getY() + panelDisp;
                    g.drawLine(nodeX - 5, nodeY, nodeX - 15, nodeY);
                    g.drawLine(nodeX - 15, nodeY, nodeX - 10, nodeY - 5);
                    g.drawLine(nodeX - 15, nodeY, nodeX - 10, nodeY + 5);

                    int highBorder;
                    if (node.getMaxTime() == timeBorders.size() / 2 + 1) {
                        highBorder = host.getLeaves().get(0).getX();
                    } 
                    
                    else {
                        highBorder = (int) Math.round(timeBorders.get(node.getMaxTime() - 1)) - 15;
                    }

                    g.drawLine(nodeX + 5, nodeY, highBorder, nodeY);
                    g.drawLine(highBorder, nodeY, highBorder - 5, nodeY - 5);
                    g.drawLine(highBorder, nodeY, highBorder - 5, nodeY + 5);
                } 
                
                //dragged time zone border past node from left
                else if (dragging && grabbedBorder > 0 && grabbedBorder >= node.getX() && node.getMinTime() == borderIndex + 2) {
                    node.setMinTime(node.getMinTime() - 1);
                } 
                
                //non-dragged arrows covering range of time zones
                else {
                    int maxBorder;
                    int minBorder;

                    if (node.getMaxTime() == timeBorders.size() / 2 + 1) {
                        if (host.getLeaves().isEmpty()) {
                            maxBorder = getWidth()/2-15;
                        }
                        else {
                            maxBorder = host.getLeaves().get(0).getX();
                        }
                    } 
                    else {
                        maxBorder = (int) Math.round(timeBorders.get(node.getMaxTime() - 1)) - 15;
                    }

                    if (node.getMinTime() == 1) {
                        minBorder = 20;
                    } 
                    else {
                        minBorder = (int) Math.round(timeBorders.get(node.getMinTime() - 2)) + 15;
                    }
                    int arrowX = node.getX();
                    int nodeY = node.getY() + panelDisp;
                    g.drawLine(arrowX + 5, nodeY, maxBorder, nodeY);
                    g.drawLine(maxBorder, nodeY, maxBorder - 5, nodeY - 5);
                    g.drawLine(maxBorder, nodeY, maxBorder - 5, nodeY + 5);
                    g.drawLine(arrowX - 5, nodeY, minBorder, nodeY);
                    g.drawLine(minBorder, nodeY, minBorder + 5, nodeY - 5);
                    g.drawLine(minBorder, nodeY, minBorder + 5, nodeY + 5);
                }
            }
        }

        for (DrawTree.Node node : parasite.getNodes()) {
            if (node.getType() != DrawTree.LEAF_TYPE && node.getMinTime() != node.getMaxTime() && grabbedArrow != node) {
                //dragging time zone border near node from right
                if (dragging && grabbedBorder > 0
                        && grabbedBorder > node.getX() && grabbedBorder - node.getX() < 35
                        && (node.getMinTime() == timeBorders.size() - borderIndex + 1)) {
                    int nodeX = node.getX();
                    int nodeY = node.getY() + panelDisp;
                    g.drawLine(nodeX + 5, nodeY, nodeX + 15, nodeY);
                    g.drawLine(nodeX + 15, nodeY, nodeX + 10, nodeY - 5);
                    g.drawLine(nodeX + 15, nodeY, nodeX + 10, nodeY + 5);

                    int highBorder;
                    if (node.getMaxTime() == timeBorders.size() / 2 + 1) {
                        highBorder = parasite.getLeaves().get(0).getX();
                    } else {
                        highBorder = (int) Math.round(timeBorders.get(timeBorders.size() - node.getMaxTime())) + 15;
                    }

                    g.drawLine(nodeX - 5, nodeY, highBorder, nodeY);
                    g.drawLine(highBorder, nodeY, highBorder + 5, nodeY - 5);
                    g.drawLine(highBorder, nodeY, highBorder + 5, nodeY + 5);
                } 
                
                //dragged time zone border past node from right
                else if (dragging && grabbedBorder > 0 && grabbedBorder <= node.getX() && node.getMinTime() == timeBorders.size() - borderIndex + 1) {
                    node.setMinTime(node.getMinTime() - 1);
                } 
                
                //non-dragged arrows covering range of time zones
                else {
                    int maxBorder;
                    int minBorder;

                    if (node.getMaxTime() == timeBorders.size() / 2 + 1) {
                        if (parasite.getLeaves().isEmpty()) {
                            maxBorder = getWidth()/2+15;
                        }
                        else {
                            maxBorder = parasite.getLeaves().get(0).getX();
                        }
                    } 
                    else {
                        maxBorder = (int) Math.round(timeBorders.get(timeBorders.size() - node.getMaxTime())) + 15;
                    }

                    if (node.getMinTime() == 1) {
                        minBorder = getWidth()-20;
                    } 
                    else {
                        minBorder = (int) Math.round(timeBorders.get(timeBorders.size() - node.getMinTime() + 1)) - 15;
                    }

                    int arrowX = node.getX();
                    int nodeY = node.getY() + panelDisp;
                    g.drawLine(arrowX - 5, nodeY, maxBorder, nodeY);
                    g.drawLine(maxBorder, nodeY, maxBorder + 5, nodeY - 5);
                    g.drawLine(maxBorder, nodeY, maxBorder + 5, nodeY + 5);
                    g.drawLine(arrowX + 5, nodeY, minBorder, nodeY);
                    g.drawLine(minBorder, nodeY, minBorder - 5, nodeY - 5);
                    g.drawLine(minBorder, nodeY, minBorder - 5, nodeY + 5);
                }
            }
        }

        //draws node arrows for single time zone (only if in time state)
        if (state == TIME_STATE && !timeBorders.isEmpty()) {
            for (DrawTree.Node node : host.getNodes()) {
                if (node.getType() != DrawTree.LEAF_TYPE && node.getMinTime() == node.getMaxTime() && grabbedArrow != node) {
                    int nodeX = node.getX();
                    int nodeY = node.getY() + panelDisp;
                    g.drawLine(nodeX + 5, nodeY, nodeX + 15, nodeY);
                    g.drawLine(nodeX + 15, nodeY, nodeX + 10, nodeY - 5);
                    g.drawLine(nodeX + 15, nodeY, nodeX + 10, nodeY + 5);
                    g.drawLine(nodeX - 5, nodeY, nodeX - 15, nodeY);
                    g.drawLine(nodeX - 15, nodeY, nodeX - 10, nodeY - 5);
                    g.drawLine(nodeX - 15, nodeY, nodeX - 10, nodeY + 5);
                }
            }
            for (DrawTree.Node node : parasite.getNodes()) {
                if (node.getType() != DrawTree.LEAF_TYPE && node.getMinTime() == node.getMaxTime() && grabbedArrow != node) {
                    int nodeX = node.getX();
                    int nodeY = node.getY() + panelDisp;
                    g.drawLine(nodeX + 5, nodeY, nodeX + 15, nodeY);
                    g.drawLine(nodeX + 15, nodeY, nodeX + 10, nodeY - 5);
                    g.drawLine(nodeX + 15, nodeY, nodeX + 10, nodeY + 5);
                    g.drawLine(nodeX - 5, nodeY, nodeX - 15, nodeY);
                    g.drawLine(nodeX - 15, nodeY, nodeX - 10, nodeY - 5);
                    g.drawLine(nodeX - 15, nodeY, nodeX - 10, nodeY + 5);
                }
            }
        }

        //dragged arrows
        if (grabbedArrow != null) { //dragging deleted
            int arrowX = grabbedArrow.getX();
            int arrowY = grabbedArrow.getY() + panelDisp;

            //right arrow dragged
            if (arrowHead > grabbedArrow.getX()) {
                g.drawLine(arrowX + 5, arrowY, arrowHead, arrowY);
                g.drawLine(arrowHead, arrowY, arrowHead - 5, arrowY - 5);
                g.drawLine(arrowHead, arrowY, arrowHead - 5, arrowY + 5);

                int lowBorder = 0;
                if (grabbedArrow.getMinTime() == grabbedArrow.getMaxTime()) //arrow opposite to dragged arrow short
                {
                    g.drawLine(arrowX - 5, arrowY, arrowX - 15, arrowY);
                    g.drawLine(arrowX - 15, arrowY, arrowX - 10, arrowY - 5);
                    g.drawLine(arrowX - 15, arrowY, arrowX - 10, arrowY + 5);
                } 
                else //arrow opposite to dragged arrow long
                {
                    if (getSide(grabbedArrow.getX()) == DrawTree.HOST_TYPE) {
                        if (grabbedArrow.getMinTime() == 1) {
                            lowBorder = 15;
                        } 
                        else {
                            lowBorder = (int) Math.round(timeBorders.get(grabbedArrow.getMinTime() - 2)) + 15;
                        }
                    } 
                    else {
                        if (grabbedArrow.getMaxTime() == timeBorders.size() / 2 + 1) {
                            lowBorder = parasite.getLeaves().get(0).getX();
                        } 
                        else {
                            lowBorder = (int) Math.round(timeBorders.get(timeBorders.size() - grabbedArrow.getMaxTime())) + 15;
                        }
                    }

                    g.drawLine(arrowX - 5, arrowY, lowBorder, arrowY);
                    g.drawLine(lowBorder, arrowY, lowBorder + 5, arrowY - 5);
                    g.drawLine(lowBorder, arrowY, lowBorder + 5, arrowY + 5);
                }
            } 
            
            //left arrow dragged
            else {
                g.drawLine(arrowX - 5, arrowY, arrowHead, arrowY);
                g.drawLine(arrowHead, arrowY, arrowHead + 5, arrowY - 5);
                g.drawLine(arrowHead, arrowY, arrowHead + 5, arrowY + 5);

                int highBorder = 0;
                if (grabbedArrow.getMinTime() == grabbedArrow.getMaxTime()) //arrow opposite to dragged arrow short
                {
                    g.drawLine(arrowX + 5, arrowY, arrowX + 15, arrowY);
                    g.drawLine(arrowX + 15, arrowY, arrowX + 10, arrowY - 5);
                    g.drawLine(arrowX + 15, arrowY, arrowX + 10, arrowY + 5);
                } 
                
                else //arrow opposite to dragged arrow long
                {
                    if (getSide(grabbedArrow.getX()) == DrawTree.HOST_TYPE) {
                        if (grabbedArrow.getMaxTime() == timeBorders.size() / 2 + 1) {
                            highBorder = host.getLeaves().get(0).getX();
                        } 
                        else {
                            highBorder = (int) Math.round(timeBorders.get(grabbedArrow.getMaxTime() - 1)) - 15;
                        }
                    } 
                    else {
                        if (grabbedArrow.getMinTime() == 1) {
                            highBorder = getWidth()-15;
                        } 
                        else {
                            highBorder = (int) Math.round(timeBorders.get(timeBorders.size() - grabbedArrow.getMinTime() + 1)) - 15;
                        }
                    }

                    g.drawLine(arrowX + 5, arrowY, highBorder, arrowY);
                    g.drawLine(highBorder, arrowY, highBorder - 5, arrowY - 5);
                    g.drawLine(highBorder, arrowY, highBorder - 5, arrowY + 5);
                }
            }
        }
    }

    /*
     * draws time zone borders
     */
    private void drawZones(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        float dash[] = {20.0f, 20.0f};
        BasicStroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, dash, 25.0f);
        g2d.setStroke(dashed);
        if (!timeBorders.isEmpty()) {
            // time zone 1 labels
            float first = timeBorders.get(0);

            if (first > 80) {
                g.drawString("Time Zone 1", (int) Math.round(timeBorders.get(0) / 2 - 40), panelDisp - 10);
            } 
            else {
                g.drawString("Time", (int) Math.round(timeBorders.get(0) / 2 - 18), panelDisp - 10);
                g.drawString("Zone 1", (int) Math.round(timeBorders.get(0) / 2 - 23), panelDisp + 10);
            }
            g.drawString("1", (int) Math.round((getWidth() + timeBorders.get(timeBorders.size() - 1)) / 2 - 3), panelDisp - 10);

            // final time zone labels
            g.drawString(Integer.toString(timeBorders.size() / 2 + 1), (getWidth() / 2 + (int) Math.round(timeBorders.get(timeBorders.size() / 2 - 1))) / 2 - 3, panelDisp - 10);
            g.drawString(Integer.toString(timeBorders.size() / 2 + 1), (getWidth() / 2 + (int) Math.round(timeBorders.get(timeBorders.size() / 2))) / 2 - 3, panelDisp - 10);

            // final time zone lines
            g2d.draw(new Line2D.Double(timeBorders.get(timeBorders.size() / 2 - 1), getHeight(), timeBorders.get(timeBorders.size() / 2 - 1), panelDisp-30));
            g2d.draw(new Line2D.Double(timeBorders.get(timeBorders.size() / 2), getHeight(), timeBorders.get(timeBorders.size() / 2), panelDisp-30));

            // rest of host time zone lines and labels
            for (int i = 0; i < timeBorders.size() / 2 - 1; i++) {
                g2d.draw(new Line2D.Double(timeBorders.get(i), getHeight(), timeBorders.get(i), panelDisp-30));
                g.drawString(Integer.toString(i + 2), ((int) Math.round(timeBorders.get(i + 1) + timeBorders.get(i)) / 2) - 3, panelDisp - 10);
            }

            //rest of parasite time zone lines and labels
            for (int i = timeBorders.size() / 2 + 1; i < timeBorders.size(); i++) {
                g2d.draw(new Line2D.Double(timeBorders.get(i), getHeight(), timeBorders.get(i), panelDisp-30));
                g.drawString(Integer.toString(timeBorders.size() - i + 1), (int) Math.round(timeBorders.get(i - 1) + timeBorders.get(i)) / 2 - 3, panelDisp - 10);
            }
        }
        g2d.setStroke(new BasicStroke());
    }

    /*
     * draws links between leaves
     */
    private void drawLinks(Graphics g) {
        DrawTree.Node linka;
        for (int i = 0; i < links.size(); i++) {
            linka = links.get(i);
            for (DrawTree.Node linkb : linka.getLink()) {
                if (linking && linka != link1 && linkb != link1) {
                    g.setColor(Color.GRAY);
                }
                else {
                    g.setColor(Color.CYAN);
                }
                g.drawLine(linka.getX(), linka.getY() + panelDisp, linkb.getX(), linkb.getY() + panelDisp);
            }
        }
        if (dragX != -1 && state == LINK_STATE && link1 != null) //dragged link 
        {
            g.setColor(Color.CYAN);
            g.drawLine(link1.getX(), link1.getY() + panelDisp, dragX, dragY);
        }
    }

    /*
     * draws selection box for selecting nodes in region mode
     */
    private void drawBox(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (clickedX < getWidth() / 2 && clickedY > panelDisp && dragX != -1 && state == REGION_STATE) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g.setColor(Color.gray);
            int startX;
            int startY;

            if (dragY < panelDisp) {
                dragY = panelDisp + 1;
            }

            if (dragX > getWidth() / 2) {
                dragX = getWidth() / 2 - 1;
            }

            if (clickedX < dragX) {
                startX = clickedX;
            } else {
                startX = dragX;
            }

            if (clickedY < dragY) {
                startY = clickedY;
            } else {
                startY = dragY;
            }

            g2d.fillRect(startX, startY, Math.abs(dragX - clickedX), Math.abs(dragY - clickedY));
            g.setColor(Color.darkGray);
            g2d.drawRect(startX, startY, Math.abs(dragX - clickedX), Math.abs(dragY - clickedY));
        }
    }

    private void paintHelper(Graphics g, DrawTree.Node current, int oldx) {
        /**
         * Recursively paints a tree from the given root down
         */
        //if current node is root, then draws to edge of window
        //Draw horizontal line from current node to previous level
        g.setColor(Color.BLACK); 
        if (oldx != 0 && oldx != getWidth() && current.getParent().getY() == current.getY()) {
            oldx += 5;
        }
        g.drawLine(current.getX(), current.getY() + panelDisp, oldx, current.getY() + panelDisp);
        oldx = current.getX();

        //Find the y of the highest and lowest child and draw a vertical line
        //between these heights through the current node
        int miny = current.getY();
        int maxy = current.getY();


        ArrayList<DrawTree.Node> children = current.getChildren();
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i).getY() < miny) {
                miny = children.get(i).getY();
            }
            if (children.get(i).getY() > maxy) {
                maxy = children.get(i).getY();
            }
        }

        //lengthen the panel if necessary
        if (maxy > getHeight() && !dragging) {
            this.setPreferredSize(new Dimension(getWidth()+100, maxy + 50));
            this.setSize(getWidth(), maxy + 50);
        }

        //draws verical line
        g.setColor(Color.BLACK);
        g.drawLine(current.getX(), miny + panelDisp, current.getX(), maxy + panelDisp);

        //draws node
        int reg = current.getRegion();
        setColor(reg, g);
        g.fillOval(current.getX() - radius, current.getY() - radius + panelDisp, 2 * radius, 2 * radius);
        if (state != LINK_STATE) {
            linking = false;
            link1 = null;
        }
        if (linking) {
            g.setColor(Color.CYAN);
            g.fillOval(link1.getX() - radius, link1.getY() - radius + panelDisp, 2 * radius, 2 * radius);
        }

        //Attempt to draw label of node
        if (current.getType() == DrawTree.LEAF_TYPE) {
            g.setColor(Color.BLACK);
            JLabel currlabel = current.getLabel();
            String text = currlabel.getText();
            if (text.length() > 0) {
                try {
                    Integer.parseInt(text);
                } 
                catch (NumberFormatException nfe) {
                    if (current.getTreeType() == DrawTree.HOST_TYPE) {
                        FontMetrics metrics = g.getFontMetrics();
                        int width = metrics.stringWidth(text);
                        currlabel.setLocation(current.getX() - 10 - width, currlabel.getY());
                    }
                    g.drawChars(text.toCharArray(), 0, text.length(), currlabel.getX(),
                            currlabel.getY() + panelDisp);
                }
            }
        }

        //Recurse with all the children
        for (int i = 0; i < children.size(); i++) {
            paintHelper(g, children.get(i), oldx);
        }

    }

    /*
     * sets color of node depending on region
     */
    private void setColor(int reg, Graphics g) {
        if (reg == 0) {
            g.setColor(Color.BLUE);
        } else {
            if (reg == 1) {
                g.setColor(Color.GREEN);
            } else if (reg == 2) {
                g.setColor(Color.ORANGE);
            } else if (reg == 3) {
                g.setColor(Color.RED);
            } else if (reg == 4) {
                g.setColor(Color.GRAY);
            } else if (reg == 10) {
                g.setColor(Color.PINK);
            } else {
                int power = roundPowerTwo(reg - 4);
                float colorNum = ((float) 1.0 / (float) (2 * power)) + (float) ((1.0 / power) * (reg - 4 - power));
                g.setColor(Color.getHSBColor(colorNum, (float) 1.0, (float) 0.75));
            }
        }
    }

    /*
     * returns time zone of leaves
     */
    private int leafTime() {
        if (!host.getLeaves().isEmpty()) {
            return host.getLeaves().get(0).getMaxTime();
        } else if (!parasite.getLeaves().isEmpty()) {
            return parasite.getLeaves().get(0).getMaxTime();
        } else {
            return 1;
        }
    }
    
    /*
     * links node (in parasite tree) to all nodes in host tree described by MultiHostPhi
     */
    private void linkAll(DrawTree.Node node, SortedSet<Integer> linkSort) {
        int linkInt = linkSort.first();
        DrawTree.Node link = host.getNodes().get(linkInt);
        node.addLink(link);
        link.addLink(node);
        links.add(link);
        linkAll(node, linkSort.tailSet(linkInt+1));
    }
    
    /*
     * returns true if at least one parasite is linked to multiple hosts
     */
    private boolean multiHostParasite() {
        for (DrawTree.Node paraLeaf : parasite.getLeaves()) {
            if (paraLeaf.getLink().size() > 1) {
                return true;
            }
        }
        return false;
    }
    
    /*
     * makes .tree file output for links
     */
    public String makePhiOutput(int off) {
        String toReturn = "";
        for (int i = 0; i < links.size(); i++) {
            DrawTree.Node current = links.get(i);
            if (current.getTreeType() == DrawTree.HOST_TYPE) {
                toReturn += (host.getNodeID(current, 0) + "\t");
                for (DrawTree.Node linkedNode : current.getLink()) {

                    if (linkedNode == current.getLink().get(current.getLink().size() - 1)) {
                        toReturn += parasite.getNodeID(linkedNode, off) + "\n";
                    } else {
                        toReturn += parasite.getNodeID(linkedNode, off) + "\t";
                    }
                }
            } else {
                for (DrawTree.Node linkedNode : current.getLink()) {
                    toReturn += (host.getNodeID(linkedNode, 0) + "\t");
                    toReturn += parasite.getNodeID(current, off) + "\n";
                }
            }
        }
        return toReturn;
    }

    /*
     * makes .tree file output for regions
     */
    public String makeRegionOutput() {
        String toReturn = "";
        for (DrawTree.Node node : host.getNodes()) {
            toReturn += host.getNodeID(node, 0) + "\t" + node.getRegion() + "\n";
        }
        return toReturn;
    }

    /*
     * makes .tree file output for region costs
     */
    public String makeRegionCostOutput() {
        String toReturn = "";
        for (ArrayList<Double> cost : regionCosts) {
            if (cost.get(2) == Double.POSITIVE_INFINITY) {
                toReturn += cost.get(0).intValue() + "\t" + cost.get(1).intValue() + "\t" + "infinity" + "\n";
            } else {
                toReturn += cost.get(0).intValue() + "\t" + cost.get(1).intValue() + "\t" + cost.get(2).intValue() + "\n";
            }
        }
        return toReturn;
    }
    
    /*
     * makes .nex file output for host and parasite trees
     */
    public String makeTreeOutputNex(DrawTree.Node current) {
        String toReturn = "";
        if (current.getType() == DrawTree.LEAF_TYPE) {
            if (current.getLabel().getText() != "") {
                toReturn += current.getLabel().getText();
            }
            else {
                if (host.getNodes().contains(current)) {
                    toReturn += host.getNodes().indexOf(current);
                }
                else {
                    toReturn += parasite.getNodes().indexOf(current)+host.getNodes().size();
                }
            }
            if (!timeBorders.isEmpty()) {
                toReturn += ":[" + current.getMaxTime() + "]";   
            }
        }
        else {
            toReturn += "(";
            for (DrawTree.Node child: current.getChildren())
            {
                toReturn += makeTreeOutputNex(child);
                if (child != current.getChildren().get(current.getChildren().size()-1)) {
                    toReturn += ",";
                }
                else {
                    toReturn +=")";
                }
            }
            if (!timeBorders.isEmpty()) {
                toReturn += ":[" + current.getMinTime();
                if (current.getMinTime() != current.getMaxTime()) {
                    toReturn += "," + current.getMaxTime();
                }
                toReturn += "]";
            }
        }
        return toReturn;
    }
    
    /*
     * makes .nex file output for links
     */
    public String makePhiOutputNex() {
        String toReturn = "";
        for (DrawTree.Node hostLink : links)
        {
            for (DrawTree.Node paraLink : hostLink.getLink())
            {
                if (paraLink.getLabel().getText() != "") {
                    toReturn += paraLink.getLabel().getText();
                }
                else {
                        toReturn += parasite.getNodes().indexOf(paraLink) + host.getNodes().size();
                }
                toReturn += " : ";
                if (hostLink.getLabel().getText() != "") {
                    toReturn += hostLink.getLabel().getText();
                }
                else {
                        toReturn += host.getNodes().indexOf(hostLink);
                }
                if (paraLink != hostLink.getLink().get(hostLink.getLink().size()-1) || hostLink != links.get(links.size()-1)) {
                    toReturn += ", ";
                }
            }
        }
        toReturn += ";";
        return toReturn;
    }

    /**
     *
     * @param filename
     * @throws IOException
     * saves as .tree file
     */
    public void saveTrees(File saveFile) throws IOException {
        String filename = saveFile.getAbsolutePath();
        FileOutputStream out;
        PrintStream file;
        String ext;
        if (filename.endsWith(".tree") || filename.endsWith(".TREE")) {
            ext = "";
        }
        else {
            ext = ".tree";
        }
        out = new FileOutputStream(filename + ext);
        file = new PrintStream(out);

        file.println("HOSTTREE");
        file.println(host.makeTreeOutput(0));

        file.println();

        file.println("HOSTNAMES");
        file.println(host.makeNameOutput(0));

        file.println();

        int offset = host.getSize();
        file.println("PARASITETREE");
        file.println(parasite.makeTreeOutput(offset));

        file.println();

        file.println("PARASITENAMES");
        file.println(parasite.makeNameOutput(offset));

        file.println();

        file.println("PHI");
        file.println(makePhiOutput(offset));

        file.println();

        file.println("HOSTRANKS");
        file.println(host.makeTimeOutput(0));

        file.println();

        file.println("PARASITERANKS");
        file.println(parasite.makeTimeOutput(offset));

        file.println();

        if (maxRegion() != 0) {
            file.println("HOSTREGIONS");
            file.println(makeRegionOutput());

            if (!regionCosts.isEmpty()) {
                file.println("REGIONCOSTS");
                file.println(makeRegionCostOutput());
            }
        }

        file.close();
        saved = true;
        
        int nameLength = saveFile.getName().length();
        int fullLength = filename.length();
        int dif = fullLength - nameLength;
        frame.setTitle(filename.substring(dif, fullLength)+ext);
        redoPanel();
    }
    
    /*
     * saves as .nex file
     */
    public void saveTreesNex(File saveFile) throws IOException {
        String filename = saveFile.getAbsolutePath();
        if (multiHostParasite()) {
            int result = JOptionPane.showOptionDialog(null, "The .nex file format does not support parasite tips linked to multiple host tips.", "", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[]{"Save as .tree file", "Cancel"}, "");
            if (result == 0) {
                frame.setExt(".tree");
                saveTrees(saveFile);
                return;
            }
            else {
                return;
            }
        }
        else if (!allZero()) {
            int result = JOptionPane.showOptionDialog(null, "Warning: Saving as a .nex file will delete region information", "", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[]{"Save as .tree file", "Continue saving as .nex file"}, "");
            if (result == 0) {
                frame.setExt(".tree");
                saveTrees(saveFile);
                return;
            }
            else {
                for (DrawTree.Node current : host.getNodes()) {
                    current.setRegion(0);
                }
                regionCosts.clear();
            }
        }
        FileOutputStream out;
        PrintStream file;
        String ext;
        if (filename.endsWith(".nex") || filename.endsWith(".NEX")) {
            ext = "";
        }
        else {
            ext = ".nex";
        }

        out = new FileOutputStream(filename + ext);
        file = new PrintStream(out);

        file.println("#nexus");
        file.println("begin host;");
        file.print("tree host =");
        file.print(makeTreeOutputNex(host.getRoot()));
        file.println(";");
        file.println("endblock;");
        file.println("begin parasite;");
        file.print("tree parasite =");
        file.print(makeTreeOutputNex(parasite.getRoot()));
        file.println(";");
        file.println("endblock;");
        file.println("begin distribution;");
        file.println("Range");
        file.println(makePhiOutputNex());
        file.println("endblock;");
        
        file.close();
        saved = true;
        int nameLength = saveFile.getName().length();
        int fullLength = filename.length();
        int dif = fullLength - nameLength;
        frame.setTitle(filename.substring(dif, fullLength)+ext);
        redoPanel();
    }

    /*
     * converts from Jane ProblemInstance to DrawTree for GUI
     */
    public void convertTrees(ProblemInstance prob) {
            
        host = new DrawTree(0, 0, getWidth() / 2 - 5, getHeight(), DrawTree.HOST_TYPE);
        if (prob.hostTree.size != 0) {
            host.getNodes().clear();
        }
        parasite = new DrawTree(getWidth() / 2 + 5, 0, getWidth() / 2 - 5, getHeight(), DrawTree.PARASITE_TYPE);
        if (prob.parasiteTree.size != 0) {
            parasite.getNodes().clear();
        }
        links = new ArrayList<DrawTree.Node>();
        timeBorders.clear();
        regionCosts.clear();
        panelDisp = 30;

        for (int treeType = 0; treeType < 2; treeType++) 
        {
            Tree tree;
            int type;
            DrawTree drawTree;
            if (treeType == 0) {
                tree = prob.hostTree;
                type = DrawTree.HOST_TYPE;
                drawTree = host;
            }
            
            else {
                tree = prob.parasiteTree;
                type = DrawTree.PARASITE_TYPE;
                drawTree = parasite;
            }

            for (int i=0; i<tree.size; i++)
            {
                DrawTree.Node newnode = drawTree.new Node(10, (getHeight()-panelDisp)/2-panelDisp, null, 0, type);
                drawTree.getNodes().add(newnode);
            }
            for (int i = 0; i < tree.size; i++) 
            {
                //parent/children information
                DrawTree.Node current = drawTree.getNodes().get(i);
                Node node = tree.node[i];
                int parent = node.parent;
                DrawTree.Node newParent;
                if (node.parent != -1) {
                    newParent = drawTree.getNodes().get(parent);
                } 
                
                else {
                    newParent = null;
                }
                
                drawTree.getNodes().get(i).setParent(newParent);
                
                if (newParent != null) {
                    newParent.getChildren().add(current);
                }

                //time zone information
                int minTime;
                int maxTime;
                if (treeType == 0)
                {
                    minTime = prob.timeZones.getHostZoneStart(i);
                    maxTime = prob.timeZones.getHostZoneEnd(i);
                }
                else
                {
                    minTime = prob.timeZones.getParasiteZoneStart(i);
                    maxTime = prob.timeZones.getParasiteZoneEnd(i);
                }
                current.setMinTime(minTime);
                current.setMaxTime(maxTime);

                //region information
                if (treeType == 0)
                {
                    if (prob.hasRegions()) {
                        int region = prob.hostRegions.regionOfNode(i);
                        current.setRegion(region);
                    } 

                    else {
                        current.setRegion(0);
                    }
                }
            }
        }
        
        for (int treeType = 0; treeType < 2; treeType++) 
        {
            Tree tree;
            int type;
            DrawTree drawTree;
            if (treeType == 0) {
                tree = prob.hostTree;
                drawTree = host;
            }
            
            else {
                tree = prob.parasiteTree;
                drawTree = parasite;
            }

            for (int i = 0; i < tree.size; i++) 
            {
                Node node = tree.node[i];
                DrawTree.Node current = drawTree.getNodes().get(i);
                
                //depth information
                int newDepth = current.findDepth();
                current.setDepth(newDepth);
                
                //sets node types
                if (current.getParent() == null)
                {
                    current.setType(DrawTree.ROOT_TYPE);
                    drawTree.setRoot(current);
                }
                
                else if (!current.getChildren().isEmpty())
                {
                    current.setType(DrawTree.INTERNAL_TYPE);
                }
                
                else {
                    current.setType(DrawTree.LEAF_TYPE);
                    drawTree.getLeaves().add(current);
                    
                    //label information
                    String newLabel = node.name;
                    current.setLabel(newLabel);
                    
                    if (treeType == 1)
                    {
                        //sets linking information
                        if (prob.phi.getHasMultihostParasites())
                        {
                            SortedSet<Integer>[] linkL = prob.phi.getMultiHostPhi();
                            SortedSet<Integer> linkSort = linkL[i];
                            linkAll(current, linkSort); 
                        }

                        else
                        {
                            int[] linkL = prob.phi.getSingleHostPhi();
                            int linkInt = linkL[i];
                            if (linkInt != -1) {
                                DrawTree.Node link = host.getNodes().get(linkInt);
                                current.addLink(link);
                                link.addLink(current);
                                links.add(link);
                            }
                        }
                    }
                }
            }
            
            //sets maxdepth
            int max = 0;
            for (DrawTree.Node leaf : drawTree.getLeaves())
            {
                if (leaf.getDepth() > max) 
                    max = leaf.getDepth();
            }
            drawTree.setMaxdepth(max);
        }
        
        //sets region switching costs
        int[][] switchCost = prob.hostRegions.getSwitchCost();
        if (switchCost != null)
        {
            for (int i=0; i<switchCost.length; i++)
            {
                for (int j=0; j<switchCost[i].length; j++)
                {
                    int cost = switchCost[i][j];
                    if (cost != 0)
                    {      
                        ArrayList<Double> regionSwitchCost = new ArrayList<Double>();
                        regionSwitchCost.add((double)i);
                        regionSwitchCost.add((double)j);
                        if (cost == INFINITE_DISTANCE) { 
                            regionSwitchCost.add(Double.POSITIVE_INFINITY);
                        }

                        else {
                            regionSwitchCost.add((double)cost);
                        }
                        regionCosts.add(regionSwitchCost);    
                    }
                }
            }
        }
        
        //sets coordinates of time zone borders
        int numTimeZones = leafTime();
        int numTimeBorders = 2*(numTimeZones-1); 
        float timeBorderX;
        for (int i=1; i<numTimeZones; i++) {
            timeBorderX = i*((getWidth()/2)/numTimeZones);
            timeBorders.add(timeBorderX);
            timeBorderX += getWidth()/2;
            timeBorders.add(timeBorderX);
        }
        Collections.sort(timeBorders);
        redoPanel(); 
        saved = true;
    }

  /**
    *
    * @param file
    * @throws IOException
    */
    public void loadTrees(File file) throws IOException {
        
        try {
            String path = file.getAbsolutePath();
            int nameLength = file.getName().length();
            int fullLength = path.length();
            int dif = fullLength - nameLength;
            frame.setTitle(path.substring(dif, fullLength));
            
            FileInputStream fstream = new FileInputStream(file);
            InputStreamReader f = new InputStreamReader(fstream);
            BufferedReader reader = new BufferedReader(f);

            ArrayList<String> hosttree = new ArrayList<String>();
            ArrayList<String> hostnames = new ArrayList<String>();
            ArrayList<String> paratree = new ArrayList<String>();
            ArrayList<String> paranames = new ArrayList<String>();
            ArrayList<String> linkings = new ArrayList<String>();
            ArrayList<String> hostranks = new ArrayList<String>();
            ArrayList<String> parasiteranks = new ArrayList<String>();
            ArrayList<String> hostregions = new ArrayList<String>();
            ArrayList<String> regioncosts = new ArrayList<String>();
            ArrayList<String> currlist = null;

            String line;
            while (reader.ready()) {
                line = reader.readLine();
                if (line.toUpperCase().contains("HOSTTREE") )
                    currlist = hosttree;
                else if (line.toUpperCase().contains("HOSTNAMES") )
                    currlist = hostnames;
                else if (line.toUpperCase().contains("PARASITETREE") )
                    currlist = paratree;
                else if (line.toUpperCase().contains("PARASITENAMES") )
                    currlist = paranames;
                else if (line.toUpperCase().contains("PHI") )
                    currlist = linkings;
                else if (line.toUpperCase().contains("HOSTRANKS") )
                    currlist = hostranks;
                else if (line.toUpperCase().contains("PARASITERANKS") )
                    currlist = parasiteranks;
                else if (line.toUpperCase().contains("HOSTREGIONS") )
                    currlist = hostregions;
                else if (line.toUpperCase().contains("REGIONCOSTS") )
                    currlist = regioncosts;
                else {
                    line = line.replaceAll("\\s+", " ");
                    if (!line.equals(" ") && !line.equals(""))
                        currlist.add(line);
                }
            }

            ArrayList<ArrayList<Integer>> htree = new ArrayList<ArrayList<Integer>>();
            String result[];
            for (int i = 0; i < hosttree.size(); i++) {
                htree.add(new ArrayList<Integer>());
                result = hosttree.get(i).split(" ");
                for (int j = 0; j < result.length; j++) {
                    try {
                        htree.get(i).add(Integer.parseInt(result[j]));
                    } catch (NumberFormatException nfe) {
                        htree.get(i).add(-1);
                    }
                }
            }

            ArrayList<ArrayList<Integer>> ptree = new ArrayList<ArrayList<Integer>>();

            for (int i = 0; i < paratree.size(); i++) {
                ptree.add(new ArrayList<Integer>());
                result = paratree.get(i).split(" ");
                for (int j = 0; j < result.length; j++) {
                    try {
                        ptree.get(i).add(Integer.parseInt(result[j]));
                    } catch (NumberFormatException nfe) {
                        ptree.get(i).add(0);
                    }
                }
            }

            timeBorders.clear();
            regionCosts.clear();
            panelDisp = 30;
            host = new DrawTree(0, 0, getWidth()/2-5, getHeight(), DrawTree.HOST_TYPE);
            host.getNodes().clear();
            parasite = new DrawTree(getWidth()/2+5, 0, getWidth()/2-5, getHeight(),DrawTree.PARASITE_TYPE);
            parasite.getNodes().clear();
            links = new ArrayList<DrawTree.Node>();

            loadTree(htree, hostnames, hostranks, hostregions,
                     DrawTree.HOST_TYPE);
            ArrayList<DrawTree.Node> hostnodes = host.getNodes();
            
            loadTree(ptree, paranames, parasiteranks, hostregions,
                     DrawTree.PARASITE_TYPE);
            ArrayList<DrawTree.Node> paranodes = parasite.getNodes();
            
            //sets time zone borders
            int numTimeZones = leafTime();
            int numTimeBorders = 2 * (numTimeZones - 1);
            float timeBorderX;
            
            for (int i = 1; i < numTimeZones; i++) {
                timeBorderX = i*((getWidth()/2)/numTimeZones);
                timeBorders.add(timeBorderX);
                timeBorderX += getWidth()/2;
                timeBorders.add(timeBorderX);
            }
            
            Collections.sort(timeBorders);

            // Sets linking
            DrawTree.Node node1, node2;
            for (int i = 0; i < linkings.size(); i++) {
                result = linkings.get(i).split(" ");
                
                ArrayList<Integer> hostNodeNums = new ArrayList<Integer>();
                for (int j=0; j<htree.size(); j++)
                    hostNodeNums.add(htree.get(j).get(0));
                int hostNodeInd = hostNodeNums.indexOf(Integer.parseInt(result[0]));
                node1 = hostnodes.get(hostNodeInd);
                
                ArrayList<Integer> paraNodeNums = new ArrayList<Integer>();
                for (int j = 0; j<ptree.size(); j++)
                    paraNodeNums.add(ptree.get(j).get(0));
                for (int j = 1; j<result.length; j++) {
                    int paraNodeInd = paraNodeNums.indexOf(Integer.parseInt(result[j]));
                    node2 = paranodes.get(paraNodeInd);
                    node1.addLink(node2);
                    node2.addLink(node1);
                    boolean contains = false;
                    for (DrawTree.Node linkedNode : links) {
                        if (node1 == linkedNode) {
                            contains = true;
                        }
                    }
                    if (!contains) {
                        links.add(node1);
                    }
                }
            }
            
            //sets region switching costs
            String[] regionCost;
            for (int i=0; i<regioncosts.size(); i++) {
                regionCost = regioncosts.get(i).split(" ");
                ArrayList<Double> regionCostL = new ArrayList<Double>();
                for (int j = 0; j < 3; j++) {
                    if (regionCost[j].toLowerCase().equals("i")
                            || regionCost[j].toLowerCase().equals("inf")
                            || regionCost[j].toLowerCase().equals("infinity")
                            || regionCost[j].toLowerCase().equals("infty")) {
                        regionCostL.add(Double.POSITIVE_INFINITY);
                    } else {
                        double regionCostsIndex = Double.valueOf(regionCost[j]).doubleValue();
                        regionCostL.add(regionCostsIndex);
                    }
                }
                regionCosts.add(regionCostL);
            }
            redoPanel();
        } catch (IOException e) {
            throw e;
        }
        
    }
    
    private void loadTree(ArrayList<ArrayList<Integer>> tree,
                           ArrayList<String> names, ArrayList<String> ranks,
                           ArrayList<String> hostregions, int type) {
        saved = true;
        DrawTree relTree;
        
        if (type == DrawTree.HOST_TYPE)
            relTree = host;
        else
            relTree = parasite;

        ArrayList<DrawTree.Node> nodes = relTree.getNodes();
        ArrayList<Integer> kids;
        String name[];
        String hostTime[];
        String paraTime[];
        String timeRange[];
        String region[];
        
        for (int i=0; i < tree.size(); i++) {
            DrawTree.Node newnode = relTree.new Node(10, 0, null, 0, type);
            relTree.getNodes().add(newnode);
        }
        
        for (int i = 0; i < tree.size(); i++) {
            // Parent/children information
            DrawTree.Node current = nodes.get(i);
            int nodeNum = tree.get(i).get(0);
            for (int j = 0; j < tree.size(); j++) {
                kids = tree.get(j);
                for (int kidInd = 1; kidInd < kids.size(); kidInd++) {
                    if (kids.get(kidInd) == nodeNum) {
                        DrawTree.Node parent = nodes.get(j);
                        current.setParent(parent);
                        parent.getChildren().add(current);
                    }
                }
            }
            
            // Sets time zone information
            if (!ranks.isEmpty()) {
                hostTime = ranks.get(i).split(" "); 
                timeRange = hostTime[1].split(",");
                int minTime = Integer.parseInt(timeRange[0]);
                
                if (timeRange.length == 1) {
                    current.setMinTime(minTime);
                    current.setMaxTime(minTime);
                } else {
                    int maxTime = Integer.parseInt(timeRange[1]);
                    current.setMinTime(minTime);
                    current.setMaxTime(maxTime);
                }
            } else {
                current.setMinTime(1);
                current.setMaxTime(1);
            }
            
            if (type == DrawTree.HOST_TYPE && !hostregions.isEmpty()) {
                region = hostregions.get(i).split(" ");
                int regionInt = Integer.parseInt(region[1]);
                current.setRegion(regionInt);
            }
        }
        
        for (int i=0; i<tree.size(); i++) {
            DrawTree.Node current = nodes.get(i);
            
            //depth information
            int newDepth = current.findDepth();
            current.setDepth(newDepth);
                
            //sets node types
            if (current.getParent() == null) {
                current.setType(DrawTree.ROOT_TYPE);
                relTree.setRoot(current);
            } else if (current.getChildren().isEmpty()) {
                current.setType(DrawTree.LEAF_TYPE);
            } else {
                current.setType(DrawTree.INTERNAL_TYPE);
            }
            
            //sets node label
            if (current.getType() == DrawTree.LEAF_TYPE) {
                name = names.get(i).split(" ");
                String nameStr = name[1];
                for (int j=2; j<name.length; j++)
                    nameStr = nameStr+" "+name[j];
                current.setLabel(nameStr);
            }
        }
        
        // adds leaves of subtree to leaf list
        for (int i=0; i<tree.size(); i++) {
            DrawTree.Node current = nodes.get(i);
            if (current.getType() == DrawTree.ROOT_TYPE)
                relTree.addLeaves(current);
        }
        
        //sets maxdepth
        int max = 0;
        for (DrawTree.Node leaf : relTree.getLeaves()) {
            if (leaf.getDepth() > max) 
                max = leaf.getDepth();
        }
        
        relTree.setMaxdepth(max);
        
    }
    
//handles changes to panel  
    private void redoPanel() {
        revalidate();
        repaint();
    }
}
