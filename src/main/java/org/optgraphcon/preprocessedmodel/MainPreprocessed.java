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
        SpatialPlanningModelPreprocessed sp = new SpatialPlanningModelPreprocessed(1, true, null);
        sp.postBudgetConstraint(0, MAX_BUDGET_CELLS);
        Solver s = sp.model.getSolver();
        s.setSearch(Search.inputOrderLBSearch(sp.decisionVars));
        s.limitTime("1m");
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