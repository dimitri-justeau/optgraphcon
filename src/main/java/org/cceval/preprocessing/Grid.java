package org.cceval.preprocessing;

import java.util.*;

public class Grid {
    public final int N;
    public final int M;
    public int[][] grid;
    public int numTerminal;
    public int numObstacle;
    private int[][] terminalBelonging;
    private ArrayList<Pixel> terminalRepresentative;
    private int[][] obstacleBelonging;
    private ArrayList<ArrayList<Pixel>> terminalConvexCorners; // The list of convex corners of each polygonal terminal
    private ArrayList<ArrayList<Pixel>> obstacleConvexCorners; // The list of convex corners of each polygonal obstacle
    private BitSet[][] status;
    private ArrayList<Node> graph;
    private int numNodes;
    private int numEdges;
    private Node[][] pixelNode;
    private HashMap<Node, ArrayList<Node>> targetNodes;

    public Grid(int[][] grid) {
        this.N = grid.length;
        this.M = grid[0].length;
        this.grid = grid;
    }

    public void processInstance() {
        prepareGrid();
        fillingProcedure();
        prepareGrid();
        drawComplexRectilinearGrid();
        getGraphFromGrid();
        graphSimplification();
    }

    @Override
    public String toString() {
        String res = "Dimensions : " + N + "," + M + "\n";
        int s = 0;
        for (ArrayList<Pixel> cc : terminalConvexCorners) {
            s += cc.size();
        }
        res += terminalConvexCorners.size() + " terminals, with " + s + " convex corners\n";
        s = 0;
        for (ArrayList<Pixel> cc : obstacleConvexCorners) {
            s += cc.size();
        }
        res += obstacleConvexCorners.size() + " obstacles, with " + s + " convex corners\n";

        for (int i = 0; i < N; i++) {
            String line = "";
            for (int j = 0; j < M; j++) {
                if (status[i][j].get(Pixel.OBSTACLE)) {
                    line += "X";
                }
                else if (status[i][j].get(Pixel.TERMINAL)) {
                    line += "O";
                }
                else if (status[i][j].get(Pixel.FREE)) {
                    if (status[i][j].get(Pixel.HORIZONTAL_LINE) && status[i][j].get(Pixel.VERTICAL_LINE)) {
                        line += "+";
                    }
                    else if (status[i][j].get(Pixel.HORIZONTAL_LINE)) {
                        line += "-";
                    }
                    else if (status[i][j].get(Pixel.VERTICAL_LINE)) {
                        line += "|";
                    }
                    else {
                        line += " ";
                    }
                }
            }
            res += line + "\n";
        }
        numNodes = graph.size();
        numEdges = 0;
        for (Node node : graph) {
            numEdges += node.getEdges().size();
        }
        numEdges = numEdges / 2;
        res += "Graph: " + numNodes + " nodes, " + numEdges + " edges\n";
        res += graph.toString() + "\n";
        return res;
    }

    /* ==========================================
        POLYGONS AND CONVEX CORNERS DETECTION
     ========================================== */

    private void updateTerminalConvexStatus(Pixel pixel, int terminal) { // Check if a given pixel is a terminal convex corner, updates the status and returns true if it is the case
        if (!status[pixel.x][pixel.y].get(Pixel.TERMINAL)) {
            return;
        }
        boolean convexCorner = false;

        if (!status[pixel.up().x][pixel.up().y].get(Pixel.TERMINAL) && !status[pixel.left().x][pixel.left().y].get(Pixel.TERMINAL)) {
            status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_TERMINAL_UP_LEFT, true);
            convexCorner = true;
        } else {status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_TERMINAL_UP_LEFT, false);}


        if (!status[pixel.up().x][pixel.up().y].get(Pixel.TERMINAL) && !status[pixel.right().x][pixel.right().y].get(Pixel.TERMINAL)) {
            status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_TERMINAL_UP_RIGHT, true);
            convexCorner = true;
        } else {status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_TERMINAL_UP_RIGHT, false);}

        if (!status[pixel.down().x][pixel.down().y].get(Pixel.TERMINAL) && !status[pixel.right().x][pixel.right().y].get(Pixel.TERMINAL)) {
            status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_TERMINAL_DOWN_RIGHT, true);
            convexCorner = true;
        } else {status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_TERMINAL_DOWN_RIGHT, false);}

        if (!status[pixel.down().x][pixel.down().y].get(Pixel.TERMINAL) && !status[pixel.left().x][pixel.left().y].get(Pixel.TERMINAL)) {
            status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_TERMINAL_DOWN_LEFT, true);
            convexCorner = true;
        } else {status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_TERMINAL_DOWN_LEFT, false);}

        if (convexCorner) {
            terminalConvexCorners.get(terminal).add(pixel);
        } else {
            terminalConvexCorners.get(terminal).remove(pixel);
        }
    }

    private boolean updateObstacleConvexStatus(Pixel pixel, int obstacle) { // Check if a given pixel is an obstacle convex corner, updates the status and returns true if it is the case
        if (!status[pixel.x][pixel.y].get(Pixel.OBSTACLE)) {
            return false;
        }
        boolean convexCorner = false;

        if(pixel.up().isBorder() || pixel.left().isBorder() || pixel.left().up().isBorder()) {
            status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_OBSTACLE_UP_LEFT, false);
        } else if (!status[pixel.up().x][pixel.up().y].get(Pixel.OBSTACLE) && !status[pixel.left().x][pixel.left().y].get(Pixel.OBSTACLE) && !status[pixel.left().up().x][pixel.left().up().y].get(Pixel.OBSTACLE) ) {
            status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_OBSTACLE_UP_LEFT, true);
            convexCorner = true;
        } else {status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_OBSTACLE_UP_LEFT, false);}


        if(pixel.up().isBorder() || pixel.right().isBorder() || pixel.right().up().isBorder()) {
            status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_OBSTACLE_UP_RIGHT, false);
        } else if (!status[pixel.up().x][pixel.up().y].get(Pixel.OBSTACLE) && !status[pixel.right().x][pixel.right().y].get(Pixel.OBSTACLE) && !status[pixel.right().up().x][pixel.right().up().y].get(Pixel.OBSTACLE)) {
            status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_OBSTACLE_UP_RIGHT, true);
            convexCorner = true;
        } else {status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_OBSTACLE_UP_RIGHT, false);}

        if(pixel.down().isBorder() || pixel.right().isBorder() || pixel.right().down().isBorder()) {
            status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_OBSTACLE_DOWN_RIGHT, false);
        } else if (!status[pixel.down().x][pixel.down().y].get(Pixel.OBSTACLE) && !status[pixel.right().x][pixel.right().y].get(Pixel.OBSTACLE) && !status[pixel.right().down().x][pixel.right().down().y].get(Pixel.OBSTACLE)) {
            status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_OBSTACLE_DOWN_RIGHT, true);
            convexCorner = true;
        } else {status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_OBSTACLE_DOWN_RIGHT, false);}

        if(pixel.down().isBorder() || pixel.left().isBorder() || pixel.left().down().isBorder()) {
            status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_OBSTACLE_DOWN_LEFT, false);
        } else if (!status[pixel.down().x][pixel.down().y].get(Pixel.OBSTACLE) && !status[pixel.left().x][pixel.left().y].get(Pixel.OBSTACLE)  && !status[pixel.left().down().x][pixel.left().down().y].get(Pixel.OBSTACLE) ) {
            status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_OBSTACLE_DOWN_LEFT, true);
            convexCorner = true;
        } else {status[pixel.x][pixel.y].set(Pixel.CONVEX_CORNER_OBSTACLE_DOWN_LEFT, false);}

        if (convexCorner) {
            if (!obstacleConvexCorners.get(obstacle).contains(pixel)) {obstacleConvexCorners.get(obstacle).add(pixel);}
        }
//        else {
//            obstacleConvexCorners.get(obstacle).remove(pixel);
//        }
        return convexCorner;
    }


    public void prepareGrid() {
        this.numObstacle = 0;
        this.numTerminal = 0;
        this.terminalBelonging = new int[N][M];
        this.terminalRepresentative = new ArrayList<>();
        this.obstacleBelonging = new int[N][M];
        this.terminalConvexCorners = new ArrayList<>();
        this.obstacleConvexCorners = new ArrayList<>();
        this.status = new BitSet[N][M];

        // Initialise the status for each pixel
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                status[i][j] = new BitSet(Pixel.num_status);
                if (grid[i][j] == Pixel.FREE) {
                    status[i][j].set(Pixel.FREE, true);
                }
                else if (grid[i][j] == Pixel.TERMINAL) {
                    status[i][j].set(Pixel.TERMINAL, true);
                }
                else if (grid[i][j] == Pixel.OBSTACLE) {
                    status[i][j].set(Pixel.OBSTACLE, true);
                }
            }
        }
        boolean[][] visited = new boolean[N][M];
        Stack<Pixel> stackDFS = new Stack<Pixel>();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                Pixel rootPixel = new Pixel(N, M, i, j);

                /**
                 * Detect the polygonal terminals and their convex corners
                  */
                if (!visited[i][j] && status[rootPixel.x][rootPixel.y].get(Pixel.TERMINAL)) {
                    visited[i][j] = true;
                    stackDFS.push(rootPixel);
                    terminalConvexCorners.add(new ArrayList<>());

                    // Pseudo-DFS inside the terminal
                    while (!stackDFS.isEmpty()) {
                        Pixel pixel = stackDFS.pop();
                        terminalBelonging[pixel.x][pixel.y] = numTerminal;

                        // Detect a convex corner
                        updateTerminalConvexStatus(pixel, numTerminal);

                        // Visit the up/down/left/right pixels
                        for (Pixel nextPixel : new Pixel[]{pixel.up(), pixel.down(), pixel.left(), pixel.right()}) {
                            if (!nextPixel.isBorder() && !visited[nextPixel.x][nextPixel.y] && status[nextPixel.x][nextPixel.y].get(Pixel.TERMINAL)) {
                                stackDFS.push(nextPixel);
                                visited[nextPixel.x][nextPixel.y] = true;
                            }
                        }
                    }
                    terminalRepresentative.add(rootPixel);
                    numTerminal ++;
                }

                /**
                 * Detect the polygonal obstacles and their convex corners
                 */
                else if (!visited[i][j] && status[rootPixel.x][rootPixel.y].get(Pixel.OBSTACLE)) {
                    visited[i][j] = true;
                    stackDFS.push(rootPixel);
                    obstacleConvexCorners.add(new ArrayList<>());

                    // Pseudo-DFS inside the obstacle
                    while (!stackDFS.isEmpty()) {
                        Pixel pixel = stackDFS.pop();
                        obstacleBelonging [pixel.x][pixel.y] = numObstacle;

                        // Detect a convex corner
                        updateObstacleConvexStatus(pixel, numObstacle);

                        // Visit the up/down/left/right/up-left/up-right/down-right/down-left pixels
                        for (Pixel nextPixel : new Pixel[]{pixel.up(), pixel.down(), pixel.left(), pixel.right(), pixel.up().left(), pixel.up().right(), pixel.down().right(), pixel.down().left()}) {
                            if (!nextPixel.isBorder() && !visited[nextPixel.x][nextPixel.y] && status[nextPixel.x][nextPixel.y].get(Pixel.OBSTACLE)) {
                                stackDFS.push(nextPixel);
                                visited[nextPixel.x][nextPixel.y] = true;
                            }
                        }
                    }
                    numObstacle ++;
                }
            }
        }
    }

    /* ==========================================
                  FILLING PROCEDURE
     ========================================== */

    private boolean condition(Pixel pixel, boolean draw) {
        return !pixel.isBorder() && ((draw && !status[pixel.x][pixel.y].get(Pixel.TEMP) && (status[pixel.x][pixel.y].get(Pixel.FREE) || status[pixel.x][pixel.y].get(Pixel.TERMINAL))) || (!draw && status[pixel.x][pixel.y].get(Pixel.TEMP)));
    }

    public Pixel drawLineFilling(Pixel source, int direction, boolean draw) { // Two modes: either draw a line until an obstacle is hit or erase the previously drawn line
        Pixel movingPixel = source;

        if(direction == Direction.UP) {
            while(condition(movingPixel, draw)) {
                status[movingPixel.x][movingPixel.y].set(Pixel.TEMP, draw);
                movingPixel = movingPixel.up();
            }
        }
        else if(direction == Direction.DOWN) {
            while(condition(movingPixel, draw)) {
                status[movingPixel.x][movingPixel.y].set(Pixel.TEMP, draw);
                movingPixel = movingPixel.down();
            }
        }
        else if(direction == Direction.LEFT) {
            while(condition(movingPixel, draw)) {
                status[movingPixel.x][movingPixel.y].set(Pixel.TEMP, draw);
                movingPixel = movingPixel.left();
            }
        }
        else if(direction == Direction.RIGHT) {
            while(condition(movingPixel, draw)) {
                status[movingPixel.x][movingPixel.y].set(Pixel.TEMP, draw);
                movingPixel = movingPixel.right();
            }
        }
        return movingPixel;
    }

    private void checkAndFill(Pixel rootPixel, int obstacle) {
        if (status[rootPixel.x][rootPixel.y].get(Pixel.TERMINAL)) {return;}
        boolean[][] visited = new boolean[N][M];
        Stack<Pixel> stackDFS = new Stack<Pixel>();
        stackDFS.push(rootPixel);
        while (!stackDFS.isEmpty()) {
            Pixel pixel = stackDFS.pop();

            // Visit the up/down/left/right pixels
            for (Pixel nextPixel : new Pixel[]{pixel.up(), pixel.down(), pixel.left(), pixel.right()}) {

                // Run through the pixels confined within the area delimited by the temporary lines (TEMP) and the obstacle
                if (!nextPixel.isBorder() && !visited[nextPixel.x][nextPixel.y] && !status[nextPixel.x][nextPixel.y].get(Pixel.TEMP)) {
                    if (status[nextPixel.x][nextPixel.y].get(Pixel.FREE)) {
                        stackDFS.push(nextPixel);
                        visited[nextPixel.x][nextPixel.y] = true;
                    }
                    // If the area contains a terminal or an obstacle different from the studied one, then we can not fill it
                    else if (status[nextPixel.x][nextPixel.y].get(Pixel.TERMINAL) || (status[nextPixel.x][nextPixel.y].get(Pixel.OBSTACLE) && obstacleBelonging[nextPixel.x][nextPixel.y] != obstacle)) {
                        return;
                    }
                }
            }
        }
        // We can fill the area
        visited = new boolean[N][M];
        stackDFS.push(rootPixel);
        while (!stackDFS.isEmpty()) {
            Pixel pixel = stackDFS.pop();
            // The pixels within the area are merged to the studied obstacle
            status[pixel.x][pixel.y].set(Pixel.OBSTACLE, true);
            status[pixel.x][pixel.y].set(Pixel.FREE, false);
            obstacleBelonging [pixel.x][pixel.y] = obstacle;
            grid[pixel.x][pixel.y] = Pixel.OBSTACLE;

            // Visit the up/down/left/right pixels
            for (Pixel nextPixel : new Pixel[]{pixel.up(), pixel.down(), pixel.left(), pixel.right()}) {

                // Run through the pixels confined within the area delimited by the temporary lines (TEMP) and the obstacle, and eventually defined by the border too
                if (!nextPixel.isBorder() && !visited[nextPixel.x][nextPixel.y] && !status[nextPixel.x][nextPixel.y].get(Pixel.TEMP)) {
                    if (status[nextPixel.x][nextPixel.y].get(Pixel.FREE)) {
                        stackDFS.add(nextPixel);
                        visited[nextPixel.x][nextPixel.y] = true;
                    }
                }
            }
        }
    }

    public void fillingProcedure() {
        Pixel hitPixel;
        for (int obstacle = 0; obstacle < numObstacle; obstacle++) {

            // Apply rule (1)
            for (int i = 0; i < obstacleConvexCorners.get(obstacle).size(); i++) {
                Pixel c = obstacleConvexCorners.get(obstacle).get(i);

                // Ensure the pixel is still a convex corner before drawing lines from it
                if (updateObstacleConvexStatus(c, obstacleBelonging[c.x][c.y])) {
                    // Draw lines from an up-left convex corner
                    if (status[c.x][c.y].get(Pixel.CONVEX_CORNER_OBSTACLE_UP_LEFT)) {

                        // Draw left line
                        hitPixel = drawLineFilling(c.up().left(), Direction.LEFT, true);
                        if (!hitPixel.isBorder() && obstacleBelonging[c.x][c.y] == obstacleBelonging[hitPixel.x][hitPixel.y]) {
                            checkAndFill(c.left(), obstacleBelonging[c.x][c.y]);
                        }
                        drawLineFilling(c.up().left(), Direction.LEFT, false);

                        // Draw up line
                        hitPixel = drawLineFilling(c.up().left(), Direction.UP, true);
                        if (!hitPixel.isBorder() && obstacleBelonging[c.x][c.y] == obstacleBelonging[hitPixel.x][hitPixel.y]) {
                            checkAndFill(c.up(), obstacleBelonging[c.x][c.y]);
                        }
                        drawLineFilling(c.up().left(), Direction.UP, false);
                    }

                    // Draw lines from an up-right convex corner
                    if (status[c.x][c.y].get(Pixel.CONVEX_CORNER_OBSTACLE_UP_RIGHT)) {

                        // Draw right line
                        hitPixel = drawLineFilling(c.up().right(), Direction.RIGHT, true);
                        if (!hitPixel.isBorder() && obstacleBelonging[c.x][c.y] == obstacleBelonging[hitPixel.x][hitPixel.y]) {
                            checkAndFill(c.right(), obstacleBelonging[c.x][c.y]);
                        }
                        drawLineFilling(c.up().right(), Direction.RIGHT, false);

                        // Draw up line
                        hitPixel = drawLineFilling(c.up().right(), Direction.UP, true);
                        if (!hitPixel.isBorder() && obstacleBelonging[c.x][c.y] == obstacleBelonging[hitPixel.x][hitPixel.y]) {
                            checkAndFill(c.up(), obstacleBelonging[c.x][c.y]);
                        }
                        drawLineFilling(c.up().right(), Direction.UP, false);
                    }

                    // Draw lines from a down-right convex corner
                    if (status[c.x][c.y].get(Pixel.CONVEX_CORNER_OBSTACLE_DOWN_RIGHT)) {

                        // Draw right line
                        hitPixel = drawLineFilling(c.down().right(), Direction.RIGHT, true);
                        if (!hitPixel.isBorder() && obstacleBelonging[c.x][c.y] == obstacleBelonging[hitPixel.x][hitPixel.y]) {
                            checkAndFill(c.right(), obstacleBelonging[c.x][c.y]);
                        }
                        drawLineFilling(c.down().right(), Direction.RIGHT, false);

                        // Draw down line
                        hitPixel = drawLineFilling(c.down().right(), Direction.DOWN, true);
                        if (!hitPixel.isBorder() && obstacleBelonging[c.x][c.y] == obstacleBelonging[hitPixel.x][hitPixel.y]) {
                            checkAndFill(c.down(), obstacleBelonging[c.x][c.y]);
                        }
                        drawLineFilling(c.down().right(), Direction.DOWN, false);
                    }

                    // Draw lines from a down-left convex corner
                    if (status[c.x][c.y].get(Pixel.CONVEX_CORNER_OBSTACLE_DOWN_LEFT)) {

                        // Draw left line
                        hitPixel = drawLineFilling(c.down().left(), Direction.LEFT, true);
                        if (!hitPixel.isBorder() && obstacleBelonging[c.x][c.y] == obstacleBelonging[hitPixel.x][hitPixel.y]) {
                            checkAndFill(c.left(), obstacleBelonging[c.x][c.y]);
                        }
                        drawLineFilling(c.down().left(), Direction.LEFT, false);

                        // Draw down line
                        hitPixel = drawLineFilling(c.down().left(), Direction.DOWN, true);
                        if (!hitPixel.isBorder() && obstacleBelonging[c.x][c.y] == obstacleBelonging[hitPixel.x][hitPixel.y]) {
                            checkAndFill(c.down(), obstacleBelonging[c.x][c.y]);
                        }
                        drawLineFilling(c.down().left(), Direction.DOWN, false);
                    }
                }

            }



            // Apply rule (2)
            for(int i = 0; i < obstacleConvexCorners.get(obstacle).size(); i++) {
                Pixel c1 = obstacleConvexCorners.get(obstacle).get(i);
                if (updateObstacleConvexStatus(c1, obstacleBelonging[c1.x][c1.y])) {
                    for (int j = 0; j < obstacleConvexCorners.get(obstacle).size(); j++) {
                        Pixel c2 = obstacleConvexCorners.get(obstacle).get(j);
                        if (updateObstacleConvexStatus(c2, obstacleBelonging[c2.x][c2.y])) {

                            /**
                             The two convex corners are on the same rectilinear line
                             */
                            //Both convex corners are different but on the same vertical line
                            if (!c1.equals(c2) && c1.isSameVerticalLine(c2)) {
                                Pixel bottomPixel = c1.isBelow(c2) ? c1 : c2;
                                Pixel topPixel = c1.isAbove(c2) ? c1 : c2;

                                // If the bottom pixel is an up-left corner and the top pixel is a down-left corner
                                if (status[bottomPixel.x][bottomPixel.y].get(Pixel.CONVEX_CORNER_OBSTACLE_UP_LEFT) && status[topPixel.x][topPixel.y].get(Pixel.CONVEX_CORNER_OBSTACLE_DOWN_LEFT)) {
                                    drawLineFilling(bottomPixel.up().left(), Direction.LEFT, true);
                                    Pixel intersectionPixel = drawLineFilling(topPixel.down().left(), Direction.DOWN, true);
                                    // If the two lines intersect then try to fill the confined area
                                    if (!intersectionPixel.isBorder() && status[intersectionPixel.x][intersectionPixel.y].get(Pixel.TEMP)) {
                                        checkAndFill(topPixel.down(), obstacle);
                                    }
                                    // Erase the lines
                                    drawLineFilling(bottomPixel.up().left(), Direction.LEFT, false);
                                    drawLineFilling(topPixel.down().left(), Direction.DOWN, false);
                                }

                                // If the bottom pixel is an up-right corner and the top pixel is a down-right corner
                                if (status[bottomPixel.x][bottomPixel.y].get(Pixel.CONVEX_CORNER_OBSTACLE_UP_RIGHT) && status[topPixel.x][topPixel.y].get(Pixel.CONVEX_CORNER_OBSTACLE_DOWN_RIGHT)) {
                                    drawLineFilling(bottomPixel.up().right(), Direction.RIGHT, true);
                                    Pixel intersectionPixel = drawLineFilling(topPixel.down().right(), Direction.DOWN, true);
                                    // If the two lines intersect then try to fill the confined area
                                    if (!intersectionPixel.isBorder() && status[intersectionPixel.x][intersectionPixel.y].get(Pixel.TEMP)) {
                                        checkAndFill(topPixel.down(), obstacle);
                                    }
                                    // Erase the lines
                                    drawLineFilling(bottomPixel.up().right(), Direction.RIGHT, false);
                                    drawLineFilling(topPixel.down().right(), Direction.DOWN, false);
                                }

                            }
                            //Both convex corners are different but on the same horizontal line
                            else if (!c1.equals(c2) && c1.isSameHorizontalLine(c2)) {
                                Pixel leftPixel = c1.isAtLeft(c2) ? c1 : c2;
                                Pixel rightPixel = c1.isAtRight(c2)? c1 : c2;

                                // If the left pixel is an up-right corner and the right pixel is a up-left corner
                                if (status[leftPixel.x][leftPixel.y].get(Pixel.CONVEX_CORNER_OBSTACLE_UP_RIGHT) && status[rightPixel.x][rightPixel.y].get(Pixel.CONVEX_CORNER_OBSTACLE_UP_LEFT)) {
                                    drawLineFilling(leftPixel.up().right(), Direction.UP, true);
                                    Pixel intersectionPixel = drawLineFilling(rightPixel.up().left(), Direction.LEFT, true);
                                    // If the two lines intersect then try to fill the confined area
                                    if (!intersectionPixel.isBorder() && status[intersectionPixel.x][intersectionPixel.y].get(Pixel.TEMP)) {
                                        checkAndFill(rightPixel.left(), obstacle);
                                    }
                                    // Erase the lines
                                    drawLineFilling(leftPixel.up().right(), Direction.UP, false);
                                    drawLineFilling(rightPixel.up().left(), Direction.LEFT, false);
                                }

                                // If the left pixel is a down-right corner and the right pixel is a down-left corner
                                if (status[leftPixel.x][leftPixel.y].get(Pixel.CONVEX_CORNER_OBSTACLE_DOWN_RIGHT) && status[rightPixel.x][rightPixel.y].get(Pixel.CONVEX_CORNER_OBSTACLE_DOWN_LEFT)) {
                                    drawLineFilling(leftPixel.down().right(), Direction.DOWN, true);
                                    Pixel intersectionPixel = drawLineFilling(rightPixel.down().left(), Direction.LEFT, true);
                                    // If the two lines intersect then try to fill the confined area
                                    if (!intersectionPixel.isBorder() && status[intersectionPixel.x][intersectionPixel.y].get(Pixel.TEMP)) {
                                        checkAndFill(rightPixel.left(), obstacle);
                                    }
                                    // Erase the lines
                                    drawLineFilling(leftPixel.down().right(), Direction.DOWN, false);
                                    drawLineFilling(rightPixel.down().left(), Direction.LEFT, false);
                                }
                            }

                            /**
                             The two convex corners are not on the same rectilinear line
                             */
                            // There is a bottom-left and a top-right convex corner
                            else if ((c1.isAtLeft(c2) && c1.isBelow(c2)) || (c1.isAtRight(c2) && c1.isAbove(c2)) ) {
                                Pixel bottomLeftPixel = c1.isAtLeft(c2) ? c1 : c2;
                                Pixel topRightPixel = c1.isAtRight(c2) ? c1 : c2;

                                // If both pixels are up-left convex corners then draw an up-line from the bottom-left pixel and a left-line from the top-right pixel
                                if (status[bottomLeftPixel.x][bottomLeftPixel.y].get(Pixel.CONVEX_CORNER_OBSTACLE_UP_LEFT) && status[topRightPixel.x][topRightPixel.y].get(Pixel.CONVEX_CORNER_OBSTACLE_UP_LEFT)) {
                                    drawLineFilling(bottomLeftPixel.up().left(), Direction.UP, true);
                                    Pixel intersectionPixel = drawLineFilling(topRightPixel.up().left(), Direction.LEFT, true);
                                    // If the two lines intersect then try to fill the confined area
                                    if (!intersectionPixel.isBorder() && status[intersectionPixel.x][intersectionPixel.y].get(Pixel.TEMP)) {
                                        checkAndFill(bottomLeftPixel.up(), obstacle);
                                        // A new convex corner might have been created
                                        updateObstacleConvexStatus(intersectionPixel.down().right(), obstacle);
                                    }
                                    // Erase the lines
                                    drawLineFilling(bottomLeftPixel.up().left(), Direction.UP, false);
                                    drawLineFilling(topRightPixel.up().left(), Direction.LEFT, false);
                                }

                                // If both pixels are down-right convex corners then draw a right-line from the bottom-left pixel and a down-line from the top-right pixel
                                if (status[bottomLeftPixel.x][bottomLeftPixel.y].get(Pixel.CONVEX_CORNER_OBSTACLE_DOWN_RIGHT) && status[topRightPixel.x][topRightPixel.y].get(Pixel.CONVEX_CORNER_OBSTACLE_DOWN_RIGHT)) {
                                    drawLineFilling(bottomLeftPixel.down().right(), Direction.RIGHT, true);
                                    Pixel intersectionPixel = drawLineFilling(topRightPixel.down().right(), Direction.DOWN, true);
                                    // If the two lines intersect then try to fill the confined area
                                    if (!intersectionPixel.isBorder() && status[intersectionPixel.x][intersectionPixel.y].get(Pixel.TEMP)) {
                                        checkAndFill(bottomLeftPixel.right(), obstacle);
                                        // A new convex corner might have been created
                                        updateObstacleConvexStatus(intersectionPixel.up().left(), obstacle);
                                    }
                                    // Erase the lines
                                    drawLineFilling(bottomLeftPixel.down().right(), Direction.RIGHT, false);
                                    drawLineFilling(topRightPixel.down().right(), Direction.DOWN, false);
                                }
                            }

                            // There is a top-left and a bottom-right convex corner
                            else if ((c1.isAtLeft(c2) && c1.isAbove(c2)) || (c1.isAtRight(c2) && c1.isBelow(c2)) ) {
                                Pixel topLeftPixel = c1.isAtLeft(c2) ? c1 : c2;
                                Pixel bottomRightPixel = c1.isAtRight(c2) ? c1 : c2;

                                // If both pixels are down-left convex corners then draw a down-line from the top-left pixel and a left-line from the bottom-right pixel
                                if (status[topLeftPixel.x][topLeftPixel.y].get(Pixel.CONVEX_CORNER_OBSTACLE_DOWN_LEFT) && status[bottomRightPixel.x][bottomRightPixel.y].get(Pixel.CONVEX_CORNER_OBSTACLE_DOWN_LEFT)) {
                                    drawLineFilling(topLeftPixel.down().left(), Direction.DOWN, true);
                                    Pixel intersectionPixel = drawLineFilling(bottomRightPixel.down().left(), Direction.LEFT, true);
                                    // If the two lines intersect then try to fill the confined area
                                    if (!intersectionPixel.isBorder() && status[intersectionPixel.x][intersectionPixel.y].get(Pixel.TEMP)) {
                                        checkAndFill(topLeftPixel.down(), obstacle);
                                        // A new convex corner might have been created
                                        updateObstacleConvexStatus(intersectionPixel.up().right(), obstacle);
                                    }
                                    // Erase the lines
                                    drawLineFilling(topLeftPixel.down().left(), Direction.DOWN, false);
                                    drawLineFilling(bottomRightPixel.down().left(), Direction.LEFT, false);
                                }

                                // If both pixels are up-right convex corners then draw an right-line from the top-left pixel and a up-line from the bottom-right pixel
                                if (status[topLeftPixel.x][topLeftPixel.y].get(Pixel.CONVEX_CORNER_OBSTACLE_UP_RIGHT) && status[bottomRightPixel.x][bottomRightPixel.y].get(Pixel.CONVEX_CORNER_OBSTACLE_UP_RIGHT)) {
                                    drawLineFilling(topLeftPixel.up().right(), Direction.RIGHT, true);
                                    Pixel intersectionPixel = drawLineFilling(bottomRightPixel.up().right(), Direction.UP, true);
                                    // If the two lines intersect then try to fill the confined area
                                    if (!intersectionPixel.isBorder() && status[intersectionPixel.x][intersectionPixel.y].get(Pixel.TEMP)) {
                                        checkAndFill(topLeftPixel.right(), obstacle);
                                        // A new convex corner might have been created
                                        updateObstacleConvexStatus(intersectionPixel.down().left(), obstacle);
                                    }
                                    // Erase the lines
                                    drawLineFilling(topLeftPixel.up().right(), Direction.RIGHT, false);
                                    drawLineFilling(bottomRightPixel.up().right(), Direction.UP, false);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /* ==========================================
              COMPLEX RECTILINEAR GRID
     ========================================== */

    public void drawLinesConvexCorner(Pixel source, boolean obstacle) {
        Pixel movingPixel;

        if (obstacle) {
            status[source.x][source.y].set(Pixel.VERTICAL_LINE, true);
            status[source.x][source.y].set(Pixel.HORIZONTAL_LINE, true);
        }

        movingPixel = source.up();
        while(status[movingPixel.x][movingPixel.y].get(Pixel.FREE) && !status[movingPixel.x][movingPixel.y].get(Pixel.VERTICAL_LINE)) {
            status[movingPixel.x][movingPixel.y].set(Pixel.VERTICAL_LINE, true);
            movingPixel = movingPixel.up();
        }
        movingPixel = source.down();
        while(status[movingPixel.x][movingPixel.y].get(Pixel.FREE) && !status[movingPixel.x][movingPixel.y].get(Pixel.VERTICAL_LINE)) {
            status[movingPixel.x][movingPixel.y].set(Pixel.VERTICAL_LINE, true);
            movingPixel = movingPixel.down();
        }
        movingPixel = source.left();
        while(status[movingPixel.x][movingPixel.y].get(Pixel.FREE) && !status[movingPixel.x][movingPixel.y].get(Pixel.HORIZONTAL_LINE)) {
            status[movingPixel.x][movingPixel.y].set(Pixel.HORIZONTAL_LINE, true);
            movingPixel = movingPixel.left();
        }
        movingPixel = source.right();
        while(status[movingPixel.x][movingPixel.y].get(Pixel.FREE) && !status[movingPixel.x][movingPixel.y].get(Pixel.HORIZONTAL_LINE)) {
            status[movingPixel.x][movingPixel.y].set(Pixel.HORIZONTAL_LINE, true);
            movingPixel = movingPixel.right();
        }
    }

    public void drawComplexRectilinearGrid() {
        for (int terminal = 0; terminal < numTerminal; terminal++) {
            for (Pixel convexCorner : terminalConvexCorners.get(terminal)) {
                drawLinesConvexCorner(convexCorner, false);
            }
        }
        for (int obstacle = 0; obstacle < numObstacle; obstacle++) {
            for (Pixel convexCorner : obstacleConvexCorners.get(obstacle)) {
                if (status[convexCorner.x][convexCorner.y].get(Pixel.CONVEX_CORNER_OBSTACLE_UP_LEFT)) {
                    drawLinesConvexCorner(convexCorner.up().left(), true);
                }
                if (status[convexCorner.x][convexCorner.y].get(Pixel.CONVEX_CORNER_OBSTACLE_UP_RIGHT)) {
                    drawLinesConvexCorner(convexCorner.up().right(), true);
                }
                if (status[convexCorner.x][convexCorner.y].get(Pixel.CONVEX_CORNER_OBSTACLE_DOWN_RIGHT)) {
                    drawLinesConvexCorner(convexCorner.down().right(), true);
                }
                if (status[convexCorner.x][convexCorner.y].get(Pixel.CONVEX_CORNER_OBSTACLE_DOWN_LEFT)) {
                    drawLinesConvexCorner(convexCorner.down().left(), true);
                }
            }
        }
    }


    /* ==========================================
           CREATION OF THE EQUIVALENT GRAPH
     ========================================== */

    public Edge findEdge(Pixel source, int direction) {
        Pixel movingPixel = source;
        int length = 0;

        if (direction == Direction.UP) {    // Find the up neighbour
            while(status[movingPixel.x][movingPixel.y].get(Pixel.FREE) && status[movingPixel.x][movingPixel.y].get(Pixel.VERTICAL_LINE) && !status[movingPixel.x][movingPixel.y].get(Pixel.HORIZONTAL_LINE)) {
                movingPixel = movingPixel.up();
                length++;
            }
        }
        else if (direction == Direction.DOWN) {    // Find the down neighbour
            while(status[movingPixel.x][movingPixel.y].get(Pixel.FREE) && status[movingPixel.x][movingPixel.y].get(Pixel.VERTICAL_LINE) && !status[movingPixel.x][movingPixel.y].get(Pixel.HORIZONTAL_LINE)) {
                movingPixel = movingPixel.down();
                length++;
            }
        }
        else if (direction == Direction.LEFT) {    // Find the left neighbour
            while(status[movingPixel.x][movingPixel.y].get(Pixel.FREE) && status[movingPixel.x][movingPixel.y].get(Pixel.HORIZONTAL_LINE) && !status[movingPixel.x][movingPixel.y].get(Pixel.VERTICAL_LINE)) {
                movingPixel = movingPixel.left();
                length++;
            }
        }
        else if (direction == Direction.RIGHT) {    // Find the right neighbour
            while(status[movingPixel.x][movingPixel.y].get(Pixel.FREE) && status[movingPixel.x][movingPixel.y].get(Pixel.HORIZONTAL_LINE) && !status[movingPixel.x][movingPixel.y].get(Pixel.VERTICAL_LINE)) {
                movingPixel = movingPixel.right();
                length++;
            }
        }

        // We hit a Steiner point
        if (status[movingPixel.x][movingPixel.y].get(Pixel.FREE) && status[movingPixel.x][movingPixel.y].get(Pixel.VERTICAL_LINE) && status[movingPixel.x][movingPixel.y].get(Pixel.HORIZONTAL_LINE)) {
            Node steinerNode = pixelNode[movingPixel.x][movingPixel.y];
            return new Edge(steinerNode, length);

        }
        // We hit a terminal
        else if (status[movingPixel.x][movingPixel.y].get(Pixel.TERMINAL)) {
            Pixel terminalPixel = terminalRepresentative.get(terminalBelonging[movingPixel.x][movingPixel.y]);
            Node terminalNode = pixelNode[terminalPixel.x][terminalPixel.y];
            return new Edge(terminalNode, length);
        }
        // We hit an obstacle or a free pixel that is not a steiner point, so there is no edge
        else {return null;}
    }

    public void getGraphFromGrid() {
        this.pixelNode = new Node[N][M];
        this.graph = new ArrayList<>();
        this.numNodes = 0;
        this.numEdges = 0;

        // Add all terminal nodes
        for (Pixel terminal : terminalRepresentative) {
            Node node = new Node(terminal, Node.TERMINAL);
            pixelNode[terminal.x][terminal.y] = node;
            graph.add(node);
            numNodes++;
        }
        // Add all steiner nodes
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                if (status[i][j].get(Pixel.FREE) && status[i][j].get(Pixel.VERTICAL_LINE) && status[i][j].get(Pixel.HORIZONTAL_LINE)) {
                    Node node = new Node(new Pixel(N,M,i,j), Node.STEINER);
                    pixelNode[i][j] = node;
                    graph.add(node);
                    numNodes++;
                }
            }
        }
        // Add all edges
        Edge edge;
        boolean[][] visited = new boolean[N][M];
        Stack<Pixel> stackDFS = new Stack<>();
        int[] directions = new int[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

        for (Node node : graph) {
            if (node.getType() == Node.STEINER) {
                Pixel pixel = node.getPixel();
                Pixel[] neighbours = new Pixel[]{pixel.up(), pixel.down(), pixel.left(), pixel.right()};

                for (int i = 0; i < 4; i++) {
                    edge = findEdge(neighbours[i], directions[i]);
                    if (edge != null && node.addEdge(edge)) {numEdges++;}
                }
            }
            else if (node.getType() == Node.TERMINAL) {

                Pixel rootPixel = node.getPixel();
                visited[rootPixel.x][rootPixel.y] = true;
                stackDFS.push(rootPixel);

                // Pseudo-DFS inside the terminal to find all the edges
                while (!stackDFS.isEmpty()) {
                    Pixel pixel = stackDFS.pop();

                    Pixel[] neighbours = new Pixel[]{pixel.up(), pixel.down(), pixel.left(), pixel.right()};

                    for (int i = 0; i < 4; i++) {
                        Pixel nextPixel = neighbours[i];

                        // We found an unexplored pixel of the terminal, so we push it into the stack
                        if (!visited[nextPixel.x][nextPixel.y] && status[nextPixel.x][nextPixel.y].get(Pixel.TERMINAL)) {
                            stackDFS.push(nextPixel);
                            visited[nextPixel.x][nextPixel.y] = true;
                        }
                        // We found a pixel alongside the terminal, so we check if it leads to the discovery of an edge in the graph
                        else if (!visited[nextPixel.x][nextPixel.y]) {
                            edge = findEdge(nextPixel, directions[i]);
                            if (edge != null && node.addEdge(edge)) {numEdges++;}
                        }
                    }
                }
            }
        }
        numEdges = numEdges / 2;
    }


    /* ==========================================
                 GRAPH SIMPLIFICATION
     ========================================== */

    private boolean nodeRemovalAndEdgeMerging() {
        boolean update = false;
        int i = 0;
        while (i < graph.size()) {
            Node node = graph.get(i);
            if (node.getType() == Node.STEINER && node.getEdges().isEmpty()) {
                graph.remove(i);
                update = true;
            }
            else if (node.getType() == Node.STEINER && node.getEdges().size() == 1) {
                graph.remove(i);
                Node neighbour = node.getEdges().get(0).destination;
                neighbour.removeEdge(node);
                targetNodes.get(neighbour).remove(node);
                update = true;
            } else if (node.getType() == Node.STEINER && node.getEdges().size() == 2) {
                graph.remove(i);
                Node neighbour1 = node.getEdges().get(0).destination;
                Node neighbour2 = node.getEdges().get(node.getEdges().size() - 1).destination;
                int weight = node.getEdges().get(0).weight + node.getEdges().get(node.getEdges().size() - 1).weight + node.getWeight();

                neighbour1.removeEdge(node);
                targetNodes.get(neighbour1).remove(node);
                if (neighbour1.addEdge(new Edge(neighbour2, weight))) {targetNodes.get(neighbour1).add(neighbour2);}

                neighbour2.removeEdge(node);
                targetNodes.get(neighbour2).remove(node);
                if (neighbour2.addEdge(new Edge(neighbour1, weight))) {targetNodes.get(neighbour2).add(neighbour1);}
                update = true;
            }
            i++;
        }
        return update;
    }

//    private void computeShortestPaths() {
//        this.weightShortestPath = new HashMap<>();
//        for (Node sourceNode : graph) {
//            HashMap<Node, Integer> distances = new HashMap<>();
//            HashSet<Node> explored = new HashSet<>();
//            // Explore the source node for which we do not count its weight in the distance
//            distances.put(sourceNode, 0);
//            explored.add(sourceNode);
//            for (Edge edge : sourceNode.getEdges()) {
//                distances.put(edge.destination, edge.weight);
//            }
//            while (explored.size() < distances.keySet().size()) {
//                int minDistance = N + M;
//                Node minNode = null;
//                for (Node node : distances.keySet()) {  // Select the unexplored node with minimum distance to the source
//                    if (!explored.contains(node) && distances.get(node) < minDistance) {
//                        minDistance = distances.get(node);
//                        minNode = node;
//                    }
//                }
//                explored.add(minNode);
//                for (Edge edge : minNode.getEdges()) { // Update the new shortest distances by taking into account the edges from minNode
//                    if (!distances.containsKey(edge.destination)) {
//                        distances.put(edge.destination, minDistance + edge.weight + minNode.getWeight());
//                    }
//                    else {
//                        distances.put(edge.destination, Math.min(distances.get(edge.destination), minDistance + edge.weight + minNode.getWeight()));
//                    }
//                }
//            }
//            // Store the shortest distances from sourNode
//            weightShortestPath.put(sourceNode, new HashMap<>());
//            for (Node node: explored) {
//                weightShortestPath.get(sourceNode).put(node, distances.get(node));
//            }
//        }
//    }
//
//    private boolean edgeRemoval() {
//        boolean update = false;
//        for (Node node : graph) {
//            int i = 0;
//            while (i < node.getEdges().size()) {
//                Edge edge = node.getEdges().get(i);
//                int minWeightPath = weightShortestPath.get(node).get(edge.destination);
//                if (minWeightPath <= edge.weight) {
//                    node.getEdges().remove(i);
//                    update = true;
//                }
//                else {i++;}
//            }
//        }
//        return update;
//    }

    private boolean edgeRemovalFromNode(Node sourceNode) {
        boolean update = false;
        HashMap<Node, Integer> distances = new HashMap<>();
        HashSet<Node> explored = new HashSet<>();
        // Explore the source node for which we do not count its weight in the distance
        distances.put(sourceNode, 0);
        explored.add(sourceNode);
        for (Edge edge : sourceNode.getEdges()) {
            distances.put(edge.destination, edge.weight);
        }
        while (explored.size() < distances.keySet().size() && !targetNodes.get(sourceNode).isEmpty()) {
            int minDistance = N + M;
            Node minNode = null;
            for (Node node : distances.keySet()) {  // Select the unexplored node with minimum distance to sourceNode
                if (!explored.contains(node) && distances.get(node) < minDistance) {
                    minDistance = distances.get(node);
                    minNode = node;
                }
            }

            explored.add(minNode);

            for (Edge edge : minNode.getEdges()) { // Update the new shortest distances by taking into account the edges from minNode
                if (!distances.containsKey(edge.destination)) {
                    distances.put(edge.destination, minDistance + edge.weight + minNode.getWeight());
                }
                else {
                    int newWeight = minDistance + edge.weight + minNode.getWeight();
                    if (newWeight <= distances.get(edge.destination)) {
                        // Update the distance like in Dijkstra's algorithm
                        distances.put(edge.destination, newWeight);

                        // We found a path shorter or equal to the edge (sourceNode, minNode)
                        // so we can remove the edge and the corresponding target for both nodes
                        if (sourceNode.removeEdge(edge.destination)) {
                            edge.destination.removeEdge(sourceNode);
                            targetNodes.get(sourceNode).remove(edge.destination);
                            targetNodes.get(edge.destination).add(sourceNode);
                            update = true;
                        }
                    }
                }
            }
        }
        // At the end of the algorithm, all edges from sourceNode that could be removed were removed,
        // and the ones remaining can not be removed in the future, so we can clear the targets of sourceNode
        targetNodes.get(sourceNode).clear();
        return update;
    }

    public void graphSimplification() {
        this.targetNodes = new HashMap<>();
        for (Node node : graph) {
            targetNodes.put(node, new ArrayList<>());
            for (Edge edge : node.getEdges()) {
                targetNodes.get(node).add(edge.destination);
            }
        }

        // Apply rules (1) (2) and (3) until the graph is unchanged
        boolean update = true;
        while (update) {
            // Prioritise rules (1) and (2) which are cheaper
            while (update) {
                update = nodeRemovalAndEdgeMerging();
            }
            // If rules (1) and (2) have reached a fixed point, then apply rule (3)
            int i = 0;
            while (!update && i < graph.size()) {
                update = edgeRemovalFromNode(graph.get(i));
                i++;
            }
        }
    }




}
