package com.maxlananas.homegui.ui;

/** An axis aligned rectangle in GUI pixels. */
public final class Rect {

    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public Rect(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    public int right() { return x + width; }

    public int bottom() { return y + height; }

    public boolean contains(int px, int py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }

    public boolean isEmpty() { return width <= 0 || height <= 0; }

    /** Shrinks the rectangle on all four sides. */
    public Rect inset(int amount) {
        return new Rect(x + amount, y + amount, width - amount * 2, height - amount * 2);
    }

    public Rect withX(int newX) { return new Rect(newX, y, width, height); }

    public Rect withY(int newY) { return new Rect(x, newY, width, height); }

    public Rect withSize(int newWidth, int newHeight) {
        return new Rect(x, y, newWidth, newHeight);
    }

    @Override
    public String toString() {
        return "Rect[" + x + ", " + y + ", " + width + "x" + height + "]";
    }
}
