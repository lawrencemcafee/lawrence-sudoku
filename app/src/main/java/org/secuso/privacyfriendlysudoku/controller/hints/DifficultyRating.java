/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.controller.hints;

import org.secuso.privacyfriendlysudoku.game.DifficultyLevel;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Immutable evidence produced by a complete human-style grading pass. */
public final class DifficultyRating {
    private final DifficultyLevel level;
    private final HumanTechnique hardestTechnique;
    private final Map<HumanTechnique, Integer> techniqueCounts;
    private final int logicalSteps;
    private final int givens;
    private final double clueRatio;
    private final int minimumBasicMoves;
    private final double averageBasicMoves;
    private final boolean forcingRequired;

    DifficultyRating(DifficultyLevel level, HumanTechnique hardestTechnique,
                     Map<HumanTechnique, Integer> techniqueCounts, int logicalSteps,
                     int givens, int cellCount, int minimumBasicMoves,
                     double averageBasicMoves, boolean forcingRequired) {
        this.level = level;
        this.hardestTechnique = hardestTechnique;
        this.techniqueCounts = Collections.unmodifiableMap(new EnumMap<>(techniqueCounts));
        this.logicalSteps = logicalSteps;
        this.givens = givens;
        this.clueRatio = cellCount == 0 ? 0 : (double) givens / cellCount;
        this.minimumBasicMoves = minimumBasicMoves;
        this.averageBasicMoves = averageBasicMoves;
        this.forcingRequired = forcingRequired;
    }

    public DifficultyLevel getLevel() { return level; }
    public HumanTechnique getHardestTechnique() { return hardestTechnique; }
    public Map<HumanTechnique, Integer> getTechniqueCounts() { return techniqueCounts; }
    public int getLogicalSteps() { return logicalSteps; }
    public int getGivens() { return givens; }
    public double getClueRatio() { return clueRatio; }
    public int getMinimumBasicMoves() { return minimumBasicMoves; }
    public double getAverageBasicMoves() { return averageBasicMoves; }
    public boolean isForcingRequired() { return forcingRequired; }
}
