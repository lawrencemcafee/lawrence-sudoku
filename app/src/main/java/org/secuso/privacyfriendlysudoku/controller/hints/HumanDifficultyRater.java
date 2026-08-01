/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.controller.hints;

import org.secuso.privacyfriendlysudoku.controller.Symbol;
import org.secuso.privacyfriendlysudoku.controller.qqwing.QQWing;
import org.secuso.privacyfriendlysudoku.game.DifficultyLevel;
import org.secuso.privacyfriendlysudoku.game.GameBoard;
import org.secuso.privacyfriendlysudoku.game.GameDifficulty;
import org.secuso.privacyfriendlysudoku.game.GameType;

import java.util.EnumMap;
import java.util.Map;

/** Rates a unique puzzle by executing the same ordered human rules used by hints. */
public final class HumanDifficultyRater {
    private HumanDifficultyRater() {}

    public static DifficultyRating rate(GameType gameType, int[] puzzle) {
        QQWing solver = new QQWing(gameType, GameDifficulty.Unspecified);
        if(!solver.setPuzzle(puzzle) || !solver.hasUniqueSolution()) {
            throw new IllegalArgumentException("Difficulty can only be rated for a valid unique puzzle.");
        }
        solver.setPuzzle(puzzle);
        if(!solver.solve()) {
            throw new IllegalArgumentException("Puzzle cannot be solved.");
        }
        return rate(gameType, puzzle, solver.getSolution());
    }

    public static DifficultyRating rate(GameType gameType, int[] puzzle, int[] solution) {
        int cellCount = gameType.getSize() * gameType.getSize();
        if(puzzle == null || puzzle.length != cellCount
                || solution == null || solution.length != cellCount) {
            throw new IllegalArgumentException("Puzzle and solution must match the game type.");
        }

        GameBoard board = new GameBoard(gameType);
        board.initCells(puzzle);
        CandidateState state = CandidateState.fromBoard(board, solution);
        Map<HumanTechnique, Integer> counts = new EnumMap<>(HumanTechnique.class);
        int givens = 0;
        for(int value : puzzle) if(value != 0) givens++;

        HumanTechnique hardest = HumanTechnique.LAST_DIGIT;
        int steps = 0;
        int basicSamples = 0;
        int basicTotal = 0;
        int minimumBasicMoves = Integer.MAX_VALUE;
        boolean forcingRequired = false;
        int maximumSteps = cellCount * gameType.getSize() * 2;

        while(!state.isComplete() && steps < maximumSteps) {
            int basicMoves = state.countBasicMoves();
            basicSamples++;
            basicTotal += basicMoves;
            minimumBasicMoves = Math.min(minimumBasicMoves, basicMoves);

            // For grading, exhausting every named logical rule is enough to establish level 10.
            // Building the full contradiction proof is reserved for the interactive hint itself.
            GameHint hint = HumanHintEngine.findNextHint(state, solution, Symbol.Default, false);
            if(hint == null) {
                forcingRequired = true;
                hardest = HumanTechnique.FORCING_CHAIN;
                counts.put(hardest, count(counts, hardest) + 1);
                break;
            }

            HumanTechnique technique = HumanTechnique.fromTitle(hint.getTitle());
            counts.put(technique, count(counts, technique) + 1);
            if(technique.getBaseLevel() > hardest.getBaseLevel()) hardest = technique;
            state.apply(hint);
            steps++;
        }

        if(!state.isComplete() && !forcingRequired) {
            forcingRequired = true;
            hardest = HumanTechnique.FORCING_CHAIN;
            counts.put(hardest, count(counts, hardest) + 1);
        }

        double averageBasicMoves = basicSamples == 0 ? 0 : (double) basicTotal / basicSamples;
        if(minimumBasicMoves == Integer.MAX_VALUE) minimumBasicMoves = 0;
        DifficultyLevel level = DifficultyLevel.of(calculateLevel(
                hardest, counts, steps, givens, cellCount, gameType.getSize(), averageBasicMoves));
        return new DifficultyRating(level, hardest, counts, steps, givens, cellCount,
                minimumBasicMoves, averageBasicMoves, forcingRequired);
    }

    private static int calculateLevel(HumanTechnique hardest,
                                      Map<HumanTechnique, Integer> counts,
                                      int steps, int givens, int cellCount, int size,
                                      double averageBasicMoves) {
        int base = hardest.getBaseLevel();
        if(base == 1) {
            double clueRatio = (double) givens / cellCount;
            boolean introductory = clueRatio >= 0.60 || (clueRatio >= 0.50
                    && averageBasicMoves >= Math.max(2.0, size / 3.0));
            return introductory ? 1 : 2;
        }
        if(base == 3) {
            int hiddenSingles = count(counts, HumanTechnique.HIDDEN_SINGLE);
            return hiddenSingles <= Math.max(3, size - 1)
                    && steps <= (cellCount - givens) + size / 2 ? 3 : 4;
        }
        if(base == 4) {
            int lockedCandidates = count(counts, HumanTechnique.POINTING_CANDIDATES)
                    + count(counts, HumanTechnique.CLAIMING_CANDIDATES);
            if(lockedCandidates <= 1) return 4;
            return lockedCandidates == 2 ? 5 : 6;
        }
        if(base == 5) {
            int pairs = count(counts, HumanTechnique.NAKED_PAIR)
                    + count(counts, HumanTechnique.HIDDEN_PAIR);
            return pairs <= 1 ? 5 : 6;
        }
        if(base == 7) {
            int hardSteps = 0;
            for(Map.Entry<HumanTechnique, Integer> entry : counts.entrySet()) {
                if(entry.getKey().getBaseLevel() >= 7) hardSteps += entry.getValue();
            }
            return hardSteps <= 2 ? 7 : 8;
        }
        return Math.min(DifficultyLevel.MAX_VALUE, base);
    }

    private static int count(Map<HumanTechnique, Integer> counts,
                             HumanTechnique technique) {
        Integer value = counts.get(technique);
        return value == null ? 0 : value;
    }
}
