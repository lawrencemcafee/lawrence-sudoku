/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.controller.training;

import org.junit.Test;

import static org.junit.Assert.*;

public class TrainingQuizTest {
    @Test
    public void completedQuizHasTenExclusiveOutcomesAndRetainsItsAnswers() {
        TrainingQuiz quiz = new TrainingQuiz(90);
        for(int index = 0; index < TrainingQuiz.LENGTH; index++) {
            TrainingQuiz.Outcome outcome = index == 0 ? TrainingQuiz.Outcome.AFTER_ERRORS
                    : index == 1 ? TrainingQuiz.Outcome.ASSISTED : TrainingQuiz.Outcome.FIRST_TRY;
            quiz.record(90 + index, new TrainingQuiz.Result(outcome,
                    new TrainingTarget(index / 9, index % 9, index % 9 + 1), index == 1));
        }
        TrainingQuiz restored = TrainingQuiz.restore(quiz.getStartSequence(), quiz.saveResults());
        assertTrue(restored.isComplete());
        assertEquals(100, restored.getNextSequence());
        assertEquals(8, restored.count(TrainingQuiz.Outcome.FIRST_TRY));
        assertEquals(1, restored.count(TrainingQuiz.Outcome.AFTER_ERRORS));
        assertEquals(1, restored.count(TrainingQuiz.Outcome.ASSISTED));
        assertEquals(new TrainingTarget(0, 1, 2), restored.getResult(91).getAnswer());
        assertTrue(restored.getResult(91).wasHintApplied());
        assertFalse(restored.getResult(90).wasHintApplied());
        assertArrayEquals(quiz.saveResults(), restored.saveResults());
        assertThrows(IllegalStateException.class, () -> restored.record(100, result()));
    }

    @Test
    public void aQuestionCannotBeSkippedOrCountedTwice() {
        TrainingQuiz quiz = new TrainingQuiz(20);
        assertThrows(IllegalStateException.class, () -> quiz.record(21, result()));
        quiz.record(20, result());
        assertThrows(IllegalStateException.class, () -> quiz.record(20, result()));
        assertEquals(1, quiz.getCompletedCount());
        assertEquals(21, quiz.getNextSequence());
        assertFalse(quiz.hasResult(21));
        assertThrows(IllegalArgumentException.class, () -> quiz.getResult(19));
    }

    @Test
    public void partialQuizRestoresAndContinuesAtItsNextQuestion() {
        TrainingQuiz quiz = new TrainingQuiz(0);
        quiz.record(0, result());
        TrainingQuiz restored = TrainingQuiz.restore(0, quiz.saveResults());
        restored.record(1, new TrainingQuiz.Result(TrainingQuiz.Outcome.ASSISTED,
                new TrainingTarget(3, 4, 5), false));
        assertEquals(2, restored.getCompletedCount());
        assertFalse(restored.isComplete());
        assertEquals(1, restored.count(TrainingQuiz.Outcome.ASSISTED));
        assertThrows(IllegalArgumentException.class, () -> TrainingQuiz.restore(0, new int[3]));
    }

    private static TrainingQuiz.Result result() {
        return new TrainingQuiz.Result(TrainingQuiz.Outcome.FIRST_TRY,
                new TrainingTarget(0, 0, 1), false);
    }
}
