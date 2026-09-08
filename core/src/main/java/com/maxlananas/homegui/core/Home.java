package com.maxlananas.homegui.core;

import java.util.Objects;
import java.util.Optional;

/**
 * One home as reported by a server.
 *
 * <p>{@code world} and {@code coordinates} are optional because most home plugins
 * only print a bare name. When a plugin does print them (CMI, EssentialsX with
 * {@code homes-list-verbose}) HomeGui shows the real position instead of the
 * approximate one captured after a teleport.
 */
public final class Home {

    private final String name;
    private final String world;
    private final Coordinates coordinates;

    public Home(String name, String world, Coordinates coordinates) {
        this.name = Objects.requireNonNull(name, "name");
        this.world = world == null ? "" : world;
        this.coordinates = coordinates;
    }

    public static Home of(String name) {
        return new Home(name, null, null);
    }

    /** Creates a home from a validated name, returning empty for anything unsafe. */
    public static Optional<Home> parse(String name) {
        return HomeNames.validate(name).map(Home::of);
    }

    public String name() { return name; }

    public String key() { return HomeNames.key(name); }

    public String world() { return world; }

    public boolean hasWorld() { return !world.isEmpty(); }

    public Optional<Coordinates> coordinates() { return Optional.ofNullable(coordinates); }

    public boolean hasCoordinates() { return coordinates != null; }

    public Home withCoordinates(String world, Coordinates coordinates) {
        return new Home(name, world, coordinates);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Home)) return false;
        Home that = (Home) other;
        return key().equals(that.key());
    }

    @Override
    public int hashCode() { return key().hashCode(); }

    @Override
    public String toString() { return "Home[" + name + "]"; }

    /** A block position reported by the server. */
    public static final class Coordinates {
        public final int x;
        public final int y;
        public final int z;

        public Coordinates(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        /** Compact form used in the interface: {@code 100 64 -200}. */
        public String shortText() { return x + " " + y + " " + z; }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Coordinates)) return false;
            Coordinates that = (Coordinates) other;
            return x == that.x && y == that.y && z == that.z;
        }

        @Override
        public int hashCode() { return Objects.hash(x, y, z); }
    }
}
