package com.maxlananas.homegui.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IconsTest {

    private static final String[] ALL = {
            Icons.STAR, Icons.SEARCH, Icons.LIST, Icons.GRID, Icons.REFRESH, Icons.CLOSE, Icons.BACK,
            Icons.CLOCK, Icons.CHART, Icons.GEAR, Icons.EXPORT, Icons.IMPORT, Icons.CHEVRON_DOWN,
            Icons.CHEVRON_UP, Icons.CHECK, Icons.WARN, Icons.HOME, Icons.SORT, Icons.BOX,
    };

    @Test
    void everyIconIsKnownAndActuallyDrawsSomething() {
        for (String name : ALL) {
            assertTrue(Icons.isKnown(name), name);
            RecordingPainter painter = new RecordingPainter();
            Icons.paint(painter, name, 0, 0, 1, 0xFFFFFFFF);
            assertTrue(painter.fills > 0, name + " drew nothing");
        }
    }

    @Test
    void anUnknownIconIsIgnoredInsteadOfThrowing() {
        RecordingPainter painter = new RecordingPainter();
        Icons.paint(painter, "not-an-icon", 0, 0, 1, 0xFFFFFFFF);
        Icons.paint(painter, null, 0, 0, 1, 0xFFFFFFFF);
        assertEquals(0, painter.fills);
        assertFalse(Icons.isKnown("not-an-icon"));
        assertFalse(Icons.isKnown(null));
    }

    @Test
    void scalingMultipliesThePixelCount() {
        RecordingPainter one = new RecordingPainter();
        Icons.paint(one, Icons.STAR, 0, 0, 1, 0xFFFFFFFF);
        RecordingPainter two = new RecordingPainter();
        Icons.paint(two, Icons.STAR, 0, 0, 2, 0xFFFFFFFF);
        assertEquals(one.fills * 4, two.fills);
    }

    @Test
    void theDrawnSizeFollowsTheScale() {
        assertEquals(Icons.SIZE, Icons.sizeAt(1));
        assertEquals(Icons.SIZE * 3, Icons.sizeAt(3));
        assertEquals(Icons.SIZE, Icons.sizeAt(0), "a scale below one is clamped, never zero");
    }
}
