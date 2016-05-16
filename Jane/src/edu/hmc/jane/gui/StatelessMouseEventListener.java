/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.gui;

/**
 * Marker interface that designates a class that does not care greatly if
 * it does not receive every single mouse event that happens (ie. if some are
 * dropped). The contract for this interface is that the implementing class
 * must be also implement either MouseMotionListener, MouseListener, or
 * MouseWheelListener, and that any methods it implements from these interfaces
 * must function correctly even if they do not receive every qualifying event.
 * Also, implementing classes must be able to properly handle a scenario in which
 * an event is sent even if nothing has changed (ie. a MouseMotionListener receiving
 * a mouse event even if the mouse has not moved, or a MouseListener receiving
 * a mouse entered event, even if the mouse was already in the bounds of the object.)
 * Finally, the contract requires that the implementor is a Component.
 * @author John
 */
public interface StatelessMouseEventListener {}
