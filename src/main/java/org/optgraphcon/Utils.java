package org.optgraphcon;

import org.optgraphcon.grid.regular.square.RegularSquareGrid;

import java.io.IOException;
import java.util.stream.IntStream;

public class Utils {

    private String basePath;
    private DataLoader dataLoader;

    public static final int ACCESSIBLE_VALUE = 0;

    public Utils(int agg) throws IOException {
        String instanceName = "agg_" + (agg*30) + "x" + (agg*30) + "/";
        basePath = getClass().getClassLoader().getResource("kaala/" + instanceName).getPath();
        dataLoader = new RasterDataLoader(
                basePath + "habitat.tif",
                basePath + "locked_out.tif",
                basePath + "restorable.tif",
                basePath + "cell_area.tif"
        );
    }

    public Utils(int agg, String basePath) throws IOException {
        String instanceName = "agg_" + (agg*30) + "x" + (agg*30) + "/";
        dataLoader = new RasterDataLoader(
                basePath + instanceName + "habitat.tif",
                basePath + instanceName + "locked_out.tif",
                basePath + instanceName + "restorable.tif",
                basePath + instanceName + "cell_area.tif"
        );
    }

    public static int[][] getInstanceAsMatrix(DataLoader dataLoader, int accessibleVal) {
        int[] flatMatrix = IntStream.range(0, dataLoader.getHabitatData().length)
                .map(i -> {
                    // Habitat pixels
                    if (dataLoader.getHabitatData()[i] == 1) {
                        return 1;
                    // Non habitat accessible
                    } else if (dataLoader.getHabitatData()[i] == 0 && dataLoader.getAccessibleData()[i] == accessibleVal) {
                        return 0;
                    // Either out or non accessible -> obstacle
                    } else {
                        return 2;
                    }
                }).toArray();
        RegularSquareGrid grid = new RegularSquareGrid(dataLoader.getHeight(), dataLoader.getWidth());
        int[][] matrix = new int[dataLoader.getHeight()][dataLoader.getWidth()];
        for (int row = 0; row < dataLoader.getHeight(); row++) {
            for (int col = 0; col < dataLoader.getWidth(); col++) {
                matrix[row][col] = flatMatrix[grid.getIndexFromCoordinates(row, col)];
            }
        }
        return matrix;
    }

    public static int[][] getInstanceAsMatrixWithBoundary(DataLoader dataLoader, int accessibleVal) {
        int[][] matrix = getInstanceAsMatrix(dataLoader, accessibleVal);
        int[][] boundaryMatrix = new int[dataLoader.getHeight() + 2][dataLoader.getWidth() + 2];
        for (int row = 0; row < dataLoader.getHeight() + 2; row++) {
            for (int col = 0; col < dataLoader.getWidth() + 2; col ++) {
                if (row == 0 || row == dataLoader.getHeight() + 1 || col == 0 || col == dataLoader.getWidth() + 1) {
                    boundaryMatrix[row][col] = 2;
                }
                else {
                    boundaryMatrix[row][col] = matrix[row - 1][col - 1];
                }
            }
        }
        return boundaryMatrix;
    }

    public static void printMatrix(int[][] matrix) {
        String header = "  -";
        for (int i = 0; i < matrix[0].length - 1; i++) {
            header += " -";
        }
        System.out.println(header);
        for (int row = 0; row < matrix.length; row++) {
            String l = "| ";
            for (int col = 0; col < matrix[0].length; col++) {
                l += matrix[row][col] + " ";
            }
            System.out.println(l + "|");
        }
        System.out.println(header);
    }

    public static DataLoader getDataLoaderOfInstance(int agg) throws IOException {
        assert agg >= 1 && agg <= 10;
        Utils utils = new Utils(agg);
        return utils.dataLoader;
    }

    public static DataLoader getDataLoaderOfInstance(int agg, String basePath) throws IOException {
        assert agg >= 1 && agg <= 10;
        Utils utils = new Utils(agg, basePath);
        return utils.dataLoader;
    }

    public static int[][] getMatrixWithBoundaryOfInstance(int agg, String basePath) throws IOException {
        return getInstanceAsMatrixWithBoundary(getDataLoaderOfInstance(agg, basePath), ACCESSIBLE_VALUE);
    }

    public static int[][] getMatrixWithBoundaryOfInstance(int agg) throws IOException {
        return getInstanceAsMatrixWithBoundary(getDataLoaderOfInstance(agg), ACCESSIBLE_VALUE);
    }

    public static void main(String[] args) throws IOException {
        int[][] matrix = getMatrixWithBoundaryOfInstance(1);
        printMatrix(matrix);
    }
}
