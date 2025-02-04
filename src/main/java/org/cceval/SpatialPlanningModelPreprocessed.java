package org.cceval;

import org.cceval.preprocessing.Edge;
import org.cceval.preprocessing.Grid;
import org.cceval.preprocessing.Node;
import org.cceval.preprocessing.Pixel;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.UndirectedGraphVar;
import org.chocosolver.util.objects.graphs.GraphFactory;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.chocosolver.util.tools.ArrayUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class SpatialPlanningModelPreprocessed {

    Grid hananGrid;
    DataLoader dataLoader;

    int[] steinerPointsIdx;

    IntVar budgetEdges;
    IntVar budgetNodes;
    IntVar totalBudget;

    BoolVar[] edgesBoolVars;
    BoolVar[] nodesBoolVars;
    BoolVar[] decisionVars;

    int[] nodesIdx;
    int[][] edgesIdx;
    int[] edgesWeights;

    Model model;
    UndirectedGraphVar habitatGraphVar;
    IntVar nbPatches;

    int nbPatchesInitial;

    static final int MAX_BUDGET_CELLS = 787;

    public SpatialPlanningModelPreprocessed(int agg, boolean verbose, String logFilePath) throws IOException {
        int[][] instance = Utils.getMatrixWithBoundaryOfInstance(agg);
        this.dataLoader = Utils.getDataLoaderOfInstance(agg);
        this.hananGrid = new Grid(instance);
        hananGrid.processInstance();
        this.model = new Model("Spatial Planning Problem -- preprocessed");
        Solver solver = model.getSolver();
        if (logFilePath != null) {
            solver.log().remove(System.out);
            solver.log().add(new PrintStream(Files.newOutputStream(Paths.get(logFilePath)), false));
        }
        if (verbose) {
            //solver.log().println("Raster dimension: " + dataLoader.getHeight() + " x " + dataLoader.getWidth());
        }

        if (verbose) {
            /*solver.log().println("Current landscape state loaded");
            solver.log().println("    Habitat cells = " + habitatPixelsComp.length + " ");
            solver.log().println("    Non habitat cells = " + nonHabitatPixels.length + " ");
            solver.log().println("    Accessible non habitat cells = " + availablePlanningUnits.length + " ");
            solver.log().println("    Out cells = " + outPixels.length);*/
        }

        ArrayList<Node> hananNodes = hananGrid.getNodes();
        int N = hananNodes.size();
        System.out.println("Number of nodes = " + N);
        System.out.println("Number of terminals = " + hananGrid.numTerminal);
        int[] allIdx = IntStream.range(0, hananNodes.size())
                .toArray();
        int[] terminalsIdx = IntStream.range(0, hananNodes.size())
                .filter(i -> hananNodes.get(i).getType() == 0)
                .toArray();
        int[] steinerPointsIdx = IntStream.range(0, hananNodes.size())
                .filter(i -> hananNodes.get(i).getType() == 1)
                .toArray();
        System.out.println("Number of terminals (verif) = " + terminalsIdx.length);
        System.out.println("Number of steiner points = " + steinerPointsIdx.length);
        System.out.println("Number of edges = " + hananGrid.getNbEdges());

        // Map pixel to idx to identify nodes by idx
        Map<Pixel, Integer> pixelToIdx = new HashMap<>();
        for (int i = 0; i < hananNodes.size(); i++) {
            pixelToIdx.put(hananNodes.get(i).getPixel(), i);
        }

        UndirectedGraph GLB = GraphFactory.makeStoredUndirectedGraph(model, N, SetType.BIPARTITESET, SetType.SMALLBIPARTITESET);
        for (int i : terminalsIdx) {
            GLB.addNode(i);
        }
        boolean[][] checkedEdges = new boolean[N][N];
        UndirectedGraph GUB = GraphFactory.makeStoredUndirectedGraph(model, N, SetType.BIPARTITESET, SetType.SMALLBIPARTITESET);
        for (int i : allIdx) {
            GUB.addNode(i);
        }
        ArrayList<Integer> weightsList = new ArrayList<>();
        ArrayList<int[]> wedgesList = new ArrayList<>();
        ArrayList<BoolVar> bedgesList = new ArrayList<>();
        for (int i : allIdx) {
            for (Edge e : hananNodes.get(i).getEdges()) {
                int j = pixelToIdx.get(e.destination.getPixel());
                GUB.addEdge(i, j);
                if (!checkedEdges[i][j] && !checkedEdges[j][i]) {
                    checkedEdges[i][j] = true;
                    weightsList.add(e.weight);
                    wedgesList.add(new int[] {i, j});
                    bedgesList.add(model.boolVar("edge(" + i + ", " + j + ")"));
                }
            }
        }

        edgesWeights = weightsList.stream().mapToInt(v -> v).toArray();
        edgesIdx = wedgesList.stream().toArray(int[][]::new);
        edgesBoolVars = bedgesList.stream().toArray(BoolVar[]::new);

        System.out.println("Number of edges (verif) = " + edgesIdx.length);

        System.out.println("Graph LB : \n" + GLB);
        System.out.println("\n ----- ");
        System.out.println("Graph UB : \n" + GUB);

        this.habitatGraphVar = model.graphVar(
                "habitatGraph",
                GLB,
                GUB
        );

        this.nbPatchesInitial = hananGrid.numTerminal;

        if (verbose) {
            solver.log().println("Nb patches initial: " + nbPatchesInitial);
        }

        this.nbPatches = model.intVar(0, GUB.getNodes().size());
        model.nbConnectedComponents(habitatGraphVar, nbPatches).post();

        for (int i = 0; i < edgesIdx.length; i++) {
            model.edgeChanneling(habitatGraphVar, edgesBoolVars[i], edgesIdx[i][0], edgesIdx[i][1]).post();
        }

        this.budgetEdges = model.intVar("Budget edges", 0, MAX_BUDGET_CELLS);
        model.scalar(edgesBoolVars, edgesWeights, "=", budgetEdges).post();

        nodesBoolVars = model.boolVarArray(steinerPointsIdx.length);
        nodesIdx = new int[nodesBoolVars.length];
        int[] nodesWeights = IntStream.range(0, nodesBoolVars.length).map(i -> 1).toArray();
        for (int i = 0; i < nodesBoolVars.length; i++) {
            model.nodeChanneling(habitatGraphVar, nodesBoolVars[i], steinerPointsIdx[i]).post();
            nodesIdx[i] = steinerPointsIdx[i];
        }
        this.budgetNodes = model.intVar("Budget nodes", 0, MAX_BUDGET_CELLS);
        model.scalar(nodesBoolVars, nodesWeights, "=", budgetNodes).post();

        this.totalBudget = model.intVar(0, MAX_BUDGET_CELLS);
        model.arithm(budgetEdges, "+", budgetNodes, "=", totalBudget).post();
        model.arithm(totalBudget, "<=", MAX_BUDGET_CELLS).post();

        decisionVars = ArrayUtils.concat(nodesBoolVars, edgesBoolVars);
    }

    public static void main(String[] args) throws IOException {
        SpatialPlanningModelPreprocessed sp = new SpatialPlanningModelPreprocessed(1, true, null);
        Solver s = sp.model.getSolver();
        s.setSearch(Search.inputOrderLBSearch(sp.decisionVars));
        s.limitTime("1m");
        s.showStatistics();
        Solution sol = s.findOptimalSolution(sp.nbPatches, false);
        System.out.println("FINAL COST = " + sol.getIntVal(sp.totalBudget));
        SolutionExporterPreprocessed solExp = new SolutionExporterPreprocessed(sp, sol,
                "/home/justeau-allaire/latex/OPTIMIZING_GRAPH_CONNECTIVITY/TEST_RESULTS/res.csv",
                "/home/justeau-allaire/latex/OPTIMIZING_GRAPH_CONNECTIVITY/TEST_RESULTS/res.tif", -1);
        solExp.export(true);
        //System.out.println(sp.hananGrid);
    }
}
