/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.gui;

import edu.hmc.jane.Stats;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.FontMetrics;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import javax.swing.ToolTipManager;

/**
 *
 * @author Tselil
 * slightly modified by amichaud
 */
public class Histogram extends JPanel {

    int solnConsidered, max, min;
    int[] dist;
    int slots, maxCount;
    int[] slotDist;

    final int MARGIN_OFFSET = 50;
    final int AXIS_BUFFER = 10;
    double xTicSpacing,yTicSpacing;
    double ticSize = 5;
    int HEIGHT_DENOMINATIONS = 1;
    int COST_DENOMINATIONS = 1;
    int MAX_Y_TIC_COUNT = 10;
    int MAX_X_TIC_COUNT = 15;
    int Y_LABEL_POS = 20;
    int TITLE_POS = 25;
    int X_LABEL_POS;
    final String X_LABEL = "Cost";
    final String Y_LABEL = "Number of Solutions";
    final String TITLE = "Distribution of Costs of Random Sample Solutions";
    Font labelFont, regularFont, titleFont;

    int leftXMargin = MARGIN_OFFSET;
    int upperYMargin = MARGIN_OFFSET;
    int rightXMargin, lowerYMargin;
    double slotSpacing;
    int yAxisLength, xAxisLength;

    boolean includesOriginal;

    public Histogram(Stats.HistoData data, int width, int height) {
        super();
        this.setSize(width, height);
        this.solnConsidered = data.orig;
        this.dist = data.dist;
        this.max = data.max;
        this.min = data.min;
        slots = max - min + 2;
        slotDist = calcSlotDist();
        this.includesOriginal = data.hasOrig;

        setOpaque(false);
    }

    private void calculateValues() {
         X_LABEL_POS = getHeight() - 10;

         // Axis Bounds
         rightXMargin = getWidth() - MARGIN_OFFSET;
         // if we have enough slots, we'll need to rotate the x-axis to make
         // them readable.  We move the x axis upward so the label isn't
         // covered.
         if (slots > 50) {
             lowerYMargin = getHeight() - MARGIN_OFFSET - 30;
         }
         else {
             lowerYMargin = getHeight() - MARGIN_OFFSET;
         }
         
         // Axis Lengths
         xAxisLength = rightXMargin - leftXMargin;
         yAxisLength = lowerYMargin - upperYMargin;

         // Maximum number of tics per axis
         MAX_Y_TIC_COUNT = yAxisLength/20;
         MAX_X_TIC_COUNT = xAxisLength/15;

         // Spacing for rendering of bars and tics
         slotSpacing = ((double)(xAxisLength - AXIS_BUFFER))/slots;
         if (maxCount > MAX_Y_TIC_COUNT) {
            HEIGHT_DENOMINATIONS = (int) Math.ceil( ((double)maxCount) / MAX_Y_TIC_COUNT ); 
         }
         yTicSpacing = (yAxisLength - AXIS_BUFFER)*HEIGHT_DENOMINATIONS/maxCount;
         if (slots > MAX_X_TIC_COUNT) {
             COST_DENOMINATIONS = (int) Math.ceil( ((double)slots ) / MAX_X_TIC_COUNT);
         }
         xTicSpacing = slotSpacing*(double)COST_DENOMINATIONS;
    }

    @Override
    public void paintComponent(Graphics g) {
        calculateValues();
        super.paintComponent(g);
        regularFont = g.getFont();
        labelFont = new Font( regularFont.getFontName(), Font.BOLD, Math.min(14, getHeight()/15));
        titleFont = new Font( regularFont.getFontName(), Font.BOLD, Math.min(16, getWidth()/25));

        g.setColor(Color.BLACK);
        drawTitle(g);
        drawAxes((Graphics2D)g);
        drawData((Graphics2D)g);
        drawMarkerLine((Graphics2D) g);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /* this method returns an array with the number of solutions of a given
     * cost corresponding to a specific index in the array. The 0th index
     * corresponds to the number of solutions with the minimum cost, and
     * every number between the max and min are represented in the indecies
     * in between (even if there are no solutions with that cost).
     */
    private int[] calcSlotDist() {
        int[] answer = new int[slots];
        maxCount = 0;
        for (int i = 0; i < dist.length; i++){
        // get the counts for each value in the histogram
            if (dist[i] != -1){
                answer[dist[i] - min]++;
                if (answer[dist[i] - min] > maxCount)
                    maxCount = answer[dist[i] - min];
            }
        }
        return answer;
    }

    private void drawAxes(Graphics2D g) {
        g.setColor(Color.BLACK);
        // Draw x-axis
        g.drawLine(leftXMargin, lowerYMargin, rightXMargin, lowerYMargin);
        // Draw y-axis
        g.drawLine(leftXMargin, upperYMargin, leftXMargin, lowerYMargin);

        // Label axes
        g.setFont(labelFont);
        // centering the x-axis
        FontMetrics fm = g.getFontMetrics(labelFont);
        Rectangle2D stringRect = fm.getStringBounds(X_LABEL, g);
        float halfString = (float)stringRect.getWidth() / 2;
        float xPos = leftXMargin + (xAxisLength / 2.0f - halfString);
        g.drawString(X_LABEL, xPos, X_LABEL_POS);
        // the original code to rotate the label for the y-axis wouldn't
        // work on my copy of Jane (and possibly other team members'
        // copies as well). this new code rotates the graphics object and
        // appends letter individuallly to draw the label. no bugs yet.
        // finding the size of the string
        float yPos;
        float labelXPos = lowerYMargin - yAxisLength / 2.0f;
        // the midpoint of the string, used to find offsets for individual
        // characters
        double half = Y_LABEL.length() / 2.0;
        double offset;
        // rotating so that the label is vertically aligned.
        // we need to rotate later to put everything back properly.
        // we're rotating about a specific point and not the origin, which is
        // why there are three arguments here
        g.rotate(-Math.PI / 2, Y_LABEL_POS, lowerYMargin - (yAxisLength / 2.0f));
        // looping over the characters in the Y_LABEL text
        for (int c = 0; c < Y_LABEL.length(); c++) {
            // getting characters and turning them into strings.
            Character curChar = Y_LABEL.charAt(c);
            String charToString = curChar.toString();
            // if the character is in the first half of the text, 
            // it gets shifted downward by the size of the substring it begins
            if (c < half) {
                stringRect = fm.getStringBounds(Y_LABEL.substring(c, (int)half), g);
                offset = stringRect.getWidth();
                yPos = Y_LABEL_POS - (float)offset; 
                // because the axis was rotated, y coordinates become x coordinates
                // and vice versa.
                g.drawString(charToString, yPos, labelXPos);
            }
            // if the character is in the middle, it is drawn exactly in the center
            else if (c == Y_LABEL.length()) {
                yPos = Y_LABEL_POS;
                g.drawString(charToString, yPos, labelXPos);
            }
            // if the character is in the second half of the text, it is 
            // shifted upward the appropriate amount.
            else {
                stringRect = fm.getStringBounds(Y_LABEL.substring((int)half, c), g);
                offset = stringRect.getWidth();
                yPos = Y_LABEL_POS + (float)offset;
                g.drawString(charToString, yPos, labelXPos);
            }
        }
        // rotating the graphics object back
        g.rotate(Math.PI / 2, Y_LABEL_POS, lowerYMargin - (yAxisLength / 2.0f));

        // Draw and label x-axis tics
        int denominations = (int)( ((double)xAxisLength) / xTicSpacing);
        g.setFont(regularFont);
        int topYPos = (int)(lowerYMargin + 3 * ticSize);
        if (slots > 50) {
            g.rotate(-Math.PI / 2);
            for (int i = 0; i < denominations - 1; i++) {
            // Draw the tic
            int ticLoc = leftXMargin + (int)(slotSpacing + i*xTicSpacing);
            g.drawLine(ticLoc, (int)(lowerYMargin + ticSize), ticLoc, (int)(lowerYMargin - ticSize));
            // Label the tic
            String label = Integer.toString(i * COST_DENOMINATIONS + min);
            float curYPos;
            for (int c = 0; c < label.length(); c++) {
                char curChar = label.charAt(c);
                String charToString = Character.toString(curChar);
                stringRect = fm.getStringBounds(label.substring(c, label.length()), g);
                offset = stringRect.getWidth();
                curYPos = (float)(topYPos + offset);
                g.drawString(charToString, -curYPos, ticLoc - 5);
            }
            }
            g.rotate(Math.PI / 2);
        }
        else {
            for (int i = 0; i < denominations - 1; i++) {
                // Draw the tic
                int ticLoc = leftXMargin + (int)(slotSpacing + i*xTicSpacing);
                g.drawLine(ticLoc, (int)(lowerYMargin + ticSize), ticLoc, (int)(lowerYMargin - ticSize));
                g.drawString( Integer.toString(i*COST_DENOMINATIONS+min), ticLoc - 5, (int)(lowerYMargin + 3*ticSize));
            } 
        }
        // Draw and label y-axis tics
        denominations = (int)( ((double)yAxisLength) / yTicSpacing);
        for (int i = 0; i <= denominations; i++) {
            // Draw the tic
            int ticLoc = (int)(lowerYMargin - i*yTicSpacing);
            g.drawLine((int)(leftXMargin - ticSize),  ticLoc, (int)(leftXMargin + ticSize), ticLoc);
            // Label the tic
            g.drawString( Integer.toString(i*HEIGHT_DENOMINATIONS), (int)(leftXMargin - 4*ticSize), ticLoc + 3);
        }
    }

    private void drawData(Graphics2D g) {
        int height;
        for (int i = 0; i < slotDist.length; i++) {
            height = (int)(slotDist[i]*yTicSpacing/HEIGHT_DENOMINATIONS);
            g.setColor(Color.BLUE);
            g.fillRect((int) (leftXMargin + (i + 0.5)*slotSpacing), lowerYMargin - height, (int)slotSpacing, height);
            // Trace the outline in black
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke());
            g.drawRect((int) (leftXMargin + (i + 0.5)*slotSpacing), lowerYMargin - height, (int)slotSpacing, height);
        }
    }

    private void drawTitle(Graphics g) {
        g.setFont(titleFont);
        FontMetrics fm = getFontMetrics(titleFont);
        Rectangle2D textsize = fm.getStringBounds(TITLE, g);
        int xPos = (getWidth() - (int)textsize.getWidth()) / 2;
        g.drawString(TITLE,  xPos, TITLE_POS);
    }

    // draws the line marking the cost of the original solution.
    private void drawMarkerLine(Graphics2D g) {
        if (includesOriginal) {
            int x = graphToPanelX(solnConsidered);
            int y2 = graphToPanelY(0);
            int y1 = upperYMargin;

            g.setColor(Color.red);
            Stroke s = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[] {5,5}, 0 );
            g.setStroke(s);
            g.drawLine(x, y1, x, y2);
        }
    }

    /*
     * calculates where a given x coordinate on the histogram falls on the panel
     */
    private int graphToPanelX(int x) {
        return (int) (leftXMargin + slotSpacing + ((x-min)/COST_DENOMINATIONS) * xTicSpacing);
    }

    /*
     * calculates where a given y coordinate on the histogram falls on the panel
     */
    private int graphToPanelY(int y) {
        return (int) (lowerYMargin - (y/HEIGHT_DENOMINATIONS) * yTicSpacing);
    }
    
    // calculates where a given x and y coordinate on the panel fall on
    // the histogram
    private double[] panelToHistogram(int x, int y) {
        double histX;
        double testX;
        double histY;
        if (x >= leftXMargin && x < rightXMargin) {
            testX = ((x - ((int)slotSpacing / 2)) - leftXMargin) / slotSpacing;
        }
        // returning a negative means the cursor is outside of the histogram
        // (in the x direction).
        else {
            testX = -1;
        }
        histX = testX;
        // scaling the y-coordinate so that it matches that axis.
        histY = (int)(((getHeight() - y) - MARGIN_OFFSET) / yTicSpacing * HEIGHT_DENOMINATIONS);
        double[] histCoords = new double[2];
        histCoords[0] = histX;
        histCoords[1] = histY;
        return histCoords;
    }

    // creates tool tip text showing the height of the column the cursor is over
    @Override
    public String getToolTipText(MouseEvent evt) {
        int cursorX = evt.getX();
        int cursorY = evt.getY();
        String line1 = "<html>Solutions with this cost: ";
        String line2 = "The current cost is: ";
        double[] histCoords = panelToHistogram(cursorX, cursorY);
        double histX = histCoords[0];
        double histY = histCoords[1];
        StringBuilder toolTipText = new StringBuilder("");
        // checking if the coordinate is inside of the chart or not.  
        // The panelToHistogram method returns -1.0 if the current mouse 
        // position is below the first histogram column, so we can use that here
        // not display tooltips for those positions.
        // the third check in the if statement is there because slotDist is one longer
        // than the actual set of data, to ensure bars are never cut off. this check makes
        // sure that we don't display a tooltip for a cost that doesn't exist.
        if (histX >= 0.0 && histX < rightXMargin && (int)histX < slotDist.length - 1) {
            // if we somehow get an incorrect value (something outside of the
            // histogram), an exception gets thrown. This try-catch catches
            // that exception and provides null instead of crashing.
            try {
                // getting height of column from array
                int count = slotDist[(int)histX];
                // We only return a tooltip if we are at or below the maximum
                // height of a bar on the histogram and above zero. Otherwise
                // we return nothing.
                if (histY >=0 && histY <= maxCount) {
                    // creating the tooltip.  
                    toolTipText.append(line1);
                    toolTipText.append(count).append("<br>");
                    toolTipText.append(line2);
                    toolTipText.append((int)histX + (int)min).append("</html>");
                    // returning the tooltip
                    return toolTipText.toString();
                }
                // returning nothing if we didn't have a proper y coordinate.
                else {
                    return null;
                }    
            }
            // returning nothing if we were out of bounds.
            catch (ArrayIndexOutOfBoundsException oops) {
                return null;
            }
        }
        // if we aren't within the margins, we return no tooltip.
        return null;
    }
}
