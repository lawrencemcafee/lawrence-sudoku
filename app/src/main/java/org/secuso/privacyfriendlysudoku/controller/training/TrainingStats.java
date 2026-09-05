/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.controller.training;

/** Persistent drill-level performance for one technique. */
public final class TrainingStats {
    private int attempts;
    private int correct;
    private int reveals;
    private int currentStreak;
    private int bestStreak;

    public TrainingStats() {}

    TrainingStats(int attempts, int correct, int reveals, int currentStreak, int bestStreak) {
        this.attempts = attempts;
        this.correct = correct;
        this.reveals = reveals;
        this.currentStreak = currentStreak;
        this.bestStreak = bestStreak;
    }

    public int getAttempts() { return attempts; }
    public int getCorrect() { return correct; }
    public int getReveals() { return reveals; }
    public int getCurrentStreak() { return currentStreak; }
    public int getBestStreak() { return bestStreak; }

    public int getAccuracyPercent() {
        return attempts == 0 ? 0 : Math.round(correct * 100f / attempts);
    }

    void recordAttempt() { attempts++; }

    void recordCorrect() {
        correct++;
        currentStreak++;
        bestStreak = Math.max(bestStreak, currentStreak);
    }

    void recordFailure() { currentStreak = 0; }
    void recordReveal() { reveals++; }
}
