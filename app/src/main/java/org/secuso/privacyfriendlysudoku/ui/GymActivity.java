/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import org.secuso.privacyfriendlysudoku.R;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanTechnique;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingMode;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingStats;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingStatsRepository;

import java.util.ArrayList;
import java.util.List;

/** Catalog for isolated human-technique drills. */
public class GymActivity extends BaseActivity {
    private static final String PREFS_NAME = "gym";
    private static final String PREF_MODE = "mode";

    private SharedPreferences preferences;
    private TrainingStatsRepository statsRepository;
    private TrainingMode mode;
    private SkillAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gym);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle(R.string.gym);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        statsRepository = new TrainingStatsRepository(this);
        try {
            mode = TrainingMode.valueOf(preferences.getString(PREF_MODE,
                    TrainingMode.FOCUSED.name()));
        } catch(IllegalArgumentException | NullPointerException failure) {
            mode = TrainingMode.FOCUSED;
        }

        RadioGroup modeToggle = findViewById(R.id.gymModeToggle);
        modeToggle.check(mode == TrainingMode.FOCUSED
                ? R.id.gymModeFocused : R.id.gymModeFull);
        modeToggle.setOnCheckedChangeListener((group, checkedId) -> {
            mode = checkedId == R.id.gymModeFull ? TrainingMode.FULL : TrainingMode.FOCUSED;
            preferences.edit().putString(PREF_MODE, mode.name()).apply();
            adapter.notifyDataSetChanged();
        });

        adapter = new SkillAdapter();
        ListView list = findViewById(R.id.gymSkillList);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            HumanTechnique technique = adapter.techniqueAt(position);
            if(technique == null) return;
            Intent intent = new Intent(this, GymDrillActivity.class);
            intent.putExtra(GymDrillActivity.EXTRA_TECHNIQUE, technique.name());
            intent.putExtra(GymDrillActivity.EXTRA_MODE, mode.name());
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if(adapter != null) {
            statsRepository = new TrainingStatsRepository(this);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_gym, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if(item.getItemId() == R.id.action_reset_gym) {
            new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                    .setMessage(R.string.gym_reset_confirmation)
                    .setPositiveButton(R.string.reset_confirmation_confirm, (dialog, which) -> {
                        statsRepository.reset();
                        adapter.notifyDataSetChanged();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private final class SkillAdapter extends BaseAdapter {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_SKILL = 1;
        private final List<CatalogEntry> entries = new ArrayList<>();
        private final LayoutInflater inflater = LayoutInflater.from(GymActivity.this);

        SkillAdapter() {
            int previousLevel = -1;
            for(HumanTechnique technique : HumanTechnique.values()) {
                if(technique.getBaseLevel() != previousLevel) {
                    previousLevel = technique.getBaseLevel();
                    entries.add(CatalogEntry.header(previousLevel));
                }
                entries.add(CatalogEntry.skill(technique));
            }
        }

        HumanTechnique techniqueAt(int position) {
            return entries.get(position).technique;
        }

        @Override public int getCount() { return entries.size(); }
        @Override public Object getItem(int position) { return entries.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public int getViewTypeCount() { return 2; }
        @Override public int getItemViewType(int position) {
            return entries.get(position).technique == null ? TYPE_HEADER : TYPE_SKILL;
        }
        @Override public boolean isEnabled(int position) {
            return entries.get(position).technique != null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CatalogEntry entry = entries.get(position);
            if(entry.technique == null) {
                TextView header = (TextView) (convertView == null
                        ? inflater.inflate(R.layout.gym_level_header, parent, false) : convertView);
                header.setText(getString(R.string.gym_level_format, entry.level));
                return header;
            }

            View row = convertView == null
                    ? inflater.inflate(R.layout.gym_skill_row, parent, false) : convertView;
            TextView title = row.findViewById(R.id.gymSkillTitle);
            TextView stats = row.findViewById(R.id.gymSkillStats);
            title.setText(entry.technique.getTitle());
            TrainingStats record = statsRepository.get(entry.technique, mode);
            stats.setText(getString(R.string.gym_stats_row_format, record.getAttempts(),
                    record.getAccuracyPercent(), record.getReveals(), record.getCurrentStreak(),
                    record.getBestStreak()));
            return row;
        }
    }

    private static final class CatalogEntry {
        final int level;
        final HumanTechnique technique;

        private CatalogEntry(int level, HumanTechnique technique) {
            this.level = level;
            this.technique = technique;
        }

        static CatalogEntry header(int level) { return new CatalogEntry(level, null); }
        static CatalogEntry skill(HumanTechnique technique) {
            return new CatalogEntry(technique.getBaseLevel(), technique);
        }
    }
}
