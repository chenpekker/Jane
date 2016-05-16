/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.gui.light;

import java.awt.Point;

/**
 *
 * @author John
 */
public class LightPair {
    public LightComponent box;
    public Point p;

    public LightPair(LightComponent box, Point p) {
        this.box=box;
        this.p=p;
    }
}
