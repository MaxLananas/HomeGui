package com.maxlananas.homegui.ui;

import java.util.ArrayList;
import java.util.List;

/** Records what the interface asked for, so painting can be asserted without a client. */
final class RecordingPainter implements Painter {

    final List<String> operations = new ArrayList<>();
    final List<int[]> rects = new ArrayList<>();
    int fills;

    @Override
    public void fill(int x, int y, int width, int height, int argb) {
        fills++;
        rects.add(new int[]{x, y, width, height});
        operations.add("fill");
    }

    /** Total painted area, so scaling can be asserted without counting pixels by hand. */
    int area() {
        int total = 0;
        for (int[] rect : rects) total += rect[2] * rect[3];
        return total;
    }

    @Override
    public void text(String text, int x, int y, int argb, boolean shadow) {
        operations.add("text:" + text);
    }

    @Override
    public void centeredText(String text, int centerX, int y, int argb, boolean shadow) {
        operations.add("center:" + text);
    }

    @Override
    public int textWidth(String text) {
        return text == null ? 0 : text.length() * 6;
    }

    @Override
    public int lineHeight() {
        return 9;
    }

    @Override
    public void clip(int x, int y, int width, int height) {
        operations.add("clip");
    }

    @Override
    public void endClip() {
        operations.add("endClip");
    }

    boolean painted(String needle) {
        for (String operation : operations) {
            if (operation.contains(needle)) return true;
        }
        return false;
    }
}
