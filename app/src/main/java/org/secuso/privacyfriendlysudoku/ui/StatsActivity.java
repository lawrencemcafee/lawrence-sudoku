/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.secuso.privacyfriendlysudoku.R;
import org.secuso.privacyfriendlysudoku.controller.SaveLoadStatistics;
import org.secuso.privacyfriendlysudoku.controller.helper.HighscoreInfoContainer;
import org.secuso.privacyfriendlysudoku.game.DifficultyCategory;
import org.secuso.privacyfriendlysudoku.game.DifficultyDisplayMode;
import org.secuso.privacyfriendlysudoku.game.DifficultyLevel;
import org.secuso.privacyfriendlysudoku.game.DifficultyPreferences;
import org.secuso.privacyfriendlysudoku.game.GameType;
import org.secuso.privacyfriendlysudoku.game.GameTypePreferences;

import java.util.Locale;

/** High scores for one browsable difficulty at a time. */
public class StatsActivity extends BaseActivity {
    private SectionsPagerAdapter sectionsPagerAdapter;
    private DifficultyPreferences difficultyPreferences;
    private int selectedDifficultyIndex;
    private TextView difficultyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.menu_highscore);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        difficultyPreferences = new DifficultyPreferences(
                android.preference.PreferenceManager.getDefaultSharedPreferences(this));
        GameTypePreferences gameTypePreferences = new GameTypePreferences(
                android.preference.PreferenceManager.getDefaultSharedPreferences(this));
        selectedDifficultyIndex = difficultyPreferences.getSelectionIndex();

        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.main_content);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
        if(savedInstanceState == null) {
            viewPager.setCurrentItem(gameTypePreferences.getCurrentGameTypeIndex(), false);
        }

        difficultyText = findViewById(R.id.stats_difficulty_text);
        SeekBar selector = findViewById(R.id.stats_difficulty_selector);
        selector.setMax(difficultyPreferences.getSelectionCount() - 1);
        selector.setProgress(selectedDifficultyIndex);
        selector.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedDifficultyIndex = progress;
                updateDifficultyText();
                if(sectionsPagerAdapter != null) sectionsPagerAdapter.refresh();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        updateDifficultyText();
    }

    DifficultyLevel getSelectedLevel() {
        return DifficultyLevel.of(selectedDifficultyIndex + 1);
    }

    DifficultyCategory getSelectedCategory() {
        return DifficultyCategory.values()[selectedDifficultyIndex];
    }

    private void updateDifficultyText() {
        if(difficultyPreferences.getMode() == DifficultyDisplayMode.NUMBERED) {
            difficultyText.setText(getString(R.string.difficulty_level_format,
                    getSelectedLevel().getValue()));
        } else {
            difficultyText.setText(getSelectedCategory().getStringResId());
        }
        SeekBar selector = findViewById(R.id.stats_difficulty_selector);
        if(selector != null) selector.setContentDescription(difficultyText.getText());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_stats, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_reset) {
            SaveLoadStatistics.resetStats(this);
            sectionsPagerAdapter.refresh();
            return true;
        }
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private final FragmentManager fragmentManager;

        SectionsPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
            this.fragmentManager = fragmentManager;
        }

        @Override public Fragment getItem(int position) { return ScoreFragment.newInstance(position); }
        @Override public int getCount() { return GameType.getValidGameTypes().size(); }
        @Override public CharSequence getPageTitle(int position) {
            return getString(GameType.getValidGameTypes().get(position).getStringResID());
        }

        void refresh() {
            for(Fragment fragment : fragmentManager.getFragments()) {
                if(fragment instanceof ScoreFragment) ((ScoreFragment) fragment).refresh();
            }
        }
    }

    public static class ScoreFragment extends Fragment {
        private static final String ARG_GAME_TYPE_INDEX = "game_type_index";
        private View root;

        static ScoreFragment newInstance(int index) {
            ScoreFragment fragment = new ScoreFragment();
            Bundle arguments = new Bundle();
            arguments.putInt(ARG_GAME_TYPE_INDEX, index);
            fragment.setArguments(arguments);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            root = inflater.inflate(R.layout.fragment_stats, container, false);
            refresh();
            return root;
        }

        void refresh() {
            if(root == null || getActivity() == null) return;
            StatsActivity activity = (StatsActivity) getActivity();
            GameType type = GameType.getValidGameTypes().get(
                    getArguments().getInt(ARG_GAME_TYPE_INDEX));
            SaveLoadStatistics loader = new SaveLoadStatistics(activity);
            HighscoreInfoContainer stats = activity.difficultyPreferences.getMode()
                    == DifficultyDisplayMode.NUMBERED
                    ? loader.loadStats(type, activity.getSelectedLevel())
                    : loader.loadStats(type, activity.getSelectedCategory());

            ((ImageView) root.findViewById(R.id.statistic_image)).setImageResource(type.getResIDImage());
            ((TextView) root.findViewById(R.id.first_diff_text)).setText(
                    activity.difficultyPreferences.getMode() == DifficultyDisplayMode.NUMBERED
                            ? activity.getString(R.string.difficulty_level_format,
                            activity.getSelectedLevel().getValue())
                            : activity.getString(activity.getSelectedCategory().getStringResId()));
            RatingBar legacyBar = root.findViewById(R.id.first_diff_bar);
            legacyBar.setVisibility(View.GONE);

            setText(R.id.numb_of_hints, stats.getNumberOfHintsUsed());
            setText(R.id.numb_of_total_games, stats.getNumberOfGames());
            ((TextView) root.findViewById(R.id.numb_of_total_time)).setText(formatTime(stats.getTime()));
            int average = stats.getNumberOfGamesNoHints() == 0 ? 0
                    : stats.getTimeNoHints() / stats.getNumberOfGamesNoHints();
            ((TextView) root.findViewById(R.id.first_ava_time)).setText(formatTime(average));
            int minimum = stats.getMinTime() == Integer.MAX_VALUE ? 0 : stats.getMinTime();
            ((TextView) root.findViewById(R.id.first_min_time)).setText(formatTime(minimum));
        }

        private void setText(int viewId, int value) {
            ((TextView) root.findViewById(viewId)).setText(String.valueOf(value));
        }

        private static String formatTime(int secondsTotal) {
            int hours = secondsTotal / 3600;
            int minutes = (secondsTotal / 60) % 60;
            int seconds = secondsTotal % 60;
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        }
    }
}
