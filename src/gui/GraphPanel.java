package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import model.Graph;
import model.Node;
import model.ParsedData;
import java.util.List;

public class GraphPanel extends JPanel implements MouseListener, MouseMotionListener {
    private Graph graph;
    private Map<Integer, Point> nodePositions;
    private static final int NODE_SIZE = 30;
    private static final Color[] PARTITION_COLORS = {
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
            Color.CYAN, Color.MAGENTA, Color.ORANGE, Color.PINK
    };

    private Point dragStart;
    private Point viewOffset;
    private BufferedImage bufferImage;
    private boolean needsFullRedraw = true;

    public GraphPanel() {
        setBackground(Color.WHITE);
        nodePositions = new HashMap<>();
        viewOffset = new Point(0, 0);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
        if (graph != null) {
            calculateNodePositions();
            needsFullRedraw = true;
            repaint();
        }
    }

    public Graph getGraph() {
        return graph;
    }

    private void calculateNodePositions() {
        nodePositions.clear();

        if (graph == null || graph.getVertices() == 0) {
            return;
        }

        ParsedData parsedData = graph.getParsedData();
        if (parsedData == null || parsedData.getLine2() == null || parsedData.getLine3() == null) {
            return;
        }

        List<Integer> line2 = parsedData.getLine2();
        List<Integer> line3 = parsedData.getLine3();
        int matrixSize = parsedData.getLine1();

        int minSpacing = (NODE_SIZE * 3) / 2;
        int totalWidth = matrixSize * minSpacing;
        int totalHeight = matrixSize * minSpacing;

        int padding = NODE_SIZE * 2;
        setPreferredSize(new Dimension(
                totalWidth + padding * 2,
                totalHeight + padding * 2
        ));

        int numberOfRows = line3.size() - 1;

        int globalMinPos = Integer.MAX_VALUE;
        int globalMaxPos = Integer.MIN_VALUE;
        for (int pos : line2) {
            globalMinPos = Math.min(globalMinPos, pos);
            globalMaxPos = Math.max(globalMaxPos, pos);
        }

        for (int row = 0; row < numberOfRows; row++) {
            int startIdx = line3.get(row);
            int endIdx = line3.get(row + 1);
            int nodesInThisRow = endIdx - startIdx;

            for (int i = startIdx; i < endIdx; i++) {
                float horizontalPosition;
                if (globalMaxPos == globalMinPos) {
                    horizontalPosition = (float)(i - startIdx) / Math.max(1, nodesInThisRow - 1);
                } else {
                    horizontalPosition = (float)(line2.get(i) - globalMinPos) / (globalMaxPos - globalMinPos);
                }

                int x = padding + (int)(horizontalPosition * (totalWidth - NODE_SIZE));
                int y = padding + (int)((float)row / numberOfRows * (totalHeight - NODE_SIZE));

                nodePositions.put(i, new Point(x, y));
            }
        }
    }

    private void drawGraph(Graphics2D g2d) {
        // Draw edges
        g2d.setColor(new Color(220, 220, 220));
        g2d.setStroke(new BasicStroke(1.0f));
        for (int i = 0; i < graph.getVertices(); i++) {
            Point p1 = nodePositions.get(i);
            Node node1 = graph.getNode(i);
            if (p1 != null) {
                for (Node neighbor : node1.getNeighbours()) {
                    Point p2 = nodePositions.get(neighbor.getId());
                    if (p2 != null) {
                        g2d.drawLine(
                                p1.x + NODE_SIZE/2, p1.y + NODE_SIZE/2,
                                p2.x + NODE_SIZE/2, p2.y + NODE_SIZE/2
                        );
                    }
                }
            }
        }

        // Draw partition edges
        g2d.setStroke(new BasicStroke(2.0f));
        for (int i = 0; i < graph.getVertices(); i++) {
            Point p1 = nodePositions.get(i);
            Node node1 = graph.getNode(i);
            if (p1 != null && node1.getPartId() >= 0) {
                for (Node neighbor : node1.getNeighbours()) {
                    Node node2 = graph.getNode(neighbor.getId());
                    if (node1.getPartId() == node2.getPartId()) {
                        g2d.setColor(PARTITION_COLORS[node1.getPartId() % PARTITION_COLORS.length]);
                        Point p2 = nodePositions.get(neighbor.getId());
                        if (p2 != null) {
                            g2d.drawLine(
                                    p1.x + NODE_SIZE/2, p1.y + NODE_SIZE/2,
                                    p2.x + NODE_SIZE/2, p2.y + NODE_SIZE/2
                            );
                        }
                    }
                }
            }
        }

        // Draw nodes
        for (int i = 0; i < graph.getVertices(); i++) {
            Point p = nodePositions.get(i);
            if (p != null) {
                Node node = graph.getNode(i);
                if (node.getPartId() >= 0) {
                    g2d.setColor(PARTITION_COLORS[node.getPartId() % PARTITION_COLORS.length]);
                } else {
                    g2d.setColor(Color.WHITE);
                }
                g2d.fillRect(p.x, p.y, NODE_SIZE, NODE_SIZE);

                g2d.setColor(Color.BLACK);
                g2d.drawRect(p.x, p.y, NODE_SIZE, NODE_SIZE);

                String nodeId = String.valueOf(i);
                FontMetrics fm = g2d.getFontMetrics();
                int textX = p.x + (NODE_SIZE - fm.stringWidth(nodeId)) / 2;
                int textY = p.y + ((NODE_SIZE + fm.getAscent()) / 2);
                g2d.drawString(nodeId, textX, textY);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();

        if (bufferImage == null || bufferImage.getWidth() != width || bufferImage.getHeight() != height) {
            bufferImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            needsFullRedraw = true;
        }

        if (needsFullRedraw) {
            Graphics2D g2d = bufferImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);

            if (graph == null) {
                g2d.setColor(Color.GRAY);
                g2d.drawString("No graph loaded", width/2 - 40, height/2);
            } else {
                g2d.translate(-viewOffset.x, -viewOffset.y);
                drawGraph(g2d);
                g2d.translate(viewOffset.x, viewOffset.y);
            }
            g2d.dispose();
            needsFullRedraw = false;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.drawImage(bufferImage, 0, 0, null);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        dragStart = e.getPoint();
        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragStart != null) {
            Point current = e.getPoint();
            int dx = current.x - dragStart.x;
            int dy = current.y - dragStart.y;

            int maxPanX = Math.max(0, getPreferredSize().width - getWidth());
            int maxPanY = Math.max(0, getPreferredSize().height - getHeight());

            viewOffset.x = Math.max(0, Math.min(maxPanX, viewOffset.x - dx));
            viewOffset.y = Math.max(0, Math.min(maxPanY, viewOffset.y - dy));

            dragStart = current;
            needsFullRedraw = true;
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragStart = null;
        setCursor(Cursor.getDefaultCursor());
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {}
}