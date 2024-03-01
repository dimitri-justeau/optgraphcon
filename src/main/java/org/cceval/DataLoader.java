package org.cceval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Class for loading and accessing problem's data.
 */
public class DataLoader {

    protected int[] habitatData;
    protected double[] restorableData;
    protected int[] accessibleData;
    protected int[] cellAreaData;
    private List<int[]> features;


    protected int width;
    protected int height;

    protected double noDataHabitat;

    public DataLoader(int[] habitatData, int[] accessibleData, double[] restorableData, int[] cellAreaData) throws IOException {
        this.habitatData = habitatData;
        this.accessibleData = accessibleData;
        this.restorableData = restorableData;
        this.cellAreaData = cellAreaData;
        this.features = new ArrayList<>();
        int n = habitatData.length;
        if (accessibleData.length != n || restorableData.length != n || cellAreaData.length != n) {
            throw new IOException("All input datasets must have the same size");
        }
    }

    public DataLoader(int[] habitatData, int[] accessibleData, double[] restorableData, int[] cellAreaData, int width,
                      int height, double noDataHabitat) throws IOException {
        this(habitatData, accessibleData, restorableData, cellAreaData);
        this.width = width;
        this.height = height;
        this.noDataHabitat = noDataHabitat;
        if (width * height != habitatData.length) {
            throw new IOException("Input width and height do not correspond to dataset size");
        }
    }

    public DataLoader(int[] habitatData, int accessibleData[], double noDataHabitat, int width, int height) throws IOException {
        this(
                habitatData,
                accessibleData,
                DoubleStream.generate(() -> 1).limit(habitatData.length).toArray(),
                IntStream.generate(() -> 1).limit(habitatData.length).toArray(),
                width, height, noDataHabitat
        );
    }

    public DataLoader(int[] habitatData, int accessibleVal, double noDataHabitat, int width, int height) throws IOException {
        this(IntStream.range(0, habitatData.length).map(i -> {
                    if (habitatData[i] == -1) {
                        return -1;
                    }
                    return habitatData[i] == 0 || habitatData[i] > 1 ? 0 : 1;
                }).toArray(),
                IntStream.range(0, habitatData.length).map(i -> habitatData[i] == accessibleVal ? accessibleVal : accessibleVal - 1).toArray(),
                DoubleStream.generate(() -> 1).limit(habitatData.length).toArray(),
                IntStream.generate(() -> 1).limit(habitatData.length).toArray(),
                width, height, noDataHabitat);
    }

    public int[] getNoDataPixels() {
        return IntStream.range(0, getHabitatData().length)
                .filter(i -> getHabitatData()[i] <= -1 || getHabitatData()[i] == noDataHabitat)
                .toArray();

    }

    public int[] getPixelsByValue(int value) {
        return IntStream.range(0, getHabitatData().length)
                .filter(i -> getHabitatData()[i] == value)
                .toArray();
    }

    public void addFeature(int[] featureData) {
        this.features.add(featureData);
    }

    public List<int[]> getFeatures() {
        return features;
    }

    public int[] getHabitatData() {
        return habitatData;
    }

    public double[] getRestorableData() {
        return restorableData;
    }

    public int[] getAccessibleData() {
        return accessibleData;
    }

    public int[] getCellAreaData() {
        return cellAreaData;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public double getNoDataValue() {
        return noDataHabitat;
    }
}
