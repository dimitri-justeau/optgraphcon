package org.cceval;

import org.chocosolver.solver.Solution;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.cceval.grid.regular.square.GroupedGrid;

import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;


public class SolutionExporter {

    public String csvDest;
    public String rastDest;
    public String template;
    public int[] sites;
    public int[] completeData;
    public SpatialPlanningModelNaive problem;
    public Solution solution;

    public SolutionExporter(SpatialPlanningModelNaive problem, Solution solution, String csvDest, String rastDest, double noDataValue) {
        this.solution = solution;
        this.problem = problem;
        if (problem.dataLoader instanceof RasterDataLoader) {
            RasterDataLoader dataLoader = (RasterDataLoader) problem.dataLoader;
            GroupedGrid grid = problem.grid;
            sites = new int[grid.getNbUngroupedCells()];
            ISet set = SetFactory.makeConstantSet(solution.getSetVal(problem.nodes));
            for (int i = 0; i < grid.getNbUngroupedCells(); i++) {
                if (grid.getGroupIndexFromPartialIndex(i) < grid.getNbGroups()) {
                    sites[i] = 2;
                } else if (set.contains(grid.getGroupIndexFromPartialIndex(i))) {
                    sites[i] = 3;
                } else {
                    sites[i] = 1;
                }
            }
            this.template = dataLoader.getHabitatRasterPath();
            this.csvDest = csvDest;
            this.rastDest = rastDest;
            completeData = new int[grid.getNbRows() * grid.getNbCols()];
            for (int i = 0; i < completeData.length; i++) {
                if (grid.getDiscardSet().contains(i)) {
                    if (problem.dataLoader.getHabitatData()[i] == 0) {
                        completeData[i] = 0;
                    } else {
                        completeData[i] = (int) noDataValue;
                    }
                } else {
                    completeData[i] = sites[grid.getPartialIndex(i)];
                }
            }
        }
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
