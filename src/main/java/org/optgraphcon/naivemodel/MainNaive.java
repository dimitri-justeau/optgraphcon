package org.optgraphcon.naivemodel;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;
import org.optgraphcon.DataLoader;
import org.optgraphcon.RasterDataLoader;

import java.io.IOException;

/**
 * Main class to run the naive problem from an IDE
 */
public class MainNaive {

    String basePath;
    DataLoader dataLoader;

    // 70ha converted in 30mx30m pixels
    static final int MAX_BUDGET_CELLS = 787;

    public MainNaive(int agg) throws IOException {
        String instanceName = "agg_" + (agg*30) + "x" + (agg*30) + "/";
        this.basePath = getClass().getClassLoader().getResource("kaala/" + instanceName).getPath();
        this.dataLoader = new RasterDataLoader(
                basePath + "habitat.tif",
                basePath + "locked_out.tif",
                basePath + "restorable.tif",
                basePath + "cell_area.tif"
        );
    }

    public void run(int agg, String destBasePath, String timeLimit) throws IOException {
        SpatialPlanningModelNaive spatialPlanningModelNaive = new SpatialPlanningModelNaive(this.dataLoader, 0, true, null);
        Solver solver = spatialPlanningModelNaive.model.getSolver();
        solver.showStatistics();
        solver.log().println(" -------------------- Agg " + (agg*30) + "x" + (agg*30) + " --------------------");
        // Use a boolset view on graph nodes to use generic heuristics
        BoolVar[] nodes = spatialPlanningModelNaive.model.setBoolsView(spatialPlanningModelNaive.nodes, spatialPlanningModelNaive.habitatGraphVar.getNbMaxNodes(), 0);
        // Deterministic search strategy
        spatialPlanningModelNaive.model.getSolver().setSearch(Search.inputOrderLBSearch(nodes));
        solver.log().println("Max budget = " + MAX_BUDGET_CELLS);
        spatialPlanningModelNaive.postBudgetConstraint(0, MAX_BUDGET_CELLS, 0.7);
        solver.limitTime(timeLimit);
        //solver.showShortStatistics();
        Solution s = solver.findOptimalSolution(spatialPlanningModelNaive.nbPatches, false);
        solver.log().println("-> Nb patches final = " + s.getIntVal(spatialPlanningModelNaive.nbPatches));
        solver.log().println("-> Budget = " + s.getIntVal(spatialPlanningModelNaive.minRestore));
        solver.log().println("-> Search state = " + solver.getSearchState());
        SolutionExporterNaive exp = new SolutionExporterNaive(spatialPlanningModelNaive, s, destBasePath + "agg" + agg + ".tif", -1);
        exp.export(true);
    }

    public static void main(String[] args) throws IOException {
        int agg = 10;
        MainNaive main = new MainNaive(agg);
        main.run(agg, "<path_to_results>", "10h");
    }
}