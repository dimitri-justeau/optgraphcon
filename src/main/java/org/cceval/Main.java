package org.cceval;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;

import java.io.*;
import java.util.stream.IntStream;

public class Main {

    String basePath;
    String habitatRasterPath;
    String restorableRasterPath;
    String cellAreaRasterPath;
    String accessibleRasterPath;
    DataLoader dataLoader;

    // 70ha converted in 30mx30m pixels
    static final int MAX_BUDGET_CELLS = 787;

    public Main(int agg, String inputBasePath) throws IOException {
        this.basePath = inputBasePath + "agg_" + (agg*30) + "x" + (agg*30) + "/";
        this.habitatRasterPath = basePath + "habitat.tif";
        this.restorableRasterPath = basePath + "restorable.tif";
        this.cellAreaRasterPath = basePath + "cell_area.tif";
        this.accessibleRasterPath = basePath + "locked_out.tif";
        this.dataLoader = new RasterDataLoader(habitatRasterPath, accessibleRasterPath, restorableRasterPath, cellAreaRasterPath);
    }

    public void run(int agg, String destBasePath, String timeLimit) throws IOException {
        String logFilePath = destBasePath + "log/agg" + agg + ".log";
        SpatialPlanningModel spatialPlanningModel = new SpatialPlanningModel(this.dataLoader, 0, true, logFilePath);
        Solver solver = spatialPlanningModel.model.getSolver();
        solver.showStatistics();
        solver.log().println(" -------------------- Agg " + (agg*30) + "x" + (agg*30) + " --------------------");
        // Use a boolset view on graph nodes to use generic heuristics
        BoolVar[] nodes = spatialPlanningModel.model.setBoolsView(spatialPlanningModel.nodes, spatialPlanningModel.habitatGraphVar.getNbMaxNodes(), 0);
        // Deterministic search strategy
        spatialPlanningModel.model.getSolver().setSearch(Search.inputOrderLBSearch(nodes));
        solver.log().println("Max budget = " + MAX_BUDGET_CELLS);
        spatialPlanningModel.postBudgetConstraint(0, MAX_BUDGET_CELLS, 0.7);
        solver.limitTime(timeLimit);
        //solver.showShortStatistics();
        Solution s = solver.findOptimalSolution(spatialPlanningModel.nbPatches, false);
        solver.log().println("-> Nb patches final = " + s.getIntVal(spatialPlanningModel.nbPatches));
        solver.log().println("-> Budget = " + s.getIntVal(spatialPlanningModel.minRestore));
        solver.log().println("-> Search state = " + solver.getSearchState());
        SolutionExporter exp = new SolutionExporter(spatialPlanningModel, s, "", destBasePath + "agg" + agg + ".tif", -1);
        exp.export(true);
    }

    public static void main(String[] args) throws IOException {
        String inputBasePath = args[0];
        String destBasePath = args[1];
        String timeLimit = args[2];
        IntStream.range(1, 11)
                .parallel()
                .forEach(i -> {
                    try {
                        Main main = new Main(i, inputBasePath);
                        main.run(i, destBasePath, timeLimit);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
/*        for (int agg = 2; agg >= 1; agg--) {
            Main main = new Main(agg, inputBasePath);
            main.run(agg, destBasePath, timeLimit);
        }*/
    }
}