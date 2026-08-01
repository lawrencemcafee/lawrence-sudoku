/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.controller.hints;

import org.secuso.privacyfriendlysudoku.game.GameBoard;
import org.secuso.privacyfriendlysudoku.game.GameCell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Immutable-sized, cheaply cloneable candidate state used by every hint rule. */
public final class CandidateState {

    private final BoardTopology topology;
    private final int[] values;
    private final int[] masks;
    private final boolean candidateModeActive;
    private final int repairedCandidateCount;

    private CandidateState(BoardTopology topology, int[] values, int[] masks,
                           boolean candidateModeActive, int repairedCandidateCount) {
        this.topology = topology;
        this.values = values;
        this.masks = masks;
        this.candidateModeActive = candidateModeActive;
        this.repairedCandidateCount = repairedCandidateCount;
    }

    public static CandidateState fromBoard(GameBoard board, int[] solution) {
        int size = board.getSize();
        BoardTopology topology = new BoardTopology(size,
                board.getGameType().getSectionHeight(), board.getGameType().getSectionWidth());
        int[] values = new int[size * size];
        int[] rawMasks = new int[size * size];
        boolean anyNotes = false;

        for(int row = 0; row < size; row++) {
            for(int col = 0; col < size; col++) {
                GameCell cell = board.getCell(row, col);
                int index = topology.index(row, col);
                values[index] = cell.getValue();
                anyNotes |= cell.getNoteCount() > 0;
            }
        }
        for(int index = 0; index < values.length; index++) {
            if(values[index] == 0) rawMasks[index] = rawMask(topology, values, index);
        }

        int repairs = 0;
        int[] masks = new int[rawMasks.length];
        for(int index = 0; index < masks.length; index++) {
            if(values[index] != 0) continue;
            GameCell cell = board.getCell(index / size, index % size);
            int raw = rawMasks[index];
            if(cell.getNoteCount() == 0) {
                masks[index] = raw;
                continue;
            }
            int noted = mask(cell.getNotes());
            int normalized = noted & raw;
            repairs += Integer.bitCount(noted & ~raw);
            if(solution != null && solution.length == values.length) {
                int expected = solution[index];
                int expectedBit = bit(expected);
                if(expected > 0 && (raw & expectedBit) != 0 && (normalized & expectedBit) == 0) {
                    normalized |= expectedBit;
                    repairs++;
                }
            }
            masks[index] = normalized;
        }
        return new CandidateState(topology, values, masks, anyNotes, repairs);
    }

    /** Test and proof helper for constructing an already-normalized state. */
    static CandidateState fromMasks(BoardTopology topology, int[] values, int[] masks) {
        if(values.length != topology.getSize() * topology.getSize()
                || masks.length != values.length) {
            throw new IllegalArgumentException("State dimensions do not match the topology.");
        }
        return new CandidateState(topology, Arrays.copyOf(values, values.length),
                Arrays.copyOf(masks, masks.length), true, 0);
    }

    CandidateState copy() {
        return new CandidateState(topology, Arrays.copyOf(values, values.length),
                Arrays.copyOf(masks, masks.length), candidateModeActive, repairedCandidateCount);
    }

    public BoardTopology getTopology() { return topology; }
    public int getSize() { return topology.getSize(); }
    public int getValue(int index) { return values[index]; }
    public int getValue(int row, int col) { return values[topology.index(row, col)]; }
    public int getMask(int index) { return masks[index]; }
    public int getMask(int row, int col) { return masks[topology.index(row, col)]; }
    public boolean hasCandidate(int index, int value) { return (masks[index] & bit(value)) != 0; }
    public boolean hasCandidate(int row, int col, int value) {
        return hasCandidate(topology.index(row, col), value);
    }
    public int candidateCount(int index) { return Integer.bitCount(masks[index]); }
    public boolean isCandidateModeActive() { return candidateModeActive; }
    public int getRepairedCandidateCount() { return repairedCandidateCount; }
    void setMask(int index, int mask) { masks[index] = mask; }
    void setValue(int index, int value) {
        values[index] = value;
        masks[index] = 0;
    }

    boolean isComplete() {
        for(int value : values) {
            if(value == 0) return false;
        }
        return true;
    }

    /** Apply a grader deduction directly to this normalized state. */
    void apply(GameHint hint) {
        switch(hint.getAction()) {
            case PLACE_VALUE:
                int index = topology.index(hint.getRow(), hint.getCol());
                setValue(index, hint.getValue());
                int bit = bit(hint.getValue());
                for(BoardTopology.Cell peer : topology.peers(topology.cell(index))) {
                    masks[peer.getIndex()] &= ~bit;
                }
                break;
            case REMOVE_CANDIDATES:
                for(GameHint.Candidate candidate : hint.getEliminations()) {
                    int candidateIndex = topology.index(candidate.getRow(), candidate.getCol());
                    masks[candidateIndex] &= ~bit(candidate.getValue());
                }
                break;
            case CLEAR_VALUE:
            default:
                throw new IllegalArgumentException("A clean puzzle grader cannot clear values.");
        }
    }

    /** Count distinct cells currently exposed by singles-level reasoning. */
    int countBasicMoves() {
        Set<Integer> cells = new HashSet<>();
        for(int index = 0; index < values.length; index++) {
            if(values[index] == 0 && candidateCount(index) == 1) cells.add(index);
        }
        for(BoardTopology.Unit unit : topology.getUnits()) {
            for(int value = 1; value <= getSize(); value++) {
                List<BoardTopology.Cell> candidates = candidateCells(unit, value);
                if(candidates.size() == 1) cells.add(candidates.get(0).getIndex());
            }
        }
        return cells.size();
    }

    public List<Integer> candidates(int index) {
        List<Integer> result = new ArrayList<>();
        for(int value = 1; value <= getSize(); value++) {
            if(hasCandidate(index, value)) result.add(value);
        }
        return result;
    }

    public List<BoardTopology.Cell> candidateCells(BoardTopology.Unit unit, int value) {
        List<BoardTopology.Cell> result = new ArrayList<>();
        for(BoardTopology.Cell cell : unit.getCells()) {
            if(hasCandidate(cell.getIndex(), value)) result.add(cell);
        }
        return result;
    }

    public int[][] previewMasks() {
        int size = getSize();
        int[][] result = new int[size][size];
        for(int row = 0; row < size; row++) {
            for(int col = 0; col < size; col++) {
                result[row][col] = masks[topology.index(row, col)];
            }
        }
        return result;
    }

    static int bit(int value) { return value <= 0 ? 0 : 1 << (value - 1); }

    private static int mask(boolean[] notes) {
        int result = 0;
        for(int index = 0; index < notes.length; index++) {
            if(notes[index]) result |= 1 << index;
        }
        return result;
    }

    private static int rawMask(BoardTopology topology, int[] values, int index) {
        int full = (1 << topology.getSize()) - 1;
        for(BoardTopology.Cell peer : topology.peers(topology.cell(index))) {
            full &= ~bit(values[peer.getIndex()]);
        }
        return full;
    }
}
