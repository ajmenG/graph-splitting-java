package io;

import model.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

public class FileWriter {

    public static boolean isInPartition(PartitionData partitionData, int partId, int vertex) {
        Partition partition = partitionData.getPartitions().get(partId);
        return partition.getPartitionNodes().contains(vertex);
    }

    public static List<Integer> getPartitionNeighbors(Graph graph, PartitionData partitionData, int partId, int vertex) {
        List<Integer> neighbors = new ArrayList<>();
        Node node = graph.getNode(vertex);
        for (Node neighbor : node.getNeighbours()) {
            if (isInPartition(partitionData, partId, neighbor.getId())) {
                neighbors.add(neighbor.getId());
            }
        }
        return neighbors;
    }

    public static void writeText(String filename, ParsedData data, PartitionData partitionData, Graph graph, int parts) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"))) {
            writer.write(data.getLine1() + "\n");

            List<Integer> line2 = data.getLine2();
            for (int i = 0; i < line2.size(); i++) {
                writer.write(line2.get(i).toString());
                if (i < line2.size() - 1) writer.write(";");
            }
            writer.write("\n");

            List<Integer> line3 = data.getLine3();
            for (int i = 0; i < line3.size(); i++) {
                writer.write(line3.get(i).toString());
                if (i < line3.size() - 1) writer.write(";");
            }
            writer.write("\n");

            // Budujemy posortowaną listę wierzchołków dla każdej części
            List<List<Integer>> sortedVerticesPerPart = new ArrayList<>();
            for (int part = 0; part < parts; part++) {
                Partition partition = partitionData.getPartitions().get(part);
                List<Integer> sortedVertices = new ArrayList<>(partition.getPartitionNodes());
                Collections.sort(sortedVertices);
                sortedVerticesPerPart.add(sortedVertices);
            }

            // 4 linia: wypisujemy posortowane wierzchołki i ich sąsiadów
            for (int part = 0; part < parts; part++) {
                List<Integer> sortedVertices = sortedVerticesPerPart.get(part);
                for (int vertex : sortedVertices) {
                    writer.write(String.valueOf(vertex));
                    List<Integer> neighbors = getPartitionNeighbors(graph, partitionData, part, vertex);
                    Collections.sort(neighbors);
                    for (int j = 0; j < neighbors.size(); j++) {
                        writer.write(";");
                        writer.write(String.valueOf(neighbors.get(j)));
                    }
                    boolean isLast = (part == parts - 1) && (vertex == sortedVertices.get(sortedVertices.size() - 1));
                    if (!isLast) writer.write(";");
                }
            }
            writer.write("\n");

            // Offsety - linie zależne od liczby parts
            int lastPos = 0;
            for (int part = 0; part < parts; part++) {
                List<Integer> sortedVertices = sortedVerticesPerPart.get(part);
                if (part == 0) {
                    writer.write("0");
                } else {
                    writer.write(String.valueOf(lastPos));
                }
                int pos = lastPos;
                for (int vertex : sortedVertices) {
                    List<Integer> neighbors = getPartitionNeighbors(graph, partitionData, part, vertex);
                    int neighborCount = neighbors.size();
                    pos += (neighborCount + 1);
                    writer.write(";" + pos);
                }
                lastPos = pos;
                writer.write("\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void encodeVByte(OutputStream out, int value) throws IOException {
        while (value >= 128) {
            out.write((value & 0x7F) | 0x80);
            value >>= 7;
        }
        out.write(value & 0x7F);
    }

    public static void writeBinary(String filename, ParsedData data, PartitionData partitionData, Graph graph, int parts) {
        try (FileOutputStream out = new FileOutputStream(filename)) {
            long separator = 0xDEADBEEFCAFEBABEL;

            encodeVByte(out, data.getLine1());
            out.write(ByteBuffer.allocate(8).putLong(separator).array());

            for (int val : data.getLine2()) encodeVByte(out, val);
            out.write(ByteBuffer.allocate(8).putLong(separator).array());

            for (int val : data.getLine3()) encodeVByte(out, val);
            out.write(ByteBuffer.allocate(8).putLong(separator).array());

            // Budujemy posortowaną listę wierzchołków dla każdej części
            List<List<Integer>> sortedVerticesPerPart = new ArrayList<>();
            for (int part = 0; part < parts; part++) {
                Partition partition = partitionData.getPartitions().get(part);
                List<Integer> sortedVertices = new ArrayList<>(partition.getPartitionNodes());
                Collections.sort(sortedVertices);
                sortedVerticesPerPart.add(sortedVertices);
            }

            // Wypisujemy posortowane wierzchołki i ich sąsiadów w formacie binarnym
            for (int part = 0; part < parts; part++) {
                List<Integer> sortedVertices = sortedVerticesPerPart.get(part);
                for (int vertex : sortedVertices) {
                    encodeVByte(out, vertex);
                    List<Integer> neighbors = getPartitionNeighbors(graph, partitionData, part, vertex);
                    Collections.sort(neighbors);
                    for (int neighbor : neighbors) {
                        encodeVByte(out, neighbor);
                    }
                }
            }
            out.write(ByteBuffer.allocate(8).putLong(separator).array());

            // Offsety binarne
            encodeVByte(out, 0);
            int lastPos = 0;
            for (int vertex : sortedVerticesPerPart.get(0)) {
                List<Integer> neighbors = getPartitionNeighbors(graph, partitionData, 0, vertex);
                int neighborCount = neighbors.size();
                lastPos += (neighborCount + 1);
                encodeVByte(out, lastPos);
            }
            out.write(ByteBuffer.allocate(8).putLong(separator).array());

            for (int part = 1; part < parts; part++) {
                encodeVByte(out, lastPos);
                int pos = lastPos;
                for (int vertex : sortedVerticesPerPart.get(part)) {
                    List<Integer> neighbors = getPartitionNeighbors(graph, partitionData, part, vertex);
                    int neighborCount = neighbors.size();
                    pos += (neighborCount + 1);
                    encodeVByte(out, pos);
                }
                lastPos = pos;
                if (part < parts - 1) out.write(ByteBuffer.allocate(8).putLong(separator).array());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
