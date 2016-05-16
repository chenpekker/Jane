/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.gui;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author andrew
 */
// a class that makes it easy to add file filters to the file save dialog.
public class SimpleFilter extends FileFilter{
    
    private String description = null;
    private String extension = null;
    
    public SimpleFilter(String ext, String desc) {
        description = desc;
        extension = ext.toLowerCase();
    }

    @Override
    public boolean accept(File f) {
        if (f == null) return false;
        if (f.isDirectory()) return true;
        return f.getName().toLowerCase().endsWith(extension);
    }

    @Override
    public String getDescription() {
        return description;
    }   
}
