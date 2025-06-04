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

    private static int[] generateSeedPoints(Graph graph, int parts) {
        Random random = new Random();
        int[] seedPoints = new int[parts];
        Arrays.fill(seedPoints, -1);

        if (graph.getVertices() == 0 && parts > 0) {
            return seedPoints;
        }
        if (parts == 0) {
            return seedPoints;
        }
        if (graph.getVertices() < parts) {
            parts = graph.getVertices();
            seedPoints = new int[parts];
            for (int i = 0; i < parts; ++i)
                seedPoints[i] = i;
            return seedPoints;
        }

        Set<Integer> chosenSeeds = new HashSet<>();

        for (int i = 0; i < parts; i++) {
            if (chosenSeeds.size() >= graph.getVertices())
                break;

            int bestVertex = -1;
            if (i == 0) {
                bestVertex = random.nextInt(graph.getVertices());
                while (chosenSeeds.contains(bestVertex))
                    bestVertex = random.nextInt(graph.getVertices());
            } else {
                int minConnections = Integer.MAX_VALUE;
                final int maxAttempts = Math.max(100, graph.getVertices() * 2);
                int attempts = 0;

                for (int j = 0; j < maxAttempts; j++) {
                    int candidate = random.nextInt(graph.getVertices());
                    if (chosenSeeds.contains(candidate)) {
                        if (attempts < maxAttempts * 2 && chosenSeeds.size() < graph.getVertices()) {
                            j--;
                            attempts++;
                        }
                        continue;
                    }
                    attempts = 0;

                    int connections = 0;
                    Node candidateNode = graph.getNode(candidate);
                    if (candidateNode == null)
                        continue;

                    for (int k = 0; k < i; k++) {
                        if (seedPoints[k] == -1)
                            continue;
                        Node seedNode = graph.getNode(seedPoints[k]);
                        if (seedNode == null)
                            continue;
                        for (Node neighbor : candidateNode.getNeighbours()) {
                            if (neighbor.getId() == seedPoints[k]) {
                                connections++;
                                break;
                            }
                        }
                        for (Node neighborOfSeed : seedNode.getNeighbours()) {
                            if (neighborOfSeed.getId() == candidate) {
                                connections++;
                                break;
                            }
                        }
                    }

                    if (connections < minConnections) {
                        minConnections = connections;
                        bestVertex = candidate;
                        if (minConnections == 0)
                            break;
                    } else if (connections == minConnections && random.nextBoolean()) {
                        bestVertex = candidate;
                    }
                }
                if (bestVertex == -1 && chosenSeeds.size() < graph.getVertices()) {
                    int fallbackCandidate = random.nextInt(graph.getVertices());
                    while (chosenSeeds.contains(fallbackCandidate))
                        fallbackCandidate = random.nextInt(graph.getVertices());
                    bestVertex = fallbackCandidate;
                }
            }

            if (bestVertex != -1) {
                seedPoints[i] = bestVertex;
                chosenSeeds.add(bestVertex);
            } else {
            }
        }
        return seedPoints;
    }

    public static boolean regionGrowing(Graph graph, int parts, PartitionData partitionData, float accuracy) {
        if (graph == null || graph.getVertices() == 0) {
            if (partitionData != null && parts > 0 && partitionData.getPartsCount() >= parts) {
                for (int i = 0; i < parts; ++i) {
                    if (i < partitionData.getPartitions().size())
                        partitionData.getPartitions().get(i).getPartitionNodes().clear();
                }
            }
            return true;
        }
        if (parts <= 0) {
            return false;
        }

        int numVertices = graph.getVertices();
        if (parts > numVertices) {
            parts = numVertices;
        }

        if (partitionData == null) {
            partitionData = new PartitionData(parts);
        } else if (partitionData.getPartsCount() != parts) {
            partitionData.setPartsCount(parts);
            partitionData.getPartitions().clear();
            for (int i = 0; i < parts; ++i)
                partitionData.getPartitions().add(new model.Partition(i, 0, new ArrayList<>()));
        } else {
            for (model.Partition p : partitionData.getPartitions()) {
                p.getPartitionNodes().clear();
                p.setPartitionVertexCount(0);
            }
        }

        for (Node node : graph.getNodes()) {
            node.setPartId(-1);
        }

        int[] seedPoints = generateSeedPoints(graph, parts);
        boolean[] visited = new boolean[numVertices];
        List<List<Integer>> frontiers = new ArrayList<>(parts);
        for (int i = 0; i < parts; i++) {
            frontiers.add(new ArrayList<>());
        }

        int[] partCounts = new int[parts];

        for (int i = 0; i < parts; i++) {
            if (seedPoints[i] != -1 && seedPoints[i] < numVertices) {
                visited[seedPoints[i]] = true;
                graph.getNode(seedPoints[i]).setPartId(i);
                partitionData.addVertexToPartition(i, seedPoints[i]);
                partCounts[i] = partitionData.getPartitions().get(i).getPartitionVertexCount();

                Node seedNode = graph.getNode(seedPoints[i]);
                for (Node neighbor : seedNode.getNeighbours()) {
                    if (neighbor.getId() < numVertices && !visited[neighbor.getId()]) {
                        frontiers.get(i).add(neighbor.getId());
                    }
                }
            }
        }

        float avgVerticesPerPart = (float) numVertices / Math.max(1, parts);
        int minVerticesPerPart = Math.max(1, (int) Math.floor(avgVerticesPerPart * (1.0f - accuracy)));

        int calculatedMaxVerticesPerPart = (int) Math.ceil(avgVerticesPerPart * (1.0f + accuracy));
        if (calculatedMaxVerticesPerPart < minVerticesPerPart)
            calculatedMaxVerticesPerPart = minVerticesPerPart;
        if (calculatedMaxVerticesPerPart == 0 && numVertices > 0)
            calculatedMaxVerticesPerPart = numVertices;
        final int finalMaxVerticesPerPart = calculatedMaxVerticesPerPart;

        int iterations = 0;
        int unassigned = numVertices - Arrays.stream(partCounts).sum();

        List<Integer> activePartitionIndices = new ArrayList<>();
        for (int i = 0; i < parts; ++i)
            if (seedPoints[i] != -1)
                activePartitionIndices.add(i);

        while (unassigned > 0 && !activePartitionIndices.isEmpty() && iterations < numVertices * parts * 2) {
            int bestPartIdxToGrow = -1;
            int currentMinPartSize = Integer.MAX_VALUE;

            activePartitionIndices
                    .removeIf(pIdx -> frontiers.get(pIdx).isEmpty() || partCounts[pIdx] >= finalMaxVerticesPerPart);
            if (activePartitionIndices.isEmpty())
                break;

            for (int pIdx : activePartitionIndices) {
                if (partCounts[pIdx] < currentMinPartSize) {
                    currentMinPartSize = partCounts[pIdx];
                    bestPartIdxToGrow = pIdx;
                }
            }

            if (bestPartIdxToGrow == -1)
                break;

            List<Integer> currentFrontier = frontiers.get(bestPartIdxToGrow);
            Integer vertexToAssignId = null;

            int originalFrontierSize = currentFrontier.size();
            for (int k = 0; k < originalFrontierSize; ++k) {
                Integer currentId = currentFrontier.get(0);
                if (visited[currentId]) {
                    currentFrontier.remove(0);
                } else {
                    vertexToAssignId = currentFrontier.remove(0);
                    break;
                }
            }

            if (vertexToAssignId != null) {
                visited[vertexToAssignId] = true;
                graph.getNode(vertexToAssignId).setPartId(bestPartIdxToGrow);
                partitionData.addVertexToPartition(bestPartIdxToGrow, vertexToAssignId);
                partCounts[bestPartIdxToGrow]++;
                unassigned--;

                Node assignedNode = graph.getNode(vertexToAssignId);
                for (Node neighbor : assignedNode.getNeighbours()) {
                    if (neighbor.getId() < numVertices && !visited[neighbor.getId()]) {
                        frontiers.get(bestPartIdxToGrow).add(neighbor.getId());
                    }
                }
            }
            iterations++;
        }

        boolean allPartitionsMeetMinSize = true;
        for (int i = 0; i < parts; i++) {
            if (partCounts[i] < minVerticesPerPart && numVertices > 0 && seedPoints[i] != -1) {
                allPartitionsMeetMinSize = false;
            }
        }
        if (unassigned > 0 && numVertices > 0) {
            for (int i = 0; i < numVertices; ++i) {
                if (graph.getNode(i).getPartId() == -1) {
                    int smallestPart = -1;
                    int smallestSize = Integer.MAX_VALUE;
                    for (int p = 0; p < parts; ++p) {
                        if (seedPoints[p] != -1 && partCounts[p] < finalMaxVerticesPerPart
                                && partCounts[p] < smallestSize) {
                            smallestSize = partCounts[p];
                            smallestPart = p;
                        }
                    }
                    if (smallestPart != -1) {
                        graph.getNode(i).setPartId(smallestPart);
                        partitionData.addVertexToPartition(smallestPart, i);
                        partCounts[smallestPart]++;
                        unassigned--;
                    }
                }
            }
        }

        for (int i = 0; i < parts; ++i) {
            if (seedPoints[i] != -1 && !isPartitionConnected(graph, i, partitionData)) {
            }
        }
        return allPartitionsMeetMinSize && unassigned == 0;
    }

    private static boolean isPartitionConnected(Graph graph, int partId, PartitionData pd) {
        if (graph == null || pd == null || partId < 0 || partId >= pd.getPartsCount())
            return true;

        List<Integer> nodeIdsInPartition = pd.getPartitions().get(partId).getPartitionNodes();
        if (nodeIdsInPartition.isEmpty() || nodeIdsInPartition.size() == 1)
            return true;

        Set<Integer> visitedNodes = new HashSet<>();
        java.util.Queue<Integer> q = new java.util.ArrayDeque<>();

        q.offer(nodeIdsInPartition.get(0));
        visitedNodes.add(nodeIdsInPartition.get(0));

        while (!q.isEmpty()) {
            int currentId = q.poll();
            Node currentNode = graph.getNode(currentId);
            if (currentNode == null)
                continue;

            for (Node neighborStub : currentNode.getNeighbours()) {
                Node actualNeighbor = graph.getNode(neighborStub.getId());
                if (actualNeighbor == null)
                    continue;

                if (actualNeighbor.getPartId() == partId && nodeIdsInPartition.contains(actualNeighbor.getId())
                        && !visitedNodes.contains(actualNeighbor.getId())) {
                    visitedNodes.add(actualNeighbor.getId());
                    q.offer(actualNeighbor.getId());
                }
            }
        }
        return visitedNodes.size() == nodeIdsInPartition.size();
    }
}
