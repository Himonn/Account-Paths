package com.accountpaths;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.runelite.api.coords.WorldPoint;

public class CollisionMap extends SplitFlagMap {
    public CollisionMap(int regionSize, Map<Position, byte[]> compressedRegions) {
        super(regionSize, compressedRegions, 2);
    }

    public boolean n(int x, int y, int z) {
        return get(x, y, z, 0);
    }

    public boolean s(int x, int y, int z) {
        return n(x, y - 1, z);
    }

    public boolean e(int x, int y, int z) {
        return get(x, y, z, 1);
    }

    public boolean w(int x, int y, int z) {
        return e(x - 1, y, z);
    }

    private boolean ne(int x, int y, int z) {
        return n(x, y, z) && e(x, y + 1, z) && e(x, y, z) && n(x + 1, y, z);
    }

    private boolean nw(int x, int y, int z) {
        return n(x, y, z) && w(x, y + 1, z) && w(x, y, z) && n(x - 1, y, z);
    }

    private boolean se(int x, int y, int z) {
        return s(x, y, z) && e(x, y - 1, z) && e(x, y, z) && s(x + 1, y, z);
    }

    private boolean sw(int x, int y, int z) {
        return s(x, y, z) && w(x, y - 1, z) && w(x, y, z) && s(x - 1, y, z);
    }

    public boolean isBlocked(int x, int y, int z) {
        return !n(x, y, z) && !s(x, y, z) && !e(x, y, z) && !w(x, y, z);
    }

    public List<WorldPoint> getNeighbors(WorldPoint position) {
        int x = position.getX();
        int y = position.getY();
        int z = position.getPlane();

        List<WorldPoint> neighbors = new ArrayList<>();
        boolean[] traversable;
        if (isBlocked(x, y, z)) {
            boolean westBlocked = isBlocked(x - 1, y, z);
            boolean eastBlocked = isBlocked(x + 1, y, z);
            boolean southBlocked = isBlocked(x, y - 1, z);
            boolean northBlocked = isBlocked(x, y + 1, z);
            boolean southWestBlocked = isBlocked(x - 1, y - 1, z);
            boolean southEastBlocked = isBlocked(x + 1, y - 1, z);
            boolean northWestBlocked = isBlocked(x - 1, y + 1, z);
            boolean northEastBlocked = isBlocked(x + 1, y + 1, z);
            traversable = new boolean[] {
                    !westBlocked,
                    !eastBlocked,
                    !southBlocked,
                    !northBlocked,
                    !southWestBlocked && !westBlocked && !southBlocked,
                    !southEastBlocked && !eastBlocked && !southBlocked,
                    !northWestBlocked && !westBlocked && !northBlocked,
                    !northEastBlocked && !eastBlocked && !northBlocked
            };
        } else {
            traversable = new boolean[] {
                    w(x, y, z), e(x, y, z), s(x, y, z), n(x, y, z), sw(x, y, z), se(x, y, z), nw(x, y, z), ne(x, y, z)
            };
        }

        for (int i = 0; i < traversable.length; i++) {
            if (traversable[i]) {
                OrdinalDirection direction = OrdinalDirection.values()[i];
                neighbors.add(position.dx(direction.x).dy(direction.y));
            }
        }

        return neighbors;
    }

    public static CollisionMap fromResources() {
        Map<SplitFlagMap.Position, byte[]> compressedRegions = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(AccountPathsPlugin.class.getResourceAsStream("/collision-map.zip"))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String[] n = entry.getName().split("_");

                compressedRegions.put(
                        new SplitFlagMap.Position(Integer.parseInt(n[0]), Integer.parseInt(n[1])),
                        Util.readAllBytes(in)
                );
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new CollisionMap(64, compressedRegions);
    }
}
