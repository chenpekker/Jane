/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.io;

import edu.hmc.jane.TimeZones;
import edu.hmc.jane.TreeSpecs;
import edu.hmc.jane.Tree;
import javax.swing.JOptionPane;
import javax.swing.JFrame;
import java.io.BufferedReader;

/**
 *
 * @author Tselil
 */

public class TimingFileReader {

    Tree hTree;
    Tree parasiteTree;
    TimeZones timeZones;
    
    public TimingFileReader(Tree hTree, Tree pTree, TimeZones timeZones) {
        this.hTree = hTree;
        this.parasiteTree = pTree;
        this.timeZones = timeZones;
    }

    public TreeSpecs loadTimingFile(java.io.File f, JFrame frame) throws java.io.FileNotFoundException, java.io.IOException {
        BufferedReader fin = new BufferedReader(new java.io.FileReader(f));
        TreeSpecs hostTiming = new TreeSpecs(hTree, parasiteTree, timeZones);

        String s;
        int iD, host, time;
        while((s = fin.readLine()) != null) {
            String[] vals = s.split(",");
            if(vals.length >= 2) {
                iD = Integer.parseInt(vals[0]);
                if (hTree.origIDToName.containsKey(iD)) {
                    host = hTree.origIDToName.get(iD);
                    time = Integer.parseInt(vals[1]);

                    // Avert null pointer exception
                    if(hTree.size < host) {
                        JOptionPane.showMessageDialog(frame, "This file does not match the current tree", "Invalid Timing File", JOptionPane.ERROR_MESSAGE);
                        return null;
                    }

                    // Don't place tips
                    if ( time != -1 && time != hostTiming.tipTime) {
                        hostTiming.putNodeAtTime(host, time);
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "This file does not match the current tree\nIt contains node(s) not present in the tree.", "Invalid Timing File", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
            }
        }
        if (!hostTiming.isConsistent()) {
            JOptionPane.showMessageDialog(frame, "This file does not match the current tree.\nIt failed the consistency check.", "Invalid Timing File", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        return hostTiming;
    }
}
