/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.

 Privacy Friendly Sudoku is distributed in the hope
 that it will be useful, but WITHOUT ANY WARRANTY; without even
 the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Privacy Friendly Sudoku. If not, see <http://www.gnu.org/licenses/>.
 */
package org.secuso.privacyfriendlysudoku.ui.view;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import org.secuso.privacyfriendlysudoku.R;
import org.secuso.privacyfriendlysudoku.controller.Symbol;
import org.secuso.privacyfriendlysudoku.controller.hints.GameHint;
import org.secuso.privacyfriendlysudoku.game.GameCell;

/**
 * Created by TMZ_LToP on 10.11.2015.
 */
public class SudokuCellView extends View {

    GameCell mGameCell;
    int mWidth;
    int mHeight;
    int mSectionHeight;
    int mSectionWidth;
    int mRow;
    int mCol;
    int size;
    boolean selected;
    CellHighlightTypes highlightType = CellHighlightTypes.Default;
    Symbol symbolsToUse = Symbol.Default;
    RelativeLayout.LayoutParams params;

    int backgroundColor;
    int backgroundErrorColor;
    int backgroundSelectedColor;
    int backgroundConnectedOuterColor;
    int backgroundConnectedInnerColor;
    int backgroundValueHighlightedColor;
    int backgroundValueHighlightedSelectedColor;
    int textColor;
    private GameHint activeHint;
    private int activeHintFrame = -1;
    private int feedbackValue;
    private boolean feedbackCorrect;

    public SudokuCellView(Context context, AttributeSet attrs){
        super(context);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SudokuCellView);
        backgroundColor = a.getColor(R.styleable.SudokuCellView_sudokuCellBackgroundColor, Color.argb(255, 200, 200, 200));
        backgroundErrorColor = a.getColor(R.styleable.SudokuCellView_sudokuCellBackgroundErrorColor, Color.argb(255, 200, 200, 200));
        backgroundSelectedColor = a.getColor(R.styleable.SudokuCellView_sudokuCellBackgroundSelectedColor, Color.argb(255, 200, 200, 200));
        backgroundConnectedOuterColor = a.getColor(R.styleable.SudokuCellView_sudokuCellBackgroundConnectedOuterColor, Color.argb(255, 200, 200, 200));
        backgroundConnectedInnerColor = a.getColor(R.styleable.SudokuCellView_sudokuCellBackgroundConnectedInnerColor, Color.argb(255, 200, 200, 200));
        backgroundValueHighlightedColor = a.getColor(R.styleable.SudokuCellView_sudokuCellBackgroundValueHighlightedColor, Color.argb(255, 200, 200, 200));
        backgroundValueHighlightedSelectedColor = a.getColor(R.styleable.SudokuCellView_sudokuCellBackgroundValueHighlightedSelectedColor, Color.argb(255, 200, 200, 200));
        textColor = a.getColor(R.styleable.SudokuCellView_sudokuCellTextColor, Color.argb(255, 200, 200, 200));
        a.recycle();
    }



    public void setSelected(boolean b) {
        this.selected = b;
    }

    public void setValues (int width, int height, int sectionHeight, int sectionWidth, GameCell gameCell,int size) {
        boolean dimensionsChanged = mWidth != width || mHeight != height
                || mRow != gameCell.getRow() || mCol != gameCell.getCol();
        mSectionHeight = sectionHeight;
        mSectionWidth = sectionWidth;
        mGameCell = gameCell;
        mWidth = width;
        mHeight = height;
        mRow = gameCell.getRow();
        mCol = gameCell.getCol();
        this.size = size;

        if(dimensionsChanged || params == null) initLayoutParams();
    }

    private void initLayoutParams() {
        if(this.params == null) {
            params = new RelativeLayout.LayoutParams(mWidth, mHeight);
        }

        // Set Layout
        params.width = mWidth;
        params.height = mHeight;
        params.topMargin = mRow*mHeight;
        params.leftMargin = mCol*mWidth;
        this.setLayoutParams(params);
    }

    public void setSymbols(Symbol s) {
        symbolsToUse = s;
    }

    public void setHighlightType(CellHighlightTypes highlightType) {
        this.highlightType = highlightType;
    }

    public void setHintOverlay(GameHint hint, int frameIndex) {
        activeHint = hint;
        activeHintFrame = frameIndex;
    }

    public void showCandidateFeedback(int value, boolean correct) {
        feedbackValue = value;
        feedbackCorrect = correct;
        invalidate();
        postDelayed(() -> {
            if(feedbackValue == value) {
                feedbackValue = 0;
                invalidate();
            }
        }, 450);
    }

    /*@Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if(mGameCell == null) return false;

        if(motionEvent.getAction() == motionEvent.ACTION_DOWN) {
            highlightType = CellHighlightTypes.Selected;
        }

        return true;
    }*/

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw single Field
        drawInfo(canvas);
    }

    private void drawInfo(Canvas canvas) {
        Paint p = new Paint();
        switch(highlightType) {
            case Default:
                p.setColor(backgroundColor);
                break;
            case Error:
                p.setColor(backgroundErrorColor);
                break;
            case Selected:
                p.setColor(backgroundSelectedColor);
                break;
            case Connected:
                p.setColor(backgroundConnectedOuterColor);
                drawBackground(canvas, 3, 3, mWidth - 3, mHeight - 3, p);
                p.setColor(backgroundConnectedInnerColor);
                p.setAlpha(100);
                break;
            case Value_Highlighted:
                p.setColor(backgroundValueHighlightedColor);
                break;
            case Value_Highlighted_Selected:
                p.setColor(backgroundValueHighlightedSelectedColor);
                break;
            default:
                p.setColor(backgroundColor);
        }


        drawBackground(canvas, 3, 3, mWidth - 3, mHeight - 3, p);

        drawHintBackground(canvas);

        // if there is no mGameCell .. we can not retrieve the information to draw
        if(mGameCell != null) {
            drawValue(canvas);
        }
    }

    public void drawBackground(Canvas canvas, int left, int top, int right, int bottom, Paint p) {
        RectF rect = new RectF(left, top, right, bottom);
        canvas.drawRect(rect, p);
    }

    public void drawValue(Canvas canvas) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(textColor);
        if(mGameCell.getValue() == 0) {
            drawCandidates(canvas, p);
            return;
        }


        if (mGameCell.isFixed()) {
            p.setTypeface(Typeface.DEFAULT_BOLD);
        }
        p.setAntiAlias(true);
        p.setTextSize(Math.min(mHeight * 3 / 4, mHeight * 3 / 4));
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(Symbol.getSymbol(symbolsToUse, mGameCell.getValue()-1), mHeight / 2, mHeight / 2 + mHeight / 4, p);
    }

    private void drawHintBackground(Canvas canvas) {
        if(activeHint == null) {
            return;
        }
        GameHint.Mark mark = activeHint.getOverlayFrame(activeHintFrame)
                .getCellMark(mRow, mCol);
        if(mark == null) {
            return;
        }
        Paint overlay = new Paint();
        overlay.setColor(HintPalette.colorFor(mark));
        overlay.setAlpha(mark == GameHint.Mark.CONTRADICTION ? 90 : 55);
        drawBackground(canvas, 3, 3, mWidth - 3, mHeight - 3, overlay);
    }

    private void drawCandidates(Canvas canvas, Paint paint) {
        int columns = Math.max(1, mSectionWidth);
        int rows = Math.max(1, mSectionHeight);
        float slotWidth = mWidth / (float) columns;
        float slotHeight = mHeight / (float) rows;
        paint.setTypeface(Typeface.SANS_SERIF);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Math.min(slotWidth, slotHeight) * 0.58f);

        GameHint.HintFrame frame = activeHint == null ? null
                : activeHint.getOverlayFrame(activeHintFrame);
        boolean preview = activeHint != null && activeHint.shouldApplyCandidatePreview();
        for(int value = 1; value <= size; value++) {
            GameHint.Mark mark = frame == null ? null
                    : frame.getCandidateMark(mRow, mCol, value);
            boolean isElimination = activeHint != null
                    && activeHint.eliminates(mRow, mCol, value);
            boolean visible = preview
                    ? activeHint.hasPreviewCandidate(mRow, mCol, value)
                    : mGameCell.getNotes()[value - 1];
            boolean feedback = feedbackValue == value;
            if(mark != null) visible = true;
            if(feedback) visible = true;
            if(!visible && !isElimination) continue;

            GameHint.Mark effectiveMark = isElimination ? GameHint.Mark.ELIMINATE : mark;
            paint.setColor(feedback
                    ? (feedbackCorrect ? Color.rgb(46, 125, 50) : Color.rgb(198, 40, 40))
                    : (effectiveMark == null ? textColor : HintPalette.colorFor(effectiveMark)));
            paint.setTypeface(effectiveMark == null ? Typeface.SANS_SERIF : Typeface.DEFAULT_BOLD);
            float x = candidateCenterX(value);
            float y = candidateCenterY(value) - (paint.ascent() + paint.descent()) / 2f;
            String glyph = Symbol.getSymbol(symbolsToUse, value - 1);
            canvas.drawText(glyph, x, y, paint);

            if(feedback) {
                Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
                ring.setStyle(Paint.Style.STROKE);
                ring.setStrokeWidth(Math.max(2f, Math.min(slotWidth, slotHeight) / 12f));
                ring.setColor(feedbackCorrect
                        ? Color.rgb(46, 125, 50) : Color.rgb(198, 40, 40));
                canvas.drawCircle(x, candidateCenterY(value),
                        Math.min(slotWidth, slotHeight) * 0.36f, ring);
            }

            if(isElimination || effectiveMark == GameHint.Mark.CONTRADICTION) {
                Paint strike = new Paint(Paint.ANTI_ALIAS_FLAG);
                strike.setColor(HintPalette.colorFor(GameHint.Mark.ELIMINATE));
                strike.setStrokeWidth(Math.max(2f, Math.min(slotWidth, slotHeight) / 12f));
                canvas.drawLine(x - slotWidth * 0.28f, candidateCenterY(value) + slotHeight * 0.20f,
                        x + slotWidth * 0.28f, candidateCenterY(value) - slotHeight * 0.20f, strike);
            }
        }
    }

    public float candidateCenterX(int value) {
        int columns = Math.max(1, mSectionWidth);
        float slotWidth = mWidth / (float) columns;
        return ((value - 1) % columns + 0.5f) * slotWidth;
    }

    public float candidateCenterY(int value) {
        int columns = Math.max(1, mSectionWidth);
        int rows = Math.max(1, mSectionHeight);
        float slotHeight = mHeight / (float) rows;
        return ((value - 1) / columns + 0.5f) * slotHeight;
    }

    public int getRow() {
        return mRow;
    }
    public int getCol() {
        return mCol;
    }

    /*@Override
    public Parcelable onSaveInstanceState() {
        Parcelable state = super.onSaveInstanceState();

        return state;
    }*/
}
