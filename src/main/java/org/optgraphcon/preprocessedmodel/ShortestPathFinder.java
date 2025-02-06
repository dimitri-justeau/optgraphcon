package org.optgraphcon.preprocessedmodel;

import org.optgraphcon.DataLoader;
import org.optgraphcon.RasterConnectivityFinder;
import org.optgraphcon.grid.neighborhood.INeighborhood;
import org.optgraphcon.grid.neighborhood.Neighborhoods;
import org.optgraphcon.grid.regular.square.PartialRegularGroupedGrid;
import org.optgraphcon.preprocessing.Pixel;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.stream.IntStream;

/**
 * Find shortest paths in the raster grid problem, from edges selected in the preprocessed problem.
 */
public class ShortestPathFinder {

    DataLoader dataLoader;
    PartialRegularGroupedGrid grid;
    INeighborhood neighborhood;

    RasterConnectivityFinder habGraph;

    UndirectedGraph graph;

    int nonHabNonAcc;
    int[] availablePlanningUnits;


    public ShortestPathFinder(DataLoader dataLoader, int accessibleVal) {
        // Load the data and compute a edge-contracted raster grid that will serve as the basis to
        // find shortest paths using BFS.
        this.dataLoader = dataLoader;
        int[] outPixels = IntStream.range(0, dataLoader.getHabitatData().length)
                .filter(i -> dataLoader.getHabitatData()[i] <= -1 || dataLoader.getHabitatData()[i] == dataLoader.noDataHabitat)
                .toArray();
        int[] nonHabitatNonAccessiblePixels = IntStream.range(0, dataLoader.getHabitatData().length)
                .filter(i -> dataLoader.getHabitatData()[i] == 0 && dataLoader.getAccessibleData()[i] != accessibleVal)
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
        int nbGroups = grid.getNbGroups();
        int[] habitatPixels = IntStream.range(0, nbGroups).toArray();
        PartialRegularGroupedGrid g = grid;
        availablePlanningUnits = IntStream.range(0, dataLoader.getAccessibleData().length)
                .filter(i -> dataLoader.getAccessibleData()[i] == accessibleVal && dataLoader.getHabitatData()[i] == 0)
                .map(i -> g.getGroupIndexFromCompleteIndex(i))
                .toArray();
        this.neighborhood = Neighborhoods.PARTIAL_GROUPED_FOUR_CONNECTED;
        graph = neighborhood.getPartialGraph(grid, ArrayUtils.concat(habitatPixels, availablePlanningUnits), SetType.BIPARTITESET, SetType.SMALLBIPARTITESET);
    }

    /**
     * Compute shortest path using BFS.
     * @param pixFrom start node
     * @param pixTo end node
     * @return An array of pixels between pixFrom and pixTo forming a shortest path.
     */
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
