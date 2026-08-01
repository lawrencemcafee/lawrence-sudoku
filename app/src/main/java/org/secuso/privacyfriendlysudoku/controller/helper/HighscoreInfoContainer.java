/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.controller.helper;

import org.secuso.privacyfriendlysudoku.controller.GameController;
import org.secuso.privacyfriendlysudoku.game.DifficultyLevel;
import org.secuso.privacyfriendlysudoku.game.GameDifficulty;
import org.secuso.privacyfriendlysudoku.game.GameType;

/** Aggregate statistics for one board type and one exact difficulty level. */
public class HighscoreInfoContainer {
    private GameType type;
    private DifficultyLevel difficulty;
    private int minTime = Integer.MAX_VALUE;
    private int time;
    private int numberOfHintsUsed;
    private int numberOfGames;
    private int numberOfGamesNoHints;
    private int timeNoHints;

    public HighscoreInfoContainer() {}

    public HighscoreInfoContainer(GameType type, DifficultyLevel difficulty) {
        this.type = type;
        this.difficulty = difficulty;
    }

    public void add(GameController controller) {
        difficulty = difficulty == null ? controller.getDifficulty() : difficulty;
        type = type == null ? controller.getGameType() : type;
        numberOfGames++;
        if(controller.getUsedHints() == 0) {
            minTime = Math.min(minTime, controller.getTime());
            timeNoHints += controller.getTime();
            numberOfGamesNoHints++;
        }
    }

    public void merge(HighscoreInfoContainer other) {
        if(other == null) return;
        time += other.time;
        numberOfHintsUsed += other.numberOfHintsUsed;
        numberOfGames += other.numberOfGames;
        minTime = Math.min(minTime, other.minTime);
        numberOfGamesNoHints += other.numberOfGamesNoHints;
        timeNoHints += other.timeNoHints;
    }

    public void incHints() { numberOfHintsUsed++; }
    public void incTime() { time++; }

    public void setInfosFromFile(String serialized) {
        if(serialized == null || serialized.isEmpty()) return;
        String[] values = serialized.split("/");
        if(values.length != 8) throw new IllegalArgumentException("Invalid statistics record.");
        time = nonNegative(values[0]);
        numberOfHintsUsed = nonNegative(values[1]);
        numberOfGames = nonNegative(values[2]);
        minTime = nonNegative(values[3]);
        type = GameType.valueOf(values[4]);
        difficulty = parseDifficulty(values[5]);
        numberOfGamesNoHints = nonNegative(values[6]);
        timeNoHints = nonNegative(values[7]);
    }

    public DifficultyLevel getDifficulty() { return difficulty; }
    public GameType getGameType() { return type; }
    public int getTime() { return time; }
    public int getMinTime() { return minTime; }
    public int getNumberOfHintsUsed() { return numberOfHintsUsed; }
    public int getNumberOfGames() { return numberOfGames; }
    public int getNumberOfGamesNoHints() { return numberOfGamesNoHints; }
    public int getTimeNoHints() { return timeNoHints; }

    public String getActualStats() {
        return time + "/" + numberOfHintsUsed + "/" + numberOfGames + "/" + minTime
                + "/" + type.name() + "/" + difficulty.getValue() + "/"
                + numberOfGamesNoHints + "/" + timeNoHints;
    }

    private static int nonNegative(String value) {
        int result = Integer.parseInt(value);
        if(result < 0) throw new IllegalArgumentException("Statistics cannot be negative.");
        return result;
    }

    private static DifficultyLevel parseDifficulty(String value) {
        try {
            return DifficultyLevel.parse(value);
        } catch(IllegalArgumentException exactFailure) {
            switch(GameDifficulty.valueOf(value)) {
                case Easy: return DifficultyLevel.of(3);
                case Moderate: return DifficultyLevel.of(5);
                case Hard: return DifficultyLevel.of(7);
                case Challenge: return DifficultyLevel.of(9);
                default: return DifficultyLevel.DEFAULT;
            }
        }
    }
}
