package org.cceval;

import java.io.IOException;
import java.util.stream.IntStream;

/**
 * Class for loading and accessing problem's data.
 */
public class RasterDataLoader extends DataLoader {

    private final String habitatRasterPath;

    public RasterDataLoader(String habitatRasterPath, int accessibleVal) throws IOException {
        super(new RasterReader(habitatRasterPath).readAsIntArray(), accessibleVal, -1, new RasterReader(habitatRasterPath).getWidth(), new RasterReader(habitatRasterPath).getHeight());
        this.habitatRasterPath = habitatRasterPath;
        RasterReader rasterHabitat = new RasterReader(habitatRasterPath);
        this.width = rasterHabitat.getWidth();
        this.height = rasterHabitat.getHeight();
        this.noDataHabitat = rasterHabitat.getNoDataValue();
    }

    public RasterDataLoader(String habitatRasterPath, String accessibleRasterPath, int accessibleVal, double noDataValue) throws IOException {
        super(
                new RasterReader(habitatRasterPath).readAsIntArray(),
                new RasterReader(accessibleRasterPath).readAsIntArray(),
                noDataValue,
                new RasterReader(habitatRasterPath).getWidth(), new RasterReader(habitatRasterPath).getHeight()
        );
        this.habitatRasterPath = habitatRasterPath;
    }

    public RasterDataLoader(String habitatRasterPath, String accessibleRasterPath, String restorableRasterPath,
                            String cellAreaRasterPath) throws IOException {
        super(
                new RasterReader(habitatRasterPath).readAsIntArray(),
                new RasterReader(accessibleRasterPath).readAsIntArray(),
                new RasterReader(restorableRasterPath).readAsDoubleArray(),
                new RasterReader(cellAreaRasterPath).readAsIntArray()
        );
        this.habitatRasterPath = habitatRasterPath;
        RasterReader rasterHabitat = new RasterReader(habitatRasterPath);
        this.width = rasterHabitat.getWidth();
        this.height = rasterHabitat.getHeight();
        this.noDataHabitat = rasterHabitat.getNoDataValue();
    }

    public String getHabitatRasterPath() {
        return habitatRasterPath;
    }
}
