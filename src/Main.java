import io.FileReader;
import model.Graph;
import model.Node;
import model.ParsedData;
import model.PartitionData;
import model.Partition;
import algorithm.RegionGrowing;
import algorithm.FmOptimization;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the file name:");
        String filename = scanner.nextLine();

        FileReader fileReader = new FileReader();
        ParsedData parsedData = null;
        try {
            parsedData = fileReader.parseFile("./data/" + filename + ".csrrg");
        } catch (Exception e) {
            System.err.println("Error reading binary file: " + e.getMessage());
            return;
        }
        System.out.println("File read successfully. Parsed data:");

        Graph graph = fileReader.loadGraph(parsedData);

        // Display basic graph information
        System.out.println("Graph loaded with " + graph.getVertices() + " vertices and " + graph.getEdges() + " edges.");
        
        // Initialize partition parameters
        int partitions = 0;
        int accuracyPercent = 0;
        float accuracy = 0.0f;
        
        // Read number of partitions with error handling
        boolean validPartitions = false;
        while (!validPartitions) {
            try {
                System.out.println("Enter number of partitions:");
                partitions = Integer.parseInt(scanner.nextLine());
                if (partitions > 0) {
                    validPartitions = true;
                } else {
                    System.out.println("Number of partitions must be positive.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid integer.");
            }
        }
        
        // Read accuracy with error handling (as percentage)
        boolean validAccuracy = false;
        while (!validAccuracy) {
            try {
                System.out.println("Enter partition balance accuracy (0-100%):");
                String accuracyStr = scanner.nextLine();
                accuracyPercent = Integer.parseInt(accuracyStr);
                if (accuracyPercent >= 0 && accuracyPercent <= 100) {
                    // Convert percentage to decimal (0.0-1.0)
                    accuracy = (float) accuracyPercent / 100.0f;
                    validAccuracy = true;
                } else {
                    System.out.println("Accuracy must be between 0 and 100%.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid integer percentage.");
            }
        }
        
        System.out.println("Using accuracy value: " + accuracy + " (" + accuracyPercent + "%)");
        
        // Set partitions count and constraints
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
        
        // Apply region growing algorithm
        System.out.println("\nApplying Region Growing algorithm for initial partitioning...");
        boolean success = RegionGrowing.regionGrowing(graph, partitions, partitionData, accuracy);
        
        if (success) {
            System.out.println("Region Growing completed successfully.");
        } else {
            System.out.println("Region Growing completed with balance issues.");
        }
        
        // Print initial partition distribution
        System.out.println("\nInitial partition distribution:");
        printPartitionSummary(graph, partitions);
        
        // Apply FM optimization
        System.out.println("\nApplying FM optimization to reduce edge cuts...");
        FmOptimization.cutEdgesOptimization(graph, partitionData, 100);
        
        // Print final partition distribution
        System.out.println("\nFinal partition distribution:");
        printPartitionSummary(graph, partitions);
        
        // Print detailed partition assignments
        System.out.println("\nDetailed partition assignments:");
        printDetailedPartitions(graph, partitions);
    }
    
    /**
     * Prints a summary of partition sizes
     */
    private static void printPartitionSummary(Graph graph, int partitions) {
        int[] partSizes = new int[partitions];
        
        // Count vertices in each partition
        for (int i = 0; i < graph.getVertices(); i++) {
            int partId = graph.getNode(i).getPartId();
            if (partId >= 0 && partId < partitions) {
                partSizes[partId]++;
            }
        }
        
        // Print partition sizes
        for (int i = 0; i < partitions; i++) {
            float percentage = 100.0f * partSizes[i] / graph.getVertices();
            System.out.println("Partition " + i + ": " + partSizes[i] + " vertices (" + 
                            String.format("%.2f", percentage) + "%)");
        }
        
        // Print edge cut information
        System.out.println("Edge cuts: " + countCutEdges(graph));
    }
    
    /**
     * Prints detailed partition assignments for each vertex
     */
    private static void printDetailedPartitions(Graph graph, int partitions) {
        // Create arrays to hold vertices for each partition
        List<List<Integer>> partitionVertices = new ArrayList<>();
        for (int i = 0; i < partitions; i++) {
            partitionVertices.add(new ArrayList<>());
        }
        
        // Assign vertices to their partitions
        for (int i = 0; i < graph.getVertices(); i++) {
            int partId = graph.getNode(i).getPartId();
            if (partId >= 0 && partId < partitions) {
                partitionVertices.get(partId).add(i);
            }
        }
        
        // Print vertices for each partition
        for (int i = 0; i < partitions; i++) {
            System.out.print("Partition " + i + ": ");
            List<Integer> vertices = partitionVertices.get(i);
            
            // Print first 20 vertices and indicate if there are more
            for (int j = 0; j < Math.min(20, vertices.size()); j++) {
                System.out.print(vertices.get(j) + " ");
            }
            
            if (vertices.size() > 20) {
                System.out.print("... (" + (vertices.size() - 20) + " more)");
            }
            
            System.out.println();
        }
    }
    
    /**
     * Counts how many edges cross partition boundaries
     */
    private static int countCutEdges(Graph graph) {
        int cutEdges = 0;
        
        // Go through all edges
        for (int i = 0; i < graph.getVertices(); i++) {
            for (Node neighbor : graph.getNode(i).getNeighbours()) {
                int neighborId = neighbor.getId();
                
                // Count only in one direction to avoid double counting
                if (i < neighborId && graph.getNode(i).getPartId() != graph.getNode(neighborId).getPartId()) {
                    cutEdges++;
                }
            }
        }
        
        return cutEdges;
    }
}
