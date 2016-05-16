/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.gui;

/**
 *
 * @author andrew
 */
// This class defines an object that can be drawn on a graphics, graphics2D, 
// or epsGraphics object.  
public final class GraphicsShape {
    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;
    private final String type;
    private final String text;
    private final char tree;
    
    // Constructor for object with no text.
    public GraphicsShape(final double x1, final double y1, final double x2, final double y2, String t, final char a) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        type = t;
        text = "";
        tree = a;
    }
    
    // Constructor for text string.
    public GraphicsShape(final double xCor, final double yCor, String string, String contents, final char a) {
        x1 = xCor;
        y1 = yCor;
        x2 = 0;
        y2 = 0;
        type = string;
        text = contents; 
        tree = a;
    }
    
    @Override
    public boolean equals(Object other) {
        boolean result = false;
        if (other instanceof GraphicsShape) {
        GraphicsShape that = (GraphicsShape) other;
        result = (this.getX1() == that.getX1() && this.getY1() == that.getY1() &&
                  this.getX2() == that.getX2() && 
                  this.getY2() == that.getY2() && 
                  this.getType().equals(that.getType()) &&
                  this.getText().equals(that.getText()) && 
                  this.getTree() == that.getTree());  
        }
        return result;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + (int) (Double.doubleToLongBits(this.x1) ^ (Double.doubleToLongBits(this.x1) >>> 32));
        hash = 11 * hash + (int) (Double.doubleToLongBits(this.y1) ^ (Double.doubleToLongBits(this.y1) >>> 32));
        hash = 11 * hash + (int) (Double.doubleToLongBits(this.x2) ^ (Double.doubleToLongBits(this.x2) >>> 32));
        hash = 11 * hash + (int) (Double.doubleToLongBits(this.y2) ^ (Double.doubleToLongBits(this.y2) >>> 32));
        hash = 11 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 11 * hash + (this.text != null ? this.text.hashCode() : 0);
        hash = 11 * hash + this.tree;
        return hash;
    }
    
    // Getters for all of the fields.  Some are useful, some aren't and are there
    // just in case.
    public double getX1() {
        return x1;
    }
    
    public double getY1() {
        return y1;
    }
    
    public double getX2() {
        return x2;
    }
    
    public double getY2() {
        return y2;
    }
    
    public String getType() {
        return type;
    }
    
    public String getText() {
        return text;
    }
    
    public char getTree() {
        return tree;
    }   
}
