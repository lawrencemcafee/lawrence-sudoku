/* Licensed under the GNU General Public License, version 3 or later. */
package org.secuso.privacyfriendlysudoku.controller.training;

import org.secuso.privacyfriendlysudoku.R;
import org.secuso.privacyfriendlysudoku.controller.hints.GameHint;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanTechnique;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Small candidate sketches, not playable puzzles. Omitted notes remain in the proof snapshot.
 * Text resources supply both the visible explanation and the diagram's accessible description. */
public final class TechniqueLesson {
    public final int caption;
    public final int steps;
    public final int rows;
    public final int columns;
    public final List<Cell> cells;
    public final List<GameHint.Link> links;
    public final List<GameHint.UnitMark> units;
    private final int[] values;
    private final int[] masks;

    public static final class Cell {
        public final int row, col, notes, removed, given, placed;
        public final GameHint.Mark mark;

        private Cell(int row, int col, int notes, int removed, int given, int placed,
                     GameHint.Mark mark) {
            this.row = row;
            this.col = col;
            this.notes = notes;
            this.removed = removed;
            this.given = given;
            this.placed = placed;
            this.mark = mark;
        }
    }

    private TechniqueLesson(Builder b, int caption, int steps) {
        this.caption = caption;
        this.steps = steps;
        rows = b.rows;
        columns = b.columns;
        cells = Collections.unmodifiableList(new ArrayList<>(b.cells));
        links = Collections.unmodifiableList(new ArrayList<>(b.links));
        units = Collections.unmodifiableList(new ArrayList<>(b.units));
        values = b.values.clone();
        masks = b.masks.clone();
    }

    public int[] getValues() { return values.clone(); }
    public int[] getCandidateMasks() { return masks.clone(); }

    public static TechniqueLesson forTechnique(HumanTechnique technique) {
        Builder b = new Builder();
        switch(technique) {
            case LAST_DIGIT:
                b.rows = 1;
                for(int col = 0; col < 9; col++) {
                    if(col == 4) b.notes(0, col, "5", GameHint.Mark.FOCUS, "", 5);
                    else b.given(0, col, col + 1);
                }
                return b.build(R.string.lesson_last_digit_example, R.array.lesson_last_digit_steps);
            case NAKED_SINGLE:
                b.rows = b.columns = 1;
                b.notes(0, 0, "123456789", GameHint.Mark.FOCUS, "12346789", 5);
                b.masks[0] = bits("5");
                return b.build(R.string.lesson_naked_single_example, R.array.lesson_naked_single_steps);
            case HIDDEN_SINGLE:
                b.rows = 1;
                b.columns = 5;
                b.restrictRow(0, "5");
                b.notes(0, 0, "127").notes(0, 1, "238").notes(0, 2, "259", GameHint.Mark.FOCUS, "", 5)
                        .notes(0, 3, "148").notes(0, 4, "367");
                return b.build(R.string.lesson_hidden_single_example, R.array.lesson_hidden_single_steps);
            case POINTING_CANDIDATES:
                b.rows = 3;
                b.restrictBlock(0, "5").unit(GameHint.UnitType.BLOCK, 0);
                b.digit(0, 0, 5).digit(0, 2, 5).remove(0, 5, "5");
                return b.build(R.string.lesson_pointing_example, R.array.lesson_pointing_steps);
            case CLAIMING_CANDIDATES:
                b.rows = 3;
                b.restrictRow(0, "5").unit(GameHint.UnitType.ROW, 0);
                b.digit(0, 0, 5).digit(0, 2, 5).remove(2, 1, "5");
                return b.build(R.string.lesson_claiming_example, R.array.lesson_claiming_steps);
            case NAKED_PAIR: return subset(2, false);
            case NAKED_TRIPLE: return subset(3, false);
            case NAKED_QUAD: return subset(4, false);
            case HIDDEN_PAIR: return subset(2, true);
            case HIDDEN_TRIPLE: return subset(3, true);
            case HIDDEN_QUAD: return subset(4, true);
            case X_WING: return fish(2);
            case SWORDFISH: return fish(3);
            case JELLYFISH: return fish(4);
            case SKYSCRAPER:
                b.restrictRow(0, "5").restrictRow(4, "5");
                b.digit(0, 1, 5).digit(0, 6, 5).digit(4, 1, 5).digit(4, 7, 5).remove(1, 7, "5");
                b.link(0, 6, 0, 1, 5, true).link(0, 1, 4, 1, 5, false).link(4, 1, 4, 7, 5, true);
                return b.build(R.string.lesson_skyscraper_example, R.array.lesson_skyscraper_steps);
            case TWO_STRING_KITE:
                b.restrictRow(0, "5").restrictColumn(1, "5");
                b.digit(0, 0, 5).digit(0, 6, 5).digit(1, 1, 5).digit(6, 1, 5).remove(6, 6, "5");
                b.link(0, 6, 0, 0, 5, true).link(0, 0, 1, 1, 5, false).link(1, 1, 6, 1, 5, true);
                return b.build(R.string.lesson_kite_example, R.array.lesson_kite_steps);
            case XY_WING:
                b.notes(1, 1, "12", GameHint.Mark.FOCUS, "", 0).notes(1, 6, "13")
                        .notes(6, 1, "23").remove(6, 6, "3");
                b.link(1, 1, 1, 6, 1, false).link(1, 1, 6, 1, 2, false);
                return b.build(R.string.lesson_xy_wing_example, R.array.lesson_xy_wing_steps);
            case XYZ_WING:
                b.rows = 3;
                b.notes(1, 1, "123", GameHint.Mark.FOCUS, "", 0).notes(1, 6, "13")
                        .notes(0, 0, "23").remove(1, 0, "3");
                b.link(1, 1, 1, 6, 1, false).link(1, 1, 0, 0, 2, false);
                return b.build(R.string.lesson_xyz_wing_example, R.array.lesson_xyz_wing_steps);
            case W_WING:
                b.restrictColumn(4, "1");
                b.notes(0, 0, "12").notes(4, 7, "12").digit(0, 4, 1).digit(4, 4, 1).remove(4, 0, "2");
                b.link(0, 0, 0, 4, 1, false).link(0, 4, 4, 4, 1, true).link(4, 4, 4, 7, 1, false);
                return b.build(R.string.lesson_w_wing_example, R.array.lesson_w_wing_steps);
            case SIMPLE_COLORING:
                b.restrictRow(0, "5").restrictColumn(4, "5").restrictRow(4, "5");
                b.digit(0, 0, 5).digit(0, 4, 5, GameHint.Mark.ASSUMPTION)
                        .digit(4, 4, 5).digit(4, 1, 5, GameHint.Mark.ASSUMPTION).remove(1, 1, "5");
                b.link(0, 0, 0, 4, 5, true).link(0, 4, 4, 4, 5, true).link(4, 4, 4, 1, 5, true);
                return b.build(R.string.lesson_coloring_example, R.array.lesson_coloring_steps);
            case X_CHAIN:
                b.restrictRow(0, "5").restrictColumn(4, "5").restrictColumn(7, "5");
                b.digit(0, 0, 5).digit(0, 3, 5).digit(1, 4, 5).digit(6, 4, 5)
                        .digit(6, 7, 5).digit(3, 7, 5);
                b.remove(3, 0, "5");
                b.link(0, 0, 0, 3, 5, true).link(0, 3, 1, 4, 5, false)
                        .link(1, 4, 6, 4, 5, true).link(6, 4, 6, 7, 5, false)
                        .link(6, 7, 3, 7, 5, true);
                return b.build(R.string.lesson_x_chain_example, R.array.lesson_x_chain_steps);
            case XY_CHAIN:
                b.notes(0, 0, "12").notes(0, 4, "23").notes(4, 4, "34")
                        .notes(4, 7, "14").remove(0, 7, "1");
                b.link(0, 0, 0, 4, 2, false).link(0, 4, 4, 4, 3, false).link(4, 4, 4, 7, 4, false);
                return b.build(R.string.lesson_xy_chain_example, R.array.lesson_xy_chain_steps);
            case FORCING_CHAIN:
                b.notes(0, 0, "12", GameHint.Mark.ASSUMPTION, "1", 0).notes(0, 4, "13")
                        .notes(4, 4, "34").notes(4, 0, "14");
                b.link(0, 0, 0, 4, 1, false).link(0, 4, 4, 4, 3, false)
                        .link(4, 4, 4, 0, 4, false).link(4, 0, 0, 0, 1, false);
                return b.build(R.string.lesson_forcing_example, R.array.lesson_forcing_steps);
            default: throw new IllegalArgumentException("Missing lesson: " + technique);
        }
    }

    private static TechniqueLesson subset(int count, boolean hidden) {
        Builder b = new Builder();
        b.rows = 1;
        b.columns = count + 1;
        String digits = "1234".substring(0, count);
        String[] pairs = count == 2 ? new String[]{"12", "12"}
                : count == 3 ? new String[]{"12", "13", "23"} : new String[]{"12", "13", "24", "34"};
        if(hidden) b.restrictRow(0, digits);
        for(int col = 0; col < count; col++) {
            String extra = Integer.toString(6 + col % 3);
            b.notes(0, col, pairs[col] + (hidden ? extra : ""), GameHint.Mark.SUPPORT, hidden ? extra : "", 0);
        }
        b.notes(0, count, hidden ? "6789" : "19", null, hidden ? "" : "1", 0);
        if(hidden) {
            return b.build(count == 2 ? R.string.lesson_hidden_pair_example : count == 3
                            ? R.string.lesson_hidden_triple_example : R.string.lesson_hidden_quad_example,
                    count == 2 ? R.array.lesson_hidden_pair_steps : count == 3
                            ? R.array.lesson_hidden_triple_steps : R.array.lesson_hidden_quad_steps);
        }
        return b.build(count == 2 ? R.string.lesson_naked_pair_example : count == 3
                        ? R.string.lesson_naked_triple_example : R.string.lesson_naked_quad_example,
                count == 2 ? R.array.lesson_naked_pair_steps : count == 3
                        ? R.array.lesson_naked_triple_steps : R.array.lesson_naked_quad_steps);
    }

    private static TechniqueLesson fish(int count) {
        Builder b = new Builder();
        int[] rows = {0, 3, 6, 8};
        int[] cols = {1, 4, 7, 8};
        for(int i = 0; i < count; i++) {
            b.restrictRow(rows[i], "5").unit(GameHint.UnitType.ROW, rows[i]);
            b.digit(rows[i], cols[i], 5).digit(rows[i], cols[(i + 1) % count], 5);
        }
        b.remove(2, cols[0], "5");
        return b.build(count == 2 ? R.string.lesson_x_wing_example : count == 3
                        ? R.string.lesson_swordfish_example : R.string.lesson_jellyfish_example,
                count == 2 ? R.array.lesson_x_wing_steps : count == 3
                        ? R.array.lesson_swordfish_steps : R.array.lesson_jellyfish_steps);
    }

    public static int bits(String digits) {
        int result = 0;
        for(char digit : digits.toCharArray()) result |= 1 << (digit - '1');
        return result;
    }

    private static final class Builder {
        int rows = 9, columns = 9;
        final int[] values = new int[81], masks = new int[81];
        final List<Cell> cells = new ArrayList<>();
        final List<GameHint.Link> links = new ArrayList<>();
        final List<GameHint.UnitMark> units = new ArrayList<>();
        Builder() { Arrays.fill(masks, 511); }
        TechniqueLesson build(int caption, int steps) { return new TechniqueLesson(this, caption, steps); }
        Builder notes(int row, int col, String digits) { return notes(row, col, digits, GameHint.Mark.SUPPORT, "", 0); }
        Builder notes(int row, int col, String digits, GameHint.Mark mark, String removed, int placed) {
            masks[row * 9 + col] = bits(digits);
            cells.add(new Cell(row, col, bits(digits), bits(removed), 0, placed, mark));
            return this;
        }
        Builder given(int row, int col, int value) {
            values[row * 9 + col] = value;
            masks[row * 9 + col] = 0;
            cells.add(new Cell(row, col, 0, 0, value, 0, null));
            return this;
        }
        Builder digit(int row, int col, int value) { return digit(row, col, value, GameHint.Mark.SUPPORT); }
        Builder digit(int row, int col, int value, GameHint.Mark mark) {
            masks[row * 9 + col] |= 1 << (value - 1);
            cells.add(new Cell(row, col, 1 << (value - 1), 0, 0, 0, mark));
            return this;
        }
        Builder remove(int row, int col, String digits) {
            masks[row * 9 + col] |= bits(digits);
            cells.add(new Cell(row, col, bits(digits), bits(digits), 0, 0, null));
            return this;
        }
        Builder restrictRow(int row, String digits) {
            for(int col = 0; col < 9; col++) masks[row * 9 + col] &= ~bits(digits);
            return this;
        }
        Builder restrictColumn(int col, String digits) {
            for(int row = 0; row < 9; row++) masks[row * 9 + col] &= ~bits(digits);
            return this;
        }
        Builder restrictBlock(int block, String digits) {
            for(int r = 0; r < 3; r++) for(int c = 0; c < 3; c++)
                masks[(block / 3 * 3 + r) * 9 + block % 3 * 3 + c] &= ~bits(digits);
            return this;
        }
        Builder unit(GameHint.UnitType type, int index) {
            units.add(new GameHint.UnitMark(type, index, GameHint.Mark.SUPPORT));
            return this;
        }
        Builder link(int r1, int c1, int r2, int c2, int digit, boolean strong) {
            links.add(new GameHint.Link(new GameHint.Candidate(r1, c1, digit),
                    new GameHint.Candidate(r2, c2, digit), strong));
            return this;
        }
    }
}
