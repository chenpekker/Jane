/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane;

import java.io.*;
import java.io.File;
import java.util.*;
import javax.swing.filechooser.FileFilter; 

/**
 * @author Nicole Wein
 * code adapted from stackoverflow answer by Lokesh Kumar 
 */
public class BothFileFilter extends FileFilter {
    public boolean accept(File f) {
        if (f.isDirectory())
            return true;
        
        String s = f.getName();
        
        return s.endsWith(".tree") || s.endsWith(".TREE")
                   || s.endsWith(".nex") || s.endsWith(".NEX");
    }

    public String getDescription() {
        return "All Compatible Files (.tree, .nex)";
    }
}
