/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.controller;

import org.secuso.privacyfriendlysudoku.controller.hints.DifficultyRating;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanDifficultyRater;
import org.secuso.privacyfriendlysudoku.controller.qqwing.QQWing;
import org.secuso.privacyfriendlysudoku.game.DifficultyLevel;
import org.secuso.privacyfriendlysudoku.game.GameDifficulty;
import org.secuso.privacyfriendlysudoku.game.GameType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Builds a unique minimal puzzle, then restores clues until it reaches an exact target. */
public final class TargetedPuzzleGenerator {
    private TargetedPuzzleGenerator() {}

    public static GeneratedPuzzle generate(GameType gameType, DifficultyLevel target,
                                           Random random) {
        GeneratedPuzzle closest = null;
        if(target.getValue() >= 5) {
            for(int[] seed : AdvancedPuzzleSeeds.transformed(gameType, random)) {
                GeneratedPuzzle candidate = restoreTowardTarget(gameType, seed, target, random);
                if(candidate.getRating().getLevel().equals(target)) return candidate;
                if(closer(candidate, closest, target)) closest = candidate;
            }
        }

        QQWing generator = new QQWing(gameType, GameDifficulty.Unspecified);
        generator.setRandom(random.nextInt());
        generator.generatePuzzle();

        int[] puzzle = generator.getPuzzle();
        generator.setPuzzle(puzzle);
        if(!generator.solve()) throw new IllegalStateException("Generated puzzle was not solvable.");
        int[] solution = generator.getSolution();

        GeneratedPuzzle randomCandidate = restoreTowardTarget(
                gameType, puzzle, solution, target, random);
        if(randomCandidate.getRating().getLevel().equals(target)) return randomCandidate;
        return closer(randomCandidate, closest, target) ? randomCandidate : closest;
    }

    private static GeneratedPuzzle restoreTowardTarget(GameType gameType, int[] puzzle,
                                                         DifficultyLevel target, Random random) {
        QQWing solver = new QQWing(gameType, GameDifficulty.Unspecified);
        if(!solver.setPuzzle(puzzle) || !solver.solve()) {
            throw new IllegalArgumentException("Advanced seed is not solvable.");
        }
        return restoreTowardTarget(gameType, puzzle, solver.getSolution(), target, random);
    }

    private static GeneratedPuzzle restoreTowardTarget(GameType gameType, int[] puzzle,
                                                         int[] solution, DifficultyLevel target,
                                                         Random random) {
        DifficultyRating rating = HumanDifficultyRater.rate(gameType, puzzle, solution);
        if(rating.getLevel().equals(target)) return new GeneratedPuzzle(puzzle, rating);
        if(rating.getLevel().compareTo(target) < 0) return new GeneratedPuzzle(puzzle, rating);

        List<Integer> missing = new ArrayList<>();
        for(int index = 0; index < puzzle.length; index++) {
            if(puzzle[index] == 0) missing.add(index);
        }
        Collections.shuffle(missing, random);

        int[] candidate = puzzle.clone();
        DifficultyRating candidateRating = rating;
        for(int index : missing) {
            if(Thread.currentThread().isInterrupted()) break;
            candidate[index] = solution[index];
            candidateRating = HumanDifficultyRater.rate(gameType, candidate, solution);
            if(candidateRating.getLevel().equals(target)) {
                return new GeneratedPuzzle(candidate, candidateRating);
            }
            if(candidateRating.getLevel().compareTo(target) < 0) break;
        }
        return new GeneratedPuzzle(candidate, candidateRating);
    }

    private static boolean closer(GeneratedPuzzle candidate, GeneratedPuzzle current,
                                  DifficultyLevel target) {
        if(candidate == null) return false;
        if(current == null) return true;
        int candidateDistance = Math.abs(candidate.rating.getLevel().getValue() - target.getValue());
        int currentDistance = Math.abs(current.rating.getLevel().getValue() - target.getValue());
        return candidateDistance < currentDistance;
    }

    public static final class GeneratedPuzzle {
        private final int[] puzzle;
        private final DifficultyRating rating;

        private GeneratedPuzzle(int[] puzzle, DifficultyRating rating) {
            this.puzzle = puzzle.clone();
            this.rating = rating;
        }

        public int[] getPuzzle() { return puzzle.clone(); }
        public DifficultyRating getRating() { return rating; }
    }
}
