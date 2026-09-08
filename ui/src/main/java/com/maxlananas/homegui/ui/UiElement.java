package com.maxlananas.homegui.ui;

/**
 * One interactive region of the interface.
 *
 * <p>Elements are described here, in loader-independent code, and materialised as
 * real Minecraft widgets by the bridge for the running version. That split is what
 * gives every loader the same layout, the same focus order and the same narration,
 * while still using vanilla widgets for hover, focus and screen reader support.
 */
public final class UiElement {

    /** What kind of control this is. Drives rendering and narration. */
    public enum Role {
        BUTTON,
        TOGGLE,
        TAB,
        FIELD,
        ITEM,
        CARD,
        SCROLL,
        LINK
    }

    public final String id;
    public final Role role;

    /** Accessible name; also what the screen reader announces. */
    public String label = "";
    /** Secondary text: a value, a hint, or the right hand side of a setting row. */
    public String value = "";
    /** Extra narration detail, for example "3 of 12". */
    public String hint = "";
    /** Icon key from {@link Icons}, or empty. */
    public String icon = "";

    public int x;
    public int y;
    public int width;
    public int height;

    public boolean enabled = true;
    public boolean checked;
    public boolean destructive;
    /** Set for the primary action in a group, which gets the accent treatment. */
    public boolean primary;
    /** Index within a list, used for arrow key navigation. */
    public int index = -1;
    /** Payload the activation handler needs, usually a home name. */
    public String payload = "";

    public UiElement(String id, Role role, int x, int y, int width, int height) {
        this.id = id;
        this.role = role;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains(int px, int py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }

    public boolean isFocusable() {
        return enabled && role != Role.SCROLL;
    }

    public String narration() {
        StringBuilder text = new StringBuilder();
        text.append(label);
        if (!value.isEmpty()) text.append(": ").append(value);
        switch (role) {
            case TOGGLE:
                text.append(checked ? " (on)" : " (off)");
                break;
            case TAB:
                text.append(" tab");
                break;
            case FIELD:
                text.append(" edit");
                break;
            default:
                break;
        }
        if (!hint.isEmpty()) text.append(". ").append(hint);
        return text.toString();
    }
}
