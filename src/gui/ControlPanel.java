package gui;

import gui.GraphPanel;
import io.FileReader;
import io.FileWriter;
import model.*;
import algorithm.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import gui.MainWindow;

public class ControlPanel extends JPanel {
    private MainWindow mainWindow;
    private JTextField partitionsField;
    private JTextField accuracyField;
    private JLabel inputFileLabel;
    private JLabel outputFileLabel;
    private JButton loadButton;
    private JButton runButton;

    private JPanel partitionsCheckboxPanel;
    private List<JCheckBox> partitionCheckboxes;

    private File selectedInputFile;
    private File selectedOutputFile;

    // 0 = Text, 1 = Binary
    private int selectedOutputType = 0;

    // Store as field to control visibility
    private JLabel showHideLabel;

    public ControlPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setPreferredSize(new Dimension(190, 800));
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        partitionCheckboxes = new ArrayList<>();

        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initializeComponents(controlsPanel);

        add(controlsPanel, BorderLayout.CENTER);
    }

    private void initializeComponents(JPanel panel) {
        // Partitions field
        JLabel partitionsLabel = new JLabel("Partitions:");
        partitionsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(partitionsLabel);

        partitionsField = new JTextField();
        partitionsField.setMaximumSize(new Dimension(160, 22));
        partitionsField.setPreferredSize(new Dimension(160, 22));
        partitionsField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(partitionsField);

        panel.add(Box.createRigidArea(new Dimension(0, 4)));

        // Accuracy field
        JLabel accuracyLabel = new JLabel("Accuracy (%):");
        accuracyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(accuracyLabel);

        accuracyField = new JTextField();
        accuracyField.setMaximumSize(new Dimension(160, 22));
        accuracyField.setPreferredSize(new Dimension(160, 22));
        accuracyField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(accuracyField);

        panel.add(Box.createRigidArea(new Dimension(0, 4)));

        // Buttons
        loadButton = new JButton("Load Graph");
        loadButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loadButton.addActionListener(e -> loadGraph());
        panel.add(loadButton);

        runButton = new JButton("Run Partitioning");
        runButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        runButton.addActionListener(e -> runPartitioning());
        panel.add(runButton);

        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // Show/Hide label and checkboxes, initially hidden
        showHideLabel = new JLabel("Show / Hide Partitions:");
        showHideLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        showHideLabel.setVisible(false);
        panel.add(showHideLabel);

        partitionsCheckboxPanel = new JPanel();
        partitionsCheckboxPanel.setLayout(new BoxLayout(partitionsCheckboxPanel, BoxLayout.Y_AXIS));
        partitionsCheckboxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        partitionsCheckboxPanel.setVisible(false);
        panel.add(partitionsCheckboxPanel);

        // File info labels (for menu bar feedback)
        inputFileLabel = new JLabel("No input file selected");
        inputFileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(inputFileLabel);

        outputFileLabel = new JLabel("No output file selected");
        outputFileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(outputFileLabel);
    }

    public void chooseInputFile() {
        JFileChooser fileChooser = new JFileChooser("./data");
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedInputFile = fileChooser.getSelectedFile();
            inputFileLabel.setText("input: " + selectedInputFile.getName());
        }
    }

    public void chooseOutputFile() {
        JFileChooser fileChooser = new JFileChooser("./data");
        FileNameExtensionFilter csrrgFilter = new FileNameExtensionFilter("Text (.csrrg)", "csrrg");
        FileNameExtensionFilter binFilter = new FileNameExtensionFilter("Binary (.bin)", "bin");
        fileChooser.addChoosableFileFilter(csrrgFilter);
        fileChooser.addChoosableFileFilter(binFilter);
        fileChooser.setFileFilter(csrrgFilter); // default

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedOutputFile = fileChooser.getSelectedFile();
            outputFileLabel.setText("output: " + selectedOutputFile.getName());

            // Determine selected filter
            javax.swing.filechooser.FileFilter selectedFilter = fileChooser.getFileFilter();
            if (selectedFilter == binFilter) {
                selectedOutputType = 1;
                if (!selectedOutputFile.getName().toLowerCase().endsWith(".bin")) {
                    selectedOutputFile = new File(selectedOutputFile.getAbsolutePath() + ".bin");
                    outputFileLabel.setText(selectedOutputFile.getName());
                }
            } else {
                selectedOutputType = 0;
                if (!selectedOutputFile.getName().toLowerCase().endsWith(".csrrg")) {
                    selectedOutputFile = new File(selectedOutputFile.getAbsolutePath() + ".csrrg");
                    outputFileLabel.setText(selectedOutputFile.getName());
                }
            }
        }
    }

    private void createPartitionCheckboxes(int partitions) {
        partitionsCheckboxPanel.removeAll();
        partitionCheckboxes.clear();

        for (int i = 0; i < partitions; i++) {
            int partId = i;
            JCheckBox cb = new JCheckBox("Partition " + partId, true);
            cb.addActionListener(e -> {
                GraphPanel gp = mainWindow.getGraphPanel();
                if (gp != null) {
                    gp.setPartitionVisible(partId, cb.isSelected());
                    gp.repaint();
                }
            });
            partitionCheckboxes.add(cb);
            partitionsCheckboxPanel.add(cb);
        }

        // Show label and panel only when partitions exist
        showHideLabel.setVisible(true);
        partitionsCheckboxPanel.setVisible(true);

        partitionsCheckboxPanel.revalidate();
        partitionsCheckboxPanel.repaint();
    }

    private void loadGraph() {
        if (selectedInputFile == null) {
            JOptionPane.showMessageDialog(this, "Please select an input file");
            return;
        }

        FileReader fileReader = new FileReader();
        try {
            ParsedData parsedData = fileReader.parseFile(selectedInputFile.getPath());
            Graph graph = fileReader.loadGraph(parsedData);
            mainWindow.updateGraph(graph);

            int partitions = graph.getPartitions() > 0 ? graph.getPartitions() : 1;
            createPartitionCheckboxes(partitions);

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

            graph.setPartitions(partitions);
            graph.setMinCount(accuracy);
            graph.setMaxCount(accuracy);

            List<Partition> partitionList = new ArrayList<>();
            for (int i = 0; i < partitions; i++) {
                partitionList.add(new Partition(i, 0, new ArrayList<>()));
            }
            PartitionData partitionData = new PartitionData(partitions);
            partitionData.setPartitions(partitionList);

            boolean success = false;
            for (int i = 0; i < 10; i++) {
                success = RegionGrowing.regionGrowing(graph, partitions, partitionData, accuracy);
                if (success) {
                    break;
                }
            }

            FmOptimization.cutEdgesOptimization(graph, partitionData, 100);

            mainWindow.updateGraph(graph);

            createPartitionCheckboxes(partitions);

            JOptionPane.showMessageDialog(this,
                    "Partitioning " + (success ? "completed successfully" : "completed with balance issues"));

            io.FileWriter file = new io.FileWriter();
            String outputFileName;
            if (selectedOutputFile != null) {
                outputFileName = selectedOutputFile.getPath();
            } else {
                outputFileName = "./data/anwser";
            }

            if (selectedOutputType == 0) { // Text
                if (!outputFileName.endsWith(".csrrg")) {
                    outputFileName += ".csrrg";
                }
                file.writeText(outputFileName, graph.getParsedData(), partitionData, graph, partitions);
            } else { // Binary
                if (!outputFileName.endsWith(".bin")) {
                    outputFileName += ".bin";
                }
                file.writeBinary(outputFileName, graph.getParsedData(), partitionData, graph, partitions);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Please enter valid numbers for partitions and accuracy",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}