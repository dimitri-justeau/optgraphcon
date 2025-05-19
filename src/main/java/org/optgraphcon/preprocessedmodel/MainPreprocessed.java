package org.optgraphcon.preprocessedmodel;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;

import java.io.IOException;

/**
 * Main class to run the naive problem from an IDE
 */
public class MainPreprocessed {

    // 70ha converted in 30mx30m pixels
    static final int MAX_BUDGET_CELLS = 787;

    public static void main(String[] args) throws IOException {
        String sagg = "null";
        String sbasePath = "null";
        String slogFilePath = "null";
        if (args.length == 3) {
            sagg = args[0];
            sbasePath = args[1];
            slogFilePath = args[2];
        }
        int agg = 1;
        String basePath = null;
        String logFilePath = null;
        if (!sagg.equals("null")) {
            agg = Integer.parseInt(sagg);
        }
        if (!sbasePath.equals("null")) {
            basePath = sbasePath;
        }
        if (!slogFilePath.equals("null")) {
            logFilePath = slogFilePath;
        }
        SpatialPlanningModelPreprocessed sp = new SpatialPlanningModelPreprocessed(agg, basePath, true, logFilePath);
        sp.postBudgetConstraint(0, MAX_BUDGET_CELLS);
        Solver s = sp.model.getSolver();
        s.setSearch(Search.inputOrderLBSearch(sp.decisionVars));
        s.limitTime("10h");
        s.showStatistics();
        Solution sol = s.findOptimalSolution(sp.nbPatches, false);
        System.out.println("FINAL COST = " + sol.getIntVal(sp.totalBudget));
        SolutionExporterPreprocessed solExp = new SolutionExporterPreprocessed(
                sp,
                sol,
                "<destination of geotif>",
                -1);
        solExp.export(true);
    }
}