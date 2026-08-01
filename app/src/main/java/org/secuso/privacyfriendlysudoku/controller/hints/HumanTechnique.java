/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.controller.hints;

/** Ordered human techniques shared by the hint engine and difficulty grader. */
public enum HumanTechnique {
    LAST_DIGIT("Last Digit", 1),
    NAKED_SINGLE("Naked Single", 1),
    HIDDEN_SINGLE("Hidden Single", 3),
    POINTING_CANDIDATES("Pointing Candidates", 4),
    CLAIMING_CANDIDATES("Claiming Candidates", 4),
    NAKED_PAIR("Naked Pair", 5),
    HIDDEN_PAIR("Hidden Pair", 5),
    NAKED_TRIPLE("Naked Triple", 6),
    HIDDEN_TRIPLE("Hidden Triple", 6),
    NAKED_QUAD("Naked Quad", 6),
    HIDDEN_QUAD("Hidden Quad", 6),
    X_WING("X-Wing", 7),
    SKYSCRAPER("Skyscraper", 7),
    TWO_STRING_KITE("2-String Kite", 7),
    SWORDFISH("Swordfish", 8),
    XY_WING("XY-Wing", 8),
    XYZ_WING("XYZ-Wing", 8),
    W_WING("W-Wing", 8),
    SIMPLE_COLORING("Simple Coloring", 8),
    JELLYFISH("Jellyfish", 9),
    X_CHAIN("X-Chain", 9),
    XY_CHAIN("XY-Chain", 9),
    FORCING_CHAIN("Forcing Chain", 10);

    private final String title;
    private final int baseLevel;

    HumanTechnique(String title, int baseLevel) {
        this.title = title;
        this.baseLevel = baseLevel;
    }

    public String getTitle() {
        return title;
    }

    public int getBaseLevel() {
        return baseLevel;
    }

    public static HumanTechnique fromTitle(String title) {
        for(HumanTechnique technique : values()) {
            if(technique.title.equals(title)) {
                return technique;
            }
        }
        throw new IllegalArgumentException("Unknown human technique: " + title);
    }
}
