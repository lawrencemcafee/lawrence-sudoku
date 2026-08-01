/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.game;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable canonical Sudoku difficulty from 1 (easiest) through 10 (hardest). */
public final class DifficultyLevel implements Comparable<DifficultyLevel>, Parcelable {
    public static final int MIN_VALUE = 1;
    public static final int MAX_VALUE = 10;
    public static final DifficultyLevel DEFAULT = new DifficultyLevel(5);

    private static final DifficultyLevel[] VALUES = new DifficultyLevel[MAX_VALUE];
    private static final List<DifficultyLevel> ALL;

    static {
        List<DifficultyLevel> values = new ArrayList<>();
        for(int value = MIN_VALUE; value <= MAX_VALUE; value++) {
            VALUES[value - 1] = new DifficultyLevel(value);
            values.add(VALUES[value - 1]);
        }
        ALL = Collections.unmodifiableList(values);
    }

    private final int value;

    private DifficultyLevel(int value) {
        this.value = value;
    }

    public static DifficultyLevel of(int value) {
        if(value < MIN_VALUE || value > MAX_VALUE) {
            throw new IllegalArgumentException("Difficulty must be between 1 and 10: " + value);
        }
        return VALUES[value - 1];
    }

    public static DifficultyLevel parse(String value) {
        return of(Integer.parseInt(value));
    }

    public static List<DifficultyLevel> all() {
        return ALL;
    }

    public int getValue() {
        return value;
    }

    public DifficultyCategory getCategory() {
        return DifficultyCategory.fromLevel(this);
    }

    @Override
    public int compareTo(DifficultyLevel other) {
        return Integer.compare(value, other.value);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DifficultyLevel && value == ((DifficultyLevel) other).value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeInt(value);
    }

    public static final Parcelable.Creator<DifficultyLevel> CREATOR =
            new Parcelable.Creator<DifficultyLevel>() {
                @Override
                public DifficultyLevel createFromParcel(Parcel source) {
                    return DifficultyLevel.of(source.readInt());
                }

                @Override
                public DifficultyLevel[] newArray(int size) {
                    return new DifficultyLevel[size];
                }
            };
}
