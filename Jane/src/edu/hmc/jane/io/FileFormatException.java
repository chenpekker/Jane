/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.io;

/**
 *
 * @author jpeebles
 */
public class FileFormatException extends Exception {
    FileFormatException(String description) {
        super(description);
    }
}
