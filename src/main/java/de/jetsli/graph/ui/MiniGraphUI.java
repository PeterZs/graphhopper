/*
 *  Copyright 2012 Peter Karich info@jetsli.de
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.jetsli.graph.ui;

import de.jetsli.graph.storage.DistEntry;
import de.jetsli.graph.storage.Graph;
import de.jetsli.graph.trees.QuadTree;
import de.jetsli.graph.trees.QuadTreeSimple;
import de.jetsli.graph.util.CoordTrig;
import de.jetsli.graph.util.MyIteratorable;
import de.jetsli.graph.util.StopWatch;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.Collection;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * @author Peter Karich
 */
public class MiniGraphUI {

    private final QuadTree<Integer> quadTree;
    private Collection<CoordTrig<Integer>> quadTreeNodes;
    private final Graph graph;
    private double scaleX = 0.001f;
    private double scaleY = 0.001f;
    // initial position to center unterfranken
    // 49.50381,9.953613 -> south unterfranken
    private double offsetX = -8.8f;
    private double offsetY = -39.7f;
    private String latLon = "";
    private double minX;
    private double minY;
    private double maxX;
    private double maxY;
    private JPanel infoPanel;
    private JPanel mainPanel;

    public MiniGraphUI(Graph g) {
        this.graph = g;

        this.quadTree = new QuadTreeSimple<Integer>(8, 6 * 8);
        StopWatch sw = new StopWatch().start();
        // TODO LATER persist quad tree to make things faster and store osm ids instead nothing
        Integer empty = new Integer(1);
        int locs = graph.getLocations();
        for (int i = 0; i < locs; i++) {
            quadTree.put(graph.getLatitude(i), graph.getLongitude(i), empty);
        }
        System.out.println("readed quad tree " + quadTree.size() + " in " + sw.stop().getSeconds() + "sec");

        infoPanel = new JPanel() {

            @Override protected void paintComponent(Graphics g) {
                g.clearRect(0, 0, 10000, 10000);

                g.setColor(Color.BLUE);
                g.drawString(latLon, 40, 20);
                g.drawString("scale:" + scaleX, 40, 40);
                g.drawString("minX:" + (int) minX + " minY:" + (int) minY
                        + " maxX:" + (int) maxX + " maxY:" + (int) maxY, 40, 60);
            }
        };

        // TODO PERFORMANCE draw graph on an offscreen image and translate + scale that one!
        // but then we have a memory problem and less resolution!
        // final BufferedImage offscreenImage = new BufferedImage(11000, 11000, BufferedImage.TYPE_INT_ARGB);
        // final Graphics2D g2 = offscreenImage.createGraphics();

        mainPanel = new JPanel() {

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.clearRect(0, 0, 10000, 10000);

//                AffineTransform at = AffineTransform.getScaleInstance(scaleX, scaleY);
//                at.concatenate(AffineTransform.getTranslateInstance(offsetX, offsetY));
//                AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
//                g2.drawImage(offscreenImage, op, 0, 0);

                int locs = graph.getLocations();
                minX = Integer.MAX_VALUE;
                minY = Integer.MAX_VALUE;
                maxX = 0;
                maxY = 0;
                g.setColor(Color.RED);
                g.drawOval((int) getX(49.990532f), (int) getY(9.020827f), 10, 10);

                int size;
                if (scaleX < 3e-5)
                    size = 2;
                else if (scaleX < 3e-4)
                    size = 1;
                else
                    size = 0;
                StopWatch sw = new StopWatch().start();
                for (int i = 0; i < locs; i++) {
                    int count = MyIteratorable.count(graph.getEdges(i));
                    float lat = graph.getLatitude(i);
                    float lon = graph.getLongitude(i);
                    plot(g2, lat, lon, count, size);

                    for (DistEntry de : graph.getOutgoing(i)) {
                        float lat2 = graph.getLatitude(de.node);
                        float lon2 = graph.getLongitude(de.node);
                        if (lat2 <= 0 || lon2 <= 0)
                            System.out.println("ERROR " + de.node + " " + de.distance + " " + lat2 + "," + lon2);
                        plotEdge(g, lat, lon, lat2, lon2);
                    }
                }
                System.out.println("frame took " + sw.stop().getSeconds() + "sec");

                if (quadTreeNodes != null) {
                    System.out.println("found neighbors:" + quadTreeNodes.size());
                    for (CoordTrig<Integer> coord : quadTreeNodes) {
                        plot(g, coord.lat, coord.lon, 1, 1);
                    }
                }

                infoPanel.repaint();
            }
        };
    }

    private void plotEdge(Graphics g, float lat, float lon, float lat2, float lon2) {
        g.drawLine((int) getX(lon), (int) getY(lat), (int) getX(lon2), (int) getY(lat2));
    }

    private double getX(float lon) {
        return (lon + offsetX) / scaleX;
    }

    private double getY(float lat) {
        return (90 - lat + offsetY) / scaleY;
    }

    private void plot(Graphics g, float lat, float lon, int count, int width) {
        double x = getX(lon);
        double y = getY(lat);
        if (y < minY)
            minY = y;
        else if (y > maxY)
            maxY = y;
        if (x < minX)
            minX = x;
        else if (x > maxX)
            maxX = x;

        Color color;

        // System.out.println(i + " y:" + y + " lat:" + lat + "," + lon + " count:" + count);
        if (count == 1)
            color = Color.RED;
        else if (count == 2)
            color = Color.BLACK;
        else if (count == 3)
            color = Color.BLUE;
        else if (count == 4)
            color = Color.GREEN;
        else if (count == 5)
            color = Color.MAGENTA;
        else
            color = new Color(Math.min(250, count * 10), 111, 111);

        g.setColor(color);

        if (count > 0)
            g.drawOval((int) x, (int) y, width, width);
    }

    public void visualize() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override public void run() {
                    int frameHeight = 800;
                    int frameWidth = 1200;
                    JFrame frame = new JFrame("GraphHopper UI - Small&Ugly ;)");
                    frame.setLayout(new BorderLayout());
                    frame.add(mainPanel, BorderLayout.CENTER);
                    frame.add(infoPanel, BorderLayout.NORTH);

                    infoPanel.setPreferredSize(new Dimension(300, 100));

                    // scale
                    mainPanel.addMouseWheelListener(new MouseWheelListener() {

                        @Override public void mouseWheelMoved(MouseWheelEvent e) {
                            double tmpFactor = 0.5f;
                            if (e.getWheelRotation() > 0)
                                tmpFactor = 2;

                            double resX = scaleX * tmpFactor;
                            if (resX > 0)
                                scaleX = resX;

                            double resY = scaleY * tmpFactor;
                            if (resY > 0)
                                scaleY = resY;

                            // TODO respect mouse x,y when scaling
                            offsetX -= offsetX * scaleX;
                            offsetY -= offsetY * scaleY;
                            mainPanel.repaint();
                        }
                    });

                    // important: calculate x/y for mouse event relative to mainPanel not frame!
                    // move graph via dragging and do something on click
                    MouseAdapter ml = new MouseAdapter() {

                        int currentPosX;
                        int currentPosY;

                        @Override public void mouseClicked(MouseEvent e) {
                            updateLatLon(e);

                            // copy to clipboard
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            StringSelection stringSelection = new StringSelection(latLon);
                            clipboard.setContents(stringSelection, new ClipboardOwner() {

                                @Override public void lostOwnership(Clipboard clipboard, Transferable contents) {
                                }
                            });

                            // show quad tree nodes
                            float lat = getLat(e.getY());
                            float lon = getLon(e.getX());
                            
                            StopWatch sw = new StopWatch().start();
                            quadTreeNodes = quadTree.getNeighbours(lat, lon, 10);
                            System.out.println("search at " + lat + "," + lon + " took " + sw.stop().getSeconds());

                            // open browser
//                            try {
//                                Desktop.getDesktop().browse(new URI("http://maps.google.de/maps?q=" + latLon));
//                            } catch (Exception ex) {
//                                ex.printStackTrace();
//                            }
                            mainPanel.repaint();
                        }

                        @Override public void mouseDragged(MouseEvent e) {
                            update(e);
                        }

                        @Override public void mouseMoved(MouseEvent e) {
                            updateLatLon(e);
                            infoPanel.repaint();
                        }

                        private void updateLatLon(MouseEvent e) {
                            latLon = getLat(e.getY()) + "," + getLon(e.getX());
                        }

                        @Override public void mousePressed(MouseEvent e) {
                            currentPosX = e.getX();
                            currentPosY = e.getY();
                        }

                        public void update(MouseEvent e) {
                            offsetX += (e.getX() - currentPosX) * scaleX;
                            offsetY += (e.getY() - currentPosY) * scaleY;
                            mainPanel.repaint();
                        }

                        float getLon(int x) {
                            return (float) (x * scaleX - offsetX);
                        }

                        float getLat(int y) {
                            return (float) (90 - (y * scaleY - offsetY));
                        }
                    };
                    mainPanel.addMouseListener(ml);
                    mainPanel.addMouseMotionListener(ml);
                    
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setSize(frameWidth + 10, frameHeight + 30);
                    frame.setVisible(true);
                }
            });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
