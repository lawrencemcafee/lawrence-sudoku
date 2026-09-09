/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.controller.training;

import java.util.ArrayList;
import java.util.List;

/** Ten consecutive positions and their outcomes within a shuffled training session. */
public final class TrainingQuiz {
    public static final int LENGTH = 10;
    private static final int RESULT_FIELDS = 5;

    public enum Outcome { FIRST_TRY, AFTER_ERRORS, ASSISTED }

    public static final class Result {
        private final Outcome outcome;
        private final TrainingTarget answer;
        private final boolean hintApplied;

        public Result(Outcome outcome, TrainingTarget answer, boolean hintApplied) {
            if(outcome == null || answer == null || answer.getRow() >= TrainingPosition.SIZE
                    || answer.getCol() >= TrainingPosition.SIZE
                    || answer.getValue() > TrainingPosition.SIZE
                    || (hintApplied && outcome != Outcome.ASSISTED)) {
                throw new IllegalArgumentException("Invalid quiz result.");
            }
            this.outcome = outcome;
            this.answer = answer;
            this.hintApplied = hintApplied;
        }

        public Outcome getOutcome() { return outcome; }
        public TrainingTarget getAnswer() { return answer; }
        public boolean wasHintApplied() { return hintApplied; }
    }

    private final long startSequence;
    private final List<Result> results = new ArrayList<>();

    public TrainingQuiz(long startSequence) {
        if(startSequence < 0 || startSequence > Long.MAX_VALUE - LENGTH) {
            throw new IllegalArgumentException("Invalid quiz start.");
        }
        this.startSequence = startSequence;
    }

    public long getStartSequence() { return startSequence; }
    public long getNextSequence() { return startSequence + results.size(); }
    public int getCompletedCount() { return results.size(); }
    public boolean isComplete() { return results.size() == LENGTH; }

    public boolean hasResult(long sequence) {
        return sequence >= startSequence && sequence < getNextSequence();
    }

    public Result getResult(long sequence) {
        if(!hasResult(sequence)) throw new IllegalArgumentException("Question has no result.");
        return results.get((int) (sequence - startSequence));
    }

    public void record(long sequence, Result result) {
        if(result == null || isComplete() || sequence != getNextSequence()) {
            throw new IllegalStateException("Quiz questions must be completed once, in order.");
        }
        results.add(result);
    }

    public int count(Outcome outcome) {
        int count = 0;
        for(Result result : results) if(result.outcome == outcome) count++;
        return count;
    }

    /** Compact activity saved state; positions are reconstructed from the session seed. */
    public int[] saveResults() {
        int[] saved = new int[results.size() * RESULT_FIELDS];
        for(int index = 0; index < results.size(); index++) {
            Result result = results.get(index);
            int offset = index * RESULT_FIELDS;
            saved[offset] = result.outcome.ordinal();
            saved[offset + 1] = result.answer.getRow();
            saved[offset + 2] = result.answer.getCol();
            saved[offset + 3] = result.answer.getValue();
            saved[offset + 4] = result.hintApplied ? 1 : 0;
        }
        return saved;
    }

    public static TrainingQuiz restore(long startSequence, int[] saved) {
        TrainingQuiz quiz = new TrainingQuiz(startSequence);
        if(saved == null) return quiz;
        if(saved.length % RESULT_FIELDS != 0 || saved.length > LENGTH * RESULT_FIELDS) {
            throw new IllegalArgumentException("Invalid saved quiz length.");
        }
        for(int offset = 0; offset < saved.length; offset += RESULT_FIELDS) {
            if(saved[offset] < 0 || saved[offset] >= Outcome.values().length
                    || saved[offset + 4] < 0 || saved[offset + 4] > 1) {
                throw new IllegalArgumentException("Invalid saved quiz result.");
            }
            quiz.record(quiz.getNextSequence(), new Result(Outcome.values()[saved[offset]],
                    new TrainingTarget(saved[offset + 1], saved[offset + 2], saved[offset + 3]),
                    saved[offset + 4] == 1));
        }
        return quiz;
    }
}
