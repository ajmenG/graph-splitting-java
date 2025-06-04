package io;

import model.Graph;
import model.Node;
import model.ParsedData;
import model.Partition;
import model.PartitionData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FileWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void testWriteTextSimpleGraph() throws IOException {
        Path outputPath = tempDir.resolve("test_simple_out.csrrg");

        ParsedData parsedData = new ParsedData();
        parsedData.setLine1(3);
        parsedData.setLine2(Arrays.asList(0, 1, 2));
        parsedData.setLine3(Arrays.asList(0, 1, 2, 3));

        Graph graph = new Graph(3);
        graph.setParsedData(parsedData);
        graph.getNode(0).addNeighbour(1);
        graph.getNode(1).addNeighbour(0);
        graph.getNode(1).addNeighbour(2);
        graph.getNode(2).addNeighbour(1);

        PartitionData partitionData = new PartitionData(1);
        Partition part0 = new Partition(0, 0, new ArrayList<>(Arrays.asList(0, 1, 2)));
        partitionData.getPartitions().set(0, part0);

        graph.setPartitions(1);
        for (Node n : graph.getNodes())
            n.setPartId(0);

        FileWriter.writeText(outputPath.toString(), parsedData, partitionData, graph, 1);

        assertTrue(Files.exists(outputPath));
        List<String> lines = Files.readAllLines(outputPath);

        assertEquals(parsedData.getLine1() + "", lines.get(0));
        assertEquals("0;1;2", lines.get(1));
        assertEquals("0;1;2;3", lines.get(2));

        assertEquals("0;1;1;0;2;2;1", lines.get(3));

        assertEquals("0;2;5;7", lines.get(4));
        assertEquals(5, lines.size());
    }

    @Test
    void testWriteTextPartitionedGraph() throws IOException {
        Path outputPath = tempDir.resolve("test_partitioned_out.csrrg");

        ParsedData parsedData = new ParsedData();
        parsedData.setLine1(4);
        parsedData.setLine2(Arrays.asList(0, 1, 2, 3));
        parsedData.setLine3(Arrays.asList(0, 1, 2, 3, 4));

        Graph graph = new Graph(4);
        graph.setParsedData(parsedData);
        graph.setPartitions(2);

        graph.getNode(0).addNeighbour(1);
        graph.getNode(0).setPartId(0);
        graph.getNode(1).addNeighbour(0);
        graph.getNode(1).setPartId(0);
        graph.getNode(2).addNeighbour(3);
        graph.getNode(2).setPartId(1);
        graph.getNode(3).addNeighbour(2);
        graph.getNode(3).setPartId(1);

        PartitionData partitionData = new PartitionData(2);
        partitionData.getPartitions().set(0, new Partition(0, 0, Arrays.asList(0, 1)));
        partitionData.getPartitions().set(1, new Partition(1, 0, Arrays.asList(2, 3)));

        FileWriter.writeText(outputPath.toString(), parsedData, partitionData, graph, 2);

        assertTrue(Files.exists(outputPath));
        List<String> lines = Files.readAllLines(outputPath);

        assertEquals(parsedData.getLine1() + "", lines.get(0));
        assertEquals("0;1;2;3", lines.get(1));
        assertEquals("0;1;2;3;4", lines.get(2));

        assertEquals("0;1;1;0;2;3;3;2", lines.get(3));

        assertEquals("0;2;4", lines.get(4));

        assertEquals("4;6;8", lines.get(5));
        assertEquals(6, lines.size());
    }

    @Test
    void testEncodeVByte() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileWriter.encodeVByte(baos, 0);
        FileWriter.encodeVByte(baos, 127);
        FileWriter.encodeVByte(baos, 128);
        FileWriter.encodeVByte(baos, 16383);
        FileWriter.encodeVByte(baos, 16384);

        byte[] encodedBytes = baos.toByteArray();
        assertArrayEquals(new byte[] { 0x00 }, Arrays.copyOfRange(encodedBytes, 0, 1));
        assertArrayEquals(new byte[] { 0x7F }, Arrays.copyOfRange(encodedBytes, 1, 2));
        assertArrayEquals(new byte[] { (byte) 0x80, 0x01 }, Arrays.copyOfRange(encodedBytes, 2, 4));
        assertArrayEquals(new byte[] { (byte) 0xFF, 0x7F }, Arrays.copyOfRange(encodedBytes, 4, 6));
        assertArrayEquals(new byte[] { (byte) 0x80, (byte) 0x80, 0x01 }, Arrays.copyOfRange(encodedBytes, 6, 9));
    }
}