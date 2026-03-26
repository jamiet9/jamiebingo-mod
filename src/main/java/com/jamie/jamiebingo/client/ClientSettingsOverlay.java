package com.jamie.jamiebingo.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientSettingsOverlay {

    private static List<String> lines = new ArrayList<>();

    public static void setLines(List<String> newLines) {
        lines = new ArrayList<>(newLines);
    }

    public static List<String> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public static void clear() {
        lines.clear();
    }
}
