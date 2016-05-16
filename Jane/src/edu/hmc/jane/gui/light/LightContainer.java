/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.gui.light;

import java.awt.Graphics2D;
import java.util.Collection;
import java.util.LinkedList;
import java.awt.Point;

/**
 *
 * @author John
 */
public abstract class LightContainer extends LightComponent {
    private Collection<LightComponent> children=new LinkedList<LightComponent>();

    public void add(LightComponent component) {
        component.setRepaintManager(getRepaintManager());
        component.setParent(this);
        children.add(component);
    }

    public void remove(LightComponent component) {
        children.remove(component);
    }

    public void paint(Graphics2D g) {
        paintBackground(g);
        paintChildren(g);
        g.setClip(0, 0, getWidth(), getHeight());
        paintOverlay(g);
    }

    @Override
    public Collection<LightPair> getComponentsContaining(Point p) {
        if(this.contains(p)) {
            Collection<LightPair> childBoxes = getChildrenContaining(p);
            if(childBoxes!=null) {
                childBoxes.addAll(super.getComponentsContaining(p));
                return childBoxes;
            } else{
                return super.getComponentsContaining(p);
            }
        } else {
            return null;
        }
    }

    public Collection<LightPair> getChildrenContaining(Point p) {
        LinkedList<LightPair> boxes=new LinkedList<LightPair>();
        for(LightComponent child : children) {
            Point pChild = p.getLocation();
            pChild.translate(-child.getX(), -child.getY());
            Collection<LightPair> pairs = child.getComponentsContaining(pChild);
            if(pairs!=null)
                boxes.addAll(pairs);
        }
        
        if(boxes.size()>0) {
            return boxes;
        } else {
            return null;
        }
    }

    @Override
    public void setRepaintManager(LightRepaintManager c) {
        for(LightComponent comp : children) {
            comp.setRepaintManager(c);
        }
        super.setRepaintManager(c);
    }

    public abstract void paintBackground(Graphics2D g);

    public void paintChildren(Graphics2D g) {
        for(LightComponent component : children) {
            int cX = component.getX();
            int cY = component.getY();
            g.translate(cX, cY);
            g.setClip(0,0,component.getWidth(),component.getHeight());
            component.paint(g);
            g.translate(-cX, -cY);
        }
    }
    
    public abstract void paintOverlay(Graphics2D g);
}
