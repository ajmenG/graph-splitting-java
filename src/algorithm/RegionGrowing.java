package algorithm;

import model.Graph;
import model.Node;
import model.PartitionData;

import java.util.*;

public class RegionGrowing {
    private static class Queue {
        private int[] items;
        private int front;
        private int rear;
        private int maxSize;

        public Queue(int maxSize) {
            this.maxSize = maxSize;
            this.items = new int[maxSize];
            this.front = 0;
            this.rear = 0;
        }

        public boolean isEmpty() {
            return front == rear;
        }

        public void addToQueue(int item) {
            if (rear < maxSize) {
                items[rear++] = item;
            } else {
                System.err.println("Queue is full");
            }
        }

        public void removeFromQueue() {
            if (!isEmpty()) {
                front++;
            } else {
                System.err.println("Queue is empty");
            }
        }
    }

    /**
     * Generates seed points for partitions, trying to keep them distant from each other
     */
    private static int[] generateSeedPoints(Graph graph, int parts) {
        Random random = new Random();
        int[] seedPoints = new int[parts];

        // Select first point randomly
        seedPoints[0] = random.nextInt(graph.getVertices());

        // For each subsequent point, find one that is minimally connected with previous points
        for (int i = 1; i < parts; i++) {
            int bestVertex = -1;
            int minConnections = graph.getVertices(); // maximum possible connections

            // Check 100 random candidates
            for (int j = 0; j < 100; j++) {
                int candidate = random.nextInt(graph.getVertices());

                // Count connections with already chosen points
                int connections = 0;
                for (int k = 0; k < i; k++) {
                    int seed = seedPoints[k];

                    // Check for direct connection
                    for (Node neighbor : graph.getNode(candidate).getNeighbours()) {
                        if (neighbor.getId() == seed) {
                            connections++;
                            break;
                        }
                    }
                }

                // Select vertex with fewest connections
                if (connections < minConnections) {
                    minConnections = connections;
                    bestVertex = candidate;

                    // If we found a completely unconnected point, that's ideal
                    if (minConnections == 0)
                        break;
                }
            }

            seedPoints[i] = bestVertex;
        }

        // Mark seed points as belonging to their respective partitions
        for (int i = 0; i < parts; i++) {
            graph.getNode(seedPoints[i]).setPartId(i);
        }

        return seedPoints;
    }

    /**
     * Main region growing algorithm
     */
    public static boolean regionGrowing(Graph graph, int parts, PartitionData partitionData, float accuracy) {
        // Check if number of partitions makes sense
        if (parts > graph.getVertices()) {
            throw new IllegalArgumentException("Number of parts cannot be greater than number of vertices");
        }

        // Initialize variables
        int[] seedPoints = generateSeedPoints(graph, parts);
        boolean[] visited = new boolean[graph.getVertices()];
        
        // Create frontiers for each partition
        List<List<Integer>> frontiers = new ArrayList<>(parts);
        for (int i = 0; i < parts; i++) {
            frontiers.add(new ArrayList<>());
        }

        // Reset partition assignments
        for (int i = 0; i < graph.getVertices(); i++) {
            graph.getNode(i).setPartId(-1); // -1 means unassigned
        }

        // Initialize partition size counters
        int[] partCounts = new int[parts];

        // Add seed points to frontiers
        for (int i = 0; i < parts; i++) {
            visited[seedPoints[i]] = true;
            graph.getNode(seedPoints[i]).setPartId(i);
            if (partitionData != null) {
                partitionData.addVertexToPartition(i, seedPoints[i]);
            }
            partCounts[i]++;

            // Add neighbors of seed point to frontier
            Node node = graph.getNode(seedPoints[i]);
            for (Node neighbor : node.getNeighbours()) {
                if (!visited[neighbor.getId()]) {
                    frontiers.get(i).add(neighbor.getId());
                }
            }
        }

        // Calculate average vertices per part
        float avgVerticesPerPart = (float) graph.getVertices() / parts;

        // Calculate min and max based on accuracy
        int minVerticesPerPart = (int) (avgVerticesPerPart * (1.0 - accuracy));
        if ((float) minVerticesPerPart < (avgVerticesPerPart * (1.0 - accuracy))) {
            minVerticesPerPart++;
        }
        int maxVerticesPerPart = (int) (avgVerticesPerPart * (1.0 + accuracy));

        // Main algorithm loop
        int iterations = 0;
        int unassigned = graph.getVertices() - parts; // all vertices minus seed points

        // Optimization - keep a list of active partitions
        List<Integer> activePartitions = new ArrayList<>(parts);
        for (int i = 0; i < parts; i++) {
            activePartitions.add(i);
        }

        while (unassigned > 0 && iterations < graph.getVertices() * 2) {
            int minPart = -1;
            int minSize = graph.getVertices();

            // Check only active partitions
            Iterator<Integer> iterator = activePartitions.iterator();
            while (iterator.hasNext()) {
                int i = iterator.next();
                if (!frontiers.get(i).isEmpty() && partCounts[i] < maxVerticesPerPart) {
                    if (minPart == -1 || partCounts[i] < minSize) {
                        minPart = i;
                        minSize = partCounts[i];
                    }
                }
                
                // If partition becomes inactive, remove it from the list
                if (frontiers.get(i).isEmpty()) {
                    iterator.remove();
                }
            }

            // Select only a vertex that has a neighbor in the partition
            boolean validVertexFound = false;
            int current = -1;

            // Check vertices in the frontier one by one
            if (minPart != -1) {
                for (int i = 0; i < frontiers.get(minPart).size(); i++) {
                    int candidate = frontiers.get(minPart).get(i);

                    // Check if candidate has a neighbor in the partition
                    if (!visited[candidate]) {
                        boolean hasNeighborInPartition = false;
                        Node node = graph.getNode(candidate);

                        for (Node neighbor : node.getNeighbours()) {
                            if (graph.getNode(neighbor.getId()).getPartId() == minPart) {
                                hasNeighborInPartition = true;
                                break;
                            }
                        }

                        if (hasNeighborInPartition) {
                            current = candidate;
                            validVertexFound = true;

                            // Remove it from frontier by swapping with last - faster than shifting
                            int lastIndex = frontiers.get(minPart).size() - 1;
                            if (i != lastIndex) {
                                frontiers.get(minPart).set(i, frontiers.get(minPart).get(lastIndex));
                            }
                            frontiers.get(minPart).remove(lastIndex);
                            break;
                        }
                    }
                }

                // If we didn't find a valid vertex, skip this partition
                if (!validVertexFound) {
                    // Reset frontier
                    frontiers.get(minPart).clear();
                    continue;
                }

                if (!visited[current]) {
                    visited[current] = true;
                    graph.getNode(current).setPartId(minPart);
                    if (partitionData != null) {
                        partitionData.addVertexToPartition(minPart, current);
                    }
                    partCounts[minPart]++;
                    unassigned--;

                    // Add neighbors to frontier
                    Node node = graph.getNode(current);
                    for (Node neighbor : node.getNeighbours()) {
                        int neighborId = neighbor.getId();
                        
                        // Check neighbor index
                        if (neighborId < 0 || neighborId >= graph.getVertices()) {
                            continue;
                        }

                        if (!visited[neighborId]) {
                            frontiers.get(minPart).add(neighborId);
                        }
                    }
                }
            }

            iterations++;

            // Check progress periodically
            if (iterations % 100 == 0) {
                // Find min and max vertex counts
                int minCount = graph.getVertices();
                int maxCount = 0;

                for (int i = 0; i < parts; i++) {
                    if (partCounts[i] < minCount)
                        minCount = partCounts[i];
                    if (partCounts[i] > maxCount)
                        maxCount = partCounts[i];
                }

                // Check if we're already balanced
                if (minCount >= minVerticesPerPart && unassigned == 0) {
                    break;
                }
            }
        }

        // Check if there are any unassigned vertices
        int unassignedCount = 0;
        for (int i = 0; i < graph.getVertices(); i++) {
            if (graph.getNode(i).getPartId() == -1) {
                unassignedCount++;
            }
        }

        if (unassignedCount > 0) {
            // Create a list of unassigned vertices
            List<Integer> unassignedVertices = new ArrayList<>(unassignedCount);
            
            for (int i = 0; i < graph.getVertices(); i++) {
                if (graph.getNode(i).getPartId() == -1) {
                    unassignedVertices.add(i);
                }
            }

            // Try to assign while maintaining connectivity
            boolean assigned = true; // flag whether we assigned anything in this iteration
            
            while (assigned && !unassignedVertices.isEmpty()) {
                assigned = false;
                
                // Check each unassigned vertex
                Iterator<Integer> unassignedIter = unassignedVertices.iterator();
                while (unassignedIter.hasNext()) {
                    int v = unassignedIter.next();
                    
                    if (graph.getNode(v).getPartId() != -1) {
                        // Already assigned in this iteration
                        unassignedIter.remove();
                        continue;
                    }
                    
                    Node node = graph.getNode(v);
                    int smallestNeighborPart = -1;
                    int smallestNeighborCount = graph.getVertices();
                    
                    // Find neighboring partition with smallest number of vertices
                    for (Node neighbor : node.getNeighbours()) {
                        int neighborId = neighbor.getId();
                        
                        if (neighborId >= 0 && neighborId < graph.getVertices() && 
                            graph.getNode(neighborId).getPartId() != -1) {
                            int neighborPart = graph.getNode(neighborId).getPartId();
                            
                            if (partCounts[neighborPart] < smallestNeighborCount) {
                                smallestNeighborCount = partCounts[neighborPart];
                                smallestNeighborPart = neighborPart;
                            }
                        }
                    }
                    
                    // If we found a neighboring partition, assign
                    if (smallestNeighborPart != -1) {
                        graph.getNode(v).setPartId(smallestNeighborPart);
                        if (partitionData != null) {
                            partitionData.addVertexToPartition(smallestNeighborPart, v);
                        }
                        partCounts[smallestNeighborPart]++;
                        assigned = true;
                        
                        unassignedIter.remove();
                    }
                }
            }
            
            // If there are still unassigned vertices, assign by force
            if (!unassignedVertices.isEmpty()) {
                for (int v : unassignedVertices) {
                    // Give to smallest partition
                    int minPart = 0;
                    for (int j = 1; j < parts; j++) {
                        if (partCounts[j] < partCounts[minPart]) {
                            minPart = j;
                        }
                    }
                    
                    graph.getNode(v).setPartId(minPart);
                    if (partitionData != null) {
                        partitionData.addVertexToPartition(minPart, v);
                    }
                    partCounts[minPart]++;
                }
            }
        }
        
        // Warn if iteration limit was exceeded
        if (iterations >= graph.getVertices() * 3) {
            System.out.println("WARNING: Loop terminated due to iteration limit");
        }
        
        // Check if we met accuracy requirements
        boolean success = true;
        float minRatio = graph.getVertices();
        float maxRatio = 0;
        
        for (int i = 0; i < parts; i++) {
            float ratio = (float)partCounts[i] / avgVerticesPerPart;
            if (ratio < minRatio)
                minRatio = ratio;
            if (ratio > maxRatio)
                maxRatio = ratio;
        }
        
        // Warning if we didn't meet requirements
        if (minRatio < (1.0 - accuracy) || maxRatio > (1.0 + accuracy)) {
            success = false;
        }
        
        // Verify connectivity and fix if needed
        boolean allConnected = true;
        
        // Check connectivity for each partition
        for (int p = 0; p < parts; p++) {
            boolean isConnected = verifyPartitionConnectivity(graph, p);
            if (!isConnected) {
                allConnected = false;
                fixDisconnectedPartition(graph, p, partCounts);
            }
        }
        
        if (!allConnected) {
            System.out.println("Fixed disconnected partitions. Re-verifying connectivity...");
            checkPartitionConnectivity(graph, parts);
        }
        
        return success;
    }
    
    /**
     * Checks the connectivity of a single partition
     */
    private static boolean verifyPartitionConnectivity(Graph graph, int partId) {
        // Count vertices in partition (just once)
        int[] partitionNodeCounts = new int[graph.getPartitions()];
        for (int i = 0; i < graph.getVertices(); i++) {
            int nodePartId = graph.getNode(i).getPartId();
            if (nodePartId >= 0 && nodePartId < graph.getPartitions()) {
                partitionNodeCounts[nodePartId]++;
            }
        }
        
        // Number of vertices in the checked partition
        int count = partitionNodeCounts[partId];
        
        // Empty or with one vertex is connected
        if (count <= 1) {
            return true;
        }
        
        // Prepare structures for BFS
        boolean[] visited = new boolean[graph.getVertices()];
        int[] queue = new int[graph.getVertices()];
        
        // Find first vertex in partition
        int start = -1;
        for (int i = 0; i < graph.getVertices(); i++) {
            if (graph.getNode(i).getPartId() == partId) {
                start = i;
                break;
            }
        }
        
        // BFS from this vertex
        int front = 0, rear = 0;
        queue[rear++] = start;
        visited[start] = true;
        int visitedCount = 1;
        
        while (front < rear) {
            int current = queue[front++];
            
            // Check neighbors
            for (Node neighbor : graph.getNode(current).getNeighbours()) {
                int neighborId = neighbor.getId();
                // Add to queue only neighbors from the same partition
                if (graph.getNode(neighborId).getPartId() == partId && !visited[neighborId]) {
                    visited[neighborId] = true;
                    queue[rear++] = neighborId;
                    visitedCount++;
                }
            }
        }
        
        // Partition is connected if we visited all vertices
        return (visitedCount == count);
    }
    
    /**
     * Fixes disconnected partitions
     */
    private static void fixDisconnectedPartition(Graph graph, int partId, int[] partCounts) {
        // Find all components in the partition
        boolean[] visited = new boolean[graph.getVertices()];
        int[] componentId = new int[graph.getVertices()];
        int[] componentSize = new int[graph.getVertices()];
        int componentCount = 0;
        
        // Go through partition vertices
        for (int i = 0; i < graph.getVertices(); i++) {
            if (graph.getNode(i).getPartId() == partId && !visited[i]) {
                // Found a new component
                int[] queue = new int[graph.getVertices()];
                
                // BFS for this component
                int front = 0, rear = 0;
                queue[rear++] = i;
                visited[i] = true;
                componentId[i] = componentCount;
                componentSize[componentCount]++;
                
                while (front < rear) {
                    int current = queue[front++];
                    
                    for (Node neighbor : graph.getNode(current).getNeighbours()) {
                        int neighborId = neighbor.getId();
                        if (graph.getNode(neighborId).getPartId() == partId && !visited[neighborId]) {
                            visited[neighborId] = true;
                            queue[rear++] = neighborId;
                            componentId[neighborId] = componentCount;
                            componentSize[componentCount]++;
                        }
                    }
                }
                
                componentCount++;
            }
        }
        
        // Keep the largest component, reassign the rest to other partitions
        int largestComponent = 0;
        for (int i = 1; i < componentCount; i++) {
            if (componentSize[i] > componentSize[largestComponent]) {
                largestComponent = i;
            }
        }
        
        // Reassign other components to nearest partitions
        for (int i = 0; i < graph.getVertices(); i++) {
            if (graph.getNode(i).getPartId() == partId && componentId[i] != largestComponent) {
                // Find a neighboring partition
                int bestPart = -1;
                Node node = graph.getNode(i);
                
                for (Node neighbor : node.getNeighbours()) {
                    int neighborId = neighbor.getId();
                    int neighborPart = graph.getNode(neighborId).getPartId();
                    
                    if (neighborPart != partId && neighborPart != -1) {
                        if (bestPart == -1 || partCounts[neighborPart] < partCounts[bestPart]) {
                            bestPart = neighborPart;
                        }
                    }
                }
                
                // If we found a neighboring partition, assign
                if (bestPart != -1) {
                    partCounts[partId]--;
                    partCounts[bestPart]++;
                    graph.getNode(i).setPartId(bestPart);
                }
            }
        }
    }
    
    /**
     * Checks connectivity of all partitions in the graph
     */
    private static void checkPartitionConnectivity(Graph graph, int parts) {
        boolean allConnected = true;
        
        // Allocate structures only once for all partitions
        boolean[] visited = new boolean[graph.getVertices()];
        int[] queue = new int[graph.getVertices()];
        
        // Check each partition in turn
        for (int p = 0; p < parts; p++) {
            // Reset visited array for new partition
            Arrays.fill(visited, false);
            
            // Count how many vertices are in this partition
            int verticesInPart = 0;
            int verticesVisited = 0;
            
            for (int i = 0; i < graph.getVertices(); i++) {
                if (graph.getNode(i).getPartId() == p)
                    verticesInPart++;
            }
            
            // If partition has 0 or 1 vertex, it's connected
            if (verticesInPart <= 1) {
                continue;
            }
            
            // Find first vertex in partition
            int startVertex = -1;
            for (int i = 0; i < graph.getVertices(); i++) {
                if (graph.getNode(i).getPartId() == p) {
                    startVertex = i;
                    break;
                }
            }
            
            // BFS starting from found vertex
            int front = 0, rear = 0;
            queue[rear++] = startVertex;
            visited[startVertex] = true;
            verticesVisited = 1;
            
            while (front < rear) {
                int current = queue[front++];
                
                for (Node neighbor : graph.getNode(current).getNeighbours()) {
                    int neighborId = neighbor.getId();
                    
                    // Add to queue only neighbors from the same partition
                    if (graph.getNode(neighborId).getPartId() == p && !visited[neighborId]) {
                        visited[neighborId] = true;
                        queue[rear++] = neighborId;
                        verticesVisited++;
                    }
                }
            }
            
            // Check if all partition vertices were visited
            boolean isConnected = (verticesVisited == verticesInPart);
            
            if (!isConnected)
                allConnected = false;
        }
    }
}
