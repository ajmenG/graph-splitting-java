package model;

import java.util.List;

public class PartitionData {
    private int partsCount;
    private List<Partition> partitions;

    public PartitionData(int partsCount, List<Partition> partitions) {
        this.partsCount = partsCount;
        this.partitions = partitions;
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
}
