/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.gui.light;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.RenderingHints;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;

/**
 *
 * @author John
 */
public class JLightAdapter extends JPanel implements LightRepaintManager, MouseListener, MouseMotionListener {
    private LightComponent contentPane;

    private Collection<LightComponent> lastMouseContainers=
            new HashSet<LightComponent>();

    private int oldWidth;
    private int oldHeight;

    public JLightAdapter(LightComponent contentPane) {
        this.contentPane = contentPane;
        
        setDoubleBuffered(true);

        addMouseMotionListener(this);
        addMouseListener(this);

        setPreferredSize(contentPane.getSize());

        setOpaque(false);

        contentPane.setRepaintManager(this);
        contentPane.repaint();
    }

    private void resizeInternals(int width, int height) {
        if (width != oldWidth || height != oldHeight) {
            oldWidth = width;
            oldHeight = height;
            contentPane.setSize(width, height);
        }
    }

    private void resizeInternals() {
        resizeInternals(getWidth(), getHeight());
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        resizeInternals();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        contentPane.paint(g2);
    }

    /**
     * @return the contentPane
     */
    public LightComponent getContentPane() {
        return contentPane;
    }

    /**
     * @param contentPane the contentPane to set
     */
    public void setContentPane(LightComponent contentPane) {
        this.contentPane = contentPane;
    }

    public void repaint(LightComponent c) {
        repaint();
    }

    public void mouseClicked(MouseEvent e) {
        send(e);
    }

    public void mousePressed(MouseEvent e) {
        send(e);
    }

    public void mouseReleased(MouseEvent e) {
        send(e);
    }

    //handled in mouseMoved
    public void mouseEntered(MouseEvent e) {}

    //handled in mouseMoved
    public void mouseExited(MouseEvent e) {}

    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    public void mouseMoved(MouseEvent e) {
        Collection<LightPair> mouseContainers = contentPane.getComponentsContaining(e.getPoint());
        handleToolTips(e, mouseContainers);
        handleEnteredAndExited(e, mouseContainers);
        send(e, mouseContainers);
    }

    private void send(MouseEvent e) {
        Collection<LightPair> receivers = contentPane.getComponentsContaining(e.getPoint());
        send(e, receivers);
    }

    /*
     * sends e to all receivers
     */
    private void send(MouseEvent e, Collection<LightPair> receivers) {
        if(receivers!=null) {
            for(LightPair receiver : receivers) {
                MouseEvent dispatchEvent = new MouseEvent(e.getComponent(),
                    e.getID(), e.getWhen(), e.getModifiers(),
                    receiver.p.x, receiver.p.y, e.getClickCount(),
                    e.isPopupTrigger()
                );
                receiver.box.dispatchEvent(e);
            }
        }
    }

    private void sendMouseEntered(LightComponent c, Point p) {        
        MouseEvent evt = new MouseEvent(this, MouseEvent.MOUSE_ENTERED,
                System.currentTimeMillis(), 0, p.x, p.y, 0, false);

        c.dispatchEvent(evt);
    }

    private void sendMouseExited(LightComponent c, Point p) {
        if (p == null)
            p=new Point(0,0);

        MouseEvent evt = new MouseEvent(this, MouseEvent.MOUSE_EXITED,
                System.currentTimeMillis(), 0, p.x, p.y, 0, false);

        c.dispatchEvent(evt);
    }

    //this code is horribly innefficient (sp?), but it works fine
    private void handleEnteredAndExited(MouseEvent e, Collection<LightPair> receivers) {

        if (receivers!=null) {
            List<LightPair> receiversCol = new LinkedList<LightPair>(receivers);
            List<LightComponent> receiversComps = new LinkedList<LightComponent>();

            for (LightPair receiver : receiversCol) {
                receiversComps.add(receiver.box);
            }

            //compute symmetric difference of two sets (all items in one set but not the other)
            Set<LightComponent> diff = new HashSet<LightComponent>(lastMouseContainers);
            diff.addAll(receiversComps);
            Set<LightComponent> union = new HashSet<LightComponent>(lastMouseContainers);
            union.retainAll(receiversComps);
            diff.removeAll(union);

            for(LightComponent c : diff) {
                if(lastMouseContainers.contains(c))
                    sendMouseExited(c, null);
                else {
                    sendMouseEntered(c,receiversCol.get(receiversComps.indexOf(c)).p);
                }
            }
            lastMouseContainers=receiversComps;
        }
    }

    private void handleToolTips(MouseEvent e, Collection<LightPair> tippers) {
        if(tippers!=null) {
            for(LightPair tipper : tippers) {
                if(tipper.box.hasToolTip()) {
                    setToolTipText(tipper.box.getToolTipText());
                    return;
                }
            }
            setToolTipText(null);
        }
    }
}
