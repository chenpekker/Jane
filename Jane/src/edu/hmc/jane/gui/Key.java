/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.gui;

import edu.hmc.jane.gui.light.LightLabel;
import edu.hmc.jane.gui.light.LightPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.LinkedList;

/**
 *
 * @author Kevin
 * @modified by amichaud to include polytomy information and to 
 * refer to host switch as duplication and host switch
 */
public class Key extends LightPanel {
    LinkedList<KeyElement> events;
    LinkedList<KeyElement> colors;
    LinkedList<KeyElement> polytomyLines;

    // The dimensions necessary to display all objects in the key.
    private int minimumWidth;
    private int minimumHeight;

    private int firstColumnWidth;
    private int secondColumnWidth;

    private static final int baseXOffset = 15;
    private static final int baseYOffset = 15;
    private static final int columnSeparation = 50;
    private static final int nodeVerticalSpace = 25;
    private static final int lineVerticalSpace = 50;
    private static final int colorTextVerticalSpace = 25;

    // Holds an element of the key, including how it is displayed and its description.
    private static class KeyElement extends LightPanel {
        private Key parent;
        protected static final int COSPECIATION = 0;
        protected static final int DUPLICATION = 1;
        protected static final int DUPLICATION_HOST_SWITCH = 2;
        protected static final int LOSS = 3;
        protected static final int FAILURE_TO_DIVERGE = 4;
        protected static final int GREEN = 5;
        protected static final int ORANGE = 6;
        protected static final int RED = 7;
        protected static final int HOST_POLYTOMY = 8;
        protected static final int PARASITE_POLYTOMY = 9;
        private int type;
        private LightLabel colorNameText;
        private LightLabel descriptionText;

        private static final int circleDiameter = 5; // Must be odd and at least 2.
        private static final int lineHeight = 40;
        private static final int textXOffset = 20;
        private static final int circleTextYOffset = (circleDiameter - 17) / 2;
        private static final int lineTextYOffset = lineHeight / 2 - 10;
        private static final int colorTextYOffset = 45 + textXOffset; // 45 is the width of 'Orange', the largest label.
        private static final boolean multilineDescriptions = false;
        
        // getting colors from DrawingObjects so code is consistent
        private static final Color hostEdgeColor = DrawingObjects.hostEdgeColor;
        private static final Color parasiteEdgeColor = DrawingObjects.parasiteEdgeColor;
        private static final Color betterPlacementsColor = DrawingObjects.betterSolutions;
        private static final Color equalPlacementsColor = DrawingObjects.equalSolutions;
        private static final Color worstPlacementsColor = DrawingObjects.worseSolutions;
        private static final Color hostPolytomyColor = DrawingObjects.hostPolytomyColor;
        private static final Color parasitePolytomyColor = DrawingObjects.parasitePolytomyColor;

        private KeyElement (Key parent, int type, int x, int y) {
            super();
            this.parent = parent;
            this.type = type;
            this.setX(x);
            this.setY(y);

            switch (type) {
                case COSPECIATION:
                    descriptionText = new LightLabel("Cospeciation");
                    descriptionText.setLocation(x + textXOffset, y + circleTextYOffset);
                    break;
                case DUPLICATION:
                    descriptionText = new LightLabel("Duplication");
                    descriptionText.setLocation(x + textXOffset, y + circleTextYOffset);
                    break;
                case DUPLICATION_HOST_SWITCH:
                    descriptionText = new LightLabel("<html>Duplication and<br>Host Switch</html>");
                    descriptionText.setLocation(x + textXOffset, y + lineTextYOffset);
                    break;
                case LOSS:
                    descriptionText = new LightLabel("Loss");
                    descriptionText.setLocation(x + textXOffset, y + lineTextYOffset);
                    break;
                case FAILURE_TO_DIVERGE:
                    descriptionText = new LightLabel("Failure to Diverge");
                    descriptionText.setLocation(x + textXOffset, y + lineTextYOffset);
                    break;
                case GREEN:
                    colorNameText = new LightLabel("Green");
                    if (multilineDescriptions) descriptionText = new LightLabel("<html>Better<br>placement<br>exists</html>");
                    else descriptionText = new LightLabel("Better placement exists");
                    colorNameText.setForeground(betterPlacementsColor);
                    descriptionText.setForeground(hostEdgeColor);
                    colorNameText.setLocation(x, y);
                    descriptionText.setLocation(x + colorTextYOffset, y);
                    break;
                case ORANGE:
                    colorNameText = new LightLabel("Orange");
                    if (multilineDescriptions) descriptionText = new LightLabel("<html>Equally<br>good<br>placement<br>exists</html>");
                    else descriptionText = new LightLabel("Equally good placement exists");
                    colorNameText.setForeground(equalPlacementsColor);
                    descriptionText.setForeground(hostEdgeColor);
                    colorNameText.setLocation(x, y);
                    descriptionText.setLocation(x + colorTextYOffset, y);
                    break;
                case RED:
                    colorNameText = new LightLabel("Red");
                    if (multilineDescriptions) descriptionText = new LightLabel("<html>All<br>other<br>placements<br>worse</html>");
                    else descriptionText = new LightLabel("All other placements worse");
                    colorNameText.setForeground(worstPlacementsColor);
                    descriptionText.setForeground(hostEdgeColor);
                    colorNameText.setLocation(x, y);
                    descriptionText.setLocation(x + colorTextYOffset, y);
                    break;
                case HOST_POLYTOMY:
                    descriptionText = new LightLabel("Host Tree Polytomy");
                    descriptionText.setLocation(x + textXOffset, y + lineTextYOffset);
                    break;
                case PARASITE_POLYTOMY:
                    descriptionText = new LightLabel("Parasite Tree Polytomy");
                    descriptionText.setLocation(x + textXOffset, y + lineTextYOffset);
                    break;
                default:
                    break;
            }

            parent.add(descriptionText);
            if (colorNameText != null) {
                parent.add(colorNameText);
            }
            
        }

        @Override
        public void paint(Graphics2D g) {
            // Extra offset from where g's clipping starts. 
            int x = 0; 
            int y = 0;
            g.setClip(this.getParent().getX()-10, this.getParent().getY(), this.getParent().getWidth(), this.getParent().getHeight());
            switch (type) {
                case COSPECIATION:
                    // this doesn't refer to the color constant for host edges because
                    // it doesn't necessarily need to be the same color as the host edge.
                    g.setColor(Color.BLACK);
                    g.fillOval(x, y, circleDiameter, circleDiameter);
                    g.setColor(Color.WHITE);
                    g.fillOval(x+1, y+1, circleDiameter-2, circleDiameter-2);
                    break;
                case DUPLICATION:
                    g.setColor(Color.BLACK);
                    g.fillOval(x, y, circleDiameter, circleDiameter);
                    break;
                case DUPLICATION_HOST_SWITCH:
                    g.setColor(parasiteEdgeColor);
                    g.drawLine(x, y, x, lineHeight-5);
                    // the circle is drawn last to ensure it covers the line
                    // and not the other way around
                    double circleRad = circleDiameter / 2;
                    g.setColor(Color.BLACK);
                    g.fill(new Ellipse2D.Double(x - circleRad, y, circleDiameter, circleDiameter));
                    int space = 8;
                    g.setColor(parasiteEdgeColor);
                    g.drawLine(x, lineHeight / 2, x - space / 2, lineHeight / 2 - space);
                    g.drawLine(x, lineHeight / 2, x + space / 2, lineHeight / 2 - space);
                    break;
                case LOSS:
                    g.setColor(parasiteEdgeColor);
                    for (int i = 0; i <= lineHeight; i += 10) {
                        g.fillRect(x, i, 2, Math.min(lineHeight - i, 5));
                    }
                    break;
                case FAILURE_TO_DIVERGE:
                    g.setColor(parasiteEdgeColor);
                    boolean swap = false;
                    for (int i = 0; i <= lineHeight; i += 3) {
                        if (!swap) g.drawLine(x, i, x+3, Math.min(lineHeight, i + 3));
                        else g.drawLine(x+3, i, x, Math.min(lineHeight, i + 3));
                        swap = !swap;
                    }
                    break;
                case HOST_POLYTOMY: 
                    g.setColor(hostPolytomyColor);
                    g.setStroke(new BasicStroke(1.5f));
                    g.drawLine(x, 0, x, lineHeight);
                    break;
                case PARASITE_POLYTOMY:
                    g.setColor(parasitePolytomyColor);
                    g.setStroke(new BasicStroke(0.5f));
                    g.drawLine(x, 0, x, lineHeight);
                    break;
                default:
                    break;
            }
        }

        protected int getMinimumWidth() {
            int minWidth = 0;
            if (colorNameText != null) minWidth += colorNameText.getWidth();
            return minWidth + baseXOffset + textXOffset + descriptionText.getWidth();
        }

        protected int getTextHeight() {
            return descriptionText.getHeight();
        }
    }

    public Key () {
        super();
        initComponents();
    }

    private void initComponents() {
        events = new LinkedList<KeyElement>();
        colors = new LinkedList<KeyElement>();
        polytomyLines = new LinkedList<KeyElement>();
        int firstColumnHeight = 2 * baseYOffset + 2 * nodeVerticalSpace + 3 * lineVerticalSpace;

        // Add the events for the first column.
        int yOffset = baseYOffset;
        KeyElement cosp = new KeyElement(this, KeyElement.COSPECIATION, baseXOffset, yOffset);
        KeyElement dup = new KeyElement(this, KeyElement.DUPLICATION, baseXOffset, yOffset += nodeVerticalSpace);
        KeyElement hs = new KeyElement(this, KeyElement.DUPLICATION_HOST_SWITCH, baseXOffset + 2, yOffset += nodeVerticalSpace);
        KeyElement loss = new KeyElement(this, KeyElement.LOSS, baseXOffset + 2, yOffset += lineVerticalSpace);
        KeyElement ftd = new KeyElement(this, KeyElement.FAILURE_TO_DIVERGE, baseXOffset + 1, yOffset += lineVerticalSpace);
        events.add(cosp);
        events.add(dup);
        events.add(hs);
        events.add(loss);
        events.add(ftd);
        for (KeyElement event : events) {
            this.add(event);
        }

        for (KeyElement event : events) {
            firstColumnWidth = Math.max(firstColumnWidth, event.getMinimumWidth());
        }

        // Add the colors for the second column's top half
        yOffset = baseYOffset + KeyElement.circleTextYOffset;
        KeyElement green = new KeyElement(this, KeyElement.GREEN, firstColumnWidth + columnSeparation, yOffset);
        KeyElement orange = new KeyElement(this, KeyElement.ORANGE, firstColumnWidth + columnSeparation, yOffset += green.getTextHeight() + colorTextVerticalSpace);
        KeyElement red = new KeyElement(this, KeyElement.RED, firstColumnWidth + columnSeparation, yOffset += orange.getTextHeight() + colorTextVerticalSpace);
        yOffset += red.getTextHeight() + colorTextVerticalSpace;
        int secondColumnHeight = yOffset;
        colors.add(green);
        colors.add(orange);
        colors.add(red);
        for (KeyElement color : colors) {
            this.add(color);
        }

        for (KeyElement color : colors) {
            secondColumnWidth = Math.max(secondColumnWidth, color.getMinimumWidth());
        }
        
        // adding the lines for the second column's bottom half.
        KeyElement hostPolytomy = new KeyElement(this, KeyElement.HOST_POLYTOMY, firstColumnWidth + columnSeparation, yOffset);
        KeyElement parasitePolytomy = new KeyElement(this, KeyElement.PARASITE_POLYTOMY, firstColumnWidth + columnSeparation, yOffset += lineVerticalSpace);
        yOffset += parasitePolytomy.getTextHeight() + lineVerticalSpace;
        secondColumnHeight = yOffset;
        polytomyLines.add(hostPolytomy);
        polytomyLines.add(parasitePolytomy);
        for (KeyElement polytomyLine : polytomyLines) {
            this.add(polytomyLine);
        }
        
        for (KeyElement polytomyLine : polytomyLines) {
            secondColumnWidth = Math.max(secondColumnWidth, polytomyLine.getMinimumWidth());
        }

        this.minimumHeight = 15 + Math.max(firstColumnHeight, secondColumnHeight);
        this.minimumWidth = 10 + firstColumnWidth + secondColumnWidth + columnSeparation;
    }

    @Override
    public void paintOverlay(Graphics2D g) {
        // Draw a dividing line to separate events from colors.
        int midPoint = firstColumnWidth + columnSeparation / 2;
        g.setColor(Color.BLACK);
        g.drawLine(midPoint, this.getY(), midPoint, this.getY() + this.getHeight());
        // draw a second line to separate colors from polytomy info
        g.setColor(Color.BLACK);
        int dividerPoint = this.getHeight() / 2;
        g.drawLine(midPoint, dividerPoint, this.getWidth(), dividerPoint);
    }

    public int getMinimumWidth() {
        return minimumWidth;
    }

    public int getMinimumHeight() {
        return minimumHeight;
    }
}
