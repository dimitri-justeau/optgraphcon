package org.cceval;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;

import java.io.IOException;
import java.nio.file.Paths;

public class Main {

    String basePath;
    String habitatRasterPath;
    String restorableRasterPath;
    String cellAreaRasterPath;
    String accessibleRasterPath;
    DataLoader dataLoader;

    // 70ha converted in 10mx10m pixels
    static final int MAX_BUDGET_CELLS = 7018;

    public Main(int agg) throws IOException {
        this.basePath = getClass().getClassLoader().getResource("kaala/agg_" + (agg*10) + "x" + (agg*10) + "/").getPath();
        this.habitatRasterPath = basePath + "habitat.tif";
        this.restorableRasterPath = basePath + "restorable.tif";
        this.cellAreaRasterPath = basePath + "cell_area.tif";
        this.accessibleRasterPath = basePath + "locked_out.tif";
        this.dataLoader = new RasterDataLoader(habitatRasterPath, accessibleRasterPath, restorableRasterPath, cellAreaRasterPath);
    }

    public static void main(String[] args) throws IOException {
        String destBasePath = args[0];
        for (int agg = 30; agg >= 1; agg--) {
            System.out.println(" -------------------- Agg " + (agg*10) + "x" + (agg*10) + " --------------------");
            Main main = new Main(agg);
            SpatialPlanningModel spatialPlanningModel = new SpatialPlanningModel(main.dataLoader, 0, true);
            // Use a boolset view on graph nodes to use generic heuristics
            BoolVar[] nodes = spatialPlanningModel.model.setBoolsView(spatialPlanningModel.nodes, spatialPlanningModel.habitatGraphVar.getNbMaxNodes(), 0);
            // Deterministic search strategy
            spatialPlanningModel.model.getSolver().setSearch(Search.inputOrderLBSearch(nodes));
            System.out.println("Max budget = " + MAX_BUDGET_CELLS);
            spatialPlanningModel.postBudgetConstraint(0, MAX_BUDGET_CELLS, 0.7);
            Solver solver = spatialPlanningModel.model.getSolver();
            solver.limitTime("30m");
            solver.showShortStatistics();
            Solution s = solver.findOptimalSolution(spatialPlanningModel.nbPatches, false);
            System.out.println("-> Nb patches final = " + s.getIntVal(spatialPlanningModel.nbPatches));
            System.out.println("-> Budget = " + s.getIntVal(spatialPlanningModel.minRestore));
            System.out.println("-> Search state = " + solver.getSearchState());
            SolutionExporter exp = new SolutionExporter(spatialPlanningModel, s, "", destBasePath + agg + ".tif", -1);
            exp.export(true);
        }
    }
}