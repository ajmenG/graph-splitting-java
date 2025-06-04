package algorithm;

import model.Graph;
import model.Node;
import model.PartitionData;

import java.util.Arrays;

public class FmOptimization {
    /**
     * Context class for the FM algorithm, holding all necessary data
     */
    private static class FmContext {
        private Graph graph;
        private PartitionData partition;
        private int maxIterations;
        private int iterations;
        private int movesMade;
        private int initialCut;
        private int currentCut;
        private int bestCut;
        private boolean[] locked;
        private boolean[] unmovable;
        private int[] gains;
        private int[] targetParts;
        private int[] partSizes;
        private int[] bestPartition;  // Not used in current implementation

        public FmContext(Graph graph, PartitionData partition, int maxIterations) {
            this.graph = graph;
            this.partition = partition;
            this.maxIterations = maxIterations;
            this.iterations = 0;
            this.movesMade = 0;
            this.initialCut = 0;
            this.currentCut = 0;
            this.bestCut = Integer.MAX_VALUE;
            this.locked = new boolean[graph.getVertices()];
            this.unmovable = new boolean[graph.getVertices()];
            this.gains = new int[graph.getVertices()];
            this.targetParts = new int[graph.getVertices()];
            this.partSizes = new int[graph.getPartitions()];
            
            // Initialize part sizes from partition data
            if (partition != null) {
                for (int i = 0; i < graph.getPartitions(); i++) {
                    if (i < partition.getPartsCount()) {
                        this.partSizes[i] = partition.getPartitions().get(i).getPartitionVertexCount();
                    }
                }
            } else {
                // Count vertices in each part directly
                for (int i = 0; i < graph.getVertices(); i++) {
                    int partId = graph.getNode(i).getPartId();
                    if (partId >= 0 && partId < graph.getPartitions()) {
                        this.partSizes[partId]++;
                    }
                }
            }

            // Initialize gains and target parts
            for (int i = 0; i < graph.getVertices(); i++) {
                this.gains[i] = 0;
                this.targetParts[i] = graph.getNode(i).getPartId();
            }
        }
    }

    /**
     * Main FM optimization algorithm
     */
    public static void cutEdgesOptimization(Graph graph, PartitionData partitionData, int maxIterations) {
        // Check if iteration count makes sense
        if (maxIterations <= 0) {
            maxIterations = 100;
            System.out.println("Warning: max_iterations was set to an invalid value, using default: 100");
        }

        // Initialize algorithm context
        FmContext context = new FmContext(graph, partitionData, maxIterations);
        if (context == null) {
            System.err.println("Failed to initialize FM context");
            return;
        }

        // Allocate memory for boundary vertices
        boolean[] isBoundary = new boolean[graph.getVertices()];

        // Calculate initial state
        context.initialCut = calculateInitialCut(context);
        context.currentCut = context.initialCut;

        // Show stats before optimization
        printPartitionStats(context);

        // Find boundary vertices
        identifyBoundaryVertices(context, isBoundary);

        // Analyze possible moves
        analyzeMoves(context, isBoundary);

        // If there's nothing to optimize, exit
        if (context.initialCut == 0) {
            System.out.println("No crossing edges to optimize. Exiting.");
            return;
        }

        System.out.println("\nStarting FM optimization with " + maxIterations + " max iterations");

        // Main algorithm loop
        for (int iter = 0; iter < maxIterations; iter++) {
            // Update boundary vertices
            identifyBoundaryVertices(context, isBoundary);

            // Unlock vertices at start of each iteration
            Arrays.fill(context.locked, false);

            // Find best possible move
            int bestMoveVertex = findBestMove(context, isBoundary);

            // If no move was found, end
            if (bestMoveVertex == -1) {
                System.out.println("No valid moves found. Stopping.");
                break;
            }

            int targetPart = context.targetParts[bestMoveVertex];

            // Execute move only if it doesn't break connectivity
            if (!applyMoveSafely(context, bestMoveVertex, targetPart)) {
                // If move can't be made, lock the vertex
                context.locked[bestMoveVertex] = true;
                System.out.println("Move not possible for vertex " + bestMoveVertex + 
                                 " to part " + targetPart + ". Blocking vertex.");
                continue;
            }

            // Check if everything is still OK after the move
            if (!verifyPartitionIntegrity(context.graph)) {
                System.out.println("CRITICAL ERROR: Partition integrity violated in iteration " + iter + "!");
                break;
            }

            System.out.println("Current cut: " + context.currentCut);
        }

        // Show results
        printCutStatistics(context);
    }

    /**
     * Finds the best possible move in the graph
     */
    private static int findBestMove(FmContext context, boolean[] isBoundary) {
        int bestVertex = -1;
        int bestGain = 0;
        int bestTargetPart = -1;

        // Check all vertices one by one
        for (int i = 0; i < context.graph.getVertices(); i++) {
            // Only boundary, unlocked and not banned vertices
            if (isBoundary[i] && !context.locked[i] && !context.unmovable[i]) {
                // Check all possible target partitions
                for (int p = 0; p < context.graph.getPartitions(); p++) {
                    // Skip the partition the vertex is already in
                    if (p != context.graph.getNode(i).getPartId()) {
                        int gain = calculateGain(context, i, p);
                        // Move must be beneficial and not break connectivity
                        if (gain > 0 && gain > bestGain && isMoveValidWithIntegrity(context, i, p)) {
                            bestGain = gain;
                            bestVertex = i;
                            bestTargetPart = p;
                        }
                    }
                }
            }
        }

        // If we found a good move, save the target partition
        if (bestVertex != -1) {
            context.targetParts[bestVertex] = bestTargetPart;
        }

        return bestVertex;
    }

    /**
     * Checks if a partition is connected
     */
    private static boolean isPartitionConnected(Graph graph, int partId) {
        // Check parameters
        if (graph == null)
            return false;

        // Count how many vertices are in this partition
        int verticesInPart = 0;
        for (int i = 0; i < graph.getVertices(); i++)
            if (graph.getNode(i).getPartId() == partId)
                verticesInPart++;

        // Empty partition is OK
        if (verticesInPart == 0)
            return true;

        // One vertex is also OK
        if (verticesInPart == 1)
            return true;

        // Allocate memory for BFS
        boolean[] visited = new boolean[graph.getVertices()];
        int[] queue = new int[graph.getVertices()];

        // Find first vertex in partition
        int startVertex = -1;
        for (int i = 0; i < graph.getVertices(); i++) {
            if (graph.getNode(i).getPartId() == partId) {
                startVertex = i;
                break;
            }
        }

        // BFS starting from found vertex
        int front = 0, rear = 0;
        queue[rear++] = startVertex;
        visited[startVertex] = true;
        int nodesVisited = 1;

        while (front < rear) {
            int current = queue[front++];

            // Check neighbors of current vertex
            for (Node neighbor : graph.getNode(current).getNeighbours()) {
                int neighborId = neighbor.getId();

                // Add to queue only neighbors from the same partition
                if (graph.getNode(neighborId).getPartId() == partId && !visited[neighborId]) {
                    visited[neighborId] = true;
                    queue[rear++] = neighborId;
                    nodesVisited++;
                }
            }
        }

        // Partition is connected if we visited all vertices
        return (nodesVisited == verticesInPart);
    }

    /**
     * Checks if a partition will remain connected after removing a vertex
     */
    private static boolean willRemainConnectedIfRemoved(Graph graph, int vertex) {
        int currentPart = graph.getNode(vertex).getPartId();

        // Count vertices in partition without the removed one
        int verticesInPart = 0;
        for (int i = 0; i < graph.getVertices(); i++) {
            if (i != vertex && graph.getNode(i).getPartId() == currentPart)
                verticesInPart++;
        }

        // If 0 or 1 vertex remains, it will be connected
        if (verticesInPart <= 1) {
            return true;
        }

        // Allocate memory for BFS
        boolean[] visited = new boolean[graph.getVertices()];
        int[] queue = new int[graph.getVertices()];

        // Find first vertex in partition (other than the one being removed)
        int startVertex = -1;
        for (int i = 0; i < graph.getVertices(); i++) {
            if (i != vertex && graph.getNode(i).getPartId() == currentPart) {
                startVertex = i;
                break;
            }
        }

        // BFS skipping the removed vertex
        int front = 0, rear = 0;
        queue[rear++] = startVertex;
        visited[startVertex] = true;
        int nodesVisited = 1;

        while (front < rear) {
            int current = queue[front++];

            for (Node neighbor : graph.getNode(current).getNeighbours()) {
                int neighborId = neighbor.getId();

                // Skip the removed vertex
                if (neighborId != vertex && 
                    graph.getNode(neighborId).getPartId() == currentPart &&
                    !visited[neighborId]) {
                    visited[neighborId] = true;
                    queue[rear++] = neighborId;
                    nodesVisited++;
                }
            }
        }

        // Check if all vertices are reachable
        return (nodesVisited == verticesInPart);
    }

    /**
     * Verifies connectivity of all partitions in the graph
     */
    private static boolean verifyPartitionIntegrity(Graph graph) {
        if (graph == null)
            return false;

        boolean allConnected = true;

        // Find all unique partition IDs
        int[] uniquePartitions = new int[graph.getVertices()];
        boolean[] found = new boolean[graph.getVertices()];
        int uniqueCount = 0;

        // Collect all partitions
        for (int i = 0; i < graph.getVertices(); i++) {
            int partId = graph.getNode(i).getPartId();
            if (partId >= 0 && !found[partId]) {
                found[partId] = true;
                uniquePartitions[uniqueCount++] = partId;
            }
        }

        // Check status of each partition
        for (int i = 0; i < uniqueCount; i++) {
            int partId = uniquePartitions[i];
            boolean isConnected = isPartitionConnected(graph, partId);

            if (!isConnected)
                allConnected = false;
        }

        return allConnected;
    }

    /**
     * Counts how many edges cross partition boundaries
     */
    private static int countCutEdges(Graph graph) {
        if (graph == null)
            return 0;

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

    /**
     * Displays partition statistics
     */
    private static void printPartitionStats(FmContext context) {
        System.out.println("\n--- Partition Statistics ---");

        // Count vertices in partitions
        int totalVertices = 0;
        for (int i = 0; i < context.graph.getPartitions(); i++) {
            // Print how many vertices are in each partition and percentage of average
            float percentage = 100.0f * context.partSizes[i] / 
                             (context.graph.getVertices() / context.graph.getPartitions());

            System.out.println("  Partition " + i + ": " + context.partSizes[i] + 
                             " vertices (" + String.format("%.2f", percentage) + "%)");
            totalVertices += context.partSizes[i];
        }
        System.out.println("  Total vertices: " + totalVertices);

        System.out.println("--- End of Statistics ---");
    }

    /**
     * Analyzes available moves
     */
    private static void analyzeMoves(FmContext context, boolean[] isBoundary) {
        int totalMoves = 0;
        int validMoves = 0;
        int positiveGainMoves = 0;
        int balanceViolations = 0;

        System.out.println("\n--- Move Analysis ---");

        // Check all vertices
        for (int i = 0; i < context.graph.getVertices(); i++) {
            if (isBoundary[i]) {
                // For each possible target partition
                for (int p = 0; p < context.graph.getPartitions(); p++) {
                    if (p != context.graph.getNode(i).getPartId()) {
                        totalMoves++;
                        boolean isValid = isValidMove(context, i, p);
                        int gain = calculateGain(context, i, p);

                        if (!isValid) {
                            balanceViolations++;
                        } else {
                            validMoves++;
                            if (gain > 0) {
                                positiveGainMoves++;
                            }
                        }
                    }
                }
            }
        }

        // Print summary
        System.out.println("  Total possible moves: " + totalMoves);
        System.out.println("  Valid moves: " + validMoves);
        System.out.println("  Balance violations: " + balanceViolations);
        System.out.println("  Moves with positive gain: " + positiveGainMoves);
        System.out.println("--- End of Analysis ---");
    }

    /**
     * Checks if a move won't break partition connectivity
     */
    private static boolean isMoveValidWithIntegrity(FmContext context, int vertex, int targetPart) {
        // First check basic conditions
        if (!isValidMove(context, vertex, targetPart)) {
            return false;
        }

        // Remember current partition
        int sourcePart = context.graph.getNode(vertex).getPartId();

        // Check if after removing vertex the partition remains connected
        if (!willRemainConnectedIfRemoved(context.graph, vertex)) {
            context.unmovable[vertex] = true; // ban vertex
            return false;
        }

        // Check if vertex has a connection to new partition
        boolean hasConnection = false;
        for (Node neighbor : context.graph.getNode(vertex).getNeighbours()) {
            if (context.graph.getNode(neighbor.getId()).getPartId() == targetPart) {
                hasConnection = true;
                break;
            }
        }

        // Can't move if no connection
        if (!hasConnection) {
            context.unmovable[vertex] = true;
            return false;
        }

        // All OK
        return true;
    }

    /**
     * Safely moves a vertex between partitions
     */
    private static boolean applyMoveSafely(FmContext context, int vertex, int targetPart) {
        // Verify again that move is valid
        if (!isMoveValidWithIntegrity(context, vertex, targetPart)) {
            context.unmovable[vertex] = true;
            return false;
        }

        // Save initial state
        int sourcePart = context.graph.getNode(vertex).getPartId();
        int gain = calculateGain(context, vertex, targetPart);

        // Execute move
        context.graph.getNode(vertex).setPartId(targetPart);
        context.partSizes[sourcePart]--;
        context.partSizes[targetPart]++;

        // Check if all partitions are still connected
        if (!verifyPartitionIntegrity(context.graph)) {
            // Rollback move
            context.graph.getNode(vertex).setPartId(sourcePart);
            context.partSizes[sourcePart]++;
            context.partSizes[targetPart]--;
            context.unmovable[vertex] = true; // ban for future
            return false;
        }

        // Update state after move
        context.currentCut -= gain;
        context.movesMade++;
        context.locked[vertex] = true;

        // Remember best result
        if (context.currentCut < context.bestCut) {
            context.bestCut = context.currentCut;
        }

        return true;
    }

    /**
     * Calculates initial number of cut edges
     */
    private static int calculateInitialCut(FmContext context) {
        int cutEdges = 0;

        // Go through all edges
        for (int i = 0; i < context.graph.getVertices(); i++) {
            for (Node neighbor : context.graph.getNode(i).getNeighbours()) {
                int neighborId = neighbor.getId();

                // Count only in one direction to avoid double counting
                if (i < neighborId && 
                    context.graph.getNode(i).getPartId() != context.graph.getNode(neighborId).getPartId()) {
                    cutEdges++;
                }
            }
        }

        return cutEdges;
    }

    /**
     * Calculates gain from moving a vertex
     */
    private static int calculateGain(FmContext context, int vertex, int targetPart) {
        // Check parameters
        if (vertex < 0 || vertex >= context.graph.getVertices() ||
            targetPart < 0 || targetPart >= context.graph.getPartitions()) {
            return 0;
        }

        int currentPart = context.graph.getNode(vertex).getPartId();
        int gain = 0;

        // Check all neighbors
        for (Node neighbor : context.graph.getNode(vertex).getNeighbours()) {
            int neighborId = neighbor.getId();
            if (neighborId < 0 || neighborId >= context.graph.getVertices()) {
                continue;
            }

            int neighborPart = context.graph.getNode(neighborId).getPartId();

            // Neighbor in target partition - we gain because edge won't be cut
            if (neighborPart == targetPart) {
                gain++;
            }
            // Neighbor in current partition - we lose because we create new cut
            else if (neighborPart == currentPart) {
                gain--;
            }
            // Neighbors in other partitions don't change the result
        }

        return gain;
    }

    /**
     * Checks if a vertex can be moved
     */
    private static boolean isValidMove(FmContext context, int vertex, int targetPart) {
        // Check parameter validity
        if (vertex < 0 || vertex >= context.graph.getVertices() ||
            targetPart < 0 || targetPart >= context.graph.getPartitions()) {
            return false;
        }

        int currentPart = context.graph.getNode(vertex).getPartId();

        // Don't move banned vertices
        if (context.unmovable[vertex]) {
            return false;
        }

        // Don't move locked vertices
        if (context.locked[vertex]) {
            return false;
        }

        // Don't move to the same partition
        if (currentPart == targetPart) {
            return false;
        }

        // Check size constraints
        int newSizeSource = context.partSizes[currentPart] - 1;
        int newSizeTarget = context.partSizes[targetPart] + 1;
        int minSize = context.graph.getMinCount();
        int maxSize = context.graph.getMaxCount();

        // If violates constraints, can't execute move
        if (newSizeSource < minSize || newSizeTarget > maxSize) {
            return false;
        }

        return true;
    }

    /**
     * Finds boundary vertices which are on partition borders
     */
    private static void identifyBoundaryVertices(FmContext context, boolean[] isBoundary) {
        for (int i = 0; i < context.graph.getVertices(); i++) {
            // Initially assume not a boundary
            isBoundary[i] = false;

            // Check all neighbors
            for (Node neighbor : context.graph.getNode(i).getNeighbours()) {
                int neighborId = neighbor.getId();
                // If neighbor is in a different partition, vertex is a boundary
                if (context.graph.getNode(i).getPartId() != context.graph.getNode(neighborId).getPartId()) {
                    isBoundary[i] = true;
                    break;
                }
            }
        }
    }

    /**
     * Displays statistics after optimization
     */
    private static void printCutStatistics(FmContext context) {
        System.out.println("\n\n  Initial cut: " + context.initialCut);
        System.out.println("  Current cut: " + context.currentCut);
        System.out.println("  Improvement: " + (context.initialCut - context.bestCut));
        System.out.println("  Moves made: " + context.movesMade);
    }
}
