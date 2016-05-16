/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.gui;

import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;
import javax.swing.JOptionPane;

/**
 *
 * @author andrew
 */
public class MacOSQuitHandler implements QuitHandler{
    
    Design parent;
    
    public MacOSQuitHandler(Design parent) {
        this.parent = parent;
    }

    @Override
    public void handleQuitRequestWith(QuitEvent qe, QuitResponse qr) {
        int option = JOptionPane.showConfirmDialog(parent, "Quitting will cause you to lose the current data. \nQuit?", "Quit?", JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
        else {
            qr.cancelQuit();
        }
    }
    
}
