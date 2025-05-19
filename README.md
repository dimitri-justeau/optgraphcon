Source code and datasets associated with the manuscript:

**Towards the 30 by 30 Kunming-Montreal Global Biodiversity Framework Target:
Optimizing Graph Connectivity in Constraint-based Spatial Planning**

The experiment dataset is located in `src.main.resources.kaala`.

The "naive" model is in the `org.optgraphcon.naivemodel` package and can be launched with the `MainNaive`
class (located in the same package).

The "preprocessed" model is in the `org.optgraphcon.preprocessedmodel` and can be launched with the `MainNaive` 
class (located in the same package)

The preprocessing method based on Hanan grids is in the `org.optgraphcon.preprocessing` package.

Note that the following classes/packages were adapted from the restopt open-source (GNU GPLv3) project (https://github.com/dimitri-justeau/restopt):

- All files in the `org.optgraphcon.grid` package.
- The `org.optgraphcon.naivemodel.SpatialPlanningModelNaive` class.
- The `org.optgraphcon.DataLoader` class.
- The `org.optgraphcon.RasterDataLoader` class.
- The `org.optgraphcon.RasterConnectivityFinder` class.
- The `org.optgraphcon.RasterReader` class.
