/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.gui;

import edu.hmc.jane.Stats;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.TreeMap;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

/**
 *
 * @author amichaud
 */
public class PValueHistogram extends JPanel{
    
    // how far the histogram axes are offset from the sides of the panel.
    private final int AXIS_OFFSET = 50;
    private int X_LEFT_OFFSET = 70;
    private int TICSIZE = 5;
    private int AXIS_BUFFER = 10;
    // labels for the histogram.
    // TODO comment this too
    private final String TITLE = "p-values of Different Cost Combinations";
    private final String X_LABEL = "p-value";
    private final String Y_LABEL = "Percent";
    private final int TITLE_POS = 20;
    private int X_LABEL_POS;
    private int Y_LABEL_POS = 10;
    // the p-values that are always drawn on the histogram and are set off from the other values
    private ArrayList<Integer> SPECIAL_P_VALUES = new ArrayList<Integer>();
    private static int COMBINATIONS;
    // these store the ends of both axes.
    private int lowerY, rightX;
    private int upperY = AXIS_OFFSET;
    private int leftX = X_LEFT_OFFSET;
    // how long the axes are, used to figure out how many tic marks should
    // be made per axis and what the counts should be (e.g. 1, 2, 3 or 2, 4, 6)
    private int xAxisLength, yAxisLength;
    
    private double xSpacing, ySpacing;
    // the number of columns needed between the minimum and maximum columns
    // (including spaces between columns).
    private int xTics;
    // the maximum count of histogram column
    private int maxCount = 0;
    // the maximum and minimum p-counts found in the tree.
    double min, max;
    double slotSpacing;
    // the maximum number of tics allowed on an axis, used to decide on the
    // scale for the tic marks.
    private int MAX_X_TICS = 15;
    private int MAX_Y_TICS = 10;
    // the ratio between xTics and MAX_X_TICS and maxCount and MAX_Y_TICS;
    private double xRatio = 1.0;
    private int yRatio = 1;
    // the treemap containing p-value percentiles as keys, and the number of
    // combinations with that percentile as values
    private TreeMap<Double, Integer> counts;
    // the current set of histogram data, used to find the currently-selected
    // column
    private Stats.HistoData data;
    // the array containing the percent of each column in the histogram, from
    // 0 to 100 (percent is zero if there were no combinations with that p-value)
    private int[] heightArray;
    private Font regularFont, labelFont, titleFont;
    
    public PValueHistogram(Stats.HistoData stats, TreeMap<Double, Integer> map, int width, int height) {
        super();
        COMBINATIONS = 0;
        // adding the special P-values to their arraylist. couldn't find any
        // way to initialize an arraylist with these values, so it has to be done here.
        SPECIAL_P_VALUES.add(1);
        SPECIAL_P_VALUES.add(5);
        SPECIAL_P_VALUES.add(10);
        SPECIAL_P_VALUES.add(100);
        // initializing variables.
        this.setSize(width, height);
        counts = map;
        data = stats;
        min = map.firstKey();
        max = map.lastKey();
        xTics = (int)(max - min + 2);
        heightArray = makeHeightArray();
        setOpaque(false);
        ToolTipManager.sharedInstance().registerComponent(this);
    }
    
    private int[] makeHeightArray() {
        // creating the array to hold the heights.
        int[] answer = new int[xTics];
        for (Integer value : counts.values()) {
            COMBINATIONS += value;
        }
        // looping through all of the percentiles between the min and max
        for (int p = 0; p < xTics; p++) {
            double key = (double)p + min;
            if (counts.containsKey(key)) {
                answer[p] = counts.get(key);
                // we keep checking for larger maxHeights to store, so that
                // when we're done looping we actually do have the maxCount 
                // from the tree.
                if (counts.get(key) > maxCount) maxCount = counts.get(key);
            } else answer[p] = 0;
        }
        return answer;
    }
    
    // calculating the various constants needed to shape the histogram properly.
    private void calculateValues() {
        X_LABEL_POS = getHeight() - AXIS_BUFFER;
        // axis bounds
        lowerY = getHeight() - AXIS_OFFSET - 15;
        rightX = getWidth() - AXIS_OFFSET;
        // axis lengths
        xAxisLength = rightX - leftX;
        yAxisLength = lowerY - upperY;
        // max number of tics per axis (used for scaling)
        MAX_X_TICS = xAxisLength / 30;
        MAX_Y_TICS = yAxisLength / 15;
        slotSpacing = (double)(xAxisLength - AXIS_BUFFER) / xTics;
        rightX += slotSpacing / 2;
        
        if (xTics > MAX_X_TICS) {
            xRatio = Math.ceil( ((double)xTics )/ MAX_X_TICS);
        }
        xSpacing = slotSpacing * (double)xRatio;
        int maxPercent = (int)Math.ceil((double)maxCount / COMBINATIONS * 100);
        if (maxPercent > MAX_Y_TICS) {
            yRatio = (int)Math.ceil((double)maxPercent / MAX_Y_TICS);
        }
        ySpacing = (yAxisLength - AXIS_BUFFER) * (double)yRatio / maxPercent;
    }
    
    // painting the axes, the data, and the titles to the graphics object.
    @Override
    public void paintComponent(Graphics g) {
        calculateValues();
        super.paintComponent(g);
        regularFont = g.getFont();
        String name = regularFont.getFontName();
        labelFont = new Font(name, Font.BOLD, Math.min(14, getHeight() / 15));
        titleFont = new Font(name, Font.BOLD, Math.min(16, getWidth() / 25));
        drawAxes((Graphics2D) g);
        drawSpecialValues((Graphics2D) g);
        drawData((Graphics2D) g);
        drawTitle((Graphics2D) g);
        ToolTipManager.sharedInstance().registerComponent(this);
    }
    
    // returns the position to center an axis label  given character showing
    // which axis it is located on.
    private int getPos(String text, char axis, Graphics2D g) {
        FontMetrics fm = g.getFontMetrics(g.getFont());
        Rectangle2D stringRect = fm.getStringBounds(text, g);
        // getting the length of half of the string (the offset needed
        // to center it)
        double halfString = stringRect.getWidth() / 2.0; 
        int pos;
        switch(axis) {
            case 'X': pos = (int)(xAxisLength / 2.0 - halfString) + AXIS_OFFSET;
                      break;
                
            case 'Y': pos = (int)(yAxisLength / 2.0 - halfString) + AXIS_OFFSET;
                      break;
                
            default:  pos =  (int)(xAxisLength / 2.0 - halfString) + AXIS_OFFSET;
                      break;
        }
        return pos;
    }
    
    // drawing the two axes, scaled to fit the data, onto the graphics object.
    private void drawAxes(Graphics2D g){
        // drawing the x and y axes.
        g.drawLine(leftX, lowerY, leftX, upperY);
        g.drawLine(leftX, lowerY, rightX, lowerY);
        // drawing the axis labels
        // drawing the x label
        g.setFont(labelFont);
        int labelPos = getPos(X_LABEL, 'X', g);
        g.drawString(X_LABEL, labelPos, X_LABEL_POS);
        // drawing the y label.
        // creating a rectangle for sizing of substrings of the Y-LABEL text.
        FontMetrics fm = g.getFontMetrics(labelFont);
        Rectangle2D stringRect;
        // finding the size of the string
        float yPos;
        float labelXPos = lowerY - yAxisLength / 2.0f;
        double half = Y_LABEL.length() / 2.0;
        double offset;
        // rotating.
        g.rotate(-Math.PI / 2, Y_LABEL_POS, lowerY - (yAxisLength / 2.0f));
        // looping over characters
        for (int c = 0; c < Y_LABEL.length(); c++) {
            // converting to strings
            Character curChar = Y_LABEL.charAt(c);
            String charToString = curChar.toString();
            // characters in the first half of the text are shifted downward
            if (c < half) {
                stringRect = fm.getStringBounds(Y_LABEL.substring(c, (int)half), g);
                offset = stringRect.getWidth();
                yPos = Y_LABEL_POS - (float)offset; 
                // coordinates are reversed because the graphics object was
                // rotated 90 degrees left
                g.drawString(charToString, yPos, labelXPos);
            }
            // the center character is drawn in the center.
            else if (c == Y_LABEL.length()) {
                yPos = Y_LABEL_POS;
                g.drawString(charToString, yPos, labelXPos);
            }
            // characters in the last half are shifted upward
            else {
                stringRect = fm.getStringBounds(Y_LABEL.substring((int)half, c), g);
                offset = stringRect.getWidth();
                yPos = Y_LABEL_POS + (float)offset;
                g.drawString(charToString, yPos, labelXPos);
            }
        }
        // rotating back and setting the font to normal for the tic mark labels.
        g.rotate(Math.PI / 2, Y_LABEL_POS, lowerY - (yAxisLength / 2.0f));
        g.setFont(regularFont);
        
        // drawing tic marks on the x-axis
        int denominations = (int)( (double)xAxisLength / xSpacing);
        String label;
        for (int t = 0; t < denominations - 1; t++) {
            int ticLoc = leftX + (int)(slotSpacing + t * xSpacing);
            g.draw(new Line2D.Double(ticLoc, (double)lowerY + TICSIZE, ticLoc, (double)lowerY - TICSIZE));
            double percentDouble = (double)(t * xRatio + min) / 100.0;
            if (!SPECIAL_P_VALUES.contains((int)(t * xRatio + min))) {
                label = Double.toString(percentDouble);
                float stringXPos;
                stringXPos = (float)ticLoc - 5;
                g.drawString(label, stringXPos, (int)(lowerY + 3 * TICSIZE));
            }
        }
        // drawing tic marks on the y-axis
        denominations = (int)( ((double)yAxisLength) / ySpacing);
        // finding the size of the highest tic mark so we know how far to
        // offset the numbers on the y-axis
        double maxTic = denominations * yRatio;
        String maxToString = Double.toString(maxTic);
        double stringWidth = fm.getStringBounds(maxToString, g).getWidth();
        int ticOffset = leftX - (int)Math.ceil(stringWidth + TICSIZE);
        if (denominations * yRatio > 100.0) {
            denominations --;
        }
        for (int t = 0; t <= denominations; t++) {
            int ticLoc = (int)(lowerY - t * ySpacing);
            g.drawLine((int)(leftX - TICSIZE), ticLoc, (int)(leftX + TICSIZE), ticLoc);
            label = Double.toString(t * yRatio);
            g.drawString(label, ticOffset, (float)ticLoc + 3);
        }
    }
       
    // marks the significant p-values (100, 10, 5, and 1) on the axis so that
    // the user can compare results to them.
    private void drawSpecialValues(Graphics2D g) {
        // looping through the four values.  The arrayList is ordered
        // 1, 5, 10, 100.
        FontMetrics fm = g.getFontMetrics(g.getFont());
        Rectangle2D stringRect;
        double offset;
        double stringLoc;
        double ticLoc;
        for (int p = 0; p < SPECIAL_P_VALUES.size(); p++) {
            Double curVal = (double)SPECIAL_P_VALUES.get(p);
            Double decimalVal = curVal / 100.0;
            ticLoc = leftX  + (slotSpacing + (curVal - min) * xSpacing / xRatio);
            stringLoc = leftX  + (slotSpacing + (curVal - min) * xSpacing / xRatio);
            // scaling the value so that the label and mark go in the
            // correct place.
            stringRect = fm.getStringBounds(decimalVal.toString(), g);
            offset = stringRect.getWidth();
            double y1 = lowerY  + (6 * TICSIZE);
            // each value has a different shade of gray.  A lighter shade
            // of gray indicates a smaller p-value.
            switch (p) {
                case 0: g.setColor(Color.LIGHT_GRAY);
                        stringLoc -= offset;
                        break;
                case 1: g.setColor(Color.GRAY);
                        stringLoc -= offset / 2;
                        break;
                case 2: g.setColor(Color.DARK_GRAY);
                        break;
                default: g.setColor(Color.BLACK);
                        break;
            }
            // the bottom point of the line marking the special p-value on
            // the histogram.
            // we only draw special values that will show up on the graph
            // (we ignore ones that will be below the min or above the max.
            if (min <= curVal && max >= curVal) {
                y1 -= TICSIZE;
                int x1 = (int)ticLoc - (int)TICSIZE;
                int x2 = (int)ticLoc + (int)TICSIZE;
                int y2 = (int)y1 - (int)TICSIZE * 2;
                int midX = (x1 + x2) / 2;
                int[] xArray = {x1, midX, x2};
                int[] yArray = {(int)y1, y2, (int)y1};
                Polygon marker = new Polygon(xArray, yArray, 3);
                g.setColor(Color.BLUE);
                g.fillPolygon(marker);
                g.setColor(Color.BLACK);
                String label = Double.toString(curVal / 100.0);
                y1 += TICSIZE * 2;
                g.drawString(label, (float)stringLoc, (float)y1);
            }
        }
    }
    
    // drawing the bars of the histogram, scaling them first so that they fit
    // nicely, regardless of how many items are in one bar.
    private void drawData(Graphics2D g) {
        int height;
        int width = (int)slotSpacing;
        double curVal;
        for (int c = 0; c < heightArray.length; c++) {
            curVal = (double)c + min;
            height = (int)((int)((double)heightArray[c] / COMBINATIONS * 100) * ySpacing / yRatio);
            if (data != null && curVal == data.percentileOfOrig ) {
                g.setColor(Color.RED);
            }
            else if (curVal <= 1.0) {
                g.setColor(Color.LIGHT_GRAY);
            }
            else if (curVal <= 5.0) {
                g.setColor(Color.GRAY);
            }
            else if (curVal <= 10.0) {
                g.setColor(Color.DARK_GRAY);
            }
            else {
                g.setColor(Color.BLACK);
            }
            double xPos = (leftX + c * slotSpacing) + width / 2;
            g.fill(new Rectangle2D.Double(xPos, lowerY - height, width, height));
        }
    }
    
    // drawing the title onto the graphics object.
    private void drawTitle(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.setFont(titleFont);
        int pos = getPos(TITLE, 'N', g);
        g.drawString(TITLE, pos, TITLE_POS);
    }
    
    private double[] panelToHistogram(int x, int y) {
        double histX;
        double testX;
        double histY;
        if (x >= leftX && x < rightX) {
            testX = ((x - ((int)slotSpacing / 2)) - leftX) / slotSpacing;
        }
        // returning a negative means the cursor is outside of the histogram
        // (in the x direction).
        else {
            testX = -1;
        }
        histX = testX;
        // scaling the y-coordinate so that it matches that axis.
        histY = (int)(((getHeight() - y) - AXIS_OFFSET) / ySpacing * yRatio);
        double[] histCoords = new double[2];
        histCoords[0] = histX;
        histCoords[1] = histY;
        return histCoords;
    }

    @Override
    public String getToolTipText(MouseEvent evt) {
        int cursorX = evt.getX();
        int cursorY = evt.getY();
        String line1 = "<html>Total combinations with this p-value: ";
        String line2 = "Percentage of combinations with this p-value: ";
        String line3 = "The current p-value is: ";
        double[] histCoords = panelToHistogram(cursorX, cursorY);
        double histX = histCoords[0];
        double histY = histCoords[1];
        StringBuilder toolTipText = new StringBuilder("");
        // checking if the coordinate is inside of the chart or not.  
        // The panelToHistogram method returns -1.0 if the current mouse 
        // position is below the first histogram column, so we can use that here
        // not display tooltips for those positions.
        // the third check in the if statement is there because heightArray is one longer
        // than the actual set of data, to ensure bars are never cut off. this check makes
        // sure that we don't display a tooltip for a p-value that doesn't exist.
        if (histX >= 0.0 && histX < rightX && (int)histX < heightArray.length - 1) {
            try {
                // getting height of column form height array             
                int count = heightArray[(int)histX];
                // finding ratio and converting to percent (.33 -> 33)
                int percentage = (int)((double)count / COMBINATIONS * 100);
                int maxPercent = (int)(((double)maxCount / COMBINATIONS) * 100);
                if (histY >=0 && histY <= maxPercent) {
                    // creating the tooltip.  There are two lines of constant 
                    // text and two numbers that need to be added (the html code 
                    // just puts everything on two lines).  I used a 
                    // stringBuilder to prevent creating a lot of unneeded 
                    // string objects, and I spaced the appends out to make 
                    // things easier to read.
                    toolTipText.append(line1);
                    toolTipText.append(count).append("<br>");
                    toolTipText.append(line2);
                    toolTipText.append(percentage).append(" %<br>");
                    toolTipText.append(line3);
                    toolTipText.append((int)histX / 100.0).append("</html>");
                    // returning the tooltip if we were within the histogram
                    return toolTipText.toString();
                }
                // returning nothing if we were not between height zero and 
                // max bar height.
                else {
                    return null;
                }
                // returning nothing if we run into an exception
            } catch (ArrayIndexOutOfBoundsException oops) {
                return null;
            }
        }
        // returning nothing if we were not within the left and right x 
        // margins.
        return null;
    }
}
