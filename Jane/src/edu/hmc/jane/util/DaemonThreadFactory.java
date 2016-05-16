/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * returns threads from the default factory, except that they are daemons
 * Daemon threads don't prevent the program from quitting if they are running.
 * The CLI will not terminate if you use non-daemon threads.
 * @author jpeebles
 */
public class DaemonThreadFactory implements ThreadFactory {
    private static ThreadFactory backer = Executors.defaultThreadFactory();
    private static DaemonThreadFactory instance = new DaemonThreadFactory();

    private DaemonThreadFactory() {}

    public Thread newThread(Runnable r) {
        Thread t = backer.newThread(r);
        t.setDaemon(true);

        return t;
    }

    public static ThreadFactory get() {
        return instance;
    }

}
