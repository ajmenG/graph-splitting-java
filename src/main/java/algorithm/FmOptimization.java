package algorithm;

import model.Graph;
import model.Node;
import model.PartitionData;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class FmOptimization {
    /**
     * Context class for the FM algorithm, holding all necessary data
     */
    private static class FmContext {
        private Graph graph;
        private PartitionData partitionData;
        private int maxIterations;
        private int initialCut;
        private int currentCut;
        private boolean[] locked;
        private int[] gains;
        private int[] partSizes;

        public FmContext(Graph graph, PartitionData partitionData, int maxIterations) {
            this.graph = graph;
            this.partitionData = partitionData;
            this.maxIterations = maxIterations;
            this.initialCut = 0;
            this.currentCut = 0;
            int numVertices = graph.getVertices();
            this.locked = new boolean[numVertices];
            this.gains = new int[numVertices * graph.getPartitions()];
            this.partSizes = new int[graph.getPartitions()];

            if (partitionData != null && partitionData.getPartitions() != null
                    && partitionData.getPartsCount() == graph.getPartitions()) {
                for (int i = 0; i < graph.getPartitions(); i++) {
                    if (i < partitionData.getPartitions().size() && partitionData.getPartitions().get(i) != null) {
                        this.partSizes[i] = partitionData.getPartitions().get(i).getPartitionNodes().size();
                    } else {
                        this.partSizes[i] = 0;
                    }
                }
            } else {
                Arrays.fill(this.partSizes, 0);
                if (graph.getNodes() != null) {
                    for (Node node : graph.getNodes()) {
                        int partId = node.getPartId();
                        if (partId >= 0 && partId < graph.getPartitions()) {
                            this.partSizes[partId]++;
                        }
                    }
                }
            }
        }
    }

    public static void cutEdgesOptimization(Graph graph, PartitionData partitionData, int maxIterations) {
        if (graph == null || graph.getVertices() == 0 || graph.getPartitions() <= 1 || partitionData == null) {
            return;
        }
        if (maxIterations <= 0) {
            maxIterations = Math.max(1, graph.getVertices() / 10);
        }

        FmContext context = new FmContext(graph, partitionData, maxIterations);
        context.initialCut = countCutEdges(graph);
        context.currentCut = context.initialCut;

        if (context.initialCut == 0) {
            return;
        }

        int[] overallBestPartitionConfig = new int[graph.getVertices()];
        if (graph.getNodes() != null) {
            for (int i = 0; i < graph.getVertices(); ++i)
                overallBestPartitionConfig[i] = graph.getNode(i).getPartId();
        }
        int overallBestCut = context.initialCut;

        for (int iter = 0; iter < context.maxIterations; iter++) {
            Arrays.fill(context.locked, false);

            int[] currentPassBestConfig = new int[graph.getVertices()];
            for (int i = 0; i < graph.getVertices(); ++i)
                currentPassBestConfig[i] = graph.getNode(i).getPartId();
            int currentPassBestCut = context.currentCut;

            for (int moveAttempt = 0; moveAttempt < graph.getVertices(); moveAttempt++) {
                int bestVertexToMove = -1;
                int maxGain = Integer.MIN_VALUE;
                int targetPartitionForBestMove = -1;

                calculateAllVertexGains(context);

                for (int vId = 0; vId < graph.getVertices(); vId++) {
                    if (!context.locked[vId]) {
                        Node node = graph.getNode(vId);
                        int originalPartId = node.getPartId();
                        for (int pTargetId = 0; pTargetId < graph.getPartitions(); pTargetId++) {
                            if (pTargetId == originalPartId)
                                continue;
                            if (!isMoveBalanced(context, vId, pTargetId, graph.getMinCount(), graph.getMaxCount()))
                                continue;

                            int currentGain = context.gains[vId * graph.getPartitions() + pTargetId];
                            if (currentGain > maxGain) {
                                maxGain = currentGain;
                                bestVertexToMove = vId;
                                targetPartitionForBestMove = pTargetId;
                            }
                        }
                    }
                }

                if (bestVertexToMove != -1) {
                    applyMove(context, bestVertexToMove, targetPartitionForBestMove);
                    context.locked[bestVertexToMove] = true;
                    context.currentCut -= maxGain;

                    if (context.currentCut < currentPassBestCut) {
                        currentPassBestCut = context.currentCut;
                        for (int i = 0; i < graph.getVertices(); ++i)
                            currentPassBestConfig[i] = graph.getNode(i).getPartId();
                    }
                } else {
                    break;
                }
            }

            if (currentPassBestCut < overallBestCut) {
                overallBestCut = currentPassBestCut;
                overallBestPartitionConfig = Arrays.copyOf(currentPassBestConfig, currentPassBestConfig.length);
            } else {
                for (int i = 0; i < graph.getVertices(); ++i) {
                    graph.getNode(i).setPartId(overallBestPartitionConfig[i]);
                }
                syncContextToConfig(context, overallBestPartitionConfig);
                context.currentCut = overallBestCut;
                break;
            }

            for (int i = 0; i < graph.getVertices(); ++i) {
                graph.getNode(i).setPartId(currentPassBestConfig[i]);
            }
            syncContextToConfig(context, currentPassBestConfig);
            context.currentCut = currentPassBestCut;

            if (overallBestCut == 0)
                break;
        }

        if (context.currentCut > overallBestCut || !Arrays.equals(getNodePartIds(graph), overallBestPartitionConfig)) {
            for (int i = 0; i < graph.getVertices(); ++i) {
                graph.getNode(i).setPartId(overallBestPartitionConfig[i]);
            }
        }
        syncContextToConfig(context, overallBestPartitionConfig);
    }

    private static int[] getNodePartIds(Graph graph) {
        int[] ids = new int[graph.getVertices()];
        for (int i = 0; i < graph.getVertices(); ++i)
            ids[i] = graph.getNode(i).getPartId();
        return ids;
    }

    private static void syncContextToConfig(FmContext context, int[] config) {
        Arrays.fill(context.partSizes, 0);
        for (int i = 0; i < config.length; ++i) {
            if (config[i] >= 0 && config[i] < context.partSizes.length) {
                context.partSizes[config[i]]++;
            }
        }
        if (context.partitionData != null && context.partitionData.getPartitions() != null) {
            for (model.Partition p : context.partitionData.getPartitions()) {
                if (p != null) {
                    p.getPartitionNodes().clear();
                    p.setPartitionVertexCount(0);
                }
            }
            for (int i = 0; i < config.length; ++i) {
                if (config[i] >= 0 && config[i] < context.partitionData.getPartsCount()) {
                    if (context.partitionData.getPartitions().get(config[i]) != null) {
                        context.partitionData.addVertexToPartition(config[i], i);
                    }
                }
            }
        }
    }

    private static void calculateAllVertexGains(FmContext context) {
        int numPartitions = context.graph.getPartitions();
        if (context.gains.length != context.graph.getVertices() * numPartitions) {
            context.gains = new int[context.graph.getVertices() * numPartitions];
        }

        for (int vId = 0; vId < context.graph.getVertices(); vId++) {
            if (context.locked[vId])
                continue;
            Node node = context.graph.getNode(vId);
            if (node == null)
                continue;
            int currentPartId = node.getPartId();
            for (int targetPartId = 0; targetPartId < numPartitions; targetPartId++) {
                if (targetPartId == currentPartId) {
                    context.gains[vId * numPartitions + targetPartId] = Integer.MIN_VALUE;
                    continue;
                }
                context.gains[vId * numPartitions + targetPartId] = calculateGainForSingleMove(context.graph, vId,
                        targetPartId);
            }
        }
    }

    private static int calculateGainForSingleMove(Graph graph, int vertexId, int targetPartId) {
        Node node = graph.getNode(vertexId);
        if (node == null)
            return Integer.MIN_VALUE;
        int currentPartId = node.getPartId();
        int gain = 0;
        for (Node neighborNode : node.getNeighbours()) {
            Node actualNeighbor = graph.getNode(neighborNode.getId());
            if (actualNeighbor == null)
                continue;
            int neighborPartId = actualNeighbor.getPartId();
            if (neighborPartId == currentPartId)
                gain++;
            if (neighborPartId == targetPartId)
                gain--;
        }
        return gain;
    }

    private static boolean isMoveBalanced(FmContext context, int vertexId, int targetPartId, int minCount,
            int maxCount) {
        Node node = context.graph.getNode(vertexId);
        if (node == null)
            return false;
        int currentPartId = node.getPartId();
        if (context.partSizes[currentPartId] - 1 < minCount && context.partSizes[currentPartId] > 0 && minCount > 0)
            return false;
        if (context.partSizes[targetPartId] + 1 > maxCount && maxCount > 0)
            return false;
        return true;
    }

    private static void applyMove(FmContext context, int vertexId, int newPartId) {
        Node node = context.graph.getNode(vertexId);
        if (node == null)
            return;
        int oldPartId = node.getPartId();
        node.setPartId(newPartId);

        if (context.partitionData != null && context.partitionData.getPartitions() != null) {
            if (oldPartId >= 0 && oldPartId < context.partitionData.getPartsCount() &&
                    context.partitionData.getPartitions().get(oldPartId) != null) {
                context.partitionData.getPartitions().get(oldPartId).getPartitionNodes()
                        .remove(Integer.valueOf(vertexId));
            }
            if (newPartId >= 0 && newPartId < context.partitionData.getPartsCount() &&
                    context.partitionData.getPartitions().get(newPartId) != null) {
                context.partitionData.getPartitions().get(newPartId).getPartitionNodes().add(vertexId);
            }
        }

        context.partSizes[oldPartId]--;
        context.partSizes[newPartId]++;
    }

    private static int countCutEdges(Graph graph) {
        int cutEdges = 0;
        if (graph.getNodes() == null)
            return 0;
        Set<String> countedEdges = new HashSet<>();
        for (Node node : graph.getNodes()) {
            if (node == null)
                continue;
            for (Node neighbor : node.getNeighbours()) {
                Node actualNeighbor = graph.getNode(neighbor.getId());
                if (actualNeighbor != null && node.getPartId() != actualNeighbor.getPartId()) {
                    String edge1 = Math.min(node.getId(), actualNeighbor.getId()) + "-"
                            + Math.max(node.getId(), actualNeighbor.getId());
                    if (!countedEdges.contains(edge1)) {
                        cutEdges++;
                        countedEdges.add(edge1);
                    }
                }
            }
        }
        return cutEdges;
    }
}
