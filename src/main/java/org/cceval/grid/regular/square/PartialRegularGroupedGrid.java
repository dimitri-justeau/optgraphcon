package org.cceval.grid.regular.square;

import org.cceval.RasterConnectivityFinder;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class PartialRegularGroupedGrid extends GroupedGrid {

    private final int[] sizeCells;
    private final int[][] attributesNodes;
    private final int nbGroups;
    private final ISet[] groups;
    private final int nbGroupedCells;
    private final int[] unGroupedId;
    private final Map<Integer, Integer> gridIdToGroupedId;
    private RasterConnectivityFinder groupGraph;

    public PartialRegularGroupedGrid(int nbRows, int nbCols, int[] toDiscard, RasterConnectivityFinder groupGraph) {
        this(nbRows, nbCols, toDiscard, groupGraph, 4);
    }

    public PartialRegularGroupedGrid(int nbRows, int nbCols, int[] toDiscard, RasterConnectivityFinder groupGraph, int maxNeighCard) {
        super(nbRows, nbCols, toDiscard);
        this.groupGraph = groupGraph;
        this.nbGroupedCells = groupGraph.getNbNodes();
        this.nbGroups = groupGraph.getNBCC();
        this.sizeCells = new int[super.getNbCells() - nbGroupedCells + nbGroups];
        this.attributesNodes = new int[super.getNbCells() - nbGroupedCells + nbGroups][groupGraph.getNbAttributes()];
        this.groups = new ISet[nbGroups];
        for (int cc = 0; cc < nbGroups; cc++) {
            int sizeCC = groupGraph.getSizeCC()[cc];
            sizeCells[cc] = sizeCC;
            for (int k = 0; k < groupGraph.getNbAttributes(); k++) {
                attributesNodes[cc][k] = groupGraph.getAttributeCC(cc)[k];
            }
            int[] g = groupGraph.getCC(cc);
            for (int i = 0; i < sizeCC; i++) {
                g[i] = getPartialIndex(g[i]);
            }
            groups[cc] = SetFactory.makeConstantSet(g);
        }
        this.gridIdToGroupedId = new HashMap<>();
        for (int cc = 0; cc < nbGroups; cc++) {
            for (int i : groups[cc]) {
                gridIdToGroupedId.put(i, cc);
            }
        }
        this.unGroupedId = new int[getNbCells() - nbGroups];
        int nbNotGrouped = 0;
        for (int i = 0; i < super.getNbCells(); i++) {
            if (!gridIdToGroupedId.containsKey(i)) {
                gridIdToGroupedId.put(i, nbNotGrouped + nbGroups);
                unGroupedId[nbNotGrouped] = i;
                sizeCells[nbNotGrouped + nbGroups] = 1;
                for (int k = 0; k < groupGraph.getNbAttributes(); k++) {
                    attributesNodes[nbNotGrouped + nbGroups][k] = groupGraph.getAttributesCell(i)[k];
                }
                nbNotGrouped++;
            }
        }
    }

    public int[] getAttributesNode(int nodeIndex) {
        return attributesNodes[nodeIndex];
    }

    @Override
    public int getNbCells() {
        return super.getNbCells() - nbGroupedCells + nbGroups;
    }

    public ISet getGroup(int groupId) {
        return groups[groupId];
    }

    public int[] getSizeCells() {
        return sizeCells;
    }

    public int getNbGroups() {
        return nbGroups;
    }

    public int getUngroupedPartialIndex(int groupedIdx) {
        if (groupedIdx < nbGroups) {
            throw new RuntimeException("A grouped cell is not associated to a complete index");
        }
        return unGroupedId[groupedIdx - nbGroups];
    }

    public int getUngroupedCompleteIndex(int groupedIdx) {
        if (groupedIdx < nbGroups) {
            throw new RuntimeException("A grouped cell is not associated to a complete index");
        }
        return getCompleteIndex(unGroupedId[groupedIdx - nbGroups]);
    }

    @Override
    public int[] getUngroupedCompleteIndices(int[] pus) {
        return IntStream.of(pus)
                .map(i -> getUngroupedCompleteIndex(i))
                .toArray();
    }


    public int getGroupIndexFromCompleteIndex(int completeIndex) {
        int partialIndex = getPartialIndex(completeIndex);
        return gridIdToGroupedId.get(partialIndex);
    }

    public int getGroupIndexFromPartialIndex(int partialIndex) {
        return gridIdToGroupedId.get(partialIndex);
    }

    /**
     * @return The cartesian coordinates of the pixels of the org.flsgen.grid.
     */
    public double[][] getCartesianCoordinates() {
        double[][] coords = new double[super.getNbCells()][];
        for (int i = 0; i < super.getNbCells(); i++) {
            int[] coord = super.getCoordinatesFromIndex(i);
            coords[i] = new double[]{coord[1], coord[0]};
        }
        return coords;
    }

    public double[] getCartesianCoordinates(int partialGroupedIndex) throws IOException {
        if (partialGroupedIndex < getNbGroups()) {
            throw new IOException("Cannot associate cartesian coordinates to a grouped cell");
        }
        int[] c = super.getCoordinatesFromIndex(getUngroupedPartialIndex(partialGroupedIndex));
        return new double[] {c[1], c[0]};
    }

    public double[] getCartesianCoordinatesFromPartialIndex(int partialIndex) {
        int[] c = super.getCoordinatesFromIndex(partialIndex);
        return new double[] {c[1], c[0]};
    }
}
