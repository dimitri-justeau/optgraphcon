package org.cceval;

import org.cceval.grid.regular.square.GroupedGrid;
import org.cceval.grid.regular.square.RegularSquareGrid;
import org.cceval.preprocessing.Pixel;
import org.chocosolver.solver.Solution;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;

import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;


public class SolutionExporterPreprocessed {

    public String csvDest;
    public String rastDest;
    public String template;
    public int[] sites;
    public int[] completeData;
    public SpatialPlanningModelPreprocessed problem;
    public Solution solution;

    public SolutionExporterPreprocessed(SpatialPlanningModelPreprocessed problem, Solution solution, String csvDest, String rastDest, double noDataValue) {
        int checkFinalCost = 0;
        this.solution = solution;
        this.problem = problem;
        if (problem.dataLoader instanceof RasterDataLoader) {
            RasterDataLoader dataLoader = (RasterDataLoader) problem.dataLoader;
            RegularSquareGrid grid = new RegularSquareGrid(dataLoader.getHeight(), dataLoader.getWidth());
            this.template = dataLoader.getHabitatRasterPath();
            this.csvDest = csvDest;
            this.rastDest = rastDest;
            completeData = new int[dataLoader.getHeight() * dataLoader.getWidth()];
            for (int i = 0; i < completeData.length; i++) {
                if (dataLoader.getHabitatData()[i] == 1) {
                    completeData[i] = 2;
                } else if (dataLoader.getHabitatData()[i] == 0 && dataLoader.getHabitatData()[i] == 0) {
                    completeData[i] = 1;
                } else if (dataLoader.getHabitatData()[i] <= -1) {
                    completeData[i] = (int) noDataValue;
                } else {
                    completeData[i] = 0;
                }
            }
            // Check steiner points
            for (int i = 0; i < problem.nodesIdx.length; i++) {
                if (solution.getIntVal(problem.nodesBoolVars[i]) == 1) {
                    Pixel pixel = problem.hananGrid.getNodes().get(problem.nodesIdx[i]).getPixel();
                    completeData[grid.getIndexFromCoordinates(pixel.x - 1, pixel.y - 1)] = 3;
                    checkFinalCost += 1;
                }
            }
            // Check edges
            ShortestPathFinder shortestPathFinder = new ShortestPathFinder(dataLoader, 0);
            for (int i = 0; i < problem.edgesIdx.length; i++) {
                if (solution.getIntVal(problem.edgesBoolVars[i]) == 1) {
                    int from = problem.edgesIdx[i][0];
                    int to = problem.edgesIdx[i][1];
                    Pixel pixFrom = problem.hananGrid.getNodes().get(from).getPixel();
                    Pixel pixTo = problem.hananGrid.getNodes().get(to).getPixel();
                    int[] path = shortestPathFinder.getShortestPath(pixFrom, pixTo);
                    if (path != null) {
                        for (int v : path) {
                            completeData[v] = 3;
                            checkFinalCost += 1;
                        }
                    }
                }
            }
        }
        System.out.println("FINAL COST (verif) = " + checkFinalCost);
    }

    public void generateRaster() throws IOException {
        File file = new File(template);
        GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D grid = reader.read(null);
        int height = grid.getRenderedImage().getHeight();
        int width = grid.getRenderedImage().getWidth();

        GeoTiffWriter writer = new GeoTiffWriter(new File(rastDest));

        DataBuffer buff = grid.getRenderedImage().getData().getDataBuffer();
        SampleModel sm = new BandedSampleModel(DataBuffer.TYPE_INT, width, height, 1);
        WritableRaster rast = Raster.createWritableRaster(sm, buff, new Point(0, 0));
        rast.setPixels(0, 0, width, height, completeData);

        GridCoverageFactory f = new GridCoverageFactory();
        GridCoverage2D destCov = f.create("rast", rast, grid.getEnvelope());

        writer.write(destCov, null);
    }

    public void export(boolean verbose) throws IOException {
        //exportCharacteristics();
        generateRaster();
        if (verbose) {
            //this.solution.printSolutionInfos();
            this.problem.model.getSolver().log().println("\nRaster exported at " + rastDest);
            //System.out.println("Solution characteristics exported at " + csvDest + ".csv\n");
        }
    }
}
