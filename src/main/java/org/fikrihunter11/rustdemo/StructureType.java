package org.fikrihunter11.rustdemo;

import org.bukkit.Material;

public enum StructureType {
    FOUNDATION(5, 2, 5, Material.STONE),
    WALL(5, 3, 1, Material.BRICKS),
    FLOOR(5, 1, 3, Material.OAK_PLANKS),
    STAIRS(1, 3, 1, Material.LADDER),
    WINDOW(3, 3, 1, Material.GLASS);

    private final int width, height, depth;
    private final Material material;

    StructureType(int width, int height, int depth, Material material) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.material = material;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }

    public Material getMaterial() {
        return material;
    }
}

