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
        this.partId = -1; // default -1 when not assigned
    }

    public void addNeighbour(int neighbourId) {
        Node neighbour = new Node(neighbourId);

        if (this.neighbours.contains(neighbour)) {
            return; // already exists
        }
        this.neighbours.add(neighbour);
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
        return "Node{" +
                "id=" + id +
                ", neighbours=" + neighbours +
                ", partId=" + partId +
                '}';
    }

    public int getNeighbourCount() {
        return neighbours.size();
    }





}
