package org.cceval;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;

import java.io.IOException;

public class Main {

    String habitatRasterPath;

    public Main() {
        this.habitatRasterPath = getClass().getClassLoader().getResource("kaala/forest_2021_agg16.tif").getPath();
    }

    public static void main(String[] args) throws IOException {
        Main main = new Main();
        RasterDataLoader data = new RasterDataLoader(main.habitatRasterPath, 0);
        SpatialPlanningModel spatialPlanningModel = new SpatialPlanningModel(data, 1, 0, true);
        // Use a boolset view on graph nodes to use generic heuristics
        BoolVar[] nodes = spatialPlanningModel.model.setBoolsView(spatialPlanningModel.nodes, spatialPlanningModel.habitatGraphVar.getNbMaxNodes(), 0);
        spatialPlanningModel.model.getSolver().setSearch(Search.minDomLBSearch(nodes));
        int target = (int) Math.ceil(spatialPlanningModel.landscapeArea * 0.3) - spatialPlanningModel.initialHabitatArea;
        System.out.println("Restore target = " + target);
        spatialPlanningModel.postBudgetConstraint(target, target);
        Solver solver = spatialPlanningModel.model.getSolver();
        solver.limitTime("2m");
        solver.showStatistics();
        Solution s = solver.findOptimalSolution(spatialPlanningModel.nbPatches, false);
        //SolutionExporter exp = new SolutionExporter(spatialPlanningModel, s, "", "/path/to/the/output/tif", -1);
        //exp.export(true);
    }
}