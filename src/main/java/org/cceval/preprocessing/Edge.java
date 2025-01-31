package org.cceval.preprocessing;

import java.util.Objects;

public class Edge {
    public final Node destination;
    public int weight;

    public Edge(Node node, int weight) {
        this.destination = node;
        this.weight = weight;
    }
}
