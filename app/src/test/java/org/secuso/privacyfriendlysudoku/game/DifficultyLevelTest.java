package org.secuso.privacyfriendlysudoku.game;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class DifficultyLevelTest {
    @Test
    public void mapsEveryPairToItsNamedCategory() {
        DifficultyCategory[] categories = DifficultyCategory.values();
        for(int value = 1; value <= 10; value++) {
            assertEquals(categories[(value - 1) / 2], DifficultyLevel.of(value).getCategory());
        }
    }

    @Test
    public void namedCategoryAlwaysExposesItsLowerAndUpperLevel() {
        for(DifficultyCategory category : DifficultyCategory.values()) {
            assertSame(category, category.getLowerLevel().getCategory());
            assertSame(category, category.getUpperLevel().getCategory());
            assertEquals(category.getLowerLevel().getValue() + 1,
                    category.getUpperLevel().getValue());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDifficultyZero() {
        DifficultyLevel.of(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDifficultyEleven() {
        DifficultyLevel.of(11);
    }
}
