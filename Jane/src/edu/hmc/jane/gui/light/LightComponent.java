/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.gui.light;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author John
 */
public abstract class LightComponent {
    private int width;
    private int height;

    private int x;
    private int y;

    private LightRepaintManager mgr;

    private Collection<MouseListener> mouseListeners=new LinkedList<MouseListener>();
    private Collection<MouseMotionListener> mouseMotionListeners=
            new LinkedList<MouseMotionListener>();

    private LightContainer parent;

    private String toolTip;

    public abstract void paint(Graphics2D g);

    public void addMouseListener(MouseListener l) {
        if(l!=null)
            mouseListeners.add(l);
    }

    public void addMouseMotionListener(MouseMotionListener l) {
        if(l!=null)
            mouseMotionListeners.add(l);
    }

    public void removeMouseMotionListener(MouseMotionListener l) {
        mouseMotionListeners.remove(l);
    }

    public void removeMouseListener(MouseListener l) {
        mouseListeners.remove(l);
    }

    public void dispatchEvent(MouseEvent e) {
        //clone mouse listeners so that nothing gets wonky if somebody
        //adds or removes a mouse listener during an event dispatch
        List<MouseListener> tmpMouse = new LinkedList<MouseListener>(mouseListeners);

        for(MouseListener l : tmpMouse) {
            int id = e.getID();
            switch(id) {
                case MouseEvent.MOUSE_CLICKED: l.mouseClicked(e); break;
                case MouseEvent.MOUSE_ENTERED: l.mouseEntered(e); break;
                case MouseEvent.MOUSE_EXITED: l.mouseExited(e); break;
                case MouseEvent.MOUSE_PRESSED: l.mousePressed(e); break;
                case MouseEvent.MOUSE_RELEASED: l.mouseReleased(e); break;
            }
        }

        List<MouseMotionListener> tmpMouseMotion = 
                new LinkedList<MouseMotionListener>(mouseMotionListeners);
        
        for(MouseMotionListener l : tmpMouseMotion) {
            int id = e.getID();
            switch(id) {
                case MouseEvent.MOUSE_MOVED: l.mouseMoved(e); break;
                case MouseEvent.MOUSE_DRAGGED: l.mouseDragged(e); break;
            }
        }
    }

    /*
     * returns this LightComponent if it contains p and null otherwise
     */
    public Collection<LightPair> getComponentsContaining(Point p) {
        if(this.contains(p)) {
            return Arrays.asList(new LightPair(this,p));
        } else {
            return null;
        }
    }

    public void paint(Graphics2D g, Rectangle r) {
        paint(g);
    }

    protected void setRepaintManager(LightRepaintManager mgr) {
        this.mgr = mgr;
    }

    protected LightRepaintManager getRepaintManager() {
        return mgr;
    }

    public void repaint() {
        if(mgr!=null)
            mgr.repaint(this);
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(int height) {
        this.height = height;
    }

    public void setBounds(int x, int y, int width, int height) {
        setLocation(x, y);
        setSize(width, height);
    }

    /**
     * @return the x
     */
    public int getX() {
        return x;
    }

    /**
     * @param x the x to set
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    public int getY() {
        return y;
    }

    /**
     * @param y the y to set
     */
    public void setY(int y) {
        this.y = y;
    }

    public void setLocation(int x, int y) {
        setX(x);
        setY(y);
    }

    public void setSize(int width, int height) {
        setWidth(width);
        setHeight(height);
    }

    public void setSize(Dimension d) {
        setSize(d.width, d.height);
    }

    public Dimension getSize() {
        return new Dimension(getWidth(), getHeight());
    }

    public boolean contains(Point p) {
        return p.x>=0 && p.y>=0
                &&p.x<getWidth()&&p.y<getHeight();
    }

    /**
     * @return the parent
     */
    public LightContainer getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     */
    protected void setParent(LightContainer parent) {
        this.parent = parent;
    }

    public void setToolTipText(String text) {
        toolTip=text;
    }

    public boolean hasToolTip() {
        return toolTip!=null;
    }

    public String getToolTipText() {
        return toolTip;
    }
}
