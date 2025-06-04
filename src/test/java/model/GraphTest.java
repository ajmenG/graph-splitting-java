package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

class GraphTest {

    @Test
    void testGraphCreation() {
        Graph graph = new Graph(5);
        assertEquals(5, graph.getVertices());
        assertEquals(0, graph.getEdges());
        assertEquals(0, graph.getPartitions());
        assertNotNull(graph.getNodes());
        assertEquals(5, graph.getNodes().size());
        for (int i = 0; i < 5; i++) {
            assertNotNull(graph.getNode(i));
            assertEquals(i, graph.getNode(i).getId());
        }
    }

    @Test
    void testAddNodeAndGetNode() {
        Graph graph = new Graph(3);
        Node node0 = new Node(0);
        Node node1 = new Node(1);
        Node node2 = new Node(2);

        assertEquals(node0, graph.getNode(0));
        assertEquals(node1, graph.getNode(1));
        assertEquals(node2, graph.getNode(2));

        assertThrows(IllegalArgumentException.class, () -> graph.getNode(3));
        assertThrows(IllegalArgumentException.class, () -> graph.getNode(-1));
    }

    @Test
    void testAddEdgesAndCount() {
        Graph graph = new Graph(4);
        graph.getNode(0).addNeighbour(1);
        graph.getNode(1).addNeighbour(0);
        graph.setEdges();
        assertEquals(1, graph.getEdges());

        graph.getNode(1).addNeighbour(2);
        graph.getNode(2).addNeighbour(1);
        graph.setEdges();
        assertEquals(2, graph.getEdges());

        graph.getNode(0).addNeighbour(1);
        graph.getNode(1).addNeighbour(0);
        graph.setEdges();
        assertEquals(2, graph.getEdges(), "Adding existing edge should not change edge count.");

        graph.setEdges(5);
        assertEquals(5, graph.getEdges());
    }

    @Test
    void testPartitions() {
        Graph graph = new Graph(5);
        graph.setPartitions(3);
        assertEquals(3, graph.getPartitions());
    }

    @Test
    void testMinMaxCountValid() {
        Graph graph = new Graph(10);
        graph.setPartitions(2);
        graph.setMinCount(0.2);
        graph.setMaxCount(0.2);
        assertEquals(4, graph.getMinCount());
        assertEquals(6, graph.getMaxCount());

        Graph graph2 = new Graph(10);
        graph2.setPartitions(3);
        graph2.setMinCount(0.1);
        graph2.setMaxCount(0.1);
        assertEquals(3, graph2.getMinCount());
        assertEquals(3, graph2.getMaxCount());
    }

    @Test
    void testMinMaxCountInvalidAccuracy() {
        Graph graph = new Graph(10);
        graph.setPartitions(2);
        assertThrows(IllegalArgumentException.class, () -> graph.setMinCount(-0.1));
        assertThrows(IllegalArgumentException.class, () -> graph.setMaxCount(1.1));
    }

    @Test
    void testMinMaxCountNoPartitions() {
        Graph graph = new Graph(10);
        assertThrows(IllegalArgumentException.class, () -> graph.setMinCount(0.1), "Should throw if partitions is 0");
        assertThrows(IllegalArgumentException.class, () -> graph.setMaxCount(0.1), "Should throw if partitions is 0");
    }

    @Test
    void testParsedDataHandling() {
        Graph graph = new Graph(2);
        ParsedData pd = new ParsedData();
        pd.setLine1(2);
        graph.setParsedData(pd);
        assertSame(pd, graph.getParsedData());
    }
}