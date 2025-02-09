package org.optgraphcon.preprocessing;

import java.util.ArrayList;

public class Node {
    public static final int TERMINAL = 0;
    public static final int STEINER = 1;

    public final Pixel pixel;
    public final int weight;
    public ArrayList<Edge> edges;

    public Node(Pixel pixel, int type) {
        this.pixel = pixel;
        this.weight = type;
        this.edges = new ArrayList<>();
    }

    public Pixel getPixel() {
        return pixel;
    }

    public int getWeight() {
        return weight;
    }

    public int getType() {
        return weight;
    }

    public ArrayList<Edge> getEdges() {
        return edges;
    }

    public boolean addEdge(Edge edge) {
        boolean multi = false;
        for (Edge e : edges) {
           if (e.destination == edge.destination) {
               e.weight = Math.min(e.weight, edge.weight);
               multi = true;
               break;
           }
        }
        if (!multi) {
            edges.add(edge);
        }
        return !multi;
    }

    public boolean removeEdge(Node node) {
        for (int i = 0; i < edges.size(); i++) {
            Edge e = edges.get(i);
            if (e.destination == node) {
                edges.remove(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        String res = pixel + "-> ";
        for (Edge edge : edges) {
            res += edge.destination.pixel + "-" + edge.weight + " ";
        }
        return res + "\n";
    }
}
