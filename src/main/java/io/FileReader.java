package io;

import model.Graph;
import model.Node;
import model.ParsedData;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FileReader {
    private static final int MAX_BUFFER = Integer.MAX_VALUE;

    public FileReader() {
    }

    // czyta liczbę zakodowaną w formacie vbyte z pliku binarnego
    private static int decodeVbyte(InputStream in) throws IOException {
        int value = 0;
        int shift = 0;
        int b;

        while ((b = in.read()) != -1) {
            value |= (b & 0x7F) << shift; // Dodaj 7 bitów do wartości
            if ((b & 0x80) == 0) { // Jeśli MSB = 0, to koniec liczby
                break;
            }
            shift += 7; // Przesuń o 7 bitów
        }

        return value;
    }

    // czyta i wyświetla zawartość pliku binarnego
    public static void readBinary(String filename) throws IOException {
        try (FileInputStream file = new FileInputStream(filename);
                DataInputStream dataIn = new DataInputStream(file)) {

            final long separator = 0xDEADBEEFCAFEBABEL;
            long readSeparator;

            int numVertices = decodeVbyte(file);
            System.out.println(numVertices + "\n");

            int val;
            // Pierwsza sekcja
            while (true) {
                val = decodeVbyte(file);
                System.out.print(val);

                byte[] peekBytes = new byte[8];
                int bytesRead = file.read(peekBytes);
                if (bytesRead == 8) {
                    long peek = bytesToLong(peekBytes);
                    if (peek == separator) {
                        break;
                    }
                    file.skip(-8); // Cofnij wskaźnik pliku
                }
                System.out.print(";");
            }
            System.out.println("\n");

            // Druga sekcja
            while (true) {
                val = decodeVbyte(file);
                System.out.print(val);

                byte[] peekBytes = new byte[8];
                int bytesRead = file.read(peekBytes);
                if (bytesRead == 8) {
                    long peek = bytesToLong(peekBytes);
                    if (peek == separator) {
                        break;
                    }
                    file.skip(-8); // Cofnij wskaźnik pliku
                }
                System.out.print(";");
            }
            System.out.println("\n");

            // Trzecia sekcja
            while (true) {
                val = decodeVbyte(file);
                System.out.print(val);

                byte[] peekBytes = new byte[8];
                int bytesRead = file.read(peekBytes);
                if (bytesRead == 8) {
                    long peek = bytesToLong(peekBytes);
                    if (peek == separator) {
                        break;
                    }
                    file.skip(-8); // Cofnij wskaźnik pliku
                    System.out.print(";");
                }
            }
            System.out.println("\n");

            // Ostatnia sekcja
            while (true) {
                if (file.available() == 0) {
                    break;
                }

                val = decodeVbyte(file);
                if (file.available() == 0) {
                    System.out.print(val);
                    break;
                }
                System.out.print(val);

                byte[] peekBytes = new byte[8];
                int bytesRead = file.read(peekBytes);

                if (bytesRead == 8) {
                    long peek = bytesToLong(peekBytes);
                    if (peek == separator) {
                        System.out.println("\n");
                        continue;
                    }
                    file.skip(-8); // Cofnij wskaźnik pliku
                    System.out.print(";");
                    continue;
                }

                if (bytesRead > 0) {
                    file.skip(-bytesRead);
                    System.out.print(";");
                }
            }
            System.out.println();
        }
    }

    // Konwersja tablicy bajtów na long
    private static long bytesToLong(byte[] bytes) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (bytes[i] & 0xFF);
        }
        return result;
    }

    // wczytuje graf z pliku tekstowego
    public Graph loadGraph(ParsedData data) {
        int numVertices = 0;
        if (data.getLine1() > 0) {
            numVertices = data.getLine1();
        } else if (data.getLine2() != null && !data.getLine2().isEmpty()) {
            numVertices = data.getLine2().size();
            System.err.println("Info: numVertices from line1 was not positive. Using line2.size() = " + numVertices);
        } else if (data.getNumberOfPartitions() <= 1 && data.getRowPointers() != null
                && data.getRowPointers().size() > 0) {
            if (data.getRowPointers().size() > 1) {
                numVertices = data.getRowPointers().size() - 1;
                System.err.println("Info: numVertices from line1/line2 was not positive. Using rowPointers.size()-1 = "
                        + numVertices);
            }
        }

        int maxActualNodeId = -1;
        if (data.getEdges() != null && !data.getEdges().isEmpty()) {
            for (Integer nodeId : data.getEdges()) {
                if (nodeId != null && nodeId > maxActualNodeId) {
                    maxActualNodeId = nodeId;
                }
            }
        }
        if (data.getRawPartitionDataLine() != null && !data.getRawPartitionDataLine().isEmpty()) {
            String[] tokens = data.getRawPartitionDataLine().split(";");
            for (String token : tokens) {
                if (token != null && !token.trim().isEmpty()) {
                    try {
                        int val = Integer.parseInt(token.trim());
                        if (val > maxActualNodeId) {
                            maxActualNodeId = val;
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Warning: Non-integer token '" + token
                                + "' in rawPartitionDataLine. Ignoring for max ID calc.");
                    }
                }
            }
        }

        if (maxActualNodeId != -1) {
            int minRequiredVertices = maxActualNodeId + 1;
            if (numVertices < minRequiredVertices) {
                System.err.println("Warning: Current numVertices (" + numVertices +
                        ") is less than implied by max actual node ID in data (" + maxActualNodeId +
                        "). Adjusting numVertices to " + minRequiredVertices);
                numVertices = minRequiredVertices;
            } else if (numVertices > minRequiredVertices) {
                System.err.println("Info: numVertices (" + numVertices +
                        ") from header/metadata is greater than max actual node ID (" + maxActualNodeId +
                        ") + 1. This might indicate isolated nodes. Using numVertices = " + numVertices + ".");
            }

        } else { // maxActualNodeId == -1 (no edge data at all)
            if (numVertices <= 0) {
                
                System.err.println(
                        "Warning: Could not determine number of vertices from any source, or graph is truly empty. numVertices = "
                                + numVertices);
                if (data.getLine1() > 0 || (data.getLine2() != null && !data.getLine2().isEmpty())) {
                } else if (numVertices == 0 && (data.getEdges() == null || data.getEdges().isEmpty())
                        && (data.getRawPartitionDataLine() == null || data.getRawPartitionDataLine().isEmpty())) {
                } else {
                    throw new IllegalArgumentException(
                            "Cannot determine number of vertices. Header lines are non-positive or absent, and no node data found.");
                }
            } else {
                System.err.println("Info: numVertices = " + numVertices
                        + " (from header/metadata). No edge data found to verify/adjust.");
            }
        }

        if (numVertices <= 0 && maxActualNodeId == -1 && data.getLine1() <= 0
                && (data.getLine2() == null || data.getLine2().isEmpty())) {
            System.err.println("Info: Proceeding with numVertices = 0 (empty graph).");
        } else if (numVertices <= 0) {
            throw new IllegalArgumentException("Calculated numVertices is not positive: " + numVertices
                    + ". MaxActualNodeId: " + maxActualNodeId + ". Line1: " + data.getLine1()
                    + ". Check input file format and content.");
        }

        Graph graph = new Graph(numVertices);
        graph.setParsedData(data);
        graph.setPartitions(data.getNumberOfPartitions());

        for (int i = 0; i < numVertices; i++) {
            Node node = new Node(i);
            graph.addNode(node);
        }

        int edgeCount = 0;

        if (data.getNumberOfPartitions() > 1 && data.getRawPartitionDataLine() != null
                && data.getRawOffsetLines() != null && !data.getRawOffsetLines().isEmpty()) {
            List<Integer> allPartitionPairs;
            try {
                allPartitionPairs = Arrays.stream(data.getRawPartitionDataLine().split(";"))
                        .map(s -> Integer.parseInt(s.trim()))
                        .collect(Collectors.toList());
            } catch (NumberFormatException e) {
                System.err.println("Error parsing rawPartitionDataLine: " + e.getMessage());
                graph.setEdges(0); 
                return graph;
            }

            List<String> offsetLines = data.getRawOffsetLines();

            for (int p = 0; p < data.getNumberOfPartitions(); p++) {
                if (p >= offsetLines.size()) {
                    System.err.println(
                            "Warning: Mismatch between numberOfPartitions and available offset lines. Skipping partition "
                                    + p);
                    continue;
                }
                List<Integer> offsetsForP = readLine(offsetLines.get(p));

                if (offsetsForP == null || offsetsForP.size() < 2) { // Need at least start and end offset for any data
                    System.err.println("Warning: Invalid or empty offset line for partition " + p + ". Content: '"
                            + offsetLines.get(p) + "'");
                    continue;
                }

                int startPairIndex = offsetsForP.get(0);
                int endPairIndex = offsetsForP.get(offsetsForP.size() - 1);

                for (int currentIdxInPairs = startPairIndex; currentIdxInPairs < endPairIndex; currentIdxInPairs += 2) {
                    if (currentIdxInPairs + 1 >= allPartitionPairs.size()) {
                        System.err.println("Warning: Partition " + p
                                + " offset data points beyond rawPartitionDataLine bounds. Start: " + startPairIndex
                                + ", End: " + endPairIndex + ", Current: " + currentIdxInPairs + ", RawDataSize: "
                                + allPartitionPairs.size());
                        break;
                    }
                    int u = allPartitionPairs.get(currentIdxInPairs);
                    int v = allPartitionPairs.get(currentIdxInPairs + 1);

                    if (u >= numVertices || v >= numVertices || u < 0 || v < 0) {
                        System.err.println("Error: Node ID (" + u + " or " + v + ") out of bounds for numVertices="
                                + numVertices + ". Skipping edge.");
                        continue;
                    }

                    Node nodeU = graph.getNode(u);
                    Node nodeV = graph.getNode(v);

                    nodeU.setPartId(p);
                    nodeV.setPartId(p);

                    if (nodeU.addNeighbour(v)) {
                        nodeV.addNeighbour(u);
                        edgeCount++;
                    }
                }
            }
            graph.setEdges(edgeCount);
            System.out.println("Loaded pre-partitioned graph with " + numVertices + " vertices, " + edgeCount
                    + " edges, and " + data.getNumberOfPartitions() + " partitions.");

        } else if (data.getEdges() != null && data.getRowPointers() != null &&
                !data.getEdges().isEmpty() && !data.getRowPointers().isEmpty()) {
            List<Integer> edges = data.getEdges();
            List<Integer> rowPointers = data.getRowPointers();

            if (rowPointers.size() - 1 != numVertices && data.getNumberOfPartitions() <= 1) {
                System.err.println("Warning: rowPointers length (" + rowPointers.size()
                        + ") does not match numVertices (" + numVertices + "). Adjacency list might be inconsistent.");
            }

            for (int i = 0; i < numVertices; i++) {
                Node currentNode = graph.getNode(i);
                if (data.getNumberOfPartitions() == 1) {
                    currentNode.setPartId(0);
                } else if (data.getNumberOfPartitions() == 0 && graph.getPartitions() <= 1) { 
                    currentNode.setPartId(0);
                }
                

                if (i < rowPointers.size() - 1) {
                    int startEdge = rowPointers.get(i);
                    int endEdge = rowPointers.get(i + 1);
                    for (int j = startEdge; j < endEdge; j++) {
                        if (j < edges.size()) {
                            int neighborId = edges.get(j);
                            if (neighborId >= numVertices || neighborId < 0) {
                                System.err.println("Error: Neighbor ID (" + neighborId + ") for node " + i
                                        + " out of bounds for numVertices=" + numVertices + ". Skipping edge.");
                                continue;
                            }
                            Node neighborNode = graph.getNode(neighborId);
                            if (currentNode.addNeighbour(neighborId)) {
                                neighborNode.addNeighbour(i);
                                edgeCount++;
                            }
                        } else {
                            System.err.println(
                                    "Warning: Edge index " + j + " out of bounds for edges list size " + edges.size());
                            break;
                        }
                    }
                }
            }
            graph.setEdges(edgeCount);
            System.out.println("Loaded graph with " + numVertices + " vertices and " + edgeCount + " edges.");
        } else {
            System.err.println(
                    "Warning: Graph data (edges/rowPointers or partitioned data) is incomplete or missing. Graph might be empty or partially loaded.");
            graph.setEdges(0);
            for (int i = 0; i < numVertices; ++i) {
                Node node = graph.getNode(i);
                if (node.getPartId() == -1) {
                    if (graph.getPartitions() <= 1) {
                        node.setPartId(0);
                    }
                }
            }
        }
        return graph;
    }

    private static void addNeighbor(Node node, int neighbor) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }
        node.addNeighbour(neighbor);
    }

    public ParsedData parseFile(String filePath) throws IOException {
        ParsedData data = new ParsedData();
        List<String> allLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                allLines.add(line);
            }
        }

        if (allLines.isEmpty()) {
            throw new IOException("File is empty: " + filePath);
        }

        data.setLine1(Integer.parseInt(allLines.get(0)));

        if (allLines.size() > 1) {
            data.setLine2(readLine(allLines.get(1)));
        }
        if (allLines.size() > 2) {
            data.setLine3(readLine(allLines.get(2)));
        }

        int numberOfPartitions = 0;
        if (allLines.size() >= 4) {
            numberOfPartitions = allLines.size() - 4;
        }
        if (numberOfPartitions < 0)
            numberOfPartitions = 0; 

        data.setNumberOfPartitions(numberOfPartitions == 0 ? 1 : numberOfPartitions);

        if (allLines.size() >= 4) {
            if (data.getNumberOfPartitions() > 1) {
                data.setRawPartitionDataLine(allLines.get(3));
                if (allLines.size() > 4) {
                    data.setRawOffsetLines(new ArrayList<>(allLines.subList(4, allLines.size())));
                }
            } else {
                data.setEdges(readLine(allLines.get(3)));
                if (allLines.size() > 4) {
                    data.setRowPointers(readLine(allLines.get(4)));
                } else {
                    data.setRowPointers(new ArrayList<>());
                }
            }
        }

        return data;
    }

    public List<Integer> readLine(String line) {
        List<Integer> numbers = new ArrayList<>();

        line = (line != null) ? line.trim() : null;

        if (line != null && !line.isEmpty()) {
            String[] tokens = line.split(";");
            for (String token : tokens) {
                try {
                    numbers.add(Integer.parseInt(token));
                } catch (NumberFormatException e) {
                    System.err.println("Błąd parsowania liczby: " + token);
                }
            }
        }
        return numbers;
    }

    private String convertListIntegerToSemicolonString(List<Integer> list) {
        if (list == null || list.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }

    public ParsedData parseBinaryDeltaEncodedFile(String filePath) throws IOException {
        ParsedData data = new ParsedData();
        List<List<Integer>> allLogicalLines = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
                DataInputStream dis = new DataInputStream(fis)) {

            while (dis.available() > 0) {
                if (dis.available() < 4) {
                    if (dis.available() > 0) {
                        System.err.println("Warning: Trailing bytes in binary file (" + dis.available()
                                + "), not enough for a full line prefix. Ignoring.");
                    }
                    break;
                }
                int numValuesInLine = dis.readInt();

                if (numValuesInLine < 0) {
                    throw new IOException(
                            "Binary file corrupted: number of values in line cannot be negative: " + numValuesInLine);
                }
                if (numValuesInLine == 0) {
                    allLogicalLines.add(new ArrayList<>());
                    continue;
                }
                if (dis.available() < (long) numValuesInLine * 2) {
                    throw new IOException("Binary file corrupted or length prefix incorrect. Expected " +
                            numValuesInLine * 2 + " bytes for data, but only " + dis.available()
                            + " available after reading length.");
                }

                List<Integer> currentLineValues = new ArrayList<>();
                if (numValuesInLine > 0) {
                    short encodedFirstValue = dis.readShort();
                    int signBitFirst = (encodedFirstValue >> 15) & 0x1;
                    int magnitudeFirst = encodedFirstValue & 0x7FFF;
                    int currentReconstructedValue = (signBitFirst == 1) ? -magnitudeFirst : magnitudeFirst;
                    currentLineValues.add(currentReconstructedValue);

                    for (int i = 1; i < numValuesInLine; i++) {
                        short encodedDelta = dis.readShort();
                        int signBitDelta = (encodedDelta >> 15) & 0x1;
                        int magnitudeDelta = encodedDelta & 0x7FFF;
                        int delta = (signBitDelta == 1) ? -magnitudeDelta : magnitudeDelta;

                        currentReconstructedValue += delta;
                        currentLineValues.add(currentReconstructedValue);
                    }
                }
                allLogicalLines.add(currentLineValues);
            }
        }

        if (allLogicalLines.isEmpty()) {
            throw new IOException("Binary file is empty or contains no valid data lines: " + filePath);
        }

        if (allLogicalLines.get(0).isEmpty()) {
            throw new IOException("Binary file Line 1 (matrix size) is missing or empty.");
        }
        data.setLine1(allLogicalLines.get(0).get(0));

        if (allLogicalLines.size() > 1) {
            data.setLine2(allLogicalLines.get(1));
        }
        if (allLogicalLines.size() > 2) {
            data.setLine3(allLogicalLines.get(2));
        }

        int numPotentialOffsetLines = 0;
        if (allLogicalLines.size() >= 4) {
            numPotentialOffsetLines = allLogicalLines.size() - 4;
        }
        data.setNumberOfPartitions(numPotentialOffsetLines == 0 ? 1 : numPotentialOffsetLines);

        if (allLogicalLines.size() >= 4) {
            if (data.getNumberOfPartitions() > 1) {
                data.setRawPartitionDataLine(convertListIntegerToSemicolonString(allLogicalLines.get(3)));
                if (allLogicalLines.size() > 4) {
                    List<String> offsetStrings = new ArrayList<>();
                    for (int i = 4; i < allLogicalLines.size(); i++) {
                        offsetStrings.add(convertListIntegerToSemicolonString(allLogicalLines.get(i)));
                    }
                    data.setRawOffsetLines(offsetStrings);
                } else {
                    data.setRawOffsetLines(new ArrayList<>());
                }
            } else {
                data.setEdges(allLogicalLines.get(3));
                if (allLogicalLines.size() > 4) {
                    data.setRowPointers(allLogicalLines.get(4));
                } else {
                    data.setRowPointers(new ArrayList<>());
                }
            }
        } else {
            if (data.getEdges() == null)
                data.setEdges(new ArrayList<>());
            if (data.getRowPointers() == null)
                data.setRowPointers(new ArrayList<>());
            if (data.getRawOffsetLines() == null)
                data.setRawOffsetLines(new ArrayList<>());
        }
        return data;
    }

    public String convertBinaryToTemporaryTextFile(String binaryFilePath) throws IOException {
        File tempFile = File.createTempFile("temp_graph_", ".csrrg");
        tempFile.deleteOnExit();

        try (FileInputStream fis = new FileInputStream(binaryFilePath);
                DataInputStream dis = new DataInputStream(fis);
                BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(tempFile))) {

            while (dis.available() > 0) {
                if (dis.available() < 4) {
                    if (dis.available() > 0) {
                        System.err.println("Warning: Trailing bytes in binary file (" + dis.available()
                                + "), not enough for a full line prefix. Ignoring.");
                    }
                    break;
                }
                int numValuesInLine = dis.readInt();

                if (numValuesInLine < 0) {
                    throw new IOException(
                            "Binary file corrupted: number of values in line cannot be negative: " + numValuesInLine);
                }
                if (numValuesInLine == 0) {
                    writer.newLine();
                    continue;
                }
                if (dis.available() < (long) numValuesInLine * 2) {
                    throw new IOException("Binary file corrupted or length prefix incorrect. Expected " +
                            numValuesInLine * 2 + " bytes, but only " + dis.available() + " available.");
                }

                List<Integer> currentLineValues = new ArrayList<>();
                if (numValuesInLine > 0) {
                    short encodedFirstValue = dis.readShort();
                    int signBitFirst = (encodedFirstValue >> 15) & 0x1;
                    int magnitudeFirst = encodedFirstValue & 0x7FFF;
                    int currentReconstructedValue = (signBitFirst == 1) ? -magnitudeFirst : magnitudeFirst;
                    currentLineValues.add(currentReconstructedValue);

                    for (int i = 1; i < numValuesInLine; i++) {
                        short encodedDelta = dis.readShort();
                        int signBitDelta = (encodedDelta >> 15) & 0x1;
                        int magnitudeDelta = encodedDelta & 0x7FFF;
                        int delta = (signBitDelta == 1) ? -magnitudeDelta : magnitudeDelta;

                        currentReconstructedValue += delta;
                        currentLineValues.add(currentReconstructedValue);
                    }
                }
                writer.write(convertListIntegerToSemicolonString(currentLineValues));
                writer.newLine();
            }
        } catch (IOException e) {
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw e;
        }
        return tempFile.getAbsolutePath();
    }
}