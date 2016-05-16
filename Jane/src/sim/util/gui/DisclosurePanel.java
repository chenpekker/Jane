/**
 * Copyright 2008 by Sean Luke and George Mason University
 * Licensed under the Academic Free License version 3.0
 * See the file "LICENSE" for more information
 */

package sim.util.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** 
 * A panel with a small disclosure triangle which toggles between two
 * subcomponents: notionally an "abridged" (short) component and an expanded
 * ("disclosed") component.
 * The panel can sprout an optional titled label.
 *
 * Thanks to:
 * http://lists.apple.com/archives/java-dev/2005/Feb/msg00171.html
 *
 * for the idea.
 *
 * Modified by amichaud.
 */
public class DisclosurePanel extends JPanel {
    JToggleButton disclosureToggle = new JToggleButton();
    Component abridgedComponent;
    Component disclosedComponent;
    
    public DisclosurePanel(
        Component abridgedComponent, Component disclosedComponent) {
        
        this(abridgedComponent, disclosedComponent, null);
    }
        
    public DisclosurePanel(
        Component abridgedComponent, Component disclosedComponent,
        String borderLabel) {

        disclosureToggle.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        disclosureToggle.setContentAreaFilled(false);
        disclosureToggle.setFocusPainted(false);
        disclosureToggle.setRequestFocusEnabled(false);
        disclosureToggle.setIcon(UIManager.getIcon("Tree.collapsedIcon"));
        disclosureToggle.setSelectedIcon(
            UIManager.getIcon("Tree.expandedIcon"));
        this.abridgedComponent = abridgedComponent;
        this.disclosedComponent = disclosedComponent;
        setLayout(new BorderLayout());
        Box b = new Box(BoxLayout.Y_AXIS);
        b.add(disclosureToggle);
        b.add(Box.createGlue());
        add(b, BorderLayout.WEST);
        add(abridgedComponent, BorderLayout.CENTER);

        if (borderLabel != null)
            setBorder(new javax.swing.border.TitledBorder(borderLabel));
        
        disclosureToggle.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                Dimension togMin = disclosureToggle.getPreferredSize();

                if (disclosureToggle.isSelected()) { // Disclose
                    DisclosurePanel.this.remove(
                        DisclosurePanel.this.abridgedComponent);
                    DisclosurePanel.this.add(
                        DisclosurePanel.this.disclosedComponent,
                        BorderLayout.CENTER);
                    
                    // Modified here so the preferred and minimum sizes of the
                    // panel are changed whenever the visible panel is changed.
                    Dimension preferred
                        = DisclosurePanel.this.disclosedComponent.
                            getPreferredSize();
                    Dimension newPref;
                    
                    if (togMin.width > preferred.width)
                        newPref = togMin;
                    else
                        newPref = preferred;
                    
                    DisclosurePanel.this.setPreferredSize(newPref);
                    Dimension min
                        = DisclosurePanel.this.disclosedComponent.
                            getMinimumSize();
                    Dimension newMin;
                    
                    if (togMin.width > min.width)
                        newMin = togMin;
                    else
                        newMin = min;
                    
                    DisclosurePanel.this.setMinimumSize(newMin);
                    DisclosurePanel.this.revalidate();
                } else { // Hide
                    DisclosurePanel.this.remove(
                        DisclosurePanel.this.disclosedComponent);
                    DisclosurePanel.this.add(
                        DisclosurePanel.this.abridgedComponent,
                        BorderLayout.CENTER);
                    
                    // Code modified here as well.
                    Dimension c
                        = DisclosurePanel.this.abridgedComponent.
                            getPreferredSize();
                    Dimension newPref;
                    
                    if (togMin.width > c.width)
                        newPref = togMin;
                    else
                        newPref = c;
                    
                    DisclosurePanel.this.setPreferredSize(newPref);
                    Dimension min
                        = DisclosurePanel.this.abridgedComponent.
                            getMinimumSize();
                    Dimension newMin;
                    
                    if (togMin.width > min.width)
                        newMin = togMin;
                    else
                        newMin = min;
                    
                    DisclosurePanel.this.setMinimumSize(newMin);
                    DisclosurePanel.this.revalidate();
                }
            }
        });
    }
      
    /**
     * Changes the state of the JToggleButton and whether the disclosed or
     * abridged panel is shown.
     */
    public void toggle() {
       disclosureToggle.setSelected(!disclosureToggle.isSelected());
    }
    
    public void setAbridgedComponent(Component abridgedComponent) {
        if (!disclosureToggle.isSelected()) {
            remove(this.abridgedComponent);
            add(abridgedComponent, BorderLayout.CENTER);
            revalidate();
        }

        this.abridgedComponent = abridgedComponent;
    }

    public void setDisclosedComponent(Component disclosedComponent) {
        if (disclosureToggle.isSelected()) {
            remove(this.disclosedComponent);
            add(disclosedComponent, BorderLayout.CENTER);
            revalidate();
        }

        this.disclosedComponent = disclosedComponent;
    }
}
