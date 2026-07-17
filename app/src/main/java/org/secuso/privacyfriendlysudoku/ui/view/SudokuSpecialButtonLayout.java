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
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.secuso.privacyfriendlysudoku.R;
import org.secuso.privacyfriendlysudoku.controller.GameController;
import org.secuso.privacyfriendlysudoku.controller.hints.GameHint;
import org.secuso.privacyfriendlysudoku.game.listener.IHighlightChangedListener;

import static org.secuso.privacyfriendlysudoku.ui.view.SudokuButtonType.Spacer;
import static org.secuso.privacyfriendlysudoku.ui.view.SudokuButtonType.getSpecialButtons;

/**
 * Created by TMZ_LToP on 17.11.2015.
 */
public class SudokuSpecialButtonLayout extends LinearLayout implements IHighlightChangedListener {


    SudokuSpecialButton[] fixedButtons;
    public int fixedButtonsCount = getSpecialButtons().size();
    GameController gameController;
    SudokuKeyboardLayout keyboard;
    Bitmap bitMap,bitResult;
    Canvas canvas;
    Context context;
    float buttonMargin;

    OnClickListener listener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v instanceof SudokuSpecialButton) {
                SudokuSpecialButton btn = (SudokuSpecialButton)v;

                //int row = gameController.getSelectedRow();
                //int col = gameController.getSelectedCol();

                switch(btn.getType()) {
                    case Delete:
                        gameController.deleteSelectedCellsValue();
                        break;
                    case NoteToggle:
                        // rotates the Drawable
                        gameController.setNoteStatus(!gameController.getNoteStatus());
                        keyboard.updateNotesEnabled();
                        onHighlightChanged();
                        break;
                    case FillCandidates:
                        gameController.fillValidCandidates();
                        break;
                    case Do:
                        gameController.ReDo();
                        break;
                    case Undo:
                        gameController.UnDo();
                        break;
                    case Hint:
                        GameHint hint = gameController.getNextHint();
                        if(hint != null) {
                            new HumanHintDialog(context, gameController, hint).show();
                        } else {
                            Toast.makeText(getContext(), R.string.hint_unavailable,
                                    Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    };


    public SudokuSpecialButtonLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SudokuSpecialButtonLayout);
        buttonMargin = a.getDimension(R.styleable.SudokuSpecialButtonLayout_sudokuSpecialKeyboardMargin, 5f);
        a.recycle();

        setWeightSum(fixedButtonsCount);
        this.context = context;
    }

    public void setButtonsEnabled(boolean enabled) {
        for(SudokuSpecialButton b : fixedButtons) {
            b.setEnabled(enabled);
        }
    }

    public void setButtons(int width, GameController gc, SudokuKeyboardLayout key, int orientation, Context cxt) {
        keyboard=key;
        gameController = gc;
        context = cxt;
        if(gameController != null) {
            gameController.registerHighlightChangedListener(this);
        }
        fixedButtons = new SudokuSpecialButton[fixedButtonsCount];
        LayoutParams p;
        int i = 0;
        //ArrayList<SudokuButtonType> type = (ArrayList<SudokuButtonType>) SudokuButtonType.getSpecialButtons();
        for (SudokuButtonType t : getSpecialButtons()){
            fixedButtons[i] = new SudokuSpecialButton(getContext(),null);
            if(orientation == LinearLayout.HORIZONTAL) {
                p = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
            } else {
                p = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1);
            }
            fixedButtons[i].setPadding((int)buttonMargin*5, 0, (int)buttonMargin*5, 0);
            p.setMargins((int)buttonMargin, (int)buttonMargin, (int)buttonMargin, (int)buttonMargin);

            //int width2 =width/(fixedButtonsCount);
            //p.width= width2-15;
            if(t == Spacer) {
                fixedButtons[i].setVisibility(View.INVISIBLE);
            }

            fixedButtons[i].setLayoutParams(p);
            fixedButtons[i].setType(t);
            fixedButtons[i].setImageDrawable(ContextCompat.getDrawable(context, fixedButtons[i].getType().getResID()));
            if(t == SudokuButtonType.FillCandidates) {
                fixedButtons[i].setContentDescription(context.getString(R.string.help_fill_candidates));
            } else if(t == SudokuButtonType.Hint) {
                fixedButtons[i].setContentDescription(context.getString(R.string.help_hint));
            }
            fixedButtons[i].setScaleType(ImageView.ScaleType.FIT_XY);
            fixedButtons[i].setAdjustViewBounds(true);
            fixedButtons[i].setOnClickListener(listener);
            fixedButtons[i].setBackgroundResource(R.drawable.numpad_highlighted_four);
            addView(fixedButtons[i]);

            i++;
        }

    }

    @Override
    public void onHighlightChanged() {
        for(int i = 0; i < fixedButtons.length; i++) {
            switch(fixedButtons[i].getType()) {
                case Undo:
                    fixedButtons[i].setBackgroundResource(gameController.isUndoAvailable() ?
                            R.drawable.numpad_highlighted_four : R.drawable.button_inactive);
                    break;
                case Do:
                    fixedButtons[i].setBackgroundResource(gameController.isRedoAvailable() ?
                            R.drawable.numpad_highlighted_four : R.drawable.button_inactive);
                    break;
                case NoteToggle:
                    Drawable drawable = ContextCompat.getDrawable(context, fixedButtons[i].getType().getResID());
                    // prepare canvas for the rotation of the note drawable
                    setUpVectorDrawable(drawable);

                    canvas.rotate(gameController.getNoteStatus() ? 45.0f : 0.0f, bitMap.getWidth()/2, bitMap.getHeight()/2);
                    canvas.drawBitmap(bitMap, 0, 0, null);
                    drawable.draw(canvas);

                    fixedButtons[i].setImageBitmap(bitResult);
                    fixedButtons[i].setBackgroundResource(gameController.getNoteStatus() ? R.drawable.numpad_highlighted_three : R.drawable.numpad_highlighted_four);

                    keyboard.updateNotesEnabled();

                    break;
                default:
                    break;
            }
        }
    }

    /*
    Set up the vector drawables so that they can be properly displayed despite using theme attributes for their fill color
     */
    private void setUpVectorDrawable(Drawable drawable) {
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        bitMap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        bitResult = Bitmap.createBitmap(bitMap.getWidth(), bitMap.getHeight(), Bitmap.Config.ARGB_8888);

        canvas = new Canvas(bitResult);
    }

}
