package model;

public class Graph {
    private int vertices; // number of vertices
    private int edges; // number of edges
    private int partitions; // number of partitions
    private int minCount; // minimum count of nodes in a partition
    private int maxCount; // maximum count of nodes in a partition
    private Node[] nodes;

    public Graph(int vertices)
    {
        this.vertices = vertices;
        this.edges = 0;
        this.partitions = 0;
        this.minCount = 0;
        this.maxCount = 0;
        this.nodes = new Node[vertices];
        for (int i = 0; i < vertices; i++) {
            nodes[i] = new Node(i);
        }
    }

    public void setEdges() {
        int edges = 0;
        for (int i = 0; i < vertices; i++) {
            edges += nodes[i].getNeighbours().size();
        }
        this.edges = edges / 2; // each edge is counted twice
    }

    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }

    public void setMinCount(int minCount) {
        this.minCount = minCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public int getVertices() {
        return vertices;
    }

    public int getEdges() {
        return edges;
    }

    public int getPartitions() {
        return partitions;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public Node[] getNodes() {
        return nodes;
    }


}