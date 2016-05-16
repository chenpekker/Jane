/**
 * EpsDocument.java
 * MODIFIED
 *
 * This file is part of the EPS Graphics Library
 * 
 * The EPS Graphics Library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * The EPS Graphics Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with the EPS Graphics Library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * Copyright (c) 2001-2004, Paul Mutton
 * 
 * Copyright (c) 2006-2009, Thomas Abeel
 *  
 * Project: http://sourceforge.net/projects/epsgraphics/
 * 
 * based on original code by Paul Mutton, http://www.jibble.org/
 * 
 * Edited by Andrew Michaud to use internal StringBuffer to store data.
 */

package net.sf.epsgraphics;

import java.util.Date;

/**
 * This represents an EPS document. Several EpsGraphics2D objects may point to
 * the same EpsDocument.
 */
final class EpsDocument {
    private StringBuilder contents;
    private int minX;
    private int minY;
    private int maxX;
    private int maxY;
    private boolean _isClipSet = false;
    private String title;

    /**
     * Constructs an empty EpsDevice that writes directly to a file. Bounds must
     * be set before use.
     */
    EpsDocument(String title, int minX, int minY, int maxX, int maxY) {
        this.title = title;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.contents = new StringBuilder("");
        //writeHeader();
    }

    /**
     * Returns the title of the EPS document.
     */
    public synchronized String getTitle() {
        return title;
    }

    /**
     * Appends a line to the EpsDocument. A new line character is added to the
     * end of the line when it is added.
     */
    public synchronized void append(EpsGraphics g, String line) {
        if (_lastG == null)
            _lastG = g;
        else if (g != _lastG) {
            EpsGraphics lastG = _lastG;
            _lastG = g;
            
            // We are being drawn on with a different EpsGraphics2D context.
            // We may need to update the clip, etc from this new context.
            if (g.getClip() != lastG.getClip())
                g.setClip(g.getClip());
            if (!g.getColor().equals(lastG.getColor()))
                g.setColor(g.getColor());
            if (!g.getBackground().equals(lastG.getBackground()))
                g.setBackground(g.getBackground());

            System.err.println("append g: "+g.hashCode());
            System.err.println("append lastg: "+lastG.hashCode());
            
            // This is weird code
            if (!g.getPaint().equals(lastG.getPaint()))
                g.setPaint(g.getPaint());
            if (g.getComposite()!=null
                    && !g.getComposite().equals(lastG.getComposite()))
                g.setComposite(g.getComposite());
            if (g.getFont()!=null && !g.getFont().equals(lastG.getFont()))
                g.setFont(g.getFont());
            if (g.getStroke()!=null
                    && !g.getStroke().equals(lastG.getStroke()))
                g.setStroke(g.getStroke());
        }

        _lastG = g;
        contents.append(line + "\n");
    }

    /**
     * Outputs the contents of the EPS document to the specified Writer,
     * complete with headers and bounding box.
     */
    public synchronized void writeHeader() {
        float offsetX = -minX;
        float offsetY = -minY;
        StringBuilder old = new StringBuilder(contents.toString());
        contents.setLength(0);
        contents.append("%!PS-Adobe-3.0 EPSF-3.0\n");
        contents.append("%%Creator: EpsGraphics " + EpsGraphics.VERSION
            + " by Thomas Abeel, http://www.sourceforge.net/epsgraphics/\n");
        contents.append("%%Title: " + title + "\n");
        contents.append("%%CreationDate: " + new Date() + "\n");
        contents.append("%%BoundingBox: 0 0 "
            + ((int) Math.ceil(maxX + offsetX)) + " "
            + ((int) Math.ceil(maxY + offsetY)) + "\n");
        contents.append("%%DocumentData: Clean7Bit\n");
        contents.append("%%LanguageLevel: 2\n");
        contents.append("%%DocumentProcessColors: Black\n");
        contents.append("%%ColorUsage: Color\n");
        contents.append("%%Origin: 0 0\n");
        contents.append("%%Pages: 1\n");
        contents.append("%%Page: 1 1\n");
        contents.append("%%EndComments\n\n");
        contents.append("gsave\n");
        contents.append(offsetX + " " + (maxY + offsetY) + " translate\n");
        contents.append(old);
    }

    private void writeFooter() {
        contents.append("grestore\n");
        if (isClipSet())
            contents.append("grestore\n");
        contents.append("\n");
        contents.append("%%EOF");
    }

    public boolean isClipSet() {
        return _isClipSet;
    }

    public void setClipSet(boolean isClipSet) {
        _isClipSet = isClipSet;
    }

    // We need to remember which was the last EpsGraphics2D object to use
    // us, as we need to replace the clipping region if another EpsGraphics2D
    // object tries to use us.
    private EpsGraphics _lastG = null;

    public final int getMaxX() {
        return maxX;
    }

    public final int getMaxY() {
        return maxY;
    }

    public final int getMinX() {
        return minX;
    }

    public final int getMinY() {
        return minY;
    }

    public String getContents() {
        writeFooter();
        return contents.toString();
    }
    
    /**
     * Clears the internal StringBuilder to reset the EpsGraphics object.
     */
    public void clear() {
        this.contents.delete(0, contents.length());
    }
}
