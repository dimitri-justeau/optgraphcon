package org.cceval;

import org.cceval.grid.neighborhood.INeighborhood;
import org.cceval.grid.neighborhood.Neighborhoods;
import org.cceval.grid.regular.square.PartialRegularSquareGrid;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.UndirectedGraphVar;
import org.chocosolver.util.graphOperations.connectivity.ConnectivityFinder;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.chocosolver.util.tools.ArrayUtils;

import java.io.IOException;
import java.util.stream.IntStream;

public class SpatialPlanningModel {

    DataLoader dataLoader;
    PartialRegularSquareGrid grid;
    INeighborhood neighborhood;

    Model model;
    UndirectedGraphVar habitatGraphVar;
    SetVar nodes;
    IntVar nbPUs;
    IntVar nbPatches;

    int landscapeArea;
    int initialHabitatArea;
    int nbPatchesInitial;

    public SpatialPlanningModel(DataLoader dataLoader, int habitatValue, int nonHabitatValue, boolean verbose) throws IOException {
        this.dataLoader = dataLoader;
        if (verbose) {
            System.out.println("Raster dimension: " + dataLoader.getHeight() + " x " + dataLoader.getWidth());
        }
        // Retrieve nodata pixels
        int[] outPixels = dataLoader.getNoDataPixels();
        // Instantiate partial grid and neighborhood
        this.grid = new PartialRegularSquareGrid(dataLoader.getHeight(), dataLoader.getWidth(), outPixels);
        this.neighborhood = Neighborhoods.PARTIAL_FOUR_CONNECTED;
        // Retrieve habitat and non habitat pixels and convert index to partial index
        int[] habitatPixels = IntStream.of(dataLoader.getPixelsByValue(habitatValue))
                .map(i -> grid.getPartialIndex(i))
                .toArray();
        int[] nonHabitatPixels = IntStream.of(dataLoader.getPixelsByValue(nonHabitatValue))
                .map(i -> grid.getPartialIndex(i))
                .toArray();
        this.initialHabitatArea = habitatPixels.length;
        this.landscapeArea = habitatPixels.length + nonHabitatPixels.length;
        // Display problem properties
        if (verbose) {
            System.out.println("Current landscape state loaded");
            System.out.println("    Habitat cells: " + habitatPixels.length + " ");
            System.out.println("    Non habitat cells: " + nonHabitatPixels.length + " ");
            System.out.println("    Out cells: " + outPixels.length);
            System.out.println("    -----------------------");
            System.out.println("    Total nodes: " + (habitatPixels.length + nonHabitatPixels.length));
        }
        // Create Choco model
        this.model = new Model("Spatial Planning Problem");
        UndirectedGraph GLB = neighborhood.getPartialGraph(grid, model, habitatPixels, SetType.BIPARTITESET, SetType.BIPARTITESET);
        UndirectedGraph GUB = neighborhood.getPartialGraph(grid, model, ArrayUtils.concat(habitatPixels, nonHabitatPixels), SetType.BIPARTITESET, SetType.BIPARTITESET);
        this.habitatGraphVar = model.nodeInducedGraphVar(
                "habitatGraph",
                GLB,
                GUB
        );
        // Compute initial number of patches and display it
        ConnectivityFinder connectivityFinder = new ConnectivityFinder(GLB);
        connectivityFinder.findAllCC();
        this.nbPatchesInitial = connectivityFinder.getNBCC();
        if (verbose) {
            System.out.println("Nb patches initial: " + nbPatchesInitial);
        }
        // Create the nbPUs (number of planning units) variable as an offset to the number of nodes of the graph var
        IntVar nbNodes = model.intVar(GLB.getNodes().size(), GUB.getNodes().size());
        model.nbNodes(habitatGraphVar, nbNodes).post();
        this.nbPUs = model.offset(nbNodes, -GLB.getNodes().size());
        // Create the nbPatches variable
        this.nbPatches = model.intVar(0, GUB.getNodes().size());
        model.nbConnectedComponents(habitatGraphVar, nbPatches).post();
        nodes = model.graphNodeSetView(habitatGraphVar);
    }

    public void postBudgetConstraint(int minBudget, int maxBudget) {
        if (minBudget == maxBudget) {
            model.arithm(nbPUs, "=", minBudget).post();
        } else {
            model.arithm(nbPUs, ">=", minBudget).post();
            model.arithm(nbPUs, "<=", maxBudget).post();
        }
    }

}
