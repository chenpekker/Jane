/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.gui;

import com.apple.eawt.AboutHandler;
import com.apple.eawt.AppEvent.AboutEvent;
import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 *
 * @author andrew
 */
// handles the "about" menu item in the "Jane" menu on Mac OS X.  
// this lets us have our custom text instead of the default menu item.
public class MacOSAboutHandler implements AboutHandler{
    
    JPanel action_panel;
    Design parent;
    
     public MacOSAboutHandler(JPanel action_panel, Design parent) {
         this.action_panel = action_panel;
         this.parent = parent;
    }
    @Override
    public void handleAbout(AboutEvent ae) {
    String message = "Version " + Design.VERSION_NUMBER + "\n"
                + "\n"
                + "Jane was developed with generous support from the National Science Foundation \n"
                + "and HHMI in the lab of Ran Libeskind-Hadas at Harvey Mudd College.  The Jane \n"
                + "design and development team includes Chris Conow, Daniel Fielder, Yaniv Ovadia, \n"
                + "Benjamin Cousins, John Peebles, Tselil Schramm, Anak Yodpinyanee, Kevin Black, \n"
                + "Rebecca Thomas, David Lingenbrink, Ki Wan Gkoo, Nicole Wein, Andrew Michaud, \n"
                + "Jordan Ezzell, Bea Metitiri, Jason Yu, and Lisa Gai. ";
    JOptionPane.showMessageDialog(action_panel, message, "About Jane", JOptionPane.INFORMATION_MESSAGE);
    }
    
}
