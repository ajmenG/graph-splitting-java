package model;

import java.util.List;

public class ParsedData {
    private int line1;
    private List<Integer> line2;
    private List<Integer> line3;
    private List<Integer> edges;
    private List<Integer> rowPointers;
    private int line2Count;
    private int line3Count;
    private int edgeCount;
    private int rowCount;

    public ParsedData() {
        this.line1 = 0;
        this.line2 = null;
        this.line3 = null;
        this.line2Count = 0;
        this.line3Count = 0;
        this.edges = null;
        this.edgeCount = 0;
        this.rowPointers = null;
        this.rowCount = 0;
    }

    public int getEdgeCount() {
        return edgeCount;
    }

    public int getLine1() {
        return line1;
    }

    public void setLine1(int line1) {
        this.line1 = line1;
    }

    public List<Integer> getLine2() {
        return line2;
    }

    public void setLine2(List<Integer> line2) {
        this.line2 = line2;
        this.line2Count = line2.size();
    }

    public List<Integer> getLine3() {
        return line3;
    }

    public void setLine3(List<Integer> line3) {
        this.line3 = line3;
        this.line3Count = line3.size();
    }

    public List<Integer> getEdges() {
        return edges;
    }

    public void setEdges(List<Integer> edges) {
        this.edges = edges;
        this.edgeCount = edges.size();
    }

    public List<Integer> getRowPointers() {
        return rowPointers;
    }

    public void setRowPointers(List<Integer> rowPointers) {
        this.rowPointers = rowPointers;
        this.rowCount = rowPointers.size();
    }

}
