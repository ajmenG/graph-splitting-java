package model;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private int id;
    private List<Node> neighbours;
    private int partId;

    public Node(int id) {
        this.id = id;
        this.neighbours = new ArrayList<Node>();
        this.partId = -1;
    }

    public boolean addNeighbour(int neighbourId) {
        Node tempNode = new Node(neighbourId);

        if (this.neighbours.contains(tempNode)) {
            return false;
        }
        this.neighbours.add(tempNode);
        return true;
    }

    public int getId() {
        return id;
    }

    public List<Node> getNeighbours() {
        return neighbours;
    }

    public int getPartId() {
        return partId;
    }

    public void setPartId(int partId) {
        this.partId = partId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Node other = (Node) obj;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

    public int getNeighbourCount() {
        return this.neighbours.size();
    }

}
