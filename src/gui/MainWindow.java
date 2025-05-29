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

        // Set up menu bar
        setJMenuBar(createMenuBar());

        // Set window size
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem openItem = new JMenuItem("Open File");
        openItem.addActionListener(e -> controlPanel.chooseInputFile());

        JMenuItem saveItem = new JMenuItem("Save File");
        saveItem.addActionListener(e -> controlPanel.chooseOutputFile());

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        menuBar.add(fileMenu);

        return menuBar;
    }

    public GraphPanel getGraphPanel() {
        return graphPanel;
    }

    public void updateGraph(Graph graph) {
        graphPanel.setGraph(graph);
        graphPanel.repaint();
    }

    public ControlPanel getControlPanel() {
        return controlPanel;
    }
}