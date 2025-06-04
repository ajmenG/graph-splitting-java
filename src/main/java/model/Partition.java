package model;

import java.util.List;

public class Partition {
    private int id;
    private int partitionVertexCount; // number of vertices in the partition
    private List<Integer> partitionNodes; // list of nodes in the partition

    public Partition(int id, int partitionVertexCount, List<Integer> partitionNodes) {
        this.id = id;
        this.partitionVertexCount = partitionVertexCount;
        this.partitionNodes = partitionNodes;
    }

    public void addNode(int nodeId) {
        if (!partitionNodes.contains(nodeId)) {
            partitionNodes.add(nodeId);
            partitionVertexCount++;
        }
    }

    public int getId() {
        return id;
    }

    public int getPartitionVertexCount() {
        return partitionVertexCount;
    }

    public List<Integer> getPartitionNodes() {
        return partitionNodes;
    }

    public void setPartitionVertexCount(int partitionVertexCount) {
        this.partitionVertexCount = partitionVertexCount;
    }

    public void setPartitionNodes(List<Integer> partitionNodes) {
        this.partitionNodes = partitionNodes;
    }

}
