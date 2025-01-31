package org.cceval.preprocessing;

public class Pixel {
    public static final int TERMINAL = 1;
    public static final int OBSTACLE = 2;
    public static final int FREE = 0;
    public static final int BORDURE = 3;

    public static final int CONVEX_CORNER_TERMINAL_UP_LEFT = 4;
    public static final int CONVEX_CORNER_TERMINAL_UP_RIGHT = 5;
    public static final int CONVEX_CORNER_TERMINAL_DOWN_LEFT = 6;
    public static final int CONVEX_CORNER_TERMINAL_DOWN_RIGHT = 7;

    public static final int OBSTACLE_BOUNDARY = 8;
    public static final int CONVEX_CORNER_OBSTACLE_UP_LEFT = 9;
    public static final int CONVEX_CORNER_OBSTACLE_UP_RIGHT = 10;
    public static final int CONVEX_CORNER_OBSTACLE_DOWN_LEFT = 11;
    public static final int CONVEX_CORNER_OBSTACLE_DOWN_RIGHT = 12;

    public static final int HORIZONTAL_LINE = 13;
    public static final int VERTICAL_LINE = 14;
    public static final int INTERSECTION = 15;

    public static final int TEMP = 16;

    public static final int num_status = 17;

    public int N;
    public int M;
    public int x;
    public int y;

    public Pixel(int N, int M, int x, int y) {
        this.N = N;
        this.M = M;
        this.x = x;
        this.y = y;
    }

    public Pixel up() {
        return new Pixel(N, M, x - 1, y);
    }

    public Pixel down() {
        return new Pixel(N, M, x + 1, y);
    }

    public Pixel left() {
        return new Pixel(N, M, x, y - 1);
    }

    public Pixel right() {
        return new Pixel(N, M, x, y + 1);
    }

    public boolean isAbove(Pixel p) {
        return x < p.x;
    }

    public boolean isBelow(Pixel p) {
        return x > p.x;
    }

    public boolean isAtLeft(Pixel p) {
        return y < p.y;
    }

    public boolean isAtRight(Pixel p) {
        return y > p.y;
    }

    public boolean isSameHorizontalLine(Pixel p) {
        return x == p.x;
    }

    public boolean isSameVerticalLine(Pixel p) {
        return y == p.y;
    }

    public boolean isBorder() {return x >= N || y >= M || x < 0 || y < 0;}

    public int getNode() {
        return N * y + x;
    }

    public boolean equals(Pixel pixel) {return N == pixel.N && M == pixel.M && x == pixel.x && y == pixel.y;
    }

    @Override
    public String toString() {
        return "(" + this.x + "," + this.y + ")";
    }
}