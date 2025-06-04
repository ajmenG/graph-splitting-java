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
            Color.CYAN, Color.MAGENTA, Color.ORANGE, Color.PINK,
            new Color(128, 0, 0), new Color(0, 128, 0), new Color(0, 0, 128),
            new Color(128, 128, 0), new Color(128, 0, 128), new Color(0, 128, 128),
            Color.DARK_GRAY, Color.LIGHT_GRAY
    };

    private Point lastMouseScreenCoord;
    private Point2D.Double viewOffset = new Point2D.Double(0, 0);
    private double zoomFactor = 1.0;
    private static final double MIN_ZOOM = 0.05;
    private static final double MAX_ZOOM = 5.0;

    private Map<Integer, Boolean> visiblePartitions = new HashMap<>();

    public GraphPanel() {
        setBackground(Color.WHITE);
        nodePositions = new HashMap<>();
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        setFocusable(true);
    }

    public void setGraph(Graph newGraph) {
        this.graph = newGraph;
        if (this.graph != null) {
            visiblePartitions.clear();
            if (this.graph.getNodes() != null) {
                for (Node node : this.graph.getNodes()) {
                    int partId = node.getPartId();
                    if (partId >= 0 && !visiblePartitions.containsKey(partId)) {
                        visiblePartitions.put(partId, true);
                    } else if (partId < 0 && !visiblePartitions.containsKey(-1)) {
                        visiblePartitions.put(-1, true);
                    }
                }
            }
            calculateNodePositions();
            resetView();
        }
        revalidate();
        repaint();
    }

    public void resetView() {
        zoomFactor = 1.0;
        if (getWidth() > 0 && getHeight() > 0 && graph != null && graph.getVertices() > 0) {
            Dimension panelSize = getPreferredSize();
            viewOffset.x = (panelSize.width - getWidth() / zoomFactor) / 2.0;
            viewOffset.y = (panelSize.height - getHeight() / zoomFactor) / 2.0;
            if (viewOffset.x < 0)
                viewOffset.x = 0;
            if (viewOffset.y < 0)
                viewOffset.y = 0;
        } else {
            viewOffset.x = 0;
            viewOffset.y = 0;
        }
        repaint();
    }

    public Graph getGraph() {
        return graph;
    }

    private void calculateNodePositions() {
        nodePositions.clear();
        if (graph == null || graph.getVertices() == 0)
            return;

        ParsedData parsedData = graph.getParsedData();
        List<Integer> line2 = (parsedData != null) ? parsedData.getLine2() : null;
        List<Integer> line3 = (parsedData != null) ? parsedData.getLine3() : null;
        int matrixSize = (parsedData != null && parsedData.getLine1() > 0) ? parsedData.getLine1()
                : (int) Math.sqrt(graph.getVertices()) + 1;

        int baseSpacing = NODE_SIZE * 2;
        int estimatedNodesPerRow = Math.max(1, (int) Math.sqrt(graph.getVertices()));
        int totalWidth = Math.max(getWidth(), estimatedNodesPerRow * baseSpacing);
        int totalHeight = Math.max(getHeight(), estimatedNodesPerRow * baseSpacing);

        if (parsedData != null && line2 != null && !line2.isEmpty() && line3 != null && line3.size() > 1) {
            int padding = NODE_SIZE;
            setPreferredSize(
                    new Dimension(matrixSize * baseSpacing + padding * 2, matrixSize * baseSpacing + padding * 2));

            int numberOfRows = line3.size() - 1;
            int globalMinPos = line2.isEmpty() ? 0 : line2.stream().min(Integer::compareTo).orElse(0);
            int globalMaxPos = line2.isEmpty() ? 0 : line2.stream().max(Integer::compareTo).orElse(0);

            for (int row = 0; row < numberOfRows; row++) {
                int startIdx = line3.get(row);
                int endIdx = line3.get(row + 1);
                int nodesInThisRow = endIdx - startIdx;

                if (nodesInThisRow <= 0)
                    continue;

                for (int k = 0; k < nodesInThisRow; k++) {
                    int nodeOriginalIndex = startIdx + k;
                    if (nodeOriginalIndex >= graph.getVertices() || nodeOriginalIndex < 0)
                        continue;
                    Node node = graph.getNodes().get(nodeOriginalIndex);
                    int nodeId = node.getId();

                    float horizontalFactor;
                    if (nodeOriginalIndex < line2.size()) {
                        horizontalFactor = (globalMaxPos == globalMinPos) ? 0.5f
                                : (float) (line2.get(nodeOriginalIndex) - globalMinPos) / (globalMaxPos - globalMinPos);
                    } else {
                        horizontalFactor = (nodesInThisRow > 1) ? (float) k / (nodesInThisRow - 1) : 0.5f;
                    }

                    int x = padding + (int) (horizontalFactor * ((matrixSize * baseSpacing) - NODE_SIZE));
                    int y = padding + (int) ((float) row / Math.max(1, numberOfRows - 1)
                            * ((matrixSize * baseSpacing) - NODE_SIZE));
                    nodePositions.put(nodeId, new Point(x, y));
                }
            }
            if (graph.getVertices() > line2.size()) {
                int currentX = padding;
                int currentY = padding + numberOfRows * baseSpacing;
                for (Node node : graph.getNodes()) {
                    if (!nodePositions.containsKey(node.getId())) {
                        nodePositions.put(node.getId(), new Point(currentX, currentY));
                        currentX += baseSpacing;
                        if (currentX > totalWidth) {
                            currentX = padding;
                            currentY += baseSpacing;
                        }
                    }
                }
            }

        } else {
            int numNodes = graph.getVertices();
            int viewWidth = Math.max(getWidth(), 600);
            int viewHeight = Math.max(getHeight(), 600);
            setPreferredSize(new Dimension(viewWidth, viewHeight));

            int centerX = viewWidth / 2;
            int centerY = viewHeight / 2;
            double angleStep = 2 * Math.PI / numNodes;
            int radius = Math.min(viewWidth, viewHeight) / 3;
            if (numNodes == 1) {
                nodePositions.put(graph.getNode(0).getId(),
                        new Point(centerX - NODE_SIZE / 2, centerY - NODE_SIZE / 2));
            } else {
                for (int i = 0; i < numNodes; i++) {
                    Node node = graph.getNode(i);
                    int x = centerX + (int) (radius * Math.cos(i * angleStep)) - NODE_SIZE / 2;
                    int y = centerY + (int) (radius * Math.sin(i * angleStep)) - NODE_SIZE / 2;
                    nodePositions.put(node.getId(), new Point(x, y));
                }
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
        if (graph == null || graph.getNodes() == null)
            return;

        g2d.setColor(new Color(220, 220, 220));
        g2d.setStroke(new BasicStroke((float) (1.0 / zoomFactor)));
        for (Node node1 : graph.getNodes()) {
            Point p1 = nodePositions.get(node1.getId());
            if (p1 == null || (node1.getPartId() >= 0 && !isPartitionVisible(node1.getPartId())))
                continue;

            for (Node neighbor : node1.getNeighbours()) {
                Node node2 = graph.getNode(neighbor.getId());
                Point p2 = nodePositions.get(node2.getId());
                if (p2 == null || (node2.getPartId() >= 0 && !isPartitionVisible(node2.getPartId())))
                    continue;

                g2d.drawLine(p1.x + NODE_SIZE / 2, p1.y + NODE_SIZE / 2,
                        p2.x + NODE_SIZE / 2, p2.y + NODE_SIZE / 2);
            }
        }

        g2d.setStroke(new BasicStroke((float) (2.0 / zoomFactor)));
        for (Node node1 : graph.getNodes()) {
            Point p1 = nodePositions.get(node1.getId());
            if (p1 == null || node1.getPartId() < 0 || !isPartitionVisible(node1.getPartId()))
                continue;

            for (Node neighbor : node1.getNeighbours()) {
                Node node2 = graph.getNode(neighbor.getId());
                Point p2 = nodePositions.get(node2.getId());
                if (p2 == null || node2.getPartId() != node1.getPartId() || !isPartitionVisible(node2.getPartId()))
                    continue;

                g2d.setColor(PARTITION_COLORS[Math.abs(node1.getPartId()) % PARTITION_COLORS.length]);
                g2d.drawLine(p1.x + NODE_SIZE / 2, p1.y + NODE_SIZE / 2,
                        p2.x + NODE_SIZE / 2, p2.y + NODE_SIZE / 2);
            }
        }

        for (Node node : graph.getNodes()) {
            Point p = nodePositions.get(node.getId());
            if (p == null || (node.getPartId() >= 0 && !isPartitionVisible(node.getPartId())))
                continue;

            if (node.getPartId() >= 0) {
                g2d.setColor(PARTITION_COLORS[Math.abs(node.getPartId()) % PARTITION_COLORS.length]);
            } else {
                g2d.setColor(Color.WHITE);
            }
            g2d.fillRect(p.x, p.y, NODE_SIZE, NODE_SIZE);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(p.x, p.y, NODE_SIZE, NODE_SIZE);

            String nodeIdStr = String.valueOf(node.getId());
            FontMetrics fm = g2d.getFontMetrics();
            int textX = p.x + (NODE_SIZE - fm.stringWidth(nodeIdStr)) / 2;
            int textY = p.y + ((NODE_SIZE - fm.getHeight()) / 2) + fm.getAscent();
            g2d.drawString(nodeIdStr, textX, textY);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2d.translate(-viewOffset.x * zoomFactor, -viewOffset.y * zoomFactor);
        g2d.scale(zoomFactor, zoomFactor);

        if (graph == null) {
            g2d.setColor(Color.GRAY);
            String noGraphMsg = "No graph loaded";
            FontMetrics fm = g2d.getFontMetrics();
            int msgWidth = fm.stringWidth(noGraphMsg);
            int x = (int) ((getWidth() / zoomFactor - msgWidth) / 2 + viewOffset.x);
            int y = (int) ((getHeight() / zoomFactor - fm.getHeight()) / 2 + viewOffset.y + fm.getAscent());
            g2d.drawString(noGraphMsg, x, y);
        } else {
            drawGraph(g2d);
        }
        g2d.dispose();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        lastMouseScreenCoord = e.getPoint();
        requestFocusInWindow();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (lastMouseScreenCoord != null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            Point currentMouseScreenCoord = e.getPoint();
            double dxScreen = currentMouseScreenCoord.x - lastMouseScreenCoord.x;
            double dyScreen = currentMouseScreenCoord.y - lastMouseScreenCoord.y;

            viewOffset.x -= dxScreen / zoomFactor;
            viewOffset.y -= dyScreen / zoomFactor;

            lastMouseScreenCoord = currentMouseScreenCoord;
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        lastMouseScreenCoord = null;
        setCursor(Cursor.getDefaultCursor());
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        Point mouseScreenCoord = e.getPoint();

        double oldZoomFactor = zoomFactor;
        double zoomMultiplier = 1.1;
        int notches = e.getWheelRotation();

        if (notches < 0) {
            zoomFactor *= zoomMultiplier;
        } else {
            zoomFactor /= zoomMultiplier;
        }
        zoomFactor = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoomFactor));

        if (zoomFactor != oldZoomFactor) {
            viewOffset.x += (mouseScreenCoord.x / oldZoomFactor) - (mouseScreenCoord.x / zoomFactor);
            viewOffset.y += (mouseScreenCoord.y / oldZoomFactor) - (mouseScreenCoord.y / zoomFactor);
            repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }
}
