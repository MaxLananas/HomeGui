package com.maxlananas.homegui.ui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Icons drawn from bitmaps instead of Unicode glyphs.
 *
 * <p>The previous interface leaned on {@code ★ ⚙ 📊 ⟳ ▦}, which render as missing
 * boxes with a lot of resource packs and with the vanilla font's fallback. Drawing
 * the marks ourselves means they look identical on every loader, every version, and
 * every resource pack, and it costs nothing: an icon is a handful of one pixel
 * fills, batched like any other GUI quad.
 */
public final class Icons {

    /** Edge length of every icon in bitmap pixels. */
    public static final int SIZE = 9;

    public static final String STAR = "star";
    public static final String SEARCH = "search";
    public static final String LIST = "list";
    public static final String GRID = "grid";
    public static final String REFRESH = "refresh";
    public static final String CLOSE = "close";
    public static final String BACK = "back";
    public static final String CLOCK = "clock";
    public static final String CHART = "chart";
    public static final String GEAR = "gear";
    public static final String EXPORT = "export";
    public static final String IMPORT = "import";
    public static final String CHEVRON_DOWN = "chevron_down";
    public static final String CHEVRON_UP = "chevron_up";
    public static final String CHECK = "check";
    public static final String WARN = "warn";
    public static final String HOME = "home";
    public static final String SORT = "sort";
    public static final String BOX = "box";

    private static final Map<String, boolean[]> BITMAPS = build();

    private Icons() {}

    private static Map<String, boolean[]> build() {
        Map<String, boolean[]> bitmaps = new LinkedHashMap<>();
        put(bitmaps, STAR,
                "....#....",
                "....#....",
                "...###...",
                "#########",
                ".#######.",
                "..#####..",
                "..##.##..",
                ".##...##.",
                "##.....##");
        put(bitmaps, SEARCH,
                "..####...",
                ".#....#..",
                "#......#.",
                "#......#.",
                "#......#.",
                ".#....#..",
                "..####...",
                "....##...",
                "......##.");
        put(bitmaps, LIST,
                "#........",
                "#########",
                ".........",
                "#........",
                "#########",
                ".........",
                "#........",
                "#########",
                ".........");
        put(bitmaps, GRID,
                "###.###..",
                "###.###..",
                "###.###..",
                ".........",
                "###.###..",
                "###.###..",
                "###.###..",
                ".........",
                ".........");
        put(bitmaps, REFRESH,
                ".######..",
                "#......#.",
                "#........",
                "#........",
                "#....###.",
                "#......#.",
                ".######..",
                "...#.....",
                "..###....");
        put(bitmaps, CLOSE,
                "#.......#",
                ".#.....#.",
                "..#...#..",
                "...#.#...",
                "....#....",
                "...#.#...",
                "..#...#..",
                ".#.....#.",
                "#.......#");
        put(bitmaps, BACK,
                "....#....",
                "...##....",
                "..###....",
                ".####....",
                "#########",
                ".####....",
                "..###....",
                "...##....",
                "....#....");
        put(bitmaps, CLOCK,
                ".#######.",
                "#.......#",
                "#...#...#",
                "#...#...#",
                "#...#...#",
                "#...##..#",
                "#.......#",
                ".#######.",
                ".........");
        put(bitmaps, CHART,
                ".........",
                ".......#.",
                ".......#.",
                "...#...#.",
                "...#...#.",
                "#..#...#.",
                "#..#.#.#.",
                "#..#.#.#.",
                "#########");
        put(bitmaps, GEAR,
                "..#...#..",
                ".#######.",
                "##.###.##",
                "#..###..#",
                "...###...",
                "#..###..#",
                "##.###.##",
                ".#######.",
                "..#...#..");
        put(bitmaps, EXPORT,
                "....#....",
                "...###...",
                "..#####..",
                "...###...",
                "....#....",
                ".........",
                ".#######.",
                ".#.....#.",
                ".#######.");
        put(bitmaps, IMPORT,
                "....#....",
                "....#....",
                "....#....",
                "...###...",
                "..#####..",
                ".........",
                ".#######.",
                ".#.....#.",
                ".#######.");
        put(bitmaps, CHEVRON_DOWN,
                ".........",
                ".........",
                "#.......#",
                ".#.....#.",
                "..#...#..",
                "...#.#...",
                "....#....",
                ".........",
                ".........");
        put(bitmaps, CHEVRON_UP,
                ".........",
                ".........",
                "....#....",
                "...#.#...",
                "..#...#..",
                ".#.....#.",
                "#.......#",
                ".........",
                ".........");
        put(bitmaps, CHECK,
                ".........",
                ".......##",
                "......##.",
                "#....##..",
                ".#..##...",
                "..###....",
                "..##.....",
                ".#.......",
                ".........");
        put(bitmaps, WARN,
                "....#....",
                "...###...",
                "..#.#.#..",
                "..#.#.#..",
                ".#.###.#.",
                ".#..#..#.",
                "#..###..#",
                "#########",
                ".........");
        put(bitmaps, HOME,
                "....#....",
                "...###...",
                "..#####..",
                ".#######.",
                "#########",
                ".##...##.",
                ".##...##.",
                ".##...##.",
                ".#######.");
        put(bitmaps, SORT,
                "#########",
                ".........",
                "######...",
                ".........",
                "####.....",
                ".........",
                "##.......",
                "....#....",
                "...###...");
        put(bitmaps, BOX,
                ".........",
                "#.......#",
                "#.......#",
                "#.......#",
                "#.......#",
                "#.......#",
                "#.......#",
                ".#######.",
                ".........");
        return Collections.unmodifiableMap(bitmaps);
    }

    private static void put(Map<String, boolean[]> bitmaps, String name, String... rows) {
        if (rows.length != SIZE) {
            throw new IllegalStateException("icon " + name + " must have " + SIZE + " rows");
        }
        boolean[] pixels = new boolean[SIZE * SIZE];
        for (int row = 0; row < SIZE; row++) {
            if (rows[row].length() != SIZE) {
                throw new IllegalStateException("icon " + name + " row " + row + " must be " + SIZE + " wide");
            }
            for (int column = 0; column < SIZE; column++) {
                pixels[row * SIZE + column] = rows[row].charAt(column) == '#';
            }
        }
        bitmaps.put(name, pixels);
    }

    public static boolean isKnown(String name) {
        return name != null && BITMAPS.containsKey(name);
    }

    /** Drawn edge length for a scale factor. */
    public static int sizeAt(int scale) {
        return SIZE * Math.max(1, scale);
    }

    /**
     * Draws an icon. Unknown names draw nothing rather than throwing, so a typo can
     * never take the interface down.
     */
    public static void paint(Painter painter, String name, int x, int y, int scale, int argb) {
        boolean[] pixels = BITMAPS.get(name);
        if (pixels == null) return;
        int step = Math.max(1, scale);
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                if (pixels[row * SIZE + column]) {
                    painter.fill(x + column * step, y + row * step, step, step, argb);
                }
            }
        }
    }
}
