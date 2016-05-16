/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.gui.light;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 *
 * @author John
 */
public class LightPanel extends LightContainer {
    private Color backgroundColor;
    private boolean opaque=false;

    @Override
    public void paintBackground(Graphics2D g) {
        if(isOpaque()&&backgroundColor!=null) {
            g.setColor(backgroundColor);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    @Override
    public void paintOverlay(Graphics2D g) {}

    public void setBackground(Color color) {
        backgroundColor = color;
    }

    public Color getBackground() {
        return backgroundColor;
    }

    /**
     * @return the opaque
     */
    public boolean isOpaque() {
        return opaque;
    }

    /**
     * @param opaque the opaque to set
     */
    public void setOpaque(boolean opaque) {
        this.opaque = opaque;
    }
}
