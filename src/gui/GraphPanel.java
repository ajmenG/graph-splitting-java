package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Graph;
import model.Node;
import model.ParsedData;

public class GraphPanel extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    private Graph graph;
    private Map<Integer, Point> nodePositions;
    private static final int NODE_SIZE = 30;
    private static final Color[] PARTITION_COLORS = {
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
            Color.CYAN, Color.MAGENTA, Color.ORANGE, Color.PINK
    };

    private Point lastMouse;
    private Point2D.Double viewOffset = new Point2D.Double(0, 0);
    private double zoomFactor = 1.0;
    private static final double MIN_ZOOM = 0.2;
    private static final double MAX_ZOOM = 3.0;

    // Mapa widoczności partycji (partId -> widoczność)
    private Map<Integer, Boolean> visiblePartitions = new HashMap<>();

    public GraphPanel() {
        setBackground(Color.WHITE);
        nodePositions = new HashMap<>();
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
        if (graph != null) {
            calculateNodePositions();

            // Inicjalizacja widoczności partycji - domyślnie widoczne
            visiblePartitions.clear();
            for (int i = 0; i < graph.getVertices(); i++) {
                Node node = graph.getNode(i);
                int partId = node.getPartId();
                if (partId >= 0 && !visiblePartitions.containsKey(partId)) {
                    visiblePartitions.put(partId, true);
                }
            }

            revalidate();
            repaint();
        }
    }

    public Graph getGraph() {
        return graph;
    }

    private void calculateNodePositions() {
        nodePositions.clear();
        if (graph == null || graph.getVertices() == 0) return;
        ParsedData parsedData = graph.getParsedData();
        if (parsedData == null || parsedData.getLine2() == null || parsedData.getLine3() == null) return;

        List<Integer> line2 = parsedData.getLine2();
        List<Integer> line3 = parsedData.getLine3();
        int matrixSize = parsedData.getLine1();

        int minSpacing = (NODE_SIZE * 3) / 2;
        int totalWidth = matrixSize * minSpacing;
        int totalHeight = matrixSize * minSpacing;
        int padding = NODE_SIZE * 2;
        setPreferredSize(new Dimension(totalWidth + padding * 2, totalHeight + padding * 2));

        int numberOfRows = line3.size() - 1;
        int globalMinPos = Integer.MAX_VALUE, globalMaxPos = Integer.MIN_VALUE;
        for (int pos : line2) {
            globalMinPos = Math.min(globalMinPos, pos);
            globalMaxPos = Math.max(globalMaxPos, pos);
        }

        for (int row = 0; row < numberOfRows; row++) {
            int startIdx = line3.get(row);
            int endIdx = line3.get(row + 1);
            int nodesInThisRow = endIdx - startIdx;
            for (int i = startIdx; i < endIdx; i++) {
                float horizontalPosition = (globalMaxPos == globalMinPos)
                        ? (float) (i - startIdx) / Math.max(1, nodesInThisRow - 1)
                        : (float) (line2.get(i) - globalMinPos) / (globalMaxPos - globalMinPos);
                int x = padding + (int) (horizontalPosition * (totalWidth - NODE_SIZE));
                int y = padding + (int) ((float) row / numberOfRows * (totalHeight - NODE_SIZE));
                nodePositions.put(i, new Point(x, y));
            }
        }
    }

    private boolean isPartitionVisible(int partId) {
        return visiblePartitions.getOrDefault(partId, true);
    }

    public void setPartitionVisible(int partId, boolean visible) {
        visiblePartitions.put(partId, visible);
        repaint();
    }

    private void drawGraph(Graphics2D g2d) {
        if (graph == null) return;

        // Rysowanie wszystkich krawędzi (niewidoczne partycje pomijamy)
        g2d.setColor(new Color(220, 220, 220));
        g2d.setStroke(new BasicStroke(1.0f));
        for (int i = 0; i < graph.getVertices(); i++) {
            Point p1 = nodePositions.get(i);
            Node node1 = graph.getNode(i);
            if (p1 != null) {
                if (node1.getPartId() >= 0 && !isPartitionVisible(node1.getPartId())) continue;
                for (Node neighbor : node1.getNeighbours()) {
                    Point p2 = nodePositions.get(neighbor.getId());
                    Node node2 = graph.getNode(neighbor.getId());
                    if (p2 != null) {
                        if (node2.getPartId() >= 0 && !isPartitionVisible(node2.getPartId())) continue;
                        g2d.drawLine(p1.x + NODE_SIZE / 2, p1.y + NODE_SIZE / 2,
                                p2.x + NODE_SIZE / 2, p2.y + NODE_SIZE / 2);
                    }
                }
            }
        }

        // Rysowanie kolorowanych krawędzi partycji (widocznych tylko)
        g2d.setStroke(new BasicStroke(2.0f));
        for (int i = 0; i < graph.getVertices(); i++) {
            Point p1 = nodePositions.get(i);
            Node node1 = graph.getNode(i);
            if (p1 != null && node1.getPartId() >= 0 && isPartitionVisible(node1.getPartId())) {
                for (Node neighbor : node1.getNeighbours()) {
                    Node node2 = graph.getNode(neighbor.getId());
                    if (node2.getPartId() == node1.getPartId() && isPartitionVisible(node1.getPartId())) {
                        g2d.setColor(PARTITION_COLORS[node1.getPartId() % PARTITION_COLORS.length]);
                        Point p2 = nodePositions.get(neighbor.getId());
                        if (p2 != null) {
                            g2d.drawLine(p1.x + NODE_SIZE / 2, p1.y + NODE_SIZE / 2,
                                    p2.x + NODE_SIZE / 2, p2.y + NODE_SIZE / 2);
                        }
                    }
                }
            }
        }

        // Rysowanie węzłów (pomijamy te z niewidocznych partycji)
        for (int i = 0; i < graph.getVertices(); i++) {
            Point p = nodePositions.get(i);
            if (p != null) {
                Node node = graph.getNode(i);
                if (node.getPartId() >= 0 && !isPartitionVisible(node.getPartId())) continue;

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
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Apply zoom, then pan (order is important!)
        g2d.scale(zoomFactor, zoomFactor);
        g2d.translate(-viewOffset.x, -viewOffset.y);

        if (graph == null) {
            g2d.setColor(Color.GRAY);
            g2d.drawString("No graph loaded", getWidth() / 2 - 40, getHeight() / 2);
        } else {
            drawGraph(g2d);
        }
        g2d.dispose();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        lastMouse = e.getPoint();
        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (lastMouse != null) {
            Point current = e.getPoint();
            double dx = (current.x - lastMouse.x) / zoomFactor;
            double dy = (current.y - lastMouse.y) / zoomFactor;
            viewOffset.x -= dx;
            viewOffset.y -= dy;
            lastMouse = current;
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        lastMouse = null;
        setCursor(Cursor.getDefaultCursor());
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        Point mouse = e.getPoint();

        double mouseGraphX = viewOffset.x + mouse.x / zoomFactor;
        double mouseGraphY = viewOffset.y + mouse.y / zoomFactor;

        double zoomMultiplier = 1.1;
        int notches = e.getWheelRotation();

        double newZoomFactor = zoomFactor;
        if (notches < 0) {
            // przybliżanie
            newZoomFactor = zoomFactor * zoomMultiplier;
        } else if (notches > 0) {
            // oddalanie
            newZoomFactor = zoomFactor / zoomMultiplier;
        }

        // Ograniczamy zoom
        newZoomFactor = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoomFactor));

        // Jeśli zoom się zmienił, to zmieniamy offset
        if (newZoomFactor != zoomFactor) {
            zoomFactor = newZoomFactor;

            // Teraz offset dostosowujemy tak, by punkt pod myszką pozostał ten sam
            viewOffset.x = mouseGraphX - mouse.x / zoomFactor;
            viewOffset.y = mouseGraphY - mouse.y / zoomFactor;

            repaint();
        }
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {}
}
