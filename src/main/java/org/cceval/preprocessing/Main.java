package org.cceval.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    public static void main(String[] args) {

        int[][] instance = instance2();
        Grid grid = new Grid(instance);
//        grid.prepareGrid();
//        //System.out.println(grid);
//        grid.fillingProcedure();
        grid.processInstance();
        System.out.println(grid);
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