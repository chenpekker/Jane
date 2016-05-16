/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import javax.swing.*;

/**
 *
 * @author amichaud
 */
public class ResizeableImagePanel extends JPanel{
    
    private BufferedImage original;
    private BufferedImage current;
    private Component comp;
    private Graphics2D originalG;
    private static int COLOR = BufferedImage.TYPE_INT_ARGB;
    private double xScale;
    private double yScale;
    private AffineTransform transformer;
    private AffineTransformOp scaleOp;

    // constructor if the default size of the component is fine
    ResizeableImagePanel(Component c) {
        super();
        // initializing variables.
        comp = c;
        original = new BufferedImage(c.getWidth(), c.getHeight(), COLOR);
        originalG = original.createGraphics();
        comp.paintAll(originalG);
        this.setSize(new Dimension(c.getWidth(), c.getHeight()));
        setOpaque(false);
        originalG.dispose();
    }
    
    // constructor if we need the panel set to a certain size (used for the
    // image preview).
    ResizeableImagePanel(Component c, Dimension d) {
        super();
        // initializing variables.
        comp = c;
        original = new BufferedImage(c.getWidth(), c.getHeight(), COLOR);
        originalG = original.createGraphics();
        comp.paintAll(originalG);
        this.setSize(d);
        setOpaque(false);
        originalG.dispose();
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        // we only need to scale the image if its dimensions are different than 
        // the window's current dimensions.
        if (getWidth() != comp.getWidth() || getHeight() != comp.getHeight()) {
            current = new BufferedImage(getWidth(), getHeight(), COLOR);
            xScale = getWidth() / (double)original.getWidth();
            yScale = getHeight() / (double)original.getHeight();
            transformer = new AffineTransform();
            transformer.scale(xScale, yScale);
            scaleOp = new AffineTransformOp(transformer, AffineTransformOp.TYPE_BILINEAR);
            current = scaleOp.filter(original, current);
            g.drawImage(current, 0, 0, null);
        }
        else {
            g.drawImage(original, 0, 0, null);
        }
    }  
}
