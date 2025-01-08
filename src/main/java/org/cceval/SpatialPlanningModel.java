package org.cceval;

import org.cceval.grid.neighborhood.INeighborhood;
import org.cceval.grid.neighborhood.Neighborhoods;
import org.cceval.grid.regular.square.GroupedGrid;
import org.cceval.grid.regular.square.PartialRegularGroupedGrid;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.UndirectedGraphVar;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.chocosolver.util.tools.ArrayUtils;

import java.io.IOException;
import java.util.stream.IntStream;

public class SpatialPlanningModel {

    DataLoader dataLoader;
    GroupedGrid grid;
    INeighborhood neighborhood;

    Model model;
    UndirectedGraphVar habitatGraphVar;
    UndirectedGraphVar restoreGraph;
    SetVar restoreSet;
    IntVar minRestore;
    RasterConnectivityFinder habGraph;
    SetVar nodes;
    IntVar nbPatches;

    int nbPatchesInitial;
    int nonHabNonAcc;
    int[] availablePlanningUnits;


    public SpatialPlanningModel(DataLoader dataLoader, int accessibleVal, boolean verbose) throws IOException {
        this.dataLoader = dataLoader;
        if (verbose) {
            System.out.println("Raster dimension: " + dataLoader.getHeight() + " x " + dataLoader.getWidth());
        }

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

        if (verbose) {
            System.out.println("Current landscape state loaded");
            System.out.println("    Habitat cells = " + habitatPixelsComp.length + " ");
            System.out.println("    Non habitat cells = " + nonHabitatPixels.length + " ");
            System.out.println("    Accessible non habitat cells = " + availablePlanningUnits.length + " ");
            System.out.println("    Out cells = " + outPixels.length);
        }

        this.neighborhood = Neighborhoods.PARTIAL_GROUPED_FOUR_CONNECTED;

        // Create Choco model
        this.model = new Model("Spatial Planning Problem");
        UndirectedGraph GLB = neighborhood.getPartialGraph(grid, model, habitatPixels, SetType.BIPARTITESET, SetType.SMALLBIPARTITESET);
        UndirectedGraph GUB = neighborhood.getPartialGraph(grid, model, ArrayUtils.concat(habitatPixels, availablePlanningUnits), SetType.BIPARTITESET, SetType.BIPARTITESET);
        this.habitatGraphVar = model.nodeInducedGraphVar(
                "habitatGraph",
                GLB,
                GUB
        );
        this.nbPatchesInitial = nbGroups;
        if (verbose) {
            System.out.println("Nb patches initial: " + nbPatchesInitial);
        }

        this.nbPatches = model.intVar(0, GUB.getNodes().size());
        model.nbConnectedComponents(habitatGraphVar, nbPatches).post();
        nodes = model.graphNodeSetView(habitatGraphVar);

        restoreGraph = model.nodeInducedSubgraphView(habitatGraphVar, SetFactory.makeConstantSet(IntStream.range(0, nbGroups).toArray()), true);
        restoreSet = model.graphNodeSetView(restoreGraph);
    }

    public void postBudgetConstraint(int minBudget, int maxBudget, double minProportion) {
        assert minProportion >= 0 && minProportion <= 1;
        int[] pus = availablePlanningUnits;
        int[] minArea = new int[pus.length];
        int[] maxRestorableArea = new int[pus.length];
        int maxCellArea = 0;
        int offset = grid.getNbGroups();
        for (int i = 0; i < availablePlanningUnits.length; i++) {
            int cell = pus[i];
            int cArea = getCellArea(cell);
            maxCellArea = maxCellArea < cArea ? cArea : maxCellArea;
            int threshold = (int) Math.ceil(cArea * (1 - minProportion));
            int restorable = getRestorableArea(cell);
            maxRestorableArea[cell - offset] = restorable;
            minArea[cell - offset] = restorable <= threshold ? 0 : restorable - threshold;
        }
        minRestore = model.intVar(minBudget, maxBudget);
        IntVar totalRestorable = model.intVar(0, pus.length * maxCellArea);
        model.sumElements(restoreSet, minArea, offset, minRestore).post();
        model.sumElements(restoreSet, maxRestorableArea, offset, totalRestorable).post();
    }

    public int getCellArea(int pu) {
        PartialRegularGroupedGrid g = (PartialRegularGroupedGrid) grid;
        return dataLoader.getCellAreaData()[g.getUngroupedCompleteIndex(pu)];
    }

    public int getRestorableArea(int pu) {
        PartialRegularGroupedGrid g = (PartialRegularGroupedGrid) grid;
        return (int) Math.round(dataLoader.getRestorableData()[g.getUngroupedCompleteIndex(pu)]);
    }
}
