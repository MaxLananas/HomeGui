package com.maxlananas.homegui.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The set of interactive regions produced by one layout pass, plus focus.
 *
 * <p>Addition order is the focus order, which keeps the tab sequence identical to
 * the visual order without any extra bookkeeping.
 */
public final class UiSurface {

    private final List<UiElement> elements = new ArrayList<>();
    private String focusedId;

    /** Starts a new layout pass. */
    public void begin() {
        elements.clear();
    }

    public UiElement add(String id, UiElement.Role role, int x, int y, int width, int height) {
        UiElement element = new UiElement(id, role, x, y, width, height);
        elements.add(element);
        return element;
    }

    public void add(UiElement element) {
        elements.add(element);
    }

    public List<UiElement> elements() {
        return Collections.unmodifiableList(elements);
    }

    public int count() { return elements.size(); }

    public UiElement byId(String id) {
        if (id == null) return null;
        for (UiElement element : elements) {
            if (element.id.equals(id)) return element;
        }
        return null;
    }

    /** Keeps focus on the element with this id, dropping it when that element is gone. */
    public void retainFocus(String id) {
        UiElement element = byId(id);
        focusedId = element != null && element.isFocusable() ? id : null;
    }

    public String focusedId() { return focusedId; }

    public void setFocusedId(String id) {
        UiElement element = byId(id);
        focusedId = element != null && element.isFocusable() ? id : null;
    }

    public UiElement focused() { return byId(focusedId); }

    /** The element under the pointer, searching from the top of the paint order. */
    public UiElement hit(int px, int py) {
        for (int i = elements.size() - 1; i >= 0; i--) {
            UiElement element = elements.get(i);
            if (element.enabled && element.contains(px, py)) return element;
        }
        return null;
    }

    public UiElement firstFocusable() {
        for (UiElement element : elements) {
            if (element.isFocusable()) return element;
        }
        return null;
    }

    /**
     * Moves focus by {@code direction} positions, skipping disabled elements and
     * wrapping around. Returns the newly focused element, or null when there is
     * nothing focusable at all.
     */
    public UiElement moveFocus(int direction) {
        List<UiElement> focusable = new ArrayList<>();
        for (UiElement element : elements) {
            if (element.isFocusable()) focusable.add(element);
        }
        if (focusable.isEmpty()) {
            focusedId = null;
            return null;
        }
        int current = -1;
        if (focusedId != null) {
            for (int i = 0; i < focusable.size(); i++) {
                if (focusable.get(i).id.equals(focusedId)) {
                    current = i;
                    break;
                }
            }
        }
        int next = current < 0
                ? (direction > 0 ? 0 : focusable.size() - 1)
                : Math.floorMod(current + direction, focusable.size());
        focusedId = focusable.get(next).id;
        return focusable.get(next);
    }
}
