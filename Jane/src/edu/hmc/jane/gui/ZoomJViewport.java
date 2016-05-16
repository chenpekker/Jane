/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JComponent;
import javax.swing.JViewport;

/**
 *
 * @author John
 */
class ZoomJViewport extends JViewport 
        implements StatelessMouseEventListener,
        MouseMotionListener, MouseWheelListener {

    boolean autoScroll=false;
    double scale = 1.0;
    JComponent view;
    private double relativeCenterX = 0;
    private double relativeCenterY = 0;

    private final double bufferWidth = 5;

    private int lastWidth;
    private int lastHeight;

    ZoomJViewport(JComponent view) {
        super();
        this.view = view;
        setView(view);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        enableAutoScroll();
        setLayout(null);
        setOpaque(false);
        OverlappingMouseEventDispatcher.register(this);
    }

    public void disableAutoScroll() {
        this.autoScroll = false;
    }

    public final void enableAutoScroll() {
        this.autoScroll = true;
    }

    public void mouseDragged(MouseEvent evt) {
        autoScroll(evt);
        evt.consume();
    }

    public void mouseMoved(MouseEvent evt) {
        autoScroll(evt);
        evt.consume();
    }

    public void autoScroll(MouseEvent evt) {
        if (autoScroll) {
            double leftX;
            double leftY;

            if(evt.getX() <= bufferWidth)
                leftX = 0;
            else if (evt.getX()>=getWidth() - bufferWidth)
                leftX = getViewSize().width - getWidth();
            else {
                double portWidth = Math.abs(getWidth()-2*bufferWidth);
                double evtX = Math.abs(evt.getX()-bufferWidth);
                double percentX = evtX / portWidth;
                double viewWidth = getViewSize().width;
                leftX = percentX*(viewWidth-portWidth);
            }

            if (evt.getY()<=bufferWidth)
                leftY=0;
            else if (evt.getY()>=getHeight()-bufferWidth)
                leftY=getViewSize().height - getHeight();
            else {
                double portHeight = Math.abs(getHeight()-2*bufferWidth);
                double evtY = Math.abs(evt.getY()-bufferWidth);
                double percentY = evtY / portHeight;
                double viewHeight = getViewSize().height;
                leftY = percentY*(viewHeight - portHeight);
            }

            setViewPosition((int) leftX, (int) leftY);
        }
    }

    public void mouseWheelMoved(MouseWheelEvent evt) {
        if(autoScroll) {
            scale*=(1d+(-1d)*evt.getUnitsToScroll()/20d);

            if(scale<1) {
                scale=1;
            }
            rescaleView();
        }
    }

    @Override
    public void paintChildren(Graphics g) {
        rescaleView();
        super.paintChildren(g);
    }

    protected void rescaleView() {
        if(getWidth()!=lastWidth || getHeight()!=lastHeight) {
            int width = getWidth();
            int height = getHeight();
            setViewSize(new Dimension((int) (scale*width), (int) (scale*height)));
            maintainRelativePosition();
        }

    }

    protected void maintainRelativePosition() {
        setRelativeCenterViewPosition(getRelativeCenterX(), getRelativeCenterY());
    }

    protected void setRelativeCenterViewPosition(double x, double y) {
        setRelativeCenterX(x);
        setRelativeCenterY(y);
        
        Point p = new Point(relativeCenterToLeftCornerX(x), relativeCenterToLeftCornerY(y));

        setViewPosition(p);
    }

    protected void setViewPosition(int x, int y) {
        setViewPosition(new Point(x,y));
    }

    @Override
    public void setViewPosition(Point p) {
        if(getViewSize().width!=0 && getViewSize().height!=0) {
            setRelativeCenterX(leftCornerToRelativeCenterX(p.x));
            setRelativeCenterY(leftCornerToRelativeCenterY(p.y));
        }

        super.setViewPosition(correctBounds(p));
    }

    protected int leftCornerToCenterX(int x) {
        return x + getWidth()/2;
    }

    protected int leftCornerToCenterY(int y) {
        return y+getHeight()/2;
    }

    protected double centerToRelativeCenterY(int y) {
        double yd = y;
        double hd = getViewSize().height;

        return yd/hd;
    }

    protected double centerToRelativeCenterX(int x) {
        double xd = x;
        double wd = getViewSize().width;

        return xd/wd;
    }

    protected double leftCornerToRelativeCenterX(int x) {
        return centerToRelativeCenterX(leftCornerToCenterX(x));
    }

    protected double leftCornerToRelativeCenterY(int y) {
        return centerToRelativeCenterY(leftCornerToCenterY(y));
    }

    protected int centerToLeftCornerX(int x) {
        return x - getWidth()/2;
    }

    protected int centerToLeftCornerY(int y) {
        return y-getHeight()/2;
    }

    protected int relativeCenterToCenterY(double y) {
        return (int) (y*getViewSize().height);
    }

    protected int relativeCenterToCenterX(double x) {
        return (int) (x*getViewSize().width);
    }

    protected int relativeCenterToLeftCornerX(double x) {
        return centerToLeftCornerX(relativeCenterToCenterX(x));
    }

    protected int relativeCenterToLeftCornerY(double y) {
        return centerToLeftCornerY(relativeCenterToCenterY(y));
    }

    private Point correctBounds(Point p) {
        int viewWidth = getViewSize().width;
        int viewHeight = getViewSize().height;

        p = new Point(p);
        
        if(p.x+getWidth() > viewWidth) {
            p.x = viewWidth-getWidth();
        } else if (p.x<0) {
            p.x = 0;
        }

        if (p.y+getHeight() > viewHeight) {
            p.y = viewHeight - getHeight();
        } else if (p.y<0) {
            p.y=0;
        }

        return p;
    }

    /**
     * @return the relativeCenterX
     */
    public double getRelativeCenterX() {
        return relativeCenterX;
    }

    /**
     * @param relativeCenterX the relativeCenterX to set
     */
    public void setRelativeCenterX(double relativeCenterX) {
        if(relativeCenterX>1) {
            relativeCenterX=1;
        } else if(relativeCenterX<0) {
            relativeCenterX = 0;
        }

        this.relativeCenterX = relativeCenterX;
    }

    /**
     * @return the relativeCenterY
     */
    public double getRelativeCenterY() {
        return relativeCenterY;
    }

    /**
     * @param relativeCenterY the relativeCenterY to set
     */
    public void setRelativeCenterY(double relativeCenterY) {
        if(relativeCenterY>1) {
            relativeCenterY=1;
        } else if(relativeCenterY<0) {
            relativeCenterY = 0;
        }

        this.relativeCenterY = relativeCenterY;
    }
}
