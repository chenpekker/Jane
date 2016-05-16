/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.gui;

import edu.hmc.jane.CostModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import net.sf.epsgraphics.ColorMode;
import net.sf.epsgraphics.EpsGraphics;
import sim.util.gui.DisclosurePanel;

/**
 *
 * @author amichaud
 */
public class JaneFileChooser extends JFrame{
    // these hold the last directory visited and the last file looked at
    // so that save dialogs can remember those.
    public static double IMAGE_WIDTH;
    public static double IMAGE_HEIGHT;
    private Dimension oldSize;
    private JPanel topPanel;
    private JFileChooser saveFile;
    private CheckBoxPanel cb;
    private DisclosurePanel sidePanel;
    private JPanel abridgedPanel;
    private JanePreviewPanel disclosedPanel;
    private double lastSideW;
    private Frame parent;
    private boolean polytomyOn;
    // Variables used for creating the key and adding it and the cost info to
    // the output file.
    // solution costModel(contains costs of events) and solution cost
    private static int SOLUTIONCOST;
    private static CostModel COSTMODEL;
    
    public JaneFileChooser(boolean extras, Component c, int cost, CostModel cm, Frame fr) {
        Utils.initIcons(this);
        this.setLayout(new BorderLayout());
        if (fr instanceof SolutionViewer) {
            polytomyOn = ((SolutionViewer)fr).polytomyOn();
        }
        SOLUTIONCOST = cost;
        COSTMODEL = cm;
        parent = fr;
        IMAGE_WIDTH = c.getWidth();
        IMAGE_HEIGHT = c.getHeight();
        // The panel holding the JFileChooser and the CheckBoxPanel.
        topPanel = new JPanel(new BorderLayout());
        // Creating the file chooser and making it a save dialog.
        if (Design.lastDirectory != null)
            saveFile = new JFileChooser(Design.lastDirectory);
        else
            saveFile = new JFileChooser();
        saveFile.setDialogType(JFileChooser.SAVE_DIALOG);
        // disabling the "AcceptAll" file filter, which is unneeded in the save dialog.
        saveFile.setAcceptAllFileFilterUsed(false);
        oldSize = saveFile.getPreferredSize();
        // creating file filters for png, and eps.
        SimpleFilter pngFilter = new SimpleFilter(".png", "PNG image files, (.png)");
        // used for EPS output, enable if EPS output is supported in Jane
        // ADD THIS FILTER BACK WHEN EPS OUTPUT IS SUPPORTED IN JANE
        //SimpleFilter epsFilter = new SimpleFilter(".eps", "EPS vector image files, (.eps)");
        // Adding file filter for png files, which are always an option.
        saveFile.addChoosableFileFilter(pngFilter);
        // adding the eps filter if needed.  Setting the checkbox panel as well.
        // For saving tress, we need all three checkboxes, which the cb 
        // constructor will give us with this input.
        if (extras) {
            // used for EPS output, enable if EPS output is supported in Jane
            //saveFile.addChoosableFileFilter(epsFilter);
            cb = new CheckBoxPanel(CheckBoxPanel.TREE);
        } 
        // The histogram doesn't need extras, so a CheckBoxPanel with only
        // the gray checkbox is returned.
        else {
            cb = new CheckBoxPanel(CheckBoxPanel.HISTOGRAM);            
        }
        disclosedPanel = new JanePreviewPanel(c, this);
        abridgedPanel = new JPanel();
        abridgedPanel.setMinimumSize(new Dimension(100, 100));
        sidePanel = new DisclosurePanel(abridgedPanel, disclosedPanel);
        // adding the side panel to the JFileChooser as an accessory.
        saveFile.setAccessory(sidePanel);
        topPanel.add(saveFile, BorderLayout.NORTH);
        topPanel.add(cb, BorderLayout.SOUTH);
        this.add(topPanel);
        addListeners(extras, c, cost, cm, fr);
        this.pack();
    }
    
    private void addListeners(final boolean extras, final Component c, final int cost, final CostModel cm, final Frame fr) {
        sidePanel.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("preferredSize".equals(evt.getPropertyName())) {
                    Dimension newPref = (Dimension)evt.getNewValue();
                    changeDim(newPref);
                }
            }
        });
        // watching to see if the save button is triggered, so we can react
        // accordingly if it is.
        saveFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                    // saves our current directory so we can come back to it later
                    Design.lastDirectory = saveFile.getCurrentDirectory();
                    // booleans that turn on the various extras that can be added to
                    final boolean keyOn;
                    final boolean costOn;
                    final boolean grayOn;
                    if (extras) {
                        keyOn  = cb.key;
                        costOn = cb.cost;
                        grayOn = cb.gray;
                    } else {
                        keyOn = false;
                        costOn = false;
                        grayOn = cb.gray;       
                    }
                    JProgressDialog.runTask("Saving Image", "Saving Image (This may take a few minutes.)", fr,
                        new Runnable() {                  
                        @Override
                        public void run() {
                            saveFile(fr, grayOn, costOn, keyOn, c);
                        }
                    });
                }
                if (e.getActionCommand().equals(JFileChooser.CANCEL_SELECTION)) {
                    JaneFileChooser.this.dispose();
                }       
            }
        });   
    }
    
    // saves a file as the requested type
    // this is called from 
    private void saveFile(final Frame fr, boolean grayOn, boolean costOn, boolean keyOn, Component c) {
        String filename;
        String path;
        // the description comes from the file filter and tells the method
        // what file type the user wants to save as. defaults to .png.
        String description;                                 
        try {
            filename = saveFile.getSelectedFile().getName();
            path = saveFile.getSelectedFile().getCanonicalPath();

            try {
                description = saveFile.getFileFilter().getDescription();                         
            } catch(NullPointerException e) {
                description = "";
            }

            // If there is no "." in the filename, we get the filename from
            // the current filter description and add that.  If there is 
            // no description, we default to ".png".
            if (!filename.contains(".")) {
                if (description.equals("")) {
                    path += ".png";
                    filename += ".png";
                } 
                else {
                    String substring = description.substring(0, 3);
                    String ext = substring.toLowerCase();
                    filename += "." + ext;
                    path += "." + ext;                              
                }
            }
            // stripping the last part of the filename so we can find where the file is
            // going and see if that file already exists.
            // we find the length of the filename, and use that to remove
            // the filename from the full path.
            int nameLength = filename.length();
            int fullLength = path.length();
            int dif = fullLength - nameLength;
            // we create a file with the full path - the filename
            String upDirPath = path.substring(0, dif);
            File newFile = new File(upDirPath);
            // we can use this to find a list of files in the same directory
            // as the file we are attempting to save
            String[] fileList = newFile.list();
            // if we find a file with the same name as the file we are attempting
            // to save, we ask the user to confirm that they want to overwrite
            // the file.
            for (int f = 0; f < fileList.length; f++) {
                if (fileList[f].equals(filename)) {
                    int option = JOptionPane.showConfirmDialog(JaneFileChooser.this, filename + " "
                            + "already exists. Would you like to overwrite it?" , "", JOptionPane.YES_NO_OPTION);
                    // if they do, we save over the existing file.
                    if (option == JOptionPane.YES_OPTION) {
                        // If the filename contains .png, we prepare to output a .png
                        // file. A graphics2d object that can draw into the 
                        // BufferedImage is created, and some settings are set on it.
                        // Finally, the component is printed into the graphicsObject 
                        // (and therefore the BufferedImage) and the image is output
                        // to the path specified as a png file.
                        if (filename.toLowerCase().endsWith(".png")) {
                            outputPNG(grayOn, costOn, keyOn, c, path);  
                            JaneFileChooser.this.dispose();
                        } 
                        /*
                         * enable this when Jane supports EPS output, it should work without
                         * modification.
                        else if (filename.toLowerCase().endsWith(".eps") && extras) {
                            outputEPS(grayOn, costOn, keyOn, c, path);
                            JaneFileChooser.this.dispose(); 
                        }
                        * 
                        */
                    }
                    // if they don't, we go back to the save dialog.
                    else {
                        return;
                    }
                }
            }
            // if we find no files with the same name as the one we're trying 
            // to save, we save the file without asking the user.
            if (filename.toLowerCase().endsWith(".png")) {
                outputPNG(grayOn, costOn, keyOn, c, path);  
                JaneFileChooser.this.dispose();
            } 
            /*
             * enable this when Jane supports EPS output, it should work without
             * modification.
            else if (filename.toLowerCase().endsWith(".eps") && extras) {
                outputEPS(grayOn, costOn, keyOn, c, path);
                JaneFileChooser.this.dispose(); 
            }
            * 
            */ 
        } catch(Exception e) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(fr, 
                        "Unable to write to the specified filename", "Error Writing File", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }
    
    // contains all of the code to output to a PNG file. Called up above
    // if the user wants to output to a PNG file.
    private void outputPNG(boolean grayOn, boolean costOn, boolean keyOn, Component c, String path) throws IOException {
        BufferedImage tree;
        BufferedImage extras;
        BufferedImage combined;
        BufferedImage colored;
        BufferedImage scaled;
        //
        int color = BufferedImage.TYPE_INT_ARGB;
        int gray = BufferedImage.TYPE_BYTE_GRAY;
        int width = c.getWidth();
        int height = c.getHeight();
        Dimension dim = new Dimension(width, height);
        int newWidth;
        int newHeight;
        int xDif;
        int yDif;
        tree = new BufferedImage(width, height, color);
        Graphics2D treeG = tree.createGraphics();
        c.print(treeG);
        treeG.dispose();
        if (keyOn || costOn) { //<editor-fold>
            if (keyOn) {
                newWidth = Utils.KEYWIDTH + width;
                combined = new BufferedImage(newWidth, height, color);
                extras = new BufferedImage(Utils.KEYWIDTH, height, color);
                xDif = Utils.KEYWIDTH;
                yDif = 0;
            } else {
                newHeight = Utils.VOFFSET + height;
                combined = new BufferedImage(width, newHeight, color);
                extras = new BufferedImage(width, Utils.VOFFSET, color);
                xDif = 0;
                yDif = Utils.VOFFSET;
            }
            Graphics2D keyG = extras.createGraphics();
            Graphics2D combinedG = combined.createGraphics();
            // adding the key and other extra info, if necessary.
            keyG = Utils.addExtraInfo(keyG, keyOn, costOn, polytomyOn, dim, SOLUTIONCOST, COSTMODEL);
            keyG.dispose();
            combinedG.drawImage(extras, 0, 0, null);
            combinedG.drawImage(tree, xDif, yDif, null);
            //</editor-fold>
        } else {
            combined = new BufferedImage(width, height, color);
            Graphics2D combinedG = combined.createGraphics();
            combinedG.drawImage(tree, 0, 0, null);  
            
        }
        Graphics2D combinedG = combined.createGraphics();
        combinedG.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        combinedG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        combinedG.dispose();
        if (grayOn) { 
            int w = combined.getWidth();
            int h = combined.getHeight();
            colored = new BufferedImage(w, h, gray);
            Graphics2D grayG = colored.createGraphics();
            grayG.setColor(Color.WHITE);
            grayG.fillRect(0, 0, w, h);
            grayG.drawImage(combined, 0, 0, null);
            grayG.dispose();
        } else {
            colored = combined;
        }
        if (IMAGE_WIDTH == c.getWidth() && IMAGE_HEIGHT == c.getHeight()) {
            scaled = colored;
        } else {
            scaled = disclosedPanel.transformImage(colored, IMAGE_WIDTH, IMAGE_HEIGHT);
        }
    ImageIO.write(scaled, "png", new File(path));
    }
    
    // contains the code to output to an eps file.  the actual eps code comes
    // from the generateEps method in Utils.java
    ////////////
    // NOTICE //
    ////////////
    // EPS output currently unsupported in Jane, but this method should work
    // correctly when it is
    private void outputEPS(boolean grayOn, boolean costOn, boolean keyOn, 
                           Component c, String path) throws IOException {
        // An EpsGraphics object is obtained, the shapes 
        // list is cleared, and the eps code is written out
        // to a file.
        FileWriter fstream = new FileWriter(path);
        BufferedWriter out = new BufferedWriter(fstream);
        int width = c.getWidth();
        int height = c.getHeight();
        double yS = IMAGE_HEIGHT / height;
        Dimension dim = new Dimension(width, height);
        Dimension curDim = new Dimension((int)IMAGE_WIDTH, (int)IMAGE_HEIGHT);
        String solution = Utils.generateEPS((SolutionViewer)parent, grayOn, keyOn, costOn, curDim, c);
        // If extras are off or if no settings are on, 
        // we don't bother with anything fancy and just
        // output the solution code.
        if (keyOn || costOn) {
            ColorMode colormode;
            String retranslate = "";
            if (grayOn)
                colormode = ColorMode.GRAYSCALE;
            else
                colormode = ColorMode.COLOR_RGB;
            if (keyOn) {
                // The h / yS is needed because the key is for some reason
                // h distance out of place otherwise, and that distance was
                // scaled before the key is added.
                retranslate = -Utils.KEYWIDTH + " " + IMAGE_HEIGHT / yS  + " translate\n";
            } else if (!keyOn && costOn) {
                retranslate = "0 " + (height + Utils.VOFFSET) + " translate\n";
            }
            out.write(solution);
            out.write(retranslate);
            EpsGraphics extra = new EpsGraphics("", 0, 0, (int)IMAGE_WIDTH, (int)IMAGE_HEIGHT, colormode);
            extra = (EpsGraphics)Utils.addExtraInfo(extra, keyOn, costOn, polytomyOn, dim, SOLUTIONCOST, COSTMODEL);
            // removing unnecessary lines from the code.
            String info = extra.toString();
            String[] lines = info.split("\n");
            for (int i = 0; i < lines.length - 5; i++) {
                out.write(lines[i]+ "\n");
            }
        } else {
            out.write(solution);
        }
        out.close();  
    }
    
    // Changes the dimensions of the filechooser to better fit its components.
    // called whenever sidePanel is shown or hidden.
    private void changeDim(Dimension newPref) {
        // The preferred size of the saveFile filechooser.
        Dimension savePref = saveFile.getPreferredSize();
        int w = savePref.width;
        int h = savePref.height;
        // the new preferred width and height of sidePanel after it's been
        // shown or hidden.
        int newW = newPref.width;
        int newH = newPref.height;
        // If the new width is greater than saveFile's old preferred width,
        // the sidePanel has been shown, and we increase the size of
        // the save file dialog to fit it properly.
        if (lastSideW < newW) {
            saveFile.setPreferredSize(new Dimension(w + newW, h + newH));
        } else
            saveFile.setPreferredSize(oldSize); 
        lastSideW = newW;
        // otherwise, we set the preferred size of the fileSaver to its default
        // size, because the sidePanel has shrunk.
        // we repaint and pack again afterwards so everything is updated 
        this.pack();
        this.repaint();
    }

    // used to change the size of the image preview.  This method calls 
    // a method in JanePreviewPanel that does the actual resizing.
    public void changeImageDim(int w, int h) {
        // setting the new dimension for the preview panel and resizing
        // the preview
        disclosedPanel.setPreviewDimensions(w, h);
        Dimension newSize = disclosedPanel.getPreferredSize();
        IMAGE_WIDTH = w;
        IMAGE_HEIGHT = h;
        changeDim(newSize);
        // toggling and repainting the sidePanel so that the new size
        // comes in.
        sidePanel.toggle();
        sidePanel.toggle();
        sidePanel.repaint();
    }
}
