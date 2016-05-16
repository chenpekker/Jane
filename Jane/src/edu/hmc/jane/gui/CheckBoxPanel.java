/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 *
 * @author andrew
 */
// panel for extra settings for the JFileChooser that saves images
public class CheckBoxPanel extends JPanel {

    // Three checkboxes for the different settings and three booleans that 
    // store their current state.
    private JCheckBox check_grayscale, check_key, check_cost;
    boolean gray, key,cost;
    public final static int HISTOGRAM = 1;
    public final static int TREE = 2;
    private JPanel leftPanel;
    private JPanel centerPanel;
    private JPanel rightPanel;
    // creates a new object with the three checkboxes we care about
    public CheckBoxPanel(int num) {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        gray = false;
        key = false;
        cost = false;
        // If we're making a panel for histogram output, the only setting 
        // we need is grayscale output, so that is the only one added.
        if (num == 1) {
            JPanel onlyPanel = new JPanel();
            ItemListener lis = new ItemListener() {
                @Override 
                public void itemStateChanged(ItemEvent e) {
                    gray = !gray;
                }
            };           
            check_grayscale = new JCheckBox("Output in Grayscale");
            check_grayscale.addItemListener(lis);
            onlyPanel.add(check_grayscale);
            this.add(onlyPanel, BorderLayout.CENTER);
            Dimension onlyPref = onlyPanel.getPreferredSize();
            Dimension onlyMin = onlyPanel.getMinimumSize();
            this.setPreferredSize(onlyPref);
            this.setMinimumSize(onlyMin);
        } else { // Otherwise, all three options are added.
            leftPanel = new JPanel();
            rightPanel = new JPanel();
            centerPanel = new JPanel();
            ItemListener lis = new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    Object source = e.getItemSelectable();
                    if (source == check_grayscale) {gray = !gray;}
                    else if (source == check_key) key = !key;
                    else cost = !cost;  
                }
            };
            check_grayscale = new JCheckBox("Output in Grayscale");
            check_key =       new JCheckBox("Include Key");
            check_cost =      new JCheckBox("Include Cost");
            check_grayscale.addItemListener(lis);
            check_key.addItemListener(lis);
            check_cost.addItemListener(lis);
            leftPanel.add(check_grayscale);
            centerPanel.add(check_key);
            rightPanel.add(check_cost);
            this.add(leftPanel, BorderLayout.WEST);
            this.add(centerPanel, BorderLayout.CENTER);
            this.add(rightPanel, BorderLayout.EAST);
            // If we have all three options, we use the minimum height of one
            // of them as the preferred height (we shouldn't need any more than
            // that), and the combined preferred width of all three as the 
            // preferred width;
            Dimension pLeft = leftPanel.getPreferredSize();
            Dimension mLeft = leftPanel.getMinimumSize();
            Dimension pCent= centerPanel.getPreferredSize();
            Dimension mCent = centerPanel.getMinimumSize();
            Dimension pRight = rightPanel.getPreferredSize();
            Dimension mRight = rightPanel.getMinimumSize();
            int preH = pLeft.height;
            int minH = mLeft.height;
            int preW = pLeft.width + pCent.width + pRight.width;
            int minW = mLeft.width + mCent.width + mRight.width;
            this.setPreferredSize(new Dimension(preW, preH));
            this.setMinimumSize(new Dimension(minW, minH));           
        }
        // A border is added above the panel to give some separation and make
        // this addition look neater.
        this.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));
    }    
}
