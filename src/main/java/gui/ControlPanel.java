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

    private int selectedOutputType = 0;

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
        JLabel partitionsLabel = new JLabel("Partitions:");
        partitionsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(partitionsLabel);

        partitionsField = new JTextField();
        partitionsField.setMaximumSize(new Dimension(160, 22));
        partitionsField.setPreferredSize(new Dimension(160, 22));
        partitionsField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(partitionsField);

        panel.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel accuracyLabel = new JLabel("Accuracy (%):");
        accuracyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(accuracyLabel);

        accuracyField = new JTextField();
        accuracyField.setMaximumSize(new Dimension(160, 22));
        accuracyField.setPreferredSize(new Dimension(160, 22));
        accuracyField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(accuracyField);

        panel.add(Box.createRigidArea(new Dimension(0, 4)));

        loadButton = new JButton("Load Graph");
        loadButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loadButton.addActionListener(e -> loadGraph());
        panel.add(loadButton);

        runButton = new JButton("Run Partitioning");
        runButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        runButton.addActionListener(e -> runPartitioning());
        panel.add(runButton);

        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        showHideLabel = new JLabel("Show / Hide Partitions:");
        showHideLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        showHideLabel.setVisible(false);
        panel.add(showHideLabel);

        partitionsCheckboxPanel = new JPanel();
        partitionsCheckboxPanel.setLayout(new BoxLayout(partitionsCheckboxPanel, BoxLayout.Y_AXIS));
        partitionsCheckboxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        partitionsCheckboxPanel.setVisible(false);
        panel.add(partitionsCheckboxPanel);

        inputFileLabel = new JLabel("No input file selected");
        inputFileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(inputFileLabel);

        outputFileLabel = new JLabel("No output file selected");
        outputFileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(outputFileLabel);
    }

    public void chooseInputFile() {
        JFileChooser fileChooser = new JFileChooser("./data");
        FileNameExtensionFilter csrrgFilter = new FileNameExtensionFilter("CSRRG files (*.csrrg)", "csrrg");
        FileNameExtensionFilter binFilter = new FileNameExtensionFilter("Binary files (*.bin, *.csrrgbin)", "bin",
                "csrrgbin");
        fileChooser.addChoosableFileFilter(csrrgFilter);
        fileChooser.addChoosableFileFilter(binFilter);
        fileChooser.setFileFilter(csrrgFilter);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedInputFile = fileChooser.getSelectedFile();
            String selectedFileNameLower = selectedInputFile.getName().toLowerCase();
            if (!selectedFileNameLower.endsWith(".csrrg") &&
                    !selectedFileNameLower.endsWith(".bin") &&
                    !selectedFileNameLower.endsWith(".csrrgbin")) {
                JOptionPane.showMessageDialog(this, "Please select a valid .csrrg, .bin, or .csrrgbin file");
                selectedInputFile = null;
                inputFileLabel.setText("No input file selected");
                return;
            }
            inputFileLabel.setText("input: " + selectedInputFile.getName());
        }
    }

    public void chooseOutputFile() {
        JFileChooser fileChooser = new JFileChooser("./data");
        FileNameExtensionFilter csrrgFilter = new FileNameExtensionFilter("Text (.csrrg)", "csrrg");
        FileNameExtensionFilter binFilter = new FileNameExtensionFilter("Binary (.bin)", "bin");
        fileChooser.addChoosableFileFilter(csrrgFilter);
        fileChooser.addChoosableFileFilter(binFilter);
        fileChooser.setFileFilter(csrrgFilter);

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File tempSelectedOutputFile = fileChooser.getSelectedFile();
            String outputFileName = tempSelectedOutputFile.getName();
            String outputFilePath = tempSelectedOutputFile.getAbsolutePath();

            javax.swing.filechooser.FileFilter selectedFilter = fileChooser.getFileFilter();
            if (selectedFilter == binFilter) {
                selectedOutputType = 1;
                if (!outputFileName.toLowerCase().endsWith(".bin")) {
                    outputFilePath += ".bin";
                }
            } else {
                selectedOutputType = 0;
                if (!outputFileName.toLowerCase().endsWith(".csrrg")) {
                    outputFilePath += ".csrrg";
                }
            }
            selectedOutputFile = new File(outputFilePath);
            outputFileLabel.setText("output: " + selectedOutputFile.getName());
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

        showHideLabel.setVisible(partitions > 0);
        partitionsCheckboxPanel.setVisible(partitions > 0);

        partitionsCheckboxPanel.revalidate();
        partitionsCheckboxPanel.repaint();
    }

    private void loadGraph() {
        if (selectedInputFile == null) {
            chooseInputFile();
            if (selectedInputFile == null) {
                JOptionPane.showMessageDialog(this, "Load operation cancelled or no input file selected.");
                return;
            }
        }

        FileReader fileReader = new FileReader();
        String originalFilePath = selectedInputFile.getPath();
        String filePathToParse = originalFilePath;
        boolean isTempFileUsed = false;

        try {
            ParsedData parsedData;
            String fileNameLower = selectedInputFile.getName().toLowerCase();

            if (fileNameLower.endsWith(".csrrgbin") || fileNameLower.endsWith(".bin")) {
                System.out.println("Attempting to convert binary file: " + originalFilePath);
                filePathToParse = fileReader.convertBinaryToTemporaryTextFile(originalFilePath);
                isTempFileUsed = true;
                System.out.println("Binary file converted to temporary text file: " + filePathToParse);
            }

            parsedData = fileReader.parseFile(filePathToParse);
            Graph graph = fileReader.loadGraph(parsedData);
            mainWindow.updateGraph(graph);

            if (graph.getPartitions() > 1) {
                boolean isTrulyPrePartitioned = false;
                if (graph.getNodes() != null && !graph.getNodes().isEmpty()) {
                    for (Node node : graph.getNodes()) {
                        if (node.getPartId() >= 0 && node.getPartId() < graph.getPartitions()) {
                            isTrulyPrePartitioned = true;
                            break;
                        }
                    }
                }

                if (isTrulyPrePartitioned) {
                    runButton.setEnabled(false);
                    partitionsField.setText(String.valueOf(graph.getPartitions()));
                    partitionsField.setEnabled(false);
                    accuracyField.setEnabled(false);
                    accuracyField.setText("");
                    createPartitionCheckboxes(graph.getPartitions());
                    System.out.println("Graph is pre-partitioned. Controls disabled.");
                } else {
                    runButton.setEnabled(true);
                    partitionsField.setEnabled(true);
                    partitionsField.setText("");
                    accuracyField.setEnabled(true);
                    accuracyField.setText("");
                    createPartitionCheckboxes(0);
                    System.out.println(
                            "Graph loaded with single partition or no valid partition data. Controls enabled.");
                }
            } else {
                runButton.setEnabled(true);
                partitionsField.setEnabled(true);
                partitionsField.setText("");
                accuracyField.setEnabled(true);
                accuracyField.setText("");
                createPartitionCheckboxes(0);
                System.out.println("Graph loaded as unpartitioned. Controls enabled.");
            }
            mainWindow.getGraphPanel().repaint();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading graph: " + e.getMessage());
            runButton.setEnabled(true);
            partitionsField.setEnabled(true);
            partitionsField.setText("");
            accuracyField.setEnabled(true);
            accuracyField.setText("");
            createPartitionCheckboxes(0);
        } finally {
            if (isTempFileUsed && filePathToParse != null) {
                File tempFile = new File(filePathToParse);
                if (tempFile.exists() && tempFile.getName().startsWith("temp_graph_")) {
                }
            }
        }
    }

    private void runPartitioning() {
        Graph graph = mainWindow.getGraphPanel().getGraph();
        if (graph == null) {
            JOptionPane.showMessageDialog(this, "Please load a graph first");
            return;
        }

        if (selectedOutputFile == null) {
            chooseOutputFile();
            if (selectedOutputFile == null) {
                JOptionPane.showMessageDialog(this, "Save operation cancelled or no output file selected.");
                return;
            }
        }

        int parts;
        double accFraction;
        try {
            parts = Integer.parseInt(partitionsField.getText());
            accFraction = Double.parseDouble(accuracyField.getText()) / 100.0;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid number of partitions or accuracy percentage.");
            return;
        }

        if (parts <= 0) {
            JOptionPane.showMessageDialog(this, "Number of partitions must be positive.");
            return;
        }
        if (parts > graph.getVertices() && graph.getVertices() > 0) {
            JOptionPane.showMessageDialog(this,
                    "Number of partitions cannot exceed number of vertices (" + graph.getVertices() + ").");
            return;
        }
        if (accFraction < 0.0 || accFraction > 1.0) {
            JOptionPane.showMessageDialog(this, "Accuracy must be between 0% and 100%.");
            return;
        }

        graph.setPartitions(parts);
        graph.setMinCount(accFraction);
        graph.setMaxCount(accFraction);

        PartitionData partitionData = new PartitionData(parts);
        if (graph.getNodes() != null) {
            for (Node node : graph.getNodes()) {
                node.setPartId(-1);
            }
        }

        System.out
                .println("Starting Region Growing with " + parts + " parts and " + (accFraction * 100) + "% accuracy.");
        boolean rgSuccess = RegionGrowing.regionGrowing(graph, parts, partitionData, (float) accFraction);
        if (!rgSuccess) {
            System.out.println("Region Growing completed, but balance criteria might not be fully met.");
        } else {
            System.out.println("Region Growing completed successfully.");
        }

        System.out.println("Starting FM Optimization.");
        int fmMaxIterations = 100;
        FmOptimization.cutEdgesOptimization(graph, partitionData, fmMaxIterations);
        System.out.println("FM Optimization completed.");

        mainWindow.updateGraph(graph);

        try {
            if (selectedOutputType == 0) {
                FileWriter.writeText(selectedOutputFile.getAbsolutePath(), graph.getParsedData(), partitionData, graph,
                        parts);
            } else {
                FileWriter.writeBinary(selectedOutputFile.getAbsolutePath(), graph.getParsedData(), partitionData,
                        graph, parts);
            }
            JOptionPane.showMessageDialog(this,
                    "Partitioning complete. Output saved to " + selectedOutputFile.getName());

            createPartitionCheckboxes(parts);
            mainWindow.getGraphPanel().repaint();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving partitioned graph: " + e.getMessage());
        }
    }

    public void setInputFileLabel(String text) {
        inputFileLabel.setText(text);
    }

    public void setOutputFileLabel(String text) {
        outputFileLabel.setText(text);
    }
}