/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import javax.swing.*;

/**
 *
 * @author andrew
 */
public class JanePreviewPanel extends JPanel{
    // limits on how large the image preview for the file we're saving can be.
    private static double PREVIEWWIDTH = 500.0;
    private static double PREVIEWHEIGHT = 400.0;
    // the current dimensions of the file that will be saved.
    public static double IMAGE_WIDTH;
    public static double IMAGE_HEIGHT;
    // the dimensions of the original component
    public static int C_WIDTH;
    public static int C_HEIGHT;
    // the panel that holds the image preview as well as the labels and
    // the resize button.
    private JPanel imagePanel;
    // label for the height and width of the preview
    private JLabel dimLabel;
    // the title for the preview panel
    private JLabel title;
    // the button that opens the resize window
    private JButton resize;
    // the actual image preview
    private JLabel preview;
    // the original component that we will be drawing from.  All scaling is
    // done from this image, and not from whatever the last image was.
    Component component;
    private BufferedImage image;
    private BufferedImage finalBI;
            
    public JanePreviewPanel(final Component c, final JaneFileChooser jfc) {
        // initializing variables and constants
        component = c;
        // initializing the image dimensions. Unnecessary now, but we need to
        // be able to retrieve that information later so that the resizing
        // frame knows how large the image should initially be.
        IMAGE_WIDTH = c.getWidth();
        IMAGE_HEIGHT = c.getHeight();
        // the dimensions of the component we're drawing from.
        C_WIDTH = c.getWidth();
        C_HEIGHT = c.getHeight();
        imagePanel = new JPanel(new BorderLayout());
        resize = new JButton("Resize");
        title = new JLabel("Image Preview");
        // setting the layout and aligning the title.
        this.setLayout(new BorderLayout());
        title.setHorizontalAlignment(SwingConstants.CENTER);
        // creating a BufferedImage at the original size of the component.
        image = new BufferedImage(C_WIDTH,  C_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        // painting the component onto the image
        Graphics2D imageG = image.createGraphics();
        c.paint(imageG);
        imageG.dispose();
        // drawing the preview image and adding a listener to the resize button.
        drawPreview(c);
        addListeners(c, jfc);
    }
    
    // getters for the image height and width.
    public double getImageHeight() {
        return IMAGE_HEIGHT;
    }

    public double getImageWidth() {
        return IMAGE_WIDTH;
    }
    
    private void addListeners(final Component c, final JaneFileChooser jfc) {
        resize.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // the constructor for this object automatically shows it, 
                // so there's no need to actually do anything to it.
                // if that's a bad programming strategy for some reason, 
                // please let amichaud know.
                ResizeableImagePanel imagePanel = new ResizeableImagePanel(c);
                ResizeableImageFrame imageFrame = new ResizeableImageFrame(jfc, imagePanel);
            }
        });
    }
    
    // draws the image preview.
    private void drawPreview(Component c) {
        BufferedImage bi;
        if (C_WIDTH != IMAGE_WIDTH && C_HEIGHT != IMAGE_HEIGHT) { 
            bi = transformImage(image, IMAGE_WIDTH, IMAGE_HEIGHT);
        } else {
            bi = image;
        }
        double yRat = IMAGE_HEIGHT / PREVIEWHEIGHT;
        double xRat = IMAGE_WIDTH / PREVIEWWIDTH;
        double w;
        double h;
        // the image needs to be scaled to fit within IMAGE_WIDTH and HEIGHT.
        // we take the ratio of those two numbers and the preview dimensions, 
        // and find which ratio is largest.  If the x-ratio is larger, 
        // we set the image width to the preview width, and scaled the height
        // accordingly to preserve the aspect ratio.  If the y-ratio is larger,
        // we set the height and scale the width. If the ratios are equal
        // we scale both.
        
        if (xRat > yRat) {
            w = PREVIEWWIDTH;
            h = IMAGE_HEIGHT / xRat;
        } else if (xRat == yRat) {
            w = PREVIEWWIDTH;
            h = PREVIEWHEIGHT;
        } else {
            h = PREVIEWHEIGHT;
            w = IMAGE_HEIGHT / yRat;
        }
        finalBI = transformImage(bi, w, h);
        preview = new JLabel(new ImageIcon(finalBI));
        preview.setLayout(new BorderLayout());
        preview.setBorder(BorderFactory.createEtchedBorder());
        // the height and width labels for the image.  Also the title for the
        // image preview.  In html code so that the two labels can have two
        // rows of text
        String width = Double.toString(IMAGE_WIDTH);
        String height = Double.toString(IMAGE_HEIGHT);
        dimLabel = new JLabel("Saved image will be " + width + " pixels by " +
                              height + " pixels");
        dimLabel.setHorizontalAlignment(SwingConstants.CENTER);
        // adding to the imagePanel
        imagePanel = new JPanel(new BorderLayout());
        imagePanel.add(title, BorderLayout.NORTH);
        imagePanel.add(preview, BorderLayout.CENTER);
        imagePanel.add(dimLabel, BorderLayout.SOUTH);
        this.add(imagePanel, BorderLayout.CENTER);
        this.add(resize, BorderLayout.SOUTH);
        this.setBorder(BorderFactory.createEtchedBorder()); 
        imagePanel.repaint();
        this.repaint();
    }
    
    // set new dimensions for the preview if the file was resized.
    public void setPreviewDimensions(double w, double h) {
        if (IMAGE_WIDTH != w) IMAGE_WIDTH = w;
        if (IMAGE_HEIGHT != h) IMAGE_HEIGHT = h;
        this.remove(imagePanel);
        drawPreview(component);
    }
    
    // returns a version of the given image scaled to the provided dimensions.
    public BufferedImage transformImage(BufferedImage oldImage, double newWidth, double newHeight) {// <editor-fold> 
        BufferedImage out = new BufferedImage((int)newWidth, (int)newHeight, oldImage.getType());
        double oldWidth = oldImage.getWidth();
        double oldHeight = oldImage.getHeight();
        double sX = newWidth / oldWidth;
        double sY = newHeight / oldHeight;
        // creating an affinetransform to scale the image to the new size.
        AffineTransform at = new AffineTransform();
        at.scale(sX, sY);
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        scaleOp.filter(oldImage, out);
        return out;
    } // </editor-fold>
}
