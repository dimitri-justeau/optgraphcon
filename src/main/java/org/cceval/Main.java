package org.cceval;

import org.chocosolver.solver.Solver;

import java.io.IOException;

public class Main {

    String habitatRasterPath;

    public Main() {
        this.habitatRasterPath = getClass().getClassLoader().getResource("kaala/forest_2021_agg20.tif").getPath();
    }

    public static void main(String[] args) throws IOException {
        Main main = new Main();
        RasterDataLoader data = new RasterDataLoader(main.habitatRasterPath, 0);
        SpatialPlanningModel spatialPlanningModel = new SpatialPlanningModel(data, 1, 0, true);
        int target = (int) Math.ceil(spatialPlanningModel.landscapeArea * 0.3) - spatialPlanningModel.initialHabitatArea;
        System.out.println("Restore target = " + target);
        spatialPlanningModel.postBudgetConstraint(target, target);
        Solver solver = spatialPlanningModel.model.getSolver();
        solver.limitTime("10m");
        solver.showStatistics();
        solver.findOptimalSolution(spatialPlanningModel.nbPatches, false);
    }
}