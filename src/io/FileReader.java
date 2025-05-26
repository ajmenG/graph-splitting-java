package io;

import model.Graph;
import model.Node;
import model.ParsedData;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileReader {
    // stałe i zmienne globalne
    private static final int MAX_BUFFER = Integer.MAX_VALUE; // maksymalny rozmiar bufora do czytania pliku

    public FileReader() {
        // konstruktor bezparametrowy
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

            // W oryginale jest błąd - readSeparator nie jest inicjalizowane przed porównaniem
            // Zakładamy, że powinna być odczytana wartość z pliku

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
        // utwórz graf
        Graph graph = new Graph(data.getLine2().size());
        int edgeCount = 0;

        // najpierw zainicjuj wszystkie węzły
        for (int i = 0; i < data.getLine2().size(); i++) {
            graph.addNode(new Node(i));
        }

        // dodaj krawędzie do grafu
        for (int i = 0; i < data.getRowPointers().size(); i++) {
            // ustaw początek wiersza
            int start = data.getRowPointers().get(i);

            // koniec wiersza to początek następnego lub koniec listy krawędzi
            int end = (i + 1 < data.getRowPointers().size()) ?
                    data.getRowPointers().get(i + 1) : data.getEdges().size();

            // dodaj krawędzie dla bieżącego wiersza
            for (int j = start; j < end; j++) {
                int currentVertex = i;  // to jest poprawne - bieżący wierzchołek to i
                int neighborVertex = data.getEdges().get(j);

                // sprawdź poprawność indeksu sąsiada
                if (neighborVertex < 0 || neighborVertex >= graph.getVertices()) {
                    System.err.println("Indeks sąsiada poza zakresem: " + neighborVertex);
                    continue;
                }

                // dodaj krawędź tylko raz (graf jest nieskierowany, ale każdą krawędź dodajemy tylko raz)
                if (currentVertex < neighborVertex) {  // dodaj krawędź tylko jeśli currentVertex < neighborVertex
                    graph.getNode(currentVertex).addNeighbour(neighborVertex);
                    graph.getNode(neighborVertex).addNeighbour(currentVertex);
                    edgeCount++;
                }
            }
        }
        
        // Aktualizacja liczby krawędzi w grafie
        graph.setEdges(edgeCount);
        
        // Dodaj debug info
        System.out.println("Loaded graph with " + graph.getVertices() + " vertices and " + graph.getEdges() + " edges");
        System.out.println("Data contains " + data.getEdges().size() + " edge entries and " + data.getRowPointers().size() + " row pointers");
        
        return graph;
    }

    // dodaje sąsiada do listy sąsiadów wierzchołka
    private static void addNeighbor(Node node, int neighbor) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }
        node.addNeighbour(neighbor);
    }

    public ParsedData parseFile(String filePath) throws IOException {
        ParsedData data = new ParsedData();

        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(filePath))) {
            // Read the first line (size of matrix)
            String line = reader.readLine();
            int matrixSize = Integer.parseInt(line);
            data.setLine1(matrixSize);

            // Read the second line
            line = reader.readLine();
            data.setLine2(readLine(line));

            // Read the third line
            line = reader.readLine();
            data.setLine3(readLine(line));

            // Read the fourth line (edges)
            line = reader.readLine();
            data.setEdges(readLine(line));

            // Read the fifth line (row pointers)
            line = reader.readLine();
            data.setRowPointers(readLine(line));
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
}