package gui;

import io.FileReader;
import model.*;
import algorithm.*;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import gui.MainWindow;

public class ControlPanel extends JPanel {
    private MainWindow mainWindow;
    private JTextField fileNameField;
    private JTextField partitionsField;
    private JTextField accuracyField;
    private JButton loadButton;
    private JButton runButton;

    public ControlPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setPreferredSize(new Dimension(250, 800));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initializeComponents();
    }

    private void initializeComponents() {
        // File input
        add(new JLabel("File Name:"));
        fileNameField = new JTextField(20);
        add(fileNameField);

        // Partitions input
        add(new JLabel("Number of Partitions:"));
        partitionsField = new JTextField(20);
        add(partitionsField);

        // Accuracy input
        add(new JLabel("Accuracy (%):"));
        accuracyField = new JTextField(20);
        add(accuracyField);

        // Buttons
        loadButton = new JButton("Load Graph");
        loadButton.addActionListener(e -> loadGraph());
        add(loadButton);

        runButton = new JButton("Run Partitioning");
        runButton.addActionListener(e -> runPartitioning());
        add(runButton);
    }

    private void loadGraph() {
        String filename = fileNameField.getText();
        if (filename.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a filename");
            return;
        }

        FileReader fileReader = new FileReader();
        try {
            ParsedData parsedData = fileReader.parseFile("./data/" + filename + ".csrrg");
            Graph graph = fileReader.loadGraph(parsedData);
            mainWindow.updateGraph(graph);
            JOptionPane.showMessageDialog(this, 
                "Graph loaded successfully with " + graph.getVertices() + " vertices and " + 
                graph.getEdges() + " edges.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading graph: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runPartitioning() {
        Graph graph = mainWindow.getGraphPanel().getGraph();
        if (graph == null) {
            JOptionPane.showMessageDialog(this, "Please load a graph first");
            return;
        }

        try {
            int partitions = Integer.parseInt(partitionsField.getText());
            int accuracyPercent = Integer.parseInt(accuracyField.getText());

            if (partitions <= 0) {
                JOptionPane.showMessageDialog(this, "Number of partitions must be positive");
                return;
            }

            if (accuracyPercent < 0 || accuracyPercent > 100) {
                JOptionPane.showMessageDialog(this, "Accuracy must be between 0 and 100%");
                return;
            }

            float accuracy = (float) accuracyPercent / 100.0f;

            // Set up partitioning
            graph.setPartitions(partitions);
            graph.setMinCount(accuracy);
            graph.setMaxCount(accuracy);

            // Initialize partition data
            List<Partition> partitionList = new ArrayList<>();
            for (int i = 0; i < partitions; i++) {
                partitionList.add(new Partition(i, 0, new ArrayList<>()));
            }
            PartitionData partitionData = new PartitionData(partitions);
            partitionData.setPartitions(partitionList);

            // Run region growing
            boolean success = RegionGrowing.regionGrowing(graph, partitions, partitionData, accuracy);

            // Run FM optimization
            FmOptimization.cutEdgesOptimization(graph, partitionData, 100);

            // Update display using MainWindow's updateGraph method
            mainWindow.updateGraph(graph);

            JOptionPane.showMessageDialog(this,
                    "Partitioning " + (success ? "completed successfully" : "completed with balance issues"));

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Please enter valid numbers for partitions and accuracy",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}