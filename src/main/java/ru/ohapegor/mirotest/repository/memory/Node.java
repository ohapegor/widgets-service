package ru.ohapegor.mirotest.repository.memory;

import java.util.Objects;

public class Node {

    String nodeId;

    int mbrMinX = Integer.MAX_VALUE;
    int mbrMinY = Integer.MAX_VALUE;
    int mbrMaxX = -Integer.MAX_VALUE;
    int mbrMaxY = -Integer.MAX_VALUE;

    int[] childrenMinX;
    int[] childrenMinY;
    int[] childrenMaxX;
    int[] childrenMaxY;

    String[] ids;
    int level;
    int entryCount;

    Node(String nodeId, int level, int maxNodeChildren) {
        this.nodeId = nodeId;
        this.level = level;
        childrenMinX = new int[maxNodeChildren];
        childrenMinY = new int[maxNodeChildren];
        childrenMaxX = new int[maxNodeChildren];
        childrenMaxY = new int[maxNodeChildren];
        ids = new String[maxNodeChildren];
    }

    void addEntry(int minX, int minY, int maxX, int maxY, String id) {
        ids[entryCount] = id;
        childrenMinX[entryCount] = minX;
        childrenMinY[entryCount] = minY;
        childrenMaxX[entryCount] = maxX;
        childrenMaxY[entryCount] = maxY;

        if (minX < mbrMinX) mbrMinX = minX;
        if (minY < mbrMinY) mbrMinY = minY;
        if (maxX > mbrMaxX) mbrMaxX = maxX;
        if (maxY > mbrMaxY) mbrMaxY = maxY;

        entryCount++;
    }

    int findEntry(int minX, int minY, int maxX, int maxY, String id) {
        for (int i = 0; i < entryCount; i++) {
            if (Objects.equals(id, ids[i]) &&
                    childrenMinX[i] == minX && childrenMinY[i] == minY &&
                    childrenMaxX[i] == maxX && childrenMaxY[i] == maxY) {
                return i;
            }
        }
        return -1;
    }

}
