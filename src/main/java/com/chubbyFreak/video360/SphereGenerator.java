package com.chubbyFreak.video360;

import java.util.ArrayList;

public class SphereGenerator {
    public static ArrayList<Integer[]> generateCoordinates(double radius, boolean middle) {
        boolean[][][] blocks = generateBlocks(radius, middle);
        blocks = purgeBlocks(blocks);

        ArrayList<Integer[]> coordinates = new ArrayList<>();
        int size = blocks.length;
        for(int z = 0; z < size; z++) {
            for(int x = 0; x < size; x++) {
                for(int y = 0; y < size; y++) {
                    if(blocks[z][x][y])
                        coordinates.add(new Integer[] {
                                x - size / 2,
                                y - size / 2,
                                z - size / 2,
                        });
                }
            }
        }

        return coordinates;
    }

    private static boolean[][][] generateBlocks(double radius, boolean middle) {
        double r2 = radius * radius;
        int size = 0;
        double halfSize = 0, offset = 0;

        if (middle) {
            size = (int) (2 * Math.ceil(radius) + 1);
            offset = Math.floor(size / 2);
        } else {
            halfSize = Math.ceil(radius) + 1;
            size = (int) halfSize * 2;
            offset = halfSize - 0.5;
        }

        boolean[][][] blocks = new boolean[size][size][size];
        for(int z = 0; z < size; z++) {
            for(int x = 0; x < size; x++) {
                for(int y = 0; y < size; y++) {
                    blocks[z][x][y] = isFull(x, y, z, offset, r2);
                }
            }
        }

        return blocks;
    }

    private static boolean[][][] purgeBlocks(boolean[][][] blocks) {
        int size = blocks.length;
        boolean[][][] newBlocks = new boolean[size][size][size];
        for(int z = 0; z < size; z++) {
            for(int x = 0; x < size; x++) {
                for(int y = 0; y < size; y++) {
                    newBlocks[z][x][y] = blocks[z][x][y] &&
                            (!blocks[z][x][y - 1] ||
                                    !blocks[z][x][y + 1] ||
                                    !blocks[z][x - 1][y] ||
                                    !blocks[z][x + 1][y] ||
                                    !blocks[z - 1][x][y] ||
                                    !blocks[z + 1][x][y]);
                }
            }
        }

        return newBlocks;
    }

    private static boolean isFull(double x, double y, double z, double offset, double r2) {
        x -= offset;
        y -= offset;
        z -= offset;
        x *= x;
        y *= y;
        z *= z;

        return x + y + z < r2;
    }
}
