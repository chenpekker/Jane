/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.gui.light;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import javax.swing.JLabel;

/**
 *
 * @author John
 */
public class LightLabel extends LightComponent {
    private JLabel label;
    private String text;

    public LightLabel(String text) {
        // this field is added so the text can be pulled out by another class.
        this.text = text;
        label = new JLabel(text);
        setSize(getPreferredSize());
    }

    @Override
    public void paint(Graphics2D g) {
        label.paint(g);
    }

    @Override
    public void setWidth(int width) {
        label.setSize(new Dimension(width, getHeight()));
        super.setWidth(width);
    }

    @Override
    public void setHeight(int height) {
        label.setSize(new Dimension(getWidth(), height));
        super.setHeight(height);
    }
    
    public void setText(String text){
        label.setText(text);
    }

    public void setTextSize(int size){
        Font curFont = label.getFont();
        label.setFont(new Font(curFont.getFontName(), curFont.getStyle(), size));
    }
    
    public void setForeground(Color fg) {
        label.setForeground(fg);
    }

    public final Dimension getPreferredSize() {
        return label.getPreferredSize();
    }

    public void setOpaque(boolean opaque) {
        label.setOpaque(opaque);
    }

    public boolean isOpaque() {
        return label.isOpaque();
    }
    
    // This method gives the text shown by the label. It's used to pull this text
    // to store in the eps code if the user wants EPS/PDF output.
    public String getText() {
        return this.text;
    }
}
