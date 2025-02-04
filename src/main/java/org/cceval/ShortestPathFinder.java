package org.cceval;

import org.cceval.grid.neighborhood.INeighborhood;
import org.cceval.grid.neighborhood.Neighborhoods;
import org.cceval.grid.regular.square.GroupedGrid;
import org.cceval.grid.regular.square.PartialRegularGroupedGrid;
import org.cceval.preprocessing.Pixel;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.UndirectedGraphVar;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.chocosolver.util.tools.ArrayUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.IntStream;

public class ShortestPathFinder {

    DataLoader dataLoader;
    PartialRegularGroupedGrid grid;
    INeighborhood neighborhood;

    RasterConnectivityFinder habGraph;

    UndirectedGraph graph;

    int nonHabNonAcc;
    int[] availablePlanningUnits;


    public ShortestPathFinder(DataLoader dataLoader, int accessibleVal) {
        this.dataLoader = dataLoader;

        int[] outPixels = IntStream.range(0, dataLoader.getHabitatData().length)
                .filter(i -> dataLoader.getHabitatData()[i] <= -1 || dataLoader.getHabitatData()[i] == dataLoader.noDataHabitat)
                .toArray();

        int[] nonHabitatNonAccessiblePixels = IntStream.range(0, dataLoader.getHabitatData().length)
                .filter(i -> dataLoader.getHabitatData()[i] == 0 && dataLoader.getAccessibleData()[i] != accessibleVal)
                .toArray();

        int[] habitatPixelsComp = IntStream.range(0, dataLoader.getHabitatData().length)
                .filter(i -> dataLoader.getHabitatData()[i] == 1)
                .toArray();

        habGraph = new RasterConnectivityFinder(
                dataLoader.getHeight(), dataLoader.getWidth(),
                dataLoader.getHabitatData(), 1,
                Neighborhoods.FOUR_CONNECTED
        );

        nonHabNonAcc = nonHabitatNonAccessiblePixels.length;

        this.grid = new PartialRegularGroupedGrid(
                dataLoader.getHeight(), dataLoader.getWidth(),
                ArrayUtils.concat(outPixels, nonHabitatNonAccessiblePixels),
                habGraph
        );

        int[] nonHabitatPixels = IntStream.range(0, dataLoader.getHabitatData().length)
                .filter(i -> dataLoader.getHabitatData()[i] == 0)
                .toArray();

        int nbGroups = grid.getNbGroups();

        int[] habitatPixels = IntStream.range(0, nbGroups).toArray();

        PartialRegularGroupedGrid g = (PartialRegularGroupedGrid) grid;
        availablePlanningUnits = IntStream.range(0, dataLoader.getAccessibleData().length)
                .filter(i -> dataLoader.getAccessibleData()[i] == accessibleVal && dataLoader.getHabitatData()[i] == 0)
                .map(i -> g.getGroupIndexFromCompleteIndex(i))
                .toArray();

        this.neighborhood = Neighborhoods.PARTIAL_GROUPED_FOUR_CONNECTED;

        graph = neighborhood.getPartialGraph(grid, ArrayUtils.concat(habitatPixels, availablePlanningUnits), SetType.BIPARTITESET, SetType.SMALLBIPARTITESET);
        System.out.println("NB GROUPS = " + grid.getNbGroups());
    }

    public int[] getShortestPath(Pixel pixFrom, Pixel pixTo) {
        int from = grid.getGroupIndexFromPartialIndex(grid.getIndexFromCoordinates(pixFrom.x - 1, pixFrom.y - 1));
        int to = grid.getGroupIndexFromPartialIndex(grid.getIndexFromCoordinates(pixTo.x - 1, pixTo.y - 1));
        int n = graph.getNbMaxNodes();
        boolean[] visited = new boolean[n];
        int[] queue = new int[n];
        int[] parent = new int[n];
        int front = 0;
        int rear = 0;
        int current;
        int[] dist = new int[n];
        for (int i = 0; i < n; i++) {
            dist[i] = -1;
        }
        dist[from] = 0;
        visited[from] = true;
        queue[front] = from;
        rear++;
        while (front != rear) {
            current = queue[front++];
            if (current == to) {
                int[] path = new int[dist[to] - 1];
                int last = parent[to];
                for (int i = dist[to] - 2; i >= 0; i--) {
                    int idx = grid.getUngroupedCompleteIndex(last);
                    path[i] = idx;
                    last = parent[last];
                }
                return path;
            }
            for (int i : graph.getNeighborsOf(current)) {
                if (!visited[i]) {
                    dist[i] = dist[current] + 1;
                    queue[rear++] = i;
                    parent[i] = current;
                    visited[i] = true;
                }
            }
        }
        return null;
    }
}
