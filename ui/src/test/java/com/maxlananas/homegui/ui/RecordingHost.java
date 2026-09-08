package com.maxlananas.homegui.ui;

import java.util.ArrayList;
import java.util.List;

/** Stands in for the loader bridge. */
final class RecordingHost implements UiHost, HomeGuiUi.ClipboardHost {

    long now = 1_000_000L;
    int invalidations;
    int refreshes;
    int closes;
    String clipboard = "";
    final List<String> announced = new ArrayList<>();

    @Override
    public void close() {
        closes++;
    }

    @Override
    public void refresh() {
        refreshes++;
    }

    @Override
    public void announce(String message) {
        announced.add(message);
    }

    @Override
    public void invalidate() {
        invalidations++;
    }

    @Override
    public long now() {
        return now;
    }

    @Override
    public String clipboard() {
        return clipboard;
    }
}
