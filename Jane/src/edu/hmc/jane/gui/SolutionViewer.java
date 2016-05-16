package edu.hmc.jane.gui;

import edu.hmc.jane.CostModel;
import edu.hmc.jane.gui.light.JLightAdapter;
import edu.hmc.jane.solving.EventSolver;
import edu.hmc.jane.solving.SolutionViewerInfo;
import edu.hmc.jane.solving.SolutionViewerInfo.EventInfo;
import edu.hmc.jane.solving.Solver;
import edu.hmc.jane.solving.SupportPopulation;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.*;
import java.io.File;
import java.util.LinkedList;
import javax.swing.*;
import net.sf.epsgraphics.ColorMode;

public class SolutionViewer extends JFrame {

    private int solutionCost;
    private Solver solver;
    public SolutionViewerInfo info; //all the information needed to draw
    private boolean tipsVisible;
    private boolean supportVisible;
    private boolean isPolytomy;
    private int supportType = 3;
    private DrawingObjects panel; //where everything's drawn
    private Key key;
    private JLightAdapter keyAdapter;
    private ZoomJViewport viewport; //for zooming
    private JMenuBar menuBar;
    private javax.swing.JFileChooser saveFile;
    private JLightAdapter adapter;
    private SolutionTableModel solutionModel;
    protected int solutionNumber;
    SupportPopulation supportPop; //access to shared support population

    public class SupportOptions {
        public static final int DISTINCT = 0;
        public static final int NONE = 1;
    }

    /**
     * Creates a new viewer frame to view solutions to cophylogeny problems.
     * hostTree and solution must both come from the same Solver instance.
     */
    public SolutionViewer(EventSolver solver, SolutionViewerInfo info, int solutionNumber, SolutionTableModel solutionModel, boolean isPolytomyInFiles, SupportPopulation supportPop) {
        Utils.initIcons(this);
        solutionCost = 0;
        this.solver = solver;
        this.info = info;
        this.solutionNumber = solutionNumber;
        this.tipsVisible = true;
        this.supportVisible = false;
        this.isPolytomy = isPolytomyInFiles;
        this.solutionModel = solutionModel;
        this.supportPop = supportPop;
        ToolTipManager.sharedInstance().setInitialDelay(300);
        initFrame();
        initKey();
    }

    public int getCost() {
        return solutionCost;
    }

    private void initFrame() {
        setSize(700, 700);
        panel = new DrawingObjects(info, this);
        adapter = new JLightAdapter(panel);
        viewport = new ZoomJViewport(adapter);
        add(viewport);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initializeMenuBar();

        setLocationByPlatform(true);

        setVisible(true);
    }

    private void initKey() {
        if (Design.keyDialog == null) {
            key = new Key();
            keyAdapter = new JLightAdapter(key);
            Design.keyDialog = new JDialog();
            Design.keyDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            Design.keyDialog.setSize(new Dimension(key.getMinimumWidth(),
                                                    key.getMinimumHeight()));
            Design.keyDialog.setResizable(false);
            Design.keyDialog.setTitle("Key");
            Design.keyDialog.add(keyAdapter);
            Design.keyDialog.addWindowListener(new WindowListener() {

                @Override
                public void windowClosed(WindowEvent e) {
                    Design.keyOpen = false;
                }

                @Override
                public void windowOpened(WindowEvent we) {
                    Design.keyOpen = true;
                }

                @Override
                public void windowClosing(WindowEvent we) {
                }

                @Override
                public void windowIconified(WindowEvent we) {
                }

                @Override
                public void windowDeiconified(WindowEvent we) {
                }

                @Override
                public void windowActivated(WindowEvent we) {
                }

                @Override
                public void windowDeactivated(WindowEvent we) {
                }
            });
        }
    }

    private void initializeMenuBar() {
        menuBar = new JMenuBar();

        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(menu);
        JMenu options = new JMenu("Options");
        options.setMnemonic(KeyEvent.VK_O);
        menuBar.add(options);
        JMenu support = new JMenu("Support Values");
        support.setMnemonic(KeyEvent.VK_V);
        menuBar.add(support);

        JMenuItem saveTiming = new JMenuItem("Save Timing", KeyEvent.VK_S);
        saveTiming.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        saveTiming.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveTiming();
            }
        });
        menu.add(saveTiming);

        JMenuItem saveImageE = new JMenuItem("Save Entire Image", KeyEvent.VK_I);
        saveImageE.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        saveImageE.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveImage(true);
            }
        });
        menu.add(saveImageE);

        JMenuItem closeWindow = new JMenuItem("Close Window", KeyEvent.VK_W);
        closeWindow.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        closeWindow.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                closeWindow();
            }
        });
        menu.add(closeWindow);

        JMenuItem toggleTipNames = new JMenuItem("Toggle Tip Names", KeyEvent.VK_T);
        toggleTipNames.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        toggleTipNames.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                toggleTipNames();
            }
        });
        options.add(toggleTipNames);

        JMenuItem toggleOnSupportValues = new JMenuItem("Show Support Values", KeyEvent.VK_D);
        toggleOnSupportValues.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        toggleOnSupportValues.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                toggleSupportValues(SupportOptions.DISTINCT);
            }
        });
        support.add(toggleOnSupportValues);

        JMenuItem toggleOffSupportValues = new JMenuItem("Hide Support Values", KeyEvent.VK_F);
        toggleOffSupportValues.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        toggleOffSupportValues.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                toggleSupportValues(SupportOptions.NONE);
            }
        });
        support.add(toggleOffSupportValues);

        JMenuItem showKey = new JMenuItem("Show Key", KeyEvent.VK_K);
        showKey.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        showKey.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                showKey();
            }
        });
        options.add(showKey);
        
        JCheckBoxMenuItem displayHostPolytomyResolution = new JCheckBoxMenuItem("Display Host Polytomy Resolution");
        displayHostPolytomyResolution.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                showHostPolytomyResolution();
            }
        });
        options.add(displayHostPolytomyResolution);
        displayHostPolytomyResolution.setSelected(true);
        if (isPolytomy) {
            displayHostPolytomyResolution.setEnabled(true);
            displayHostPolytomyResolution.setSelected(true);
        } else {
            displayHostPolytomyResolution.setEnabled(false);
            displayHostPolytomyResolution.setSelected(false);
        }
        
        JCheckBoxMenuItem displayParasitePolytomyResolution = new JCheckBoxMenuItem("Display Parasite Polytomy Resolution");
        displayParasitePolytomyResolution.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                showParasitePolytomyResolution();
            }
        });
        options.add(displayParasitePolytomyResolution);
        if (isPolytomy) {
            displayParasitePolytomyResolution.setEnabled(true);
            displayParasitePolytomyResolution.setSelected(true);
        } else {
            displayParasitePolytomyResolution.setEnabled(false);
            displayParasitePolytomyResolution.setSelected(false);
        }

        setJMenuBar(menuBar);
    }

    private void toggleTipNames() {
        if (tipsVisible) {
            panel.turnOffTips();
            tipsVisible = false;
        } else {
            panel.turnOnTips();
            tipsVisible = true;
        }
    }

    private void toggleSupportValues(int type) {
        switch(type) {
            case SupportOptions.DISTINCT:
                if (supportVisible && panel.supportType == SupportOptions.DISTINCT) {
                    supportVisible = false;
                    panel.turnOffSupport();
                } else {
                    panel.supportType = SupportOptions.DISTINCT;
                    supportVisible = true;
                    panel.turnOnSupport();
                }
                break;
            case SupportOptions.NONE:
                supportVisible = false;
                panel.turnOffSupport();
                break;
            default:
                if (supportVisible) {
                    supportVisible = false;
                    panel.turnOffSupport();
                } else {
                    supportVisible = true;
                    panel.turnOnSupport();
                }
        }
    }

    private void showKey() {
        Design.keyDialog.setVisible(true);
        Design.keyOpen = true;
    }

    private void saveTiming() {
        // setting the directory to the last visited directory, if there was
        // one
        if (Design.lastDirectory != null)
            saveFile = new JFileChooser(Design.lastDirectory);
        else
            saveFile = new JFileChooser();
        SimpleFilter tmgFilter = new SimpleFilter(".tmg", "Jane timing file, (.tmg)");
        saveFile.addChoosableFileFilter(tmgFilter);
        saveFile.setFileFilter(tmgFilter);
        saveFile.setAcceptAllFileFilterUsed(false);
        int saveResult = saveFile.showSaveDialog(this);
        if (saveResult == javax.swing.JFileChooser.APPROVE_OPTION) {
            // checking if the file already exists before saving.
            String filename = saveFile.getSelectedFile().getName();
            String filepath = saveFile.getSelectedFile().getAbsolutePath();
            System.out.println("filepath = " + filepath);
            if (!filename.contains(".")) {
                filename += ".tmg";
                filepath += ".tmg";
            }
            int nameLength = filename.length();
            int fullLength = filepath.length();
            int dif = fullLength - nameLength;
            String upDirPath = filepath.substring(0, dif);
            File newFile = new File(upDirPath);
            String[] fileList = newFile.list();
            for (int f = 0; f < fileList.length; f++) {
                if (fileList[f].equals(filename)) {
                    int option = JOptionPane.showConfirmDialog(this, filename + " "
                            + "already exists. Would you like to overwrite it?" , "", JOptionPane.YES_NO_OPTION);
                    // asking the user to confirm that they would like to overwrite the file.
                    if (option == JOptionPane.YES_OPTION) {
                        try {
                            java.io.FileWriter fw = new java.io.FileWriter(filepath);
                            fw.write(info.hostTiming.fileTimingString());
                            fw.close();
                        } catch(java.io.IOException e) {
                            JOptionPane.showMessageDialog(this, "Unable to write to the specified filename", "Error Writing File", JOptionPane.ERROR_MESSAGE);
                            System.err.println(e);
                        }
                        // changing last visited directory.
                        Design.lastDirectory = saveFile.getCurrentDirectory();
                    }
                    // user didn't want to overwrite the file, exiting.
                    else{
                        return;
                    }
                }
            }
            try {
                java.io.FileWriter fw = new java.io.FileWriter(filepath);
                fw.write(info.hostTiming.fileTimingString());
                fw.close();
            } catch(java.io.IOException e) {
                JOptionPane.showMessageDialog(this, "Unable to write to the specified filename", "Error Writing File", JOptionPane.ERROR_MESSAGE);
                System.err.println(e);
            }
            // changing last visited directory.
            Design.lastDirectory = saveFile.getCurrentDirectory();
        }
    }

    private void saveImage(boolean saveEntireImage) {
        viewport.disableAutoScroll();
        Component c;
        if (saveEntireImage)
            c = adapter;
        else
            c = viewport;
        solutionCost = panel.solutionCost;
        Utils.promptAndSave(this, c, panel.solutionCost, true);
        viewport.enableAutoScroll();
    }
    
    private void closeWindow() {
        this.dispose();
    }
    
    private void showHostPolytomyResolution() {
        this.panel.highlightHostPolytomy = ! panel.highlightHostPolytomy;
        panel.repaint();
        
        // POLY: redraw!
    }
    
    private void showParasitePolytomyResolution() {
        panel.highlightParasitePolytomy = !panel.highlightParasitePolytomy;
        panel.repaint();
    }

    public void updateSupportValues() {
        if (panel.modified) {
           panel.pRoot.drawEventSupport(null, panel.pRoot.event);
           return;
        }
        if (supportVisible) {
            solver.events = info.events;  
            solutionModel.computeConfidence(solver, panel, supportPop);
        }      
    }
    
    public EventInfo getEventTree(){
        return panel.pRoot.event;
    }
    
    public double getScale() {
        return viewport.scale;
    }
    
    // passed into JaneFileChooser so we know if polytomies should be displayed on
    // the solution key.
    public boolean polytomyOn() {
        boolean result;
        boolean hasPolytomy;
        boolean highlightPolytomy;
        hasPolytomy = (panel.info.parasiteTree.isPolytomyResolution() || panel.info.hostTree.isPolytomyResolution());
        highlightPolytomy = (panel.highlightHostPolytomy || panel.highlightParasitePolytomy);
        result = (hasPolytomy && highlightPolytomy);
        return result;
    }
    
    /* Generates the encapsulated postScript (EPS) to display the final embedding
     * of the parasite tree onto the host tree.  This is called from Utils.java
     * where the decision to generate EPS is made.
     * 
     * TODO: Dragging nodes move properly in preliminary tests, but nodeType
     * doesn't always update properly.  This can probably be fixed in the
     * dragging code.
     * 
     * TODO: LOSSES still are not drawn properly, though it appears that 
     * I can find them. Loss status might not be removed after node drag.
     * 
     * NOTE: This feature works for a good number of trees but is NOT complete.
     */
    public String generateEPS(int height, boolean grayOn) {
        //The first half of this is dealing with generating the EPS for the host
        //tree.

        ColorMode cm;
        if (grayOn) cm = ColorMode.GRAYSCALE;
        else cm = ColorMode.COLOR_RGB;
        HostNode hroot = this.panel.hRoot;
        ParasiteNode proot = this.panel.pRoot;

        String result = "1.5 setlinewidth\n"
                      + hroot.horizPosition + " " + (height - hroot.vertPosition) + " moveto\n"
                      + "0 " + (height - hroot.vertPosition) + " lineto\nstroke\n";

        HostNode currHost;
        LinkedList<HostNode> hostList = new LinkedList<HostNode>();
        hostList.add(hroot.Lchild);
        hostList.add(hroot.Rchild);

        //FIXME
        //       Uses +3 and -3 for positioning, maybe create a global
        //       constant called NODEOFFSET
        result += "/Helvetica findfont 12 scalefont setfont\n";
        while (hostList.size() != 0) {
            currHost = hostList.pop();

            if (currHost.Lchild != null) {
                hostList.add(currHost.Lchild);
            }
            if (currHost.Rchild != null) {
                hostList.add(currHost.Rchild);
            }

            result += currHost.horizPosition + " " + (height - currHost.vertPosition) + " moveto \n"
                    + currHost.Parent.horizPosition + " " + (height - currHost.vertPosition) + " lineto \n"
                    + currHost.Parent.horizPosition + " " + (height - currHost.Parent.vertPosition)
                    + " lineto \n stroke \n newpath\n";
                
            if (currHost.isTip) {

                result += moveTo(currHost.horizPosition, height - 3 - currHost.vertPosition)
                        + "( " + currHost.tipName.getText() + " ) show\n";
            }
            //TODO: Adjust text position accounted for by -3.
            //      Font size doesn't seem to be scaled with the rest of the tree
        }

        // This next block deals with generating the EPS for the parasite tree 

        ParasiteNode currPar;
        LinkedList<ParasiteNode> parList = new LinkedList<ParasiteNode>();

        parList.add(proot.Lchild);
        parList.add(proot.Rchild);

        result += "1 setlinewidth\n";

        //Draw the Handle and the root node
        result += moveTo(proot.getX() - 3, (height - proot.getY()))
                + lineTo((proot.getX() - 15), (height - proot.getY()))
                + moveTo(proot.getX(), height - proot.getY())
                + changePSColor(Color.blue, cm)
                + "stroke\n"
                + changePSColor(proot.color, cm)
                + drawNode(proot.event.eventType, proot.getX(), height - proot.getY())
                + changePSColor(Color.blue, cm);

        if (proot.hasLoss) {
            System.out.println("dero");
                if (proot.Lchild.getY() == proot.lossY) {
                    proot.Lchild.isLoss = true;
                } else {
                    proot.Rchild.isLoss = true;
                }
            }
        
        // Use parList as a Queue, going through the tree breadth-first, drawing
        // as desired.
        while (parList.size() != 0) {
            String nodePS = "";
            currPar = parList.pop();

            if (currPar.Lchild != null) {
                parList.add(currPar.Lchild);
            }
            if (currPar.Rchild != null) {
                parList.add(currPar.Rchild);
            }

            result += moveTo((currPar.getX()), (height - currPar.getY()));

            if (currPar.event.eventType == CostModel.TIP) {
                nodePS += "gsave\n"
                        + changePSColor(currPar.color, cm)
                        + moveTo(currPar.getX(), height - 3 - currPar.getY())
                        + "( " + currPar.tipName.getText() + " ) show\n"
                        + drawNode(CostModel.TIP, currPar.getX(), (height - currPar.getY()))
                        + "grestore\n";
            }

            if (currPar.event.eventType == CostModel.COSPECIATION) {
                nodePS += "gsave\n"
                        + changePSColor(currPar.color, cm)
                        + drawNode(CostModel.COSPECIATION, currPar.getX(), (height - currPar.getY()))
                        + "grestore \n";
            }

            if (currPar.event.eventType == CostModel.HOST_SWITCH) {
                nodePS += "gsave\n"
                        + changePSColor(currPar.color, cm)
                        + drawNode(CostModel.HOST_SWITCH, currPar.getX(), (height - currPar.getY()))
                        + "grestore \n";
            }

            if (currPar.event.eventType == CostModel.DUPLICATION) {
                nodePS += "gsave\n"
                        + changePSColor(currPar.color, cm)
                        + drawNode(CostModel.DUPLICATION, currPar.getX(), (height - currPar.getY()))
                        + "grestore \n";
            }
            
            if (currPar.event.eventType == CostModel.FAILURE_TO_DIVERGE) {
                nodePS += "gsave\n"
                        + moveTo(currPar.getX(), height- currPar.getY())
                        + "(FTD) show\n"
                        + "grestore\n";
            }

            // If Parent has loss mark child (who draws the edge) to let the child
            // know it needs to draw a loss edge
            if (currPar.hasLoss) {
                if (currPar.Lchild.getY() == currPar.lossY) {
                    currPar.Lchild.isLoss = true;
                } else {
                    currPar.Rchild.isLoss = true;
                }
            }

            //Check here if Parent was a host switch in order to draw the arrow
            if (currPar.Parent.event.eventType == CostModel.HOST_SWITCH) {

                int arrowTipX = currPar.Parent.getX();
                int arrowTipY = ((height - currPar.Parent.getY()) + (height - currPar.getY())) / 2;
                
                if (currPar.Parent.getY() < currPar.getY()) {
                    result += drawHSArrowUp(arrowTipX, arrowTipY);
                    
                } else if (currPar.Parent.getY() > currPar.getY()) {
                    result += drawHSArrowDown(arrowTipX, arrowTipY);
                }
            }


            result += moveTo(currPar.getX() - 3, (height - currPar.getY()));
            
            
            //TODO: Get this working for multiple losses in a row and simultaneous losses
            if (currPar.isLoss) {
                HostNode hostNode = currPar.hostNodes[currPar.getHostLocation().ID].Parent;
                
                result += "gsave\n" + moveTo(currPar.getX(), height - currPar.getY())
                        +lineTo(hostNode.horizPosition - panel.edgeSpacing, height- currPar.getY())
                        + lineTo(hostNode.horizPosition- panel.edgeSpacing, height - hostNode.vertPosition + panel.edgeSpacing);
                
                hostNode = hostNode.Parent;
                
                HostNode stepNode = currPar.hostNodes[currPar.getHostLocation().ID].Parent;
                while (hostNode.index != (currPar.Parent.hostNodes[currPar.Parent.getHostLocation().ID].index)) {
                    result += lineTo(hostNode.horizPosition - panel.edgeSpacing, height- stepNode.vertPosition + panel.edgeSpacing)
                            + lineTo(hostNode.horizPosition- panel.edgeSpacing, height - hostNode.vertPosition + panel.edgeSpacing); //This line needs to be dotted
                    stepNode = hostNode;
                    hostNode = hostNode.Parent;
                }
                
                result += lineTo(currPar.Parent.getX(), height - stepNode.vertPosition + panel.edgeSpacing)
                        + lineTo(currPar.Parent.getX(), height - currPar.Parent.getY())
                        + "stroke\n"
                        + "grestore \n";
                
            } else {
                // If node and its parent are on the same horizontal line, then
                // don't send the line into the node        
                if (currPar.Parent.getY() == currPar.getY()) {
                    result += lineTo((currPar.Parent.getX() + 3), (height - currPar.getY()));
                } else {
                    result += lineTo(currPar.Parent.getX(), (height - currPar.getY()));
                }

                // Don't send vertical lines into the node either
                if (currPar.Parent.getY() < currPar.getY()) {
                    result += lineTo(currPar.Parent.getX(), (height - 3 - currPar.Parent.getY()));
                } else if (currPar.Parent.getY() > currPar.getY()) {
                    result += lineTo(currPar.Parent.getX(), (height + 3 - currPar.Parent.getY()));
                }
            }
            result += "stroke \nnewpath\n" + nodePS;
        }
        return result;
    }

    // Returns the postScript string that changes the color things are drawn in.
    private String changePSColor(Color color, ColorMode colormode) {
        String out = "";
        if (colormode.equals(ColorMode.COLOR_RGB)) {
            if (color == Color.orange) {
                out = "1.0 0.7 0.0 setrgbcolor\n";
            } else if (color == Color.green) {
                out = "0.0 1.0 0.0 setrgbcolor\n";
            } else if (color == Color.red) {
                out = "1.0 0.0 0.0 setrgbcolor\n";
            } else if (color == Color.blue) {
                out = "0.0 0.0 1.0 setrgbcolor\n";
            } else {
                out = "0.0 0.0 0.0 setrgbcolor\n";
            }
        } else if (colormode.equals(ColorMode.GRAYSCALE)) {
            if (color == Color.orange) {
                out = "0.57 setgray\n";
            } else if (color == Color.green) {
                out = "0.33 setgray\n";
            } else if (color == Color.red) {
                out = "0.33 setgray\n";
            } else if (color == Color.blue) {
                out = "0.33 setgray\n";
            } else {
                out = "0 setgray\n";
            }
        }
        return out;
    }

    // Returns a String containing the postScript code moving the drawing point
    // to the given x and y location
    private String moveTo(int x, int y) {
        return x + " " + y + " moveto\n";
    }

    // Returns the postScript code drawing a line from the current cursor location
    // to the given x,y location.
    private String lineTo(int x, int y) {
        return x + " " + y + " lineto\n";
    }

    /*
     * Returns the postScript code required to draw a node at the provided x, y
     * location, differentiating between Cospeciation nodes which are not filled
     * in, and the rest that are filled in.
     * 
     * TODO: Move color change code down here instead of the color being changed
     * before drawing the code.
     */
    private String drawNode(int nodeType, int x, int y) {
        if (nodeType == CostModel.COSPECIATION) {
            return x + " " + y + " 3 0 360 arc closepath\n"
                    + x + " " + y + " 2 360 0 arcn closepath \nfill\n";
        } else {
            return x + " " + y + " 3 0 360 arc closepath\nfill\n";
        }
    }
    
    private String drawHSArrowUp(int arrowTipX, int arrowTipY) {
        return "\n%%Drawing arrow tip\n"
                            + "gsave\n"
                            + moveTo(arrowTipX, arrowTipY)
                            + lineTo(arrowTipX - 4, arrowTipY + 8)
                            + moveTo(arrowTipX, arrowTipY)
                            + lineTo(arrowTipX + 4, arrowTipY + 8)
                            + "stroke\n"
                            + "grestore\n";
    }
    
    private String drawHSArrowDown(int arrowTipX, int arrowTipY) {
        return "\n%%Drawing arrow tip\n"
                            + "gsave\n"
                            + moveTo(arrowTipX, arrowTipY)
                            + lineTo(arrowTipX - 4, arrowTipY - 8)
                            + moveTo(arrowTipX, arrowTipY)
                            + lineTo(arrowTipX + 4, arrowTipY - 8)
                            + "stroke\n"
                            + "grestore\n";
    }
}
