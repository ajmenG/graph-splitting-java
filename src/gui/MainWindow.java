package gui;

import javax.swing.*;
import java.awt.*;
import model.Graph;

public class MainWindow extends JFrame {
    private GraphPanel graphPanel;
    private ControlPanel controlPanel;

    public MainWindow() {
        setTitle("Graph Partitioning Visualizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create panels
        graphPanel = new GraphPanel();
        controlPanel = new ControlPanel(this);

        // Add panels to frame
        add(graphPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.EAST);

        // Set window size
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    public GraphPanel getGraphPanel() {
        return graphPanel;
    }

    public void updateGraph(Graph graph) {
        graphPanel.setGraph(graph);
        graphPanel.repaint();
    }
}