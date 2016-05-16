/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.gui;

import edu.hmc.jane.CostModel;
import edu.hmc.jane.RegionedCostModel;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import net.sf.epsgraphics.EpsGraphics;

/**
 *
 * @author jpeebles
 * @modified by amichaud
 */
public class Utils {

    // Variables used for creating the key and adding it and the cost info to
    // the output file.
    final static int KEYWIDTH = 180;
    final static int COLWIDTH = KEYWIDTH / 2;
    final static int COLUMN_1 = 1;
    final static int COLUMN_2 = 2;
    final static int CENTER   = 3;
    // offset of dividers from top of screen
    public final static int VOFFSET = 30;
    // offset from left side of image/dividers
    final static int XPOS = COLWIDTH / 2;
    final static int COL2_XPOS = COLWIDTH / 2 + COLWIDTH;
    // diameter of ovals for the tree and for the key.  
    final static int DIAMETER = 7;
    final static double RAD = DIAMETER / 2.0;
    final static int HS_SIZE = 4;
    // line thicknesses
    final static Stroke parasiteStroke = new BasicStroke(0.5f);
    final static Stroke hostStroke = new BasicStroke(1.5f);
    // dimensions of the component used in the save dialog.
    static int C_WIDTH;
    static int C_HEIGHT;

    
    /*
     * changes the taskbar icon from the default java one to the nice Jane
     * one for the given window, this must be called from the constructor of
     * every window you create, though only the first one that opens makes
     * the biggest difference
     * NOTE: only necessary for Windows, as Mac applications don't have
     * icons in their menu bars (or at least Jane doesn't).
     */
    public static void initIcons(Window w) {
        try {
            List<Image> icons = new LinkedList<Image>();
            Class<? extends Window> wc = w.getClass();
            icons.add(ImageIO.read(wc.getResource("/images/icon-small.png")));
            icons.add(ImageIO.read(wc.getResource("/images/icon-large.png")));

            //the method JFrame.setIconImages is not supported on JDK 1.5,
            //so to maintain compatibility, we try to call it via reflection,
            //and if that fails, fall back to setIconImage
            try {
                wc.getDeclaredMethod("setIconImages", new Class[] {java.util.List.class})
                    .invoke(w, new Object[] {icons});

            } catch(Exception e) {
                if(w instanceof JFrame)
                    ((JFrame) w).setIconImage(icons.get(0));
            }
        } catch (Exception ignore) {}
    }
         
    /* amichaud
     * Prompt and Save method for files that won't ever use EPS output or 
     * solution cost output.  Used to save histogram images and probably
     * timings.
     */
    public static void promptAndSave(final Frame fr, final Component c) {
        // The cost won't matter either if the method is being used from another file.
        int cost = 0;
        // False is fed into the regular promptAndSave method, so that it knows that 
        // we won't be adding the solution key or cost to our output (if this
        // method was called).
        promptAndSave(fr, c, cost, false);
    }
    
    /*
     * brings up a save dialog box, and if the user hits save (rather than cancel),
     * saves an image of the component to the target file. Most windows spawned
     * will have Frame f as their parent.  Supports saving to multiple file types.
     * The method will respect either .eps or .png extensions if the user types them
     * into the filename. The user can also select a file filter to choose a file type.
     * The method defaults to .png images.
     */
    public static void promptAndSave(final Frame fr, final Component c, final int cost, final boolean extras) {
        // initializing variables needed for any kind of output.
        C_WIDTH = c.getWidth();
        C_HEIGHT = c.getHeight();
        // The cost model is stored and then passed to addExtraInfo so that the 
        // event costs can be used in the key.
        SolutionViewer sv;
        final CostModel cm;
        try {
            sv = (SolutionViewer)fr;
        } catch (ClassCastException e) {
            sv = null;
        }
        if (sv != null)
            cm = sv.info.costModel;
        else
            cm = null;
        JaneFileChooser jsave = new JaneFileChooser(extras, c, cost, cm, fr);
        jsave.setVisible(true);
    }
       
    /* Returns the x-position a string in the key should start at in order to be
     * centered. Takes the string and the column the string is in as 
     * arguments.
     */
    public static float getPos(String text, int col, Graphics2D g) {
        Font f = g.getFont();
        FontMetrics fm = g.getFontMetrics(f);
        Rectangle2D stringRect = fm.getStringBounds(text, g);
        // getting the length of half of the string (the offset needed
        // to center it)
        double halfString = stringRect.getWidth()/2.0; 
        double stringXPos = 0;
        // If the string is in neither column (the key text or solution cost
        // text), it is centered on the dividing line.
        switch(col) {
            // Cost only, centered on middle of the image.
            case 0:
                stringXPos = C_WIDTH / 2.0 - halfString;
                break;
            // Something in column 1, centered on middle of that column.
            case 1:
                stringXPos = KEYWIDTH / 4.0 - halfString;
                break;
            // Something in column 2, centered on middle of that column.
            case 2:
                stringXPos = KEYWIDTH * .75 - halfString;
                break;
            // Something above/below columns 1 and two, centered on column
            // dividing line
            case 3:
                stringXPos = COLWIDTH - halfString;
                break;
        }
        return (float)stringXPos;
    }
    
    /* Generates encapsulated PostScript code containing the solution image
     * found in the passed in SolutionViewer frame.
     */
    static public String generateEPS(final SolutionViewer frame, boolean grayOn, boolean keyOn, boolean costOn, Dimension dim, Component c) {
        String translate;
        int normWidth = c.getWidth();
        int normHeight = c.getHeight();
        double curWidth = dim.getWidth();
        double curHeight = dim.getHeight();

        if (keyOn) {
            curWidth += KEYWIDTH;
            normWidth += KEYWIDTH;
            translate = KEYWIDTH + " 0 translate \n";
        } else if  (!keyOn && costOn) {
            curHeight += VOFFSET; 
            normHeight += VOFFSET;
            translate = "";
        } else {
            translate = "";
        }
        String trees = frame.generateEPS(normHeight, grayOn);   
        double xS = curWidth / normWidth;
        double yS = curHeight / normHeight;
        
        double xScale = 792.0/curWidth;
        double yScale = 612.0/curHeight;
        //FIXME: Bounding Box size needs to be set dynamically based on the window size
        String header = "%!PS-Adobe-3.0 EPSF-3.0 \n" // attempted dynamic resizing based on component (JPanel or zoomed JPanel)
                + "%%BoundingBox: 0 0 " + curWidth +  " " + curHeight + " \n"
                + "%%DocumentData: Clean7Bit\n"
                + "%%LanguageLevel: 2\n"
                + "%%ColorUsage: Color\n"
                + "%%Origin: 0 0\n"
                + "%%Pages: 1\n"
                + "%%Page: 1 1\n"
                + "%%EndComments \n";
        // Seems to be unnecessary, left here if it is actually important.
        /*
        if (xScale < yScale)
                header += xScale + " " + xScale +" scale\n";
        else header += yScale + " " +  yScale + " scale\n";
        * 
        */
        String scale = xS + " " + yS + " scale\n";
        //NOTE: Remove showpage in final since EPS does not require showpage.
        //  However, useful for testing purposes.
        String result = header + scale + translate + trees;// + "showpage";
        return result;
    }
    
    // Draws the key and cost, if requested, onto the provided graphics object.
    // returns that same object when done.
    public static Graphics2D addExtraInfo(Graphics2D g, boolean keyOn, boolean costOn, boolean polytomyOn, Dimension dim, int cost, CostModel cm) {  
        // This code writes to the provided Graphics2D object, which can
        // be either a regular graphics object or an epsgraphics object. 
        // Regular graphics objects draw to the right and down of where you
        // actually specify because the coordinates you specify are between
        // the actual pixels.  EpsGraphics objects do not work this way. So, 
        // we shift non-eps graphics objects left and up by one pixel so
        // they react exactly the same way.
        Color pPolytomyColor = DrawingObjects.parasitePolytomyColor;
        Color hPolytomyColor = DrawingObjects.hostPolytomyColor;
        // TODO remove statement
        if (!(g instanceof EpsGraphics)) {
            g.translate(-1, -1);
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // costs for different events
        Double COSP_COST = new Double(cm.getCospeciationCost());
        Double DUP_COST  = new Double(cm.getDuplicationCost());
        Double LOSS_COST = new Double(cm.getLossCost());
        Double FTD_COST  = new Double(cm.getFailureToDivergeCost());
        Double DHS_COST  = new Double(cm.getHostSwitchCost());
        // getting individual dimensions
        int height = dim.height;
        int width = dim.width;
        // the size of the key, from one divider to the other divider (the
        // space after the bottom divider is not used even if costInfo is
        // off
        int keySize;
        if (costOn) {
            keySize = height - 2 * VOFFSET;
        } else {
            keySize = height - VOFFSET;
        }
        // creating the size of an individual section of the key.
        // calculated here so that we can add polytomy info properly.
        int sectionSize = keySize / 4;
        // adding polytomy info to key
        // setting the color and font used for dividers and related text
        g.setColor(Color.BLACK);
        g.setFont(new Font("Helvetica", Font.PLAIN , 12));
        if (keyOn) {// <editor-fold>     
            // drawing the border around the key
            g.drawRect(1, 1, KEYWIDTH - 1, height - 1);
            // cost and polytomy info are on
            if (costOn && polytomyOn) {
                // DIVIDERS
                // top and bottom dividers. 
                g.drawRect(0, VOFFSET, KEYWIDTH, height - 2 * VOFFSET);
                // middle divider, stops at polytomy info
                g.drawLine(COLWIDTH, VOFFSET, COLWIDTH, height - VOFFSET - sectionSize);
                // polytomy divider
                g.drawLine(0, height - VOFFSET - sectionSize, KEYWIDTH, height - VOFFSET - sectionSize);
                // solution cost labelling
                // If both key and cost are on, the solution cost is printed
                // at the bottom of the key.
                String solCost = "Solution Cost: " + cost;
                float solCostPos = getPos(solCost, CENTER, g);
                g.drawString(solCost, solCostPos, height - VOFFSET / 2);
                // DRAWING POLYTOMIES
                g.setColor(pPolytomyColor);
                g.setStroke(parasiteStroke);
                int yPos = height - sectionSize - VOFFSET / 2;
                g.drawLine(XPOS, yPos + VOFFSET, XPOS, height - 2 * VOFFSET);
                g.setColor(hPolytomyColor);
                g.setStroke(hostStroke);
                g.drawLine(COL2_XPOS, yPos + VOFFSET, COL2_XPOS, height - 2 * VOFFSET);
                // LABELLING POLYTOMIES
                g.setColor(Color.BLACK);
                String poly = "Polytomy Coloring";
                float polyPos = getPos(poly, CENTER, g);
                g.drawString(poly, polyPos, yPos);
                String pPoly = "Parasite";
                float pPolyPos = getPos(pPoly, COLUMN_1, g);
                String hPoly = "Host";
                float hPolyPos = getPos(hPoly, COLUMN_2, g);
                g.drawString(pPoly, pPolyPos, height - VOFFSET * 1.5f);
                g.drawString(hPoly, hPolyPos, height - VOFFSET * 1.5f);
            }
            // cost info is not on, polytomy info is on
            else if (!costOn && polytomyOn) {
                // DIVIDERS
                // drawing the top divider
                g.drawLine(0, VOFFSET, KEYWIDTH, VOFFSET);
                // drawing the middle divider, which again stops at polytomy info
                g.drawLine(COLWIDTH, VOFFSET, COLWIDTH, height - sectionSize);
                // drawing the polytomy info divider
                g.drawLine(0, height - sectionSize, KEYWIDTH, height - sectionSize);
                // DRAWING POLYTOMIES
                g.setColor(pPolytomyColor);
                g.setStroke(parasiteStroke);
                int yPos = height - sectionSize;
                g.drawLine(XPOS, yPos + VOFFSET, XPOS, height - VOFFSET);
                g.setColor(hPolytomyColor);
                g.setStroke(hostStroke);
                g.drawLine(COL2_XPOS, yPos + VOFFSET, COL2_XPOS, height - VOFFSET);
                // LABELLING POLYTOMIES
                g.setColor(Color.BLACK); 
                String poly = "Polytomy Coloring";
                float polyPos = getPos(poly, CENTER, g);
                g.drawString(poly, polyPos, yPos + VOFFSET / 2);
                String pPoly = "Parasite";
                float pPolyPos = getPos(pPoly, COLUMN_1, g);
                String hPoly = "Host";
                float hPolyPos = getPos(hPoly, COLUMN_2, g);
                g.drawString(pPoly, pPolyPos, height - VOFFSET / 2);
                g.drawString(hPoly, hPolyPos, height - VOFFSET / 2);
            }
            // cost info is on, polytomy info is not
            else if (costOn && !polytomyOn) {
                // DIVIDERS
                // top and bottom dividers. 
                g.drawRect(0, VOFFSET, KEYWIDTH, height - 2 * VOFFSET);
                // middle divider, stops at cost info
                g.drawLine(COLWIDTH, VOFFSET, COLWIDTH, height - VOFFSET);
                // solution cost labelling
                // If both key and cost are on, the solution cost is printed
                // at the bottom of the key.
                String solCost = "Solution Cost: " + cost;
                float solCostPos = getPos(solCost, CENTER, g);
                g.drawString(solCost, solCostPos, height - VOFFSET / 2);
                
            }
            else {
                // DIVIDERS
                // top divider
                g.drawLine(0, VOFFSET, KEYWIDTH, VOFFSET);
                // middle divider
                g.drawLine(COLWIDTH, VOFFSET, COLWIDTH, height);
            }
            // adding title.  
            g.setColor(Color.BLACK);
            String solKey = "Solution Key";
            float solKeyPos = getPos(solKey, CENTER, g);
            g.drawString(solKey, solKeyPos, (float)VOFFSET / 2);
            //////////////////////////
            // KEY GENERATION CODE  //
            //////////////////////////
            // ________________________________________
            // |Solution Key                           |
            // |_______________________________________|
            // |Cospeciation      | Duplication        |
            // |Loss              | Dup + Host Switch  |
            // |FailuretoDiverge  |  //Infestation//   |
            // |---------------------------------------|
            // | Polytomy Info                         |
            // |                                       |
            // |_______________________________________|
            // |Solution Cost: cost                    |
            // |_______________________________________|
            
            // the Y position will be incremented by one section for each symbol covered
            int symYPos = (VOFFSET + sectionSize) / 2;
            // labels are offset from symbols by VOFFSET distance
            int labelYPos = symYPos + VOFFSET;
            // costs are offset from the label by VOFFSET / 2
            // distance
            int costYPos = labelYPos + VOFFSET / 2;
            g.setFont(new Font("Helvetica", Font.PLAIN, 10));
            
            // COSPECIATION //
            // cospeciation draws one red circle then a smaller white circle
            // to create a hollow circle.
            g.setColor(DrawingObjects.hostEdgeColor);
            // naming the strings for convenience
            String cosp = "Cospeciation";
            String cospCost = "Cost: " + COSP_COST;
            // getting the x positions for those strings (so they get centered
            // properly).
            float cospPos = getPos(cosp, COLUMN_1, g);
            float cospCostPos = getPos(cospCost, COLUMN_1, g);
            // drawing the strings
            g.drawString(cosp, cospPos, labelYPos);
            g.drawString(cospCost, cospCostPos, costYPos);
            // drawing and filling two ellipses (so that the symbol is hollow
            // like it needs to be.
            // using ellipses so that they can be centered properly.
            double circPos = XPOS - RAD;
            double yPos = symYPos - RAD;
            double leftPos = circPos - 2.5 * DIAMETER;
            double rightPos = circPos + 2.5 * DIAMETER;
            // we draw the symbol in the three different colors nodes can
            // be.
            
            g.setColor(DrawingObjects.worseSolutions);
            g.fill(new Ellipse2D.Double(leftPos, yPos, DIAMETER, DIAMETER));
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(leftPos + 1, yPos + 1, DIAMETER - 2, DIAMETER - 2));
            
            g.setColor(DrawingObjects.equalSolutions);
            g.fill(new Ellipse2D.Double(circPos, yPos, DIAMETER, DIAMETER));
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(circPos + 1, yPos + 1, DIAMETER - 2, DIAMETER - 2));
            
            g.setColor(DrawingObjects.betterSolutions);
            g.fill(new Ellipse2D.Double(rightPos, yPos, DIAMETER, DIAMETER));
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(rightPos + 1, yPos + 1, DIAMETER - 2, DIAMETER - 2));
            // incrementing positions so everything spaces out nicely.
            symYPos += sectionSize;
            labelYPos += sectionSize;
            costYPos += sectionSize;
            
            // LOSS //
            // specifying the number of dashes in the loss icon, as well as 
            // the size of dashes and the spaces between them.
            int quartSize = sectionSize / 8;
            float dashSize = quartSize / 3;
            // creating a stroke which will draw a dashed line.
            // taken from histogram.java
            Stroke oldStroke = g.getStroke();
            Stroke dashedStroke = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, 
                    BasicStroke.JOIN_MITER, 10.0f, new float[] {dashSize}, 0.0f);
            g.setStroke(dashedStroke);
            g.setColor(DrawingObjects.parasiteEdgeColor);
            double y1 = costYPos - sectionSize + VOFFSET * 1.5;
            g.draw(new Line2D.Double(XPOS, y1, XPOS, symYPos));
            g.setStroke(oldStroke);
            // more centering
            String loss = "Loss";
            String lossCost = "Cost: " + LOSS_COST;
            float lossPos = getPos(loss, COLUMN_1, g);
            float lossCostPos = getPos(lossCost, COLUMN_1, g);   
            
            g.setColor(Color.BLACK);
            g.drawString("Loss", lossPos, labelYPos);
            g.drawString(lossCost, lossCostPos, costYPos);
            // incrementing
            symYPos += sectionSize;
            labelYPos += sectionSize;
            costYPos += sectionSize;
            
            // FAILURE TO DIVERGE //
            // The failure to diverge is a jagged line created by this loop.        
            y1 = costYPos - sectionSize + 1.5 * VOFFSET;
            g.setColor(DrawingObjects.parasiteEdgeColor);
            boolean swap = false;
            // TODO clean up magic numbers
                for (int i = (int)y1; i <= symYPos; i += 3) {
                    if (!swap) {
                        g.drawLine(XPOS - 2, (int)(i - 1.5), XPOS + 2, (int)(i + 1.5));
                    }
                    else {
                        g.drawLine(XPOS - 2, (int)(i + 1.5), XPOS + 2, (int)(i - 1.5));
                    }
                    swap = !swap;
                }
            // centering
            String ftd1 = "Failure to Diverge";
            String ftdCost = "Cost: " + FTD_COST;
            float ftd1Pos = getPos(ftd1, COLUMN_1, g);
            float ftdCostPos = getPos(ftdCost, COLUMN_1, g);
            // drawing
            g.setColor(Color.BLACK);
            g.drawString(ftd1, ftd1Pos, (float)labelYPos);
            g.drawString(ftdCost, ftdCostPos, costYPos);
            
            // SECOND COLUMN //
            // Starting the second column.  Y positions reset
            symYPos = (VOFFSET + sectionSize) / 2;
            labelYPos = symYPos + VOFFSET;
            costYPos = labelYPos + 15;
            
            // DUPLICATION //
            // setting up the positions for the circles
            circPos = COL2_XPOS - RAD;
            yPos = symYPos - RAD;
            leftPos = circPos - 2.5 * DIAMETER;
            rightPos = circPos + 2.5 * DIAMETER;
            // centering and drawing the labels
            String dup = "Duplication";
            String dupCost = "Cost: " + DUP_COST;
            float dupPos = getPos(dup, COLUMN_2, g);
            float dupCostPos = getPos(dupCost, COLUMN_2, g);
            g.setColor(Color.BLACK);         
            g.drawString(dup, dupPos, labelYPos);
            g.drawString(dupCost, dupCostPos, costYPos);
            // again, we draw the symbol in all possible colors.
            yPos = symYPos - RAD;
            g.setColor(DrawingObjects.worseSolutions);
            g.fill(new Ellipse2D.Double(leftPos, yPos, DIAMETER, DIAMETER));
            g.setColor(DrawingObjects.equalSolutions);
            g.fill(new Ellipse2D.Double(circPos, yPos, DIAMETER, DIAMETER));
            g.setColor(DrawingObjects.betterSolutions);
            g.fill(new Ellipse2D.Double(rightPos, yPos, DIAMETER, DIAMETER));
            
            symYPos += sectionSize;
            labelYPos += sectionSize;
            costYPos += sectionSize;
            
            // DUPLICATION AND HOST SWITCH //
            // for the duplication and host switch, a line is drawn across the 
            // section, and then two diagonal lines are added
            // to create the arrow. All have the same center.
            // An oval is drawn on top of the main line
            
            // label          
            g.setColor(Color.BLACK);
            String dHS1 = "Duplication and";
            String dHS2 = "Host Switch";
            // the cost of a duplication + host switch event can either be
            // dependent on which regions are switched between or constant.
            // this checks which case is currently true. if the cost is constant
            // it is displayed; otherwise "Region Dependent" is shown instead.
            String hsCost;
            if (cm instanceof RegionedCostModel) {
                hsCost = "Cost: Variable"; 
            }
            else {
                hsCost = "Cost: " + DHS_COST;
            }
            float dHS1Pos = getPos(dHS1, COLUMN_2, g);
            float dHS2Pos = getPos(dHS2, COLUMN_2, g);
            float dHSCostPos = getPos(hsCost, COLUMN_2, g);

            g.drawString(dHS1, dHS1Pos, labelYPos);
            g.drawString(dHS2, dHS2Pos, labelYPos + VOFFSET / 2);
            g.drawString(hsCost, dHSCostPos, costYPos + VOFFSET / 2);
            // main line
            // the line for the symbol starts VOFFSET / 2 from the last 
            // section's cost label, to make sure we don't cross over anything.
            y1 = costYPos - sectionSize + VOFFSET * 1.5;
            // adding a duplication symbol centered on the line.
            g.setColor(DrawingObjects.equalSolutions);
            // using same circle position as center circle above
            g.fill(new Ellipse2D.Double(circPos, y1, DIAMETER, DIAMETER));
            g.setColor(DrawingObjects.parasiteEdgeColor);
            g.draw(new Line2D.Double(COL2_XPOS, y1 + DIAMETER, COL2_XPOS, symYPos));
            // arrow
            // the center of the line for the host switch symbol.  
            double center = (y1 + symYPos) / 2;
            g.draw(new Line2D.Double((int)(COL2_XPOS - HS_SIZE), center - HS_SIZE, COL2_XPOS, center + HS_SIZE));
            g.draw(new Line2D.Double(COL2_XPOS, center + HS_SIZE, (int)(COL2_XPOS + HS_SIZE), center - HS_SIZE));
            //

            /*
             * // THIS CODE ADDS A SECTION FOR THE INFESTATION EVENT .       //
             * // PLEASE UNCOMMENT IT AND ADD THE APPROPRIATE SYMBOL FOR THE //
             * // EVENT WHEN/IF INFESTATIONS ARE IMPLEMENTED IN JANE         //
            // INFESTATION //
            // incrementing positions
            symYPos += sectionSize;
            labelYPos += sectionSize;
            costYPos += sectionSize;
            // DRAW THE SYMBOL HERE
            // creating strings and finding positions for them.
            String inf = "Infestation";
            String infCost;
            if (cm instanceof RegionedCostModel) {
                infCost = "Cost: Variable"; 
            }
            else {
                infCost = "Cost: " + DHS_COST;
            }
            float infPos = getPos(inf, COLUMN_2, g);
            float infCostPos = getPos(infCost, COLUMN_2, g);
            // drawing strings
            g.setColor(Color.BLACK);
            g.drawString(inf, infPos, labelYPos);
            g.drawString(infCost, infCostPos, costYPos);
            /*
            * 
            */
            // return graphics object 
            return g;
        }// </editor-fold>    
        // If only the cost is on, a different divider is created that
        // spans the curWidth of the screen 30 pixels from the top.  
        else if (costOn) {
            String solCost = "Solution Cost: " + cost;
            double solCostPos = getPos(solCost, 0, g);
            g.drawLine(0, VOFFSET - 1 , width, VOFFSET - 1 ); 
            g.drawString(solCost, (float)solCostPos, VOFFSET / 2);
            return g;
        }
        // return g unchanged if neither the key nor the cost was requested.
        else return g;         
    }  
    
}
