package io;

import model.Graph;
import model.ParsedData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class FileReaderTest {

    @TempDir
    Path tempDir;

    private File createTemporaryTextFile(String content, String fileName) throws IOException {
        Path filePath = tempDir.resolve(fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            writer.write(content);
        }
        return filePath.toFile();
    }

    private File createTemporaryBinaryFile(byte[] content, String fileName) throws IOException {
        Path filePath = tempDir.resolve(fileName);
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(content);
        }
        return filePath.toFile();
    }

    private byte[] encodeSigned16BitForTest(int value) {
        byte[] bytes = new byte[2];
        int signBit = (value < 0) ? 1 : 0;
        int magnitude = Math.abs(value);
        if (magnitude > 0x7FFF) {
            throw new IllegalArgumentException("Test value magnitude too large for 15 bits: " + magnitude);
        }
        int encodedValue = (signBit << 15) | magnitude;
        bytes[0] = (byte) ((encodedValue >> 8) & 0xFF);
        bytes[1] = (byte) (encodedValue & 0xFF);
        return bytes;
    }

    @Test
    void testParseSimpleCsrrgFile() throws IOException {
        String fileContent = "3\n" +
                "0;1;2\n" +
                "0;1;2;3\n" +
                "0;1;1;2\n" +
                "0;2;3";

        File testFile = createTemporaryTextFile(fileContent, "simple.csrrg");
        FileReader fileReader = new FileReader();
        ParsedData data = fileReader.parseFile(testFile.getAbsolutePath());

        assertEquals(3, data.getLine1());
        assertEquals(Arrays.asList(0, 1, 2), data.getLine2());
        assertEquals(Arrays.asList(0, 1, 2, 3), data.getLine3());
        assertEquals(Arrays.asList(0, 1, 1, 2), data.getEdges());
        assertEquals(Arrays.asList(0, 2, 3), data.getRowPointers());
        assertEquals(1, data.getNumberOfPartitions());
        assertNull(data.getRawPartitionDataLine());
        assertTrue(data.getRawOffsetLines() == null || data.getRawOffsetLines().isEmpty());
    }

    @Test
    void testParsePartitionedCsrrgFile() throws IOException {
        String fileContent = "4\n" +
                "0;1;2;3\n" +
                "0;1;2;3;4\n" +
                "0;1;2;3\n" +
                "0;2\n" +
                "2;4";

        File testFile = createTemporaryTextFile(fileContent, "partitioned.csrrg");
        FileReader fileReader = new FileReader();
        ParsedData data = fileReader.parseFile(testFile.getAbsolutePath());

        assertEquals(4, data.getLine1());
        assertEquals(2, data.getNumberOfPartitions());
        assertEquals("0;1;2;3", data.getRawPartitionDataLine());
        assertEquals(Arrays.asList("0;2", "2;4"), data.getRawOffsetLines());
        assertTrue(data.getEdges() == null || data.getEdges().isEmpty());
        assertTrue(data.getRowPointers() == null || data.getRowPointers().isEmpty());
    }

    @Test
    void testLoadGraphFromSimpleParsedData() {
        ParsedData data = new ParsedData();
        data.setLine1(3);
        data.setLine2(Arrays.asList(0, 1, 2));
        data.setEdges(Arrays.asList(1, 2, 0, 2, 0, 1));
        data.setRowPointers(Arrays.asList(0, 2, 4, 6));
        data.setNumberOfPartitions(1);

        FileReader fileReader = new FileReader();
        Graph graph = fileReader.loadGraph(data);

        assertEquals(3, graph.getVertices());
        assertEquals(3, graph.getEdges());
        assertEquals(1, graph.getPartitions());
        if (graph.getVertices() > 0) {
            assertEquals(0, graph.getNode(0).getPartId());
        }
    }

    @Test
    void testLoadGraphFromPartitionedParsedData() {
        ParsedData data = new ParsedData();
        data.setLine1(4);
        data.setLine2(Arrays.asList(0, 1, 2, 3));
        data.setNumberOfPartitions(2);
        data.setRawPartitionDataLine("0;1;2;3");
        data.setRawOffsetLines(Arrays.asList("0;2", "2;4"));

        FileReader fileReader = new FileReader();
        Graph graph = fileReader.loadGraph(data);

        assertEquals(4, graph.getVertices());
        assertEquals(2, graph.getPartitions());
        assertEquals(2, graph.getEdges());
        assertEquals(0, graph.getNode(0).getPartId());
        assertEquals(0, graph.getNode(1).getPartId());
        assertEquals(1, graph.getNode(2).getPartId());
        assertEquals(1, graph.getNode(3).getPartId());
    }

    private int decodeSigned16BitMagnitude(short encodedValue) {
        int signBit = (encodedValue >> 15) & 0x1;
        int magnitude = encodedValue & 0x7FFF;
        return (signBit == 1) ? -magnitude : magnitude;
    }

    @Test
    void testSignMagnitudeDecodingLogic() {
        assertEquals(5, decodeSigned16BitMagnitude((short) 5));
        assertEquals(-5, decodeSigned16BitMagnitude((short) ((1 << 15) | 5)));
        assertEquals(0, decodeSigned16BitMagnitude((short) 0));
        assertEquals(32767, decodeSigned16BitMagnitude((short) 0x7FFF));
        assertEquals(-32767, decodeSigned16BitMagnitude((short) ((1 << 15) | 0x7FFF)));
    }

    @Test
    void testConvertBinaryToTemporaryTextFile_SimpleLine() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(2);

        byte[] val1Bytes = encodeSigned16BitForTest(10);
        dos.write(val1Bytes);

        byte[] delta1Bytes = encodeSigned16BitForTest(2);
        dos.write(delta1Bytes);
        dos.close();

        byte[] binaryContent = baos.toByteArray();
        File binaryTestFile = createTemporaryBinaryFile(binaryContent, "test_line.csrrgbin");

        FileReader fileReader = new FileReader();
        String tempTextFilePath = fileReader.convertBinaryToTemporaryTextFile(binaryTestFile.getAbsolutePath());

        assertNotNull(tempTextFilePath);
        File tempTextFile = new File(tempTextFilePath);
        assertTrue(tempTextFile.exists());

        String textContent = Files.readString(Path.of(tempTextFilePath));
        assertEquals("10;12", textContent.trim());
    }

    @Test
    void testParseFile_EmptyFile() throws IOException {
        File emptyFile = createTemporaryTextFile("", "empty.csrrg");
        FileReader fileReader = new FileReader();
        assertThrows(IOException.class, () -> {
            fileReader.parseFile(emptyFile.getAbsolutePath());
        });
    }

    @Test
    void testConvertBinaryToTemporaryTextFile_CorruptedLength() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(5);
        dos.writeShort(10);
        dos.close();
        byte[] binaryContent = baos.toByteArray();
        File corruptedFile = createTemporaryBinaryFile(binaryContent, "corrupted.csrrgbin");
        FileReader fileReader = new FileReader();
        assertThrows(IOException.class, () -> {
            fileReader.convertBinaryToTemporaryTextFile(corruptedFile.getAbsolutePath());
        });
    }
}