/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.ui.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import org.secuso.privacyfriendlysudoku.R;
import org.secuso.privacyfriendlysudoku.controller.GameController;
import org.secuso.privacyfriendlysudoku.controller.hints.GameHint;

import java.util.List;

/**
 * Explanation-first hint dialog with the same Summary, Details, and Apply flow as the web
 * reference. Details are paged so each deduction can be considered on the highlighted board.
 */
public final class HumanHintDialog {

    private final Context context;
    private final GameController gameController;
    private final GameHint hint;
    private AlertDialog dialog;
    private int detailIndex = -1;

    public HumanHintDialog(Context context, GameController gameController, GameHint hint) {
        this.context = context;
        this.gameController = gameController;
        this.hint = hint;
    }

    public void show() {
        dialog = new AlertDialog.Builder(context, R.style.AppTheme_Dialog)
                .setTitle(hint.getTitle())
                .setMessage(hint.getSummary())
                .setPositiveButton(R.string.hint_apply, null)
                .setNeutralButton(R.string.hint_details, null)
                .setNegativeButton(R.string.hint_close, null)
                .create();

        configureWindow();
        dialog.setOnDismissListener(ignored -> gameController.endHint());
        dialog.setOnShowListener(ignored -> {
            gameController.beginHint(hint);
            configureWindow();
            showSummary();
        });
        dialog.show();
    }

    private void configureWindow() {
        Window window = dialog.getWindow();
        if(window == null) {
            return;
        }
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.dimAmount = 0.35f;
        attributes.gravity = Gravity.BOTTOM;
        window.setAttributes(attributes);
    }

    private void showSummary() {
        detailIndex = -1;
        gameController.showHintFrame(-1);
        dialog.setTitle(hint.getTitle());
        dialog.setMessage(hint.getSummary());

        Button apply = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        apply.setText(R.string.hint_apply);
        apply.setOnClickListener(view -> applyHint());

        Button details = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        if(hint.getDetails().isEmpty()) {
            details.setVisibility(View.GONE);
        } else {
            details.setVisibility(View.VISIBLE);
            details.setText(R.string.hint_details);
            details.setOnClickListener(view -> showDetail(0));
        }

        Button close = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        close.setText(R.string.hint_close);
        close.setOnClickListener(view -> dialog.dismiss());
    }

    private void showDetail(int index) {
        List<String> details = hint.getDetails();
        detailIndex = index;
        gameController.showHintFrame(detailIndex);
        dialog.setTitle(context.getString(R.string.hint_detail_title,
                hint.getTitle(), detailIndex + 1, details.size()));
        dialog.setMessage(details.get(detailIndex));

        Button next = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if(detailIndex == details.size() - 1) {
            next.setText(R.string.hint_apply);
            next.setOnClickListener(view -> applyHint());
        } else {
            next.setText(R.string.hint_next);
            next.setOnClickListener(view -> showDetail(detailIndex + 1));
        }

        Button back = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        back.setVisibility(View.VISIBLE);
        back.setText(R.string.hint_back);
        back.setOnClickListener(view -> {
            if(detailIndex == 0) {
                showSummary();
            } else {
                showDetail(detailIndex - 1);
            }
        });
    }

    private void applyHint() {
        gameController.applyHint(hint);
        dialog.dismiss();
    }
}
