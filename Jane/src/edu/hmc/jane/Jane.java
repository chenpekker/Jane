package edu.hmc.jane;
import java.util.Random;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author John
 */
public class Jane {

    public static boolean VERBOSE = false;

    public static Random rand = new Random();

    public static int getNumThreads() {
        return 2*Runtime.getRuntime().availableProcessors();
    }

    public static boolean is64BitJVM() {
        String arch = System.getProperty("os.arch");
        return arch.indexOf("64")!=-1;
    }
}
