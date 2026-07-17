/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.controller.hints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A human-readable hint and the single board change offered by its Apply button.
 */
public final class GameHint {

    public enum Action {
        PLACE_VALUE,
        CLEAR_VALUE
    }

    private final String title;
    private final String summary;
    private final List<String> details;
    private final int row;
    private final int col;
    private final int value;
    private final Action action;

    GameHint(String title, String summary, List<String> details,
             int row, int col, int value, Action action) {
        this.title = title;
        this.summary = summary;
        this.details = Collections.unmodifiableList(new ArrayList<>(details));
        this.row = row;
        this.col = col;
        this.value = value;
        this.action = action;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getDetails() {
        return details;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public int getValue() {
        return value;
    }

    public Action getAction() {
        return action;
    }
}
