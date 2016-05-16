/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * JProgressDialog.java
 *
 * Created on Jul 28, 2010, 2:06:58 PM
 */

package edu.hmc.jane.gui;

import java.awt.Frame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import org.jdesktop.layout.GroupLayout;

/**
 *
 * @author jpeebles
 */
public class JProgressDialog extends javax.swing.JDialog {
    private Thread t;
    

    private JLabel label = new JLabel();;
    private JProgressBar progress = new JProgressBar();;

    private static int pbptCount = 0;

    /** Creates new form JProgressDialog */
    public JProgressDialog(Frame parent, String title, String text) {
        super(parent, title);
        label.setText(text);
        initComponents();

        //set location to middle of parent if we have one
        if(parent!=null) {
            setLocation(parent.getX()+parent.getWidth()/2-getWidth()/2,
                        parent.getY()+parent.getHeight()/2-getHeight()/2);
        }
    }

    /*
     * shows the dialog, but does so in a new thread, so that this method doesn't
     * block further execution of the original thread. This should never be
     * called from the Swing EDT, as the dialog shown will be empty.
     */
    public void display() {
        t=new Thread("Progress Blocking Prevention Thread-" + ++pbptCount) {
            @Override
            public void run() {
                setVisible(true);
            }
        };
        t.start();
    }

    public void done() {
        setVisible(false);
        dispose();
    }

    /*
     * runs the given Runnable outside of the Swing EDT while displaying a
     * JProgressDialog until it completes
     */
    public static void runTask(String taskName, String taskDescription, Frame parent, Runnable r) {
        final JProgressDialog dialog = new JProgressDialog(parent, taskName, taskDescription);
        dialog.display();

        new Thread(r,"Task Associated with Progress Dialog-" + pbptCount) {
            @Override
            public void run() {
                super.run(); //ignore the netbeans advice here
                dialog.done();
            }
        }.start();

    }
   
    public static void runTaskAndWait(String taskName, String taskDescription, Frame parent, Runnable r) {
        final JProgressDialog dialog = new JProgressDialog(parent, taskName, taskDescription);
        dialog.display();

        Thread taskThread = new Thread(r,"Task Associated with Progress Dialog-" + pbptCount) {
            @Override
            public void run() {
                super.run(); //ignore the netbeans advice here
                dialog.done();
            }
        };
        taskThread.setPriority(Thread.MAX_PRIORITY);
        taskThread.start();
        
    }

    public static void main(String[] args) {
        new JProgressDialog(null, "", "").display();
    }


    private void initComponents() {

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        progress.setIndeterminate(true);

        label.setHorizontalTextPosition(SwingConstants.CENTER);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        layout.setAutocreateGaps(true);
        layout.setAutocreateContainerGaps(true);

        int width = label.getPreferredSize().width;

        int progHeight = progress.getPreferredSize().height;

        layout.setHorizontalGroup(
            layout.createParallelGroup()
                .add(label, width, width, width)
                .add(progress, width, width, width)
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .add(label)
                .add(progress, progHeight, progHeight, progHeight)
        );

        pack();
    }
}
