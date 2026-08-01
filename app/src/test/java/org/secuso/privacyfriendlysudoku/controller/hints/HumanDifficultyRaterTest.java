package org.secuso.privacyfriendlysudoku.controller.hints;

import org.junit.Test;
import org.secuso.privacyfriendlysudoku.game.DifficultyCategory;
import org.secuso.privacyfriendlysudoku.game.DifficultyLevel;
import org.secuso.privacyfriendlysudoku.game.GameType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class HumanDifficultyRaterTest {
    private static final int[] SOLUTION_9 = digits(
            "123456789" +
            "456789123" +
            "789123456" +
            "234567891" +
            "567891234" +
            "891234567" +
            "345678912" +
            "678912345" +
            "912345678");

    @Test
    public void generousSingleMovePuzzleIsBeginnerLevelOne() {
        int[] puzzle = SOLUTION_9.clone();
        puzzle[0] = 0;

        DifficultyRating rating = HumanDifficultyRater.rate(
                GameType.Default_9x9, puzzle, SOLUTION_9);

        assertEquals(DifficultyLevel.of(1), rating.getLevel());
        assertEquals(DifficultyCategory.Beginner, rating.getLevel().getCategory());
        assertFalse(rating.isForcingRequired());
    }

    private static int[] digits(String encoded) {
        int[] values = new int[encoded.length()];
        for(int index = 0; index < encoded.length(); index++) {
            values[index] = encoded.charAt(index) - '0';
        }
        return values;
    }
}
