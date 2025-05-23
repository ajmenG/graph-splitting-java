package model;

import java.util.List;

public class Partition {
    private int id;
    private int partitionVertexCount; // number of vertices in the partition
    private List<Integer> partitionNodes; // list of nodes in the partition
    private int partitionCapacity;

    public Partition(int id, int partitionVertexCount, List<Integer> partitionNodes, int partitionCapacity) {
        this.id = id;
        this.partitionVertexCount = partitionVertexCount;
        this.partitionNodes = partitionNodes;
        this.partitionCapacity = partitionCapacity;
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

    public int getPartitionCapacity() {
        return partitionCapacity;
    }

    public void setPartitionVertexCount(int partitionVertexCount) {
        this.partitionVertexCount = partitionVertexCount;
    }

    public void setPartitionNodes(List<Integer> partitionNodes) {
        this.partitionNodes = partitionNodes;
    }

    public void setPartitionCapacity(int partitionCapacity) {
        this.partitionCapacity = partitionCapacity;
    }

}
