package org.secuso.privacyfriendlysudoku.controller.helper;

import org.junit.Test;
import org.secuso.privacyfriendlysudoku.game.DifficultyLevel;
import org.secuso.privacyfriendlysudoku.game.GameType;

import static org.junit.Assert.assertEquals;

public class HighscoreInfoContainerTest {
    @Test
    public void readsLegacyCategoryStatisticsWithoutInventingAnUpperLevel() {
        HighscoreInfoContainer stats = new HighscoreInfoContainer();

        stats.setInfosFromFile("360/2/3/90/Default_9x9/Hard/2/210");

        assertEquals(GameType.Default_9x9, stats.getGameType());
        assertEquals(DifficultyLevel.of(7), stats.getDifficulty());
        assertEquals(3, stats.getNumberOfGames());
        assertEquals(90, stats.getMinTime());
    }

    @Test
    public void mergesBothExactLevelsForNamedStatistics() {
        HighscoreInfoContainer lower = new HighscoreInfoContainer();
        lower.setInfosFromFile("100/1/2/40/Default_9x9/5/1/40");
        HighscoreInfoContainer upper = new HighscoreInfoContainer();
        upper.setInfosFromFile("200/3/4/30/Default_9x9/6/2/90");

        lower.merge(upper);

        assertEquals(300, lower.getTime());
        assertEquals(4, lower.getNumberOfHintsUsed());
        assertEquals(6, lower.getNumberOfGames());
        assertEquals(30, lower.getMinTime());
        assertEquals(3, lower.getNumberOfGamesNoHints());
        assertEquals(130, lower.getTimeNoHints());
    }
}
