package org.secuso.privacyfriendlysudoku.controller;

import org.junit.Test;
import org.secuso.privacyfriendlysudoku.game.DifficultyLevel;
import org.secuso.privacyfriendlysudoku.game.GameType;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class TargetedPuzzleGeneratorTest {
    @Test(timeout = 30000)
    public void restoresCluesToProduceAnIntroductoryPuzzle() {
        TargetedPuzzleGenerator.GeneratedPuzzle generated = TargetedPuzzleGenerator.generate(
                GameType.Default_6x6, DifficultyLevel.of(1), new Random(7));

        assertEquals(DifficultyLevel.of(1), generated.getRating().getLevel());
    }

    @Test(timeout = 120000)
    public void producesEveryExactNineByNineTarget() {
        for(int value = 1; value <= 10; value++) {
            TargetedPuzzleGenerator.GeneratedPuzzle generated = TargetedPuzzleGenerator.generate(
                    GameType.Default_9x9, DifficultyLevel.of(value), new Random(100 + value));
            assertEquals(DifficultyLevel.of(value), generated.getRating().getLevel());
        }
    }
}
