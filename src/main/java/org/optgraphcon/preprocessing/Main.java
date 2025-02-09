package org.optgraphcon.preprocessing;

import org.optgraphcon.Utils;

import java.io.IOException;

/**
 * Test class for the preprocessing based on Hanan grids.
 */
public class Main {

    public static void main(String[] args) throws IOException {

        long t = System.currentTimeMillis();
        int[][] instance = Utils.getMatrixWithBoundaryOfInstance(1);
        HananGrid hananGrid = new HananGrid(instance);
        hananGrid.processInstance();
        System.out.println(hananGrid);
        System.out.println("PREPROCESSING_TIME = " + ((System.currentTimeMillis() - t) * 0.001) + "s");
    }

    public static int[][] instance1() {
        int[][] instance = new int[][]{
                new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2},
                new int[]{2, 2, 0, 1, 1, 1, 0, 0, 0, 2},
                new int[]{2, 2, 2, 1, 2, 0, 0, 0, 0, 2},
                new int[]{2, 1, 0, 1, 0, 2, 0, 0, 0, 2},
                new int[]{2, 0, 0, 0, 2, 0, 2, 0, 0, 2},
                new int[]{2, 0, 0, 2, 0, 0, 2, 0, 0, 2},
                new int[]{2, 0, 0, 0, 2, 2, 0, 0, 0, 2},
                new int[]{2, 0, 0, 0, 0, 0, 0, 0, 1, 2},
                new int[]{2, 0, 0, 0, 0, 0, 0, 0, 0, 2},
                new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2}
        };
        return instance;
    }

    public static int[][] instance2() {
        int[][] instance = new int[][]{
                new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2},
                new int[]{2, 2, 0, 0, 0, 0, 1, 1, 0, 2},
                new int[]{2, 0, 2, 0, 0, 2, 1, 1, 0, 2},
                new int[]{2, 0, 0, 0, 2, 1, 0, 2, 0, 2},
                new int[]{2, 1, 0, 1, 2, 2, 2, 0, 0, 2},
                new int[]{2, 0, 0, 1, 0, 0, 0, 2, 1, 2},
                new int[]{2, 0, 0, 0, 2, 2, 0, 0, 1, 2},
                new int[]{2, 0, 0, 1, 0, 0, 2, 0, 1, 2},
                new int[]{2, 0, 1, 0, 0, 0, 0, 1, 1, 2},
                new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2}
        };
        return instance;
    }

    public static int[][] instance3() {
        int[][] instance = new int[][]{
                new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2},
                new int[]{2, 2, 2, 0, 0, 0, 1, 1, 0, 2},
                new int[]{2, 2, 2, 0, 2, 2, 0, 1, 0, 2},
                new int[]{2, 0, 0, 0, 2, 2, 0, 0, 0, 2},
                new int[]{2, 1, 0, 1, 2, 2, 0, 0, 0, 2},
                new int[]{2, 0, 0, 1, 0, 0, 0, 0, 0, 2},
                new int[]{2, 0, 2, 0, 2, 2, 2, 0, 0, 2},
                new int[]{2, 0, 2, 0, 1, 2, 2, 0, 0, 2},
                new int[]{2, 0, 0, 0, 2, 2, 2, 0, 0, 2},
                new int[]{2, 0, 0, 0, 2, 2, 2, 0, 0, 2},
                new int[]{2, 0, 0, 1, 2, 2, 2, 0, 1, 2},
                new int[]{2, 0, 1, 0, 0, 0, 0, 1, 1, 2},
                new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2}
        };
        return instance;
    }
}