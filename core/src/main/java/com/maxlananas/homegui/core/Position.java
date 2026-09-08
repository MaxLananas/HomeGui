package com.maxlananas.homegui.core;

/**
 * A client-side position sample. Carried through the core so that teleport
 * confirmation and coordinate capture can be tested without Minecraft.
 */
public final class Position {

    public final double x;
    public final double y;
    public final double z;
    public final String dimension;

    public Position(double x, double y, double z, String dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension == null ? "" : dimension;
    }

    public static Position of(double x, double y, double z) {
        return new Position(x, y, z, "overworld");
    }

    /** Horizontal-and-vertical distance in blocks, ignoring dimension. */
    public double distanceTo(Position other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public boolean sameDimension(Position other) {
        return dimension.equals(other.dimension);
    }

    public Home.Coordinates toCoordinates() {
        return new Home.Coordinates((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    @Override
    public String toString() {
        return "Position[" + x + ", " + y + ", " + z + ", " + dimension + "]";
    }
}
