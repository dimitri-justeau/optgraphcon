package org.cceval;

import org.cceval.grid.neighborhood.regular.square.partial.PartialFourConnected;
import org.cceval.grid.regular.square.PartialRegularSquareGrid;
import org.chocosolver.solver.Solution;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.chocosolver.util.tools.ArrayUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.cceval.grid.regular.square.GroupedGrid;

import java.awt.*;
import java.awt.image.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class SolutionExporter {

    public String csvDest;
    public String rastDest;
    public String template;
    public int[] sites;
    public int[] completeData;
    public SpatialPlanningModel problem;
    public Solution solution;

    public SolutionExporter(SpatialPlanningModel problem, Solution solution, String csvDest, String rastDest, double noDataValue) {
        this.solution = solution;
        this.problem = problem;
        if (problem.dataLoader instanceof RasterDataLoader) {
            RasterDataLoader dataLoader = (RasterDataLoader) problem.dataLoader;
            PartialRegularSquareGrid grid = problem.grid;
            sites = new int[grid.getNbCells()];
            ISet nodes = SetFactory.makeConstantSet(solution.getSetVal(problem.nodes));
            for (int i = 0; i < problem.habitatGraphVar.getNbMaxNodes(); i++) {
                if (problem.dataLoader.getHabitatData()[grid.getCompleteIndex(i)] == 1) {
                    sites[i] = 2;
                } else if (nodes.contains(i)) {
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
                if (problem.dataLoader.getHabitatData()[i] == 4) {
                    completeData[i] = 0;
                }
            }
        }
    }

/*    public void exportCharacteristics() throws IOException {
        String[][] orderedCharacteristics = new String[2][];
        String[] allKeys = ArrayUtils.append(solution.KEYS, solution.getObjective().getAdditionalKeys());
        orderedCharacteristics[0] = allKeys;
        orderedCharacteristics[1] = new String[allKeys.length];
        for (int i = 0; i < allKeys.length; i++) {
            orderedCharacteristics[1][i] = solution.getCharacteristics().get(allKeys[i]);
        }
        BufferedWriter br = new BufferedWriter(new FileWriter(csvDest));
        StringBuilder sb = new StringBuilder();
        for (String[] line : orderedCharacteristics) {
            int i = 0;
            for (String s : line) {
                i++;
                sb.append(s);
                if (i < line.length) {
                    sb.append(",");
                }
            }
            sb.append("\n");
        }
        br.write(sb.toString());
        br.close();
    }*/

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
            System.out.println("\nRaster exported at " + rastDest + ".tif");
            //System.out.println("Solution characteristics exported at " + csvDest + ".csv\n");
        }
    }
}
