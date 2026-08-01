/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.controller;

import org.secuso.privacyfriendlysudoku.game.GameType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Proven advanced templates from the original bundled corpus. House-preserving
 * transformations turn each template into many distinct puzzles without changing its logic.
 */
final class AdvancedPuzzleSeeds {
    private AdvancedPuzzleSeeds() {}

    private static final String[] SIX = {
            "630010002000001000040020400006003040",
            "000000060130006000050603030005000041",
            "502360000040430050000030650000000000",
            "000004300050006010001003000561000000",
            "000000042000003140064030000320200000",
            "004200200000003002600050300400046000",
            "003050200003502000000000640002000045",
            "004030000000003020205004400000006500",
            "001650000320100000000060000000065000",
            "500004013005004020000000042500100000"
    };

    private static final String[] NINE = {
            "000800400008004093009003060000700000000400000060002900091000670200000100403006802",
            "000006040000050000600040080043000200500810000100300605209080460004500000001030000",
            "080000000040000000000071300050130700004006000003508014000000081012600405070000002",
            "030200000008000096700600050013720009006000700802009000000903082500010070000000000",
            "007500200120093700004007050000000305500800090040300018000000009000715000000400030",
            "086000000000000070090000304968027030000100060200900000072006100000000296000000740",
            "450900001001400000600010004006700000500000000100024060002000030000062400040005700",
            "100000020006020091000600000030070000260040000008010000000380060700000049040100002",
            "040100800059408061700000002500000009080700000007004000000000090801009200000000685",
            "007000050300710000140000000000500406001000907000370005790030001060004000005620000"
    };

    private static final String[] TWELVE = {
            "00600500000004000000002050004C00800A28049000050000A900C000000000B00000A00B0560000C900C708A00000B0002000000769000008000B002B0C6000017C00107400908",
            "020000B000A608070530000B9050200A03800030980010B001000B6C30900000032000CAB0000000000000067000B000000500000A000C09000081302B0100070950836000100000",
            "0A00070050B107B0060A8000005090020C0A500040001060000000A000040B00219000000107B000602C8090C000001700400A00058B00000000000620000B400090090400800000",
            "000000908A0002900080006B0000C000007005800B2C00006020300000043B090806500C090400C000300060A700040500A00400002800000CA0000900000060B000A0B0003970C6",
            "0070000000000020009B0100000A030000000C039002004800904005000B100B008070507040C20005A0000870B32000C5000810070980100000000000000A070C000A0500000090",
            "2000000000000B070C00000000AC100000000020050CA00BB60002097800079001000C60400000B0070A0000A7000298005048000010004000070009A10600C00B000C00B0000020",
            "01000002000C00640A800070000A0000082020000008100B0C081090000050A0060C079009BC000A04010500097000000000000000000000904000866070B100020000000C00B009",
            "000C0B020600400900600A020800140000C000000013050000040600900300000C0500B49A0250000000B0C0002000300143C000B0060000800064000000000B10000000020A8B09",
            "08060000000100000765A0000C00400000B0605001C7000019C0A4000002A000000B0040000000000100B500000080740020100005690A000000048B706000009020000020807000",
            "B000730000000000090040000C00B00000003A07180000C00000000CB0000000400A200040B2000000A8000020006700710A06000C00000BA0200170A700603002B0043000090060"
    };

    private static final String[] SIXTEEN = {
            "0002D000G0100C70509062010000000D0C0B3045000001E00000000B0007F90308000003000FG00470310000000000C00F5091B600800000B000C0E0039007013B0000000E00001010040C000BF3760060F000009108E00000000E0D0045000002080000C0EB000040CE0000700200000000005009000000FGD0E07084300B50"
    };

    static List<int[]> transformed(GameType type, Random random) {
        String[] encoded;
        switch(type) {
            case Default_6x6: encoded = SIX; break;
            case Default_9x9: encoded = NINE; break;
            case Default_12x12: encoded = TWELVE; break;
            case Default_16x16: encoded = SIXTEEN; break;
            default: return Collections.emptyList();
        }
        List<int[]> result = new ArrayList<>();
        for(String seed : encoded) result.add(transform(type, decode(seed), random));
        Collections.shuffle(result, random);
        return result;
    }

    private static int[] transform(GameType type, int[] source, Random random) {
        int size = type.getSize();
        int[] symbols = shuffledRange(size, random);
        int[] rows = shuffledHouses(size, type.getSectionHeight(), random);
        int[] columns = shuffledHouses(size, type.getSectionWidth(), random);
        int[] result = new int[source.length];
        for(int row = 0; row < size; row++) {
            for(int column = 0; column < size; column++) {
                int value = source[rows[row] * size + columns[column]];
                result[row * size + column] = value == 0 ? 0 : symbols[value - 1] + 1;
            }
        }
        return result;
    }

    private static int[] shuffledHouses(int size, int houseSize, Random random) {
        List<Integer> houses = new ArrayList<>();
        for(int house = 0; house < size / houseSize; house++) houses.add(house);
        Collections.shuffle(houses, random);
        int[] result = new int[size];
        int cursor = 0;
        for(int house : houses) {
            List<Integer> members = new ArrayList<>();
            for(int offset = 0; offset < houseSize; offset++) {
                members.add(house * houseSize + offset);
            }
            Collections.shuffle(members, random);
            for(int member : members) result[cursor++] = member;
        }
        return result;
    }

    private static int[] shuffledRange(int size, Random random) {
        List<Integer> values = new ArrayList<>();
        for(int value = 0; value < size; value++) values.add(value);
        Collections.shuffle(values, random);
        int[] result = new int[size];
        for(int index = 0; index < size; index++) result[index] = values.get(index);
        return result;
    }

    private static int[] decode(String encoded) {
        int[] result = new int[encoded.length()];
        for(int index = 0; index < encoded.length(); index++) {
            char symbol = encoded.charAt(index);
            result[index] = symbol >= 'A' ? symbol - 'A' + 10 : symbol - '0';
        }
        return result;
    }
}
