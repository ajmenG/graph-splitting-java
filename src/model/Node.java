package model;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private int id;
    private List<Node> neighbours;
    private int partId;
    private int neighbourCount;

    public Node(int id) {
        this.id = id;
        this.neighbours = new ArrayList<Node>();
        this.partId = -1; // default -1 when not assigned
        this.neighbourCount = 0;
    }

    public void addNeighbour(int neighbourId) {
        // Tworzymy tymczasowy obiekt tylko do sprawdzenia, czy już istnieje w liście
        Node tempNode = new Node(neighbourId);

        if (this.neighbours.contains(tempNode)) {
            return; // already exists
        }
        this.neighbours.add(tempNode);
        setNeighbourCount();
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

    public void setNeighbourCount() {
        this.neighbourCount = getNeighbourCount();
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
