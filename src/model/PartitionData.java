package model;

import java.util.ArrayList;
import java.util.List;

public class PartitionData {
    private int partsCount;
    private List<Partition> partitions;

    public PartitionData(int partsCount) {
        this.partsCount = partsCount;
        this.partitions = new ArrayList<>(partsCount);
        for (int i = 0; i < partsCount; i++) {
            partitions.add(new Partition(i, 0, new ArrayList<>()));
        }
    }

    public int getPartsCount() {
        return partsCount;
    }

    public List<Partition> getPartitions() {
        return partitions;
    }

    public void setPartsCount(int partsCount) {
        this.partsCount = partsCount;
    }

    public void setPartitions(List<Partition> partitions) {
        this.partitions = partitions;
    }

    public void addVertexToPartition(int partitionId, int vertexId) {
        if (partitionId < 0 || partitionId >= partsCount) {
            throw new IllegalArgumentException("Invalid partition ID");
        }
        Partition partition = partitions.get(partitionId);
        partition.addNode(vertexId);
    }
}
