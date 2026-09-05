/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.controller.training;

import org.secuso.privacyfriendlysudoku.controller.hints.GameHint;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanTechnique;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Versioned compact codec for the immutable bundled Gym corpus. */
public final class TrainingCorpusCodec {
    static final int MAGIC = 0x4c47594d; // LGYM
    public static final int VERSION = 1;
    public static final int POSITIONS_PER_TECHNIQUE = 100;

    private TrainingCorpusCodec() {}

    public static Map<HumanTechnique, List<TrainingPosition>> read(InputStream source)
            throws IOException {
        try(DataInputStream input = new DataInputStream(new BufferedInputStream(source))) {
            if(input.readInt() != MAGIC) throw new IOException("Unrecognized Gym corpus.");
            int version = input.readInt();
            if(version != VERSION) throw new IOException("Unsupported Gym corpus version: " + version);
            int techniqueCount = input.readInt();
            if(techniqueCount != HumanTechnique.values().length) {
                throw new IOException("Gym corpus technique count is " + techniqueCount + ".");
            }

            Map<HumanTechnique, List<TrainingPosition>> result =
                    new EnumMap<>(HumanTechnique.class);
            for(int techniqueIndex = 0; techniqueIndex < techniqueCount; techniqueIndex++) {
                HumanTechnique technique = HumanTechnique.valueOf(input.readUTF());
                int count = input.readInt();
                if(count != POSITIONS_PER_TECHNIQUE || result.containsKey(technique)) {
                    throw new IOException("Invalid Gym count for " + technique + ": " + count);
                }
                List<TrainingPosition> positions = new ArrayList<>(count);
                for(int index = 0; index < count; index++) {
                    String id = input.readUTF();
                    int[] values = readBytes(input);
                    int[] masks = readMasks(input);
                    int[] solution = readBytes(input);
                    GameHint.Action action = GameHint.Action.values()[input.readUnsignedByte()];
                    int targetCount = input.readUnsignedShort();
                    List<TrainingTarget> targets = new ArrayList<>(targetCount);
                    for(int targetIndex = 0; targetIndex < targetCount; targetIndex++) {
                        targets.add(new TrainingTarget(input.readUnsignedByte(),
                                input.readUnsignedByte(), input.readUnsignedByte()));
                    }
                    // Retired candidate filter in the bundled v1 format; never hide candidates.
                    input.readUnsignedShort();
                    positions.add(new TrainingPosition(id, technique, values, masks, solution,
                            action, targets));
                }
                result.put(technique, positions);
            }
            return result;
        } catch(IllegalArgumentException | ArrayIndexOutOfBoundsException malformed) {
            throw new IOException("Gym corpus contains invalid enum data.", malformed);
        }
    }

    public static void write(OutputStream destination,
                             Map<HumanTechnique, List<TrainingPosition>> corpus)
            throws IOException {
        try(DataOutputStream output = new DataOutputStream(new BufferedOutputStream(destination))) {
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeInt(HumanTechnique.values().length);
            for(HumanTechnique technique : HumanTechnique.values()) {
                List<TrainingPosition> positions = corpus.get(technique);
                if(positions == null || positions.size() != POSITIONS_PER_TECHNIQUE) {
                    throw new IOException("Expected exactly 100 positions for " + technique + ".");
                }
                output.writeUTF(technique.name());
                output.writeInt(positions.size());
                for(TrainingPosition position : positions) {
                    output.writeUTF(position.getId());
                    writeBytes(output, position.getValues());
                    writeMasks(output, position.getCandidateMasks());
                    writeBytes(output, position.getSolution());
                    output.writeByte(position.getAction().ordinal());
                    output.writeShort(position.getTargets().size());
                    for(TrainingTarget target : position.getTargets()) {
                        output.writeByte(target.getRow());
                        output.writeByte(target.getCol());
                        output.writeByte(target.getValue());
                    }
                    output.writeShort(0); // Reserved legacy candidate-filter field in v1.
                }
            }
        }
    }

    private static int[] readBytes(DataInputStream input) throws IOException {
        int[] result = new int[TrainingPosition.CELL_COUNT];
        for(int index = 0; index < result.length; index++) result[index] = input.readUnsignedByte();
        return result;
    }

    private static int[] readMasks(DataInputStream input) throws IOException {
        int[] result = new int[TrainingPosition.CELL_COUNT];
        for(int index = 0; index < result.length; index++) result[index] = input.readUnsignedShort();
        return result;
    }

    private static void writeBytes(DataOutputStream output, int[] values) throws IOException {
        for(int value : values) output.writeByte(value);
    }

    private static void writeMasks(DataOutputStream output, int[] masks) throws IOException {
        for(int mask : masks) output.writeShort(mask);
    }
}
