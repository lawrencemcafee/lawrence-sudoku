/* Licensed under the GNU General Public License, version 3 or later. */
package org.secuso.privacyfriendlysudoku.ui.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import org.secuso.privacyfriendlysudoku.controller.hints.GameHint;
import org.secuso.privacyfriendlysudoku.controller.training.TechniqueLesson;

/** Resolution-independent, read-only candidate sketches for the catalog lessons. */
public final class TechniqueDiagramView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private TechniqueLesson lesson;
    private final boolean dark;
    private final int ink, paper, grid;

    public TechniqueDiagramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        ink = dark ? Color.rgb(233, 238, 244) : Color.rgb(38, 51, 66);
        paper = dark ? Color.rgb(29, 36, 45) : Color.rgb(248, 250, 253);
        grid = dark ? Color.rgb(73, 87, 103) : Color.rgb(207, 217, 228);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setLesson(TechniqueLesson lesson) {
        this.lesson = lesson;
        setContentDescription(getResources().getString(lesson.caption));
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = resolveSize(dp(320), widthSpec);
        float cell = cellSize(width);
        int height = Math.round(cell * (lesson == null ? 9 : lesson.rows) + dp(28));
        setMeasuredDimension(width, resolveSize(height, heightSpec));
    }

    private float cellSize(int width) {
        return Math.min(dp(76), (Math.min(width, dp(420)) - dp(24f))
                / (lesson == null ? 9 : lesson.columns));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(lesson == null) return;
        float cell = cellSize(getWidth());
        float left = (getWidth() - cell * lesson.columns + dp(16)) / 2;
        float top = dp(20);
        float right = left + cell * lesson.columns;
        float bottom = top + cell * lesson.rows;
        fill(paper);
        canvas.drawRect(left, top, right, bottom, paint);

        for(GameHint.UnitMark unit : lesson.units) {
            RectF rect;
            if(unit.getType() == GameHint.UnitType.ROW) {
                rect = new RectF(left, top + unit.getIndex() * cell, right, top + (unit.getIndex() + 1) * cell);
            } else if(unit.getType() == GameHint.UnitType.COLUMN) {
                rect = new RectF(left + unit.getIndex() * cell, top, left + (unit.getIndex() + 1) * cell, bottom);
            } else {
                rect = new RectF(left + unit.getIndex() % 3 * 3 * cell, top + unit.getIndex() / 3 * 3 * cell,
                        left + (unit.getIndex() % 3 + 1) * 3 * cell, top + (unit.getIndex() / 3 + 1) * 3 * cell);
            }
            fill(tint(color(GameHint.Mark.SUPPORT), 30));
            canvas.drawRect(rect, paint);
        }

        for(TechniqueLesson.Cell item : lesson.cells) {
            if(item.mark == null && item.removed == 0) continue;
            int color = color(item.mark == null ? GameHint.Mark.ELIMINATE : item.mark);
            fill(tint(color, 48));
            canvas.drawRect(left + item.col * cell, top + item.row * cell,
                    left + (item.col + 1) * cell, top + (item.row + 1) * cell, paint);
        }

        for(int row = 0; row <= lesson.rows; row++) {
            stroke(grid, dp(row % 3 == 0 ? 1.4f : .6f));
            canvas.drawLine(left, top + row * cell, right, top + row * cell, paint);
        }
        for(int col = 0; col <= lesson.columns; col++) {
            stroke(grid, dp(col % 3 == 0 ? 1.4f : .6f));
            canvas.drawLine(left + col * cell, top, left + col * cell, bottom, paint);
        }
        for(int col = 0; col < lesson.columns; col++)
            text(canvas, Integer.toString(col + 1), left + (col + .5f) * cell, dp(9), dp(10), ink, false);
        for(int row = 0; row < lesson.rows; row++)
            text(canvas, Integer.toString(row + 1), left - dp(12), top + (row + .5f) * cell, dp(10), ink, false);

        for(GameHint.Link link : lesson.links) {
            float x1 = left + (link.getFrom().getCol() + .5f) * cell;
            float y1 = top + (link.getFrom().getRow() + .5f) * cell;
            float x2 = left + (link.getTo().getCol() + .5f) * cell;
            float y2 = top + (link.getTo().getRow() + .5f) * cell;
            stroke(color(GameHint.Mark.SUPPORT), dp(1.7f));
            if(!link.isStrong()) paint.setPathEffect(new DashPathEffect(new float[]{dp(4), dp(4)}, 0));
            canvas.drawLine(x1, y1, x2, y2, paint);
            paint.setPathEffect(null);
            // Adjacent cells leave too little room for a badge; keep the link itself visible.
            if(Math.hypot(x2 - x1, y2 - y1) > cell * 2) {
                float mx = (x1 + x2) / 2, my = (y1 + y2) / 2;
                fill(paper);
                canvas.drawCircle(mx, my, dp(7), paint);
                text(canvas, Integer.toString(link.getFrom().getValue()), mx, my, dp(10), ink, true);
            }
        }

        for(TechniqueLesson.Cell item : lesson.cells) {
            float x = left + (item.col + .5f) * cell;
            float y = top + (item.row + .5f) * cell;
            int markColor = item.mark == null ? ink : color(item.mark);
            if(!lesson.links.isEmpty()) {
                fill(paper);
                canvas.drawRoundRect(new RectF(x - cell * .4f, y - cell * .37f,
                        x + cell * .4f, y + cell * .37f), dp(4), dp(4), paint);
                stroke(markColor, dp(1));
                canvas.drawRoundRect(new RectF(x - cell * .4f, y - cell * .37f,
                        x + cell * .4f, y + cell * .37f), dp(4), dp(4), paint);
            }
            if(item.given != 0) {
                text(canvas, Integer.toString(item.given), x, y, cell * .53f, ink, false);
                continue;
            }
            int count = Integer.bitCount(item.notes);
            int index = 0;
            int across = count <= 1 ? 1 : count <= 4 ? 2 : 3;
            int down = (count + across - 1) / across;
            float size = cell * (count <= 1 ? .54f : count <= 4 ? .30f : .23f);
            for(int value = 1; value <= 9; value++) {
                int bit = 1 << (value - 1);
                if((item.notes & bit) == 0) continue;
                float nx = x + (index % across - (across - 1) / 2f) * cell * .34f;
                float ny = y + (index / across - (down - 1) / 2f) * cell * .32f;
                boolean removed = (item.removed & bit) != 0;
                int noteColor = removed ? color(GameHint.Mark.ELIMINATE)
                        : item.placed == value ? color(GameHint.Mark.FOCUS) : markColor;
                text(canvas, Integer.toString(value), nx, ny, size, noteColor, true);
                if(removed) {
                    stroke(noteColor, dp(1.5f));
                    canvas.drawLine(nx - size * .4f, ny + size * .42f,
                            nx + size * .4f, ny - size * .42f, paint);
                } else if(item.placed == value) {
                    stroke(noteColor, dp(1.5f));
                    canvas.drawCircle(nx, ny, size * .64f, paint);
                }
                index++;
            }
        }
    }

    private int color(GameHint.Mark mark) {
        int base = HintPalette.colorFor(mark);
        return dark ? Color.rgb((Color.red(base) + 255) / 2, (Color.green(base) + 255) / 2,
                (Color.blue(base) + 255) / 2) : base;
    }

    private static int tint(int color, int alpha) { return (color & 0x00ffffff) | (alpha << 24); }
    private void fill(int color) { paint.setStyle(Paint.Style.FILL); paint.setColor(color); }
    private void stroke(int color, float width) {
        paint.setStyle(Paint.Style.STROKE); paint.setColor(color); paint.setStrokeWidth(width);
    }
    private void text(Canvas canvas, String text, float x, float y, float size, int color, boolean bold) {
        fill(color);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(size);
        paint.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        canvas.drawText(text, x, y - (paint.ascent() + paint.descent()) / 2, paint);
    }
    private int dp(int value) { return Math.round(dp((float) value)); }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
}
