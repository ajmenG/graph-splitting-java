package model;

import java.util.ArrayList;

public class Graph {
    private int vertices; // number of vertices
    private int edges; // number of edges
    private int partitions; // number of partitions
    private int minCount; // minimum count of nodes in a partition
    private int maxCount; // maximum count of nodes in a partition
    private ArrayList<Node> nodes;
    private int[] rowIndices;

    public Graph(int vertices) {
        this.vertices = vertices;
        this.edges = 0;
        this.partitions = 0;
        this.minCount = 0;
        this.maxCount = 0;
        this.nodes = new ArrayList<>(vertices);
        for (int i = 0; i < vertices; i++) {
            this.nodes.add(new Node(i));
        }
    }

    public void setEdges() {
        int edges = 0;
        for (int i = 0; i < vertices; i++) {
            edges += nodes.get(i).getNeighbours().size();
        }
        this.edges = edges / 2; // each edge is counted twice
    }

    public void setEdges(int edges) {
        this.edges = edges;
    }

    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }

    private ParsedData parsedData;

    public ParsedData getParsedData() {
        return parsedData;
    }

    public void setParsedData(ParsedData parsedData) {
        this.parsedData = parsedData;
    }

    public void setMinCount(double accuracy) {
        if (accuracy < 0.0 || accuracy > 1.0 || this.partitions <= 0) {
            throw new IllegalArgumentException(
                    "Accuracy must be between 0.0 and 1.0 and partitions must be greater than 0.");
        } else {
            // Minimalny rozmiar partycji to średni rozmiar * (1.0 - accuracy)
            float avgSize = (float) this.vertices / this.partitions;
            this.minCount = (int) Math.ceil(avgSize * (1.0 - accuracy));
            if (this.minCount < 1) {
                this.minCount = 1; // ensure minCount is at least 1
            }
            System.out.println(
                    "Min count set to: " + this.minCount + " (average: " + avgSize + ", accuracy: " + accuracy + ")");
        }
    }

    public void setMaxCount(double accuracy) {
        if (accuracy < 0.0 || accuracy > 1.0 || this.partitions <= 0) {
            throw new IllegalArgumentException(
                    "Accuracy must be between 0.0 and 1.0 and partitions must be greater than 0.");
        } else {
            // Maksymalny rozmiar partycji to średni rozmiar * (1.0 + accuracy)
            float avgSize = (float) this.vertices / this.partitions;
            this.maxCount = (int) Math.floor(avgSize * (1.0 + accuracy));
            if (this.maxCount < 1) {
                this.maxCount = 1; // ensure maxCount is at least 1
            }
            System.out.println(
                    "Max count set to: " + this.maxCount + " (average: " + avgSize + ", accuracy: " + accuracy + ")");
        }
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

    public ArrayList<Node> getNodes() {
        return nodes;
    }

    public int[] getRowIndices() {
        return rowIndices;
    }

    public void setRowIndices(int[] rowIndices) {
        this.rowIndices = rowIndices;
    }

    public void printPartitionNeighbours() {
        for (Node node : nodes) {
            System.out.print("Node " + node.getId() + " neighbours: ");
            for (Node neighbour : node.getNeighbours()) {
                System.out.print(neighbour.getId() + " ");
            }
            System.out.println();
        }
    }

    public Node getNode(int id) {
        if (id < 0 || id >= vertices) {
            throw new IllegalArgumentException("Node ID out of bounds: " + id);
        }
        return nodes.get(id);
    }

    public void addNode(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }
        if (node.getId() < 0 || node.getId() >= vertices) {
            throw new IllegalArgumentException("Node ID out of bounds: " + node.getId());
        }
        nodes.set(node.getId(), node);
        // Dodajemy sąsiadów używając ID
        for (Node neighbour : node.getNeighbours()) {
            if (neighbour.getId() >= 0 && neighbour.getId() < vertices) {
                nodes.get(neighbour.getId()).addNeighbour(node.getId());
            }
        }
    }
}