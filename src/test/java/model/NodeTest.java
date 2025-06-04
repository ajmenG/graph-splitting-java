package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NodeTest {

    @Test
    void testNodeCreation() {
        Node node = new Node(5);
        assertEquals(5, node.getId());
        assertEquals(-1, node.getPartId());
        assertTrue(node.getNeighbours().isEmpty());
        assertEquals(0, node.getNeighbourCount());
    }

    @Test
    void testSetPartId() {
        Node node = new Node(1);
        node.setPartId(3);
        assertEquals(3, node.getPartId());
    }

    @Test
    void testAddNeighbour() {
        Node node0 = new Node(0);
        Node node1 = new Node(1);

        node0.addNeighbour(1);
        assertEquals(1, node0.getNeighbourCount());
        assertTrue(node0.getNeighbours().stream().anyMatch(n -> n.getId() == 1));

        node0.addNeighbour(1);
        assertEquals(1, node0.getNeighbourCount(), "Adding the same neighbour should not increase count");
    }

    @Test
    void testEqualsAndHashCode() {
        Node nodeA1 = new Node(10);
        Node nodeA2 = new Node(10);
        Node nodeB = new Node(20);

        assertEquals(nodeA1, nodeA2, "Nodes with the same ID should be equal");
        assertNotEquals(nodeA1, nodeB, "Nodes with different IDs should not be equal");
        assertEquals(nodeA1.hashCode(), nodeA2.hashCode(), "Hashcodes for equal nodes should be the same");
        assertNotEquals(nodeA1.hashCode(), nodeB.hashCode(), "Hashcodes for unequal nodes should ideally differ");
        assertNotEquals(nodeA1, null, "Node should not be equal to null");
        assertNotEquals(nodeA1, new Object(), "Node should not be equal to an object of a different type");
    }

    @Test
    void testToString() {
        Node node = new Node(7);
        assertEquals("7", node.toString());
    }
}