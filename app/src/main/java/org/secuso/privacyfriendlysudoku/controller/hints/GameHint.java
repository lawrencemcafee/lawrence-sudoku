/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.controller.hints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A human-readable deduction, its visual explanation frames, and the board change offered by
 * Apply.  The solver deliberately returns structured evidence instead of HTML or view objects so
 * the same result can be tested on the JVM and rendered by any Android view.
 */
public final class GameHint {

    public enum Action {
        PLACE_VALUE,
        REMOVE_CANDIDATES,
        CLEAR_VALUE
    }

    public enum Mark {
        FOCUS,
        SUPPORT,
        ASSUMPTION,
        ELIMINATE,
        CONTRADICTION
    }

    public enum UnitType {
        ROW,
        COLUMN,
        BLOCK
    }

    public static final class Candidate {
        private final int row;
        private final int col;
        private final int value;

        public Candidate(int row, int col, int value) {
            this.row = row;
            this.col = col;
            this.value = value;
        }

        public int getRow() { return row; }
        public int getCol() { return col; }
        public int getValue() { return value; }

        @Override
        public boolean equals(Object other) {
            if(!(other instanceof Candidate)) {
                return false;
            }
            Candidate candidate = (Candidate) other;
            return row == candidate.row && col == candidate.col && value == candidate.value;
        }

        @Override
        public int hashCode() {
            return (row * 31 + col) * 31 + value;
        }
    }

    public static final class CellMark {
        private final int row;
        private final int col;
        private final Mark mark;

        public CellMark(int row, int col, Mark mark) {
            this.row = row;
            this.col = col;
            this.mark = mark;
        }

        public int getRow() { return row; }
        public int getCol() { return col; }
        public Mark getMark() { return mark; }
    }

    public static final class CandidateMark {
        private final Candidate candidate;
        private final Mark mark;

        public CandidateMark(Candidate candidate, Mark mark) {
            this.candidate = candidate;
            this.mark = mark;
        }

        public Candidate getCandidate() { return candidate; }
        public Mark getMark() { return mark; }
    }

    public static final class UnitMark {
        private final UnitType type;
        private final int index;
        private final Mark mark;

        public UnitMark(UnitType type, int index, Mark mark) {
            this.type = type;
            this.index = index;
            this.mark = mark;
        }

        public UnitType getType() { return type; }
        public int getIndex() { return index; }
        public Mark getMark() { return mark; }
    }

    public static final class Link {
        private final Candidate from;
        private final Candidate to;
        private final boolean strong;

        public Link(Candidate from, Candidate to, boolean strong) {
            this.from = from;
            this.to = to;
            this.strong = strong;
        }

        public Candidate getFrom() { return from; }
        public Candidate getTo() { return to; }
        public boolean isStrong() { return strong; }
    }

    public static final class HintFrame {
        private final String message;
        private final List<CellMark> cellMarks;
        private final List<CandidateMark> candidateMarks;
        private final List<UnitMark> unitMarks;
        private final List<Link> links;

        public HintFrame(String message, List<CellMark> cellMarks,
                         List<CandidateMark> candidateMarks, List<UnitMark> unitMarks,
                         List<Link> links) {
            this.message = message;
            this.cellMarks = immutable(cellMarks);
            this.candidateMarks = immutable(candidateMarks);
            this.unitMarks = immutable(unitMarks);
            this.links = immutable(links);
        }

        public static HintFrame message(String message) {
            return new HintFrame(message, Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList());
        }

        public String getMessage() { return message; }
        public List<CellMark> getCellMarks() { return cellMarks; }
        public List<CandidateMark> getCandidateMarks() { return candidateMarks; }
        public List<UnitMark> getUnitMarks() { return unitMarks; }
        public List<Link> getLinks() { return links; }

        public Mark getCellMark(int row, int col) {
            for(CellMark cellMark : cellMarks) {
                if(cellMark.row == row && cellMark.col == col) {
                    return cellMark.mark;
                }
            }
            return null;
        }

        public Mark getCandidateMark(int row, int col, int value) {
            for(CandidateMark candidateMark : candidateMarks) {
                Candidate candidate = candidateMark.candidate;
                if(candidate.row == row && candidate.col == col && candidate.value == value) {
                    return candidateMark.mark;
                }
            }
            return null;
        }

        private static <T> List<T> immutable(List<T> source) {
            if(source == null || source.isEmpty()) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(new ArrayList<>(source));
        }
    }

    private final String title;
    private final String summary;
    private final List<HintFrame> frames;
    private final int row;
    private final int col;
    private final int value;
    private final Action action;
    private final List<Candidate> eliminations;
    private final int[][] candidatePreview;
    private final boolean applyCandidatePreview;
    private final int repairedCandidateCount;

    GameHint(String title, String summary, List<HintFrame> frames,
             int row, int col, int value, Action action, List<Candidate> eliminations,
             int[][] candidatePreview, boolean applyCandidatePreview,
             int repairedCandidateCount) {
        this.title = title;
        this.summary = summary;
        this.frames = Collections.unmodifiableList(new ArrayList<>(frames));
        this.row = row;
        this.col = col;
        this.value = value;
        this.action = action;
        this.eliminations = eliminations == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(eliminations));
        this.candidatePreview = copy(candidatePreview);
        this.applyCandidatePreview = applyCandidatePreview;
        this.repairedCandidateCount = repairedCandidateCount;
    }

    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public List<HintFrame> getFrames() { return frames; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public int getValue() { return value; }
    public Action getAction() { return action; }
    public List<Candidate> getEliminations() { return eliminations; }
    public boolean shouldApplyCandidatePreview() { return applyCandidatePreview; }
    public int getRepairedCandidateCount() { return repairedCandidateCount; }

    /** Compatibility accessor used by the existing paged dialog. */
    public List<String> getDetails() {
        List<String> details = new ArrayList<>();
        for(HintFrame frame : frames) {
            details.add(frame.message);
        }
        return Collections.unmodifiableList(details);
    }

    /** The summary uses the final conclusion overlay; detail pages use their matching frame. */
    public HintFrame getOverlayFrame(int detailIndex) {
        if(frames.isEmpty()) {
            return HintFrame.message(summary);
        }
        if(detailIndex < 0) {
            return frames.get(frames.size() - 1);
        }
        return frames.get(Math.min(detailIndex, frames.size() - 1));
    }

    public boolean hasPreviewCandidate(int row, int col, int value) {
        return candidatePreview != null && row >= 0 && row < candidatePreview.length
                && col >= 0 && col < candidatePreview[row].length && value > 0
                && (candidatePreview[row][col] & (1 << (value - 1))) != 0;
    }

    public boolean[] getPreviewNotes(int row, int col) {
        int size = candidatePreview.length;
        boolean[] notes = new boolean[size];
        int mask = candidatePreview[row][col];
        for(int value = 1; value <= size; value++) {
            notes[value - 1] = (mask & (1 << (value - 1))) != 0;
        }
        return notes;
    }

    public boolean eliminates(int row, int col, int value) {
        return eliminations.contains(new Candidate(row, col, value));
    }

    private static int[][] copy(int[][] source) {
        if(source == null) {
            return new int[0][0];
        }
        int[][] result = new int[source.length][];
        for(int row = 0; row < source.length; row++) {
            result[row] = Arrays.copyOf(source[row], source[row].length);
        }
        return result;
    }
}
