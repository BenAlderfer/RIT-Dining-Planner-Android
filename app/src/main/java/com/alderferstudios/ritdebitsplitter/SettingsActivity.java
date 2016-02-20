package com.alderferstudios.ritdebitsplitter;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

/**
 * Settings activity of the Budget Splitter
 *
 * @author Ben Alderfer
 *         Alderfer Studios
 */
public class SettingsActivity extends AppCompatActivity {

    /**
     * SharedPreferences objects for saving values
     */
    protected static SharedPreferences shared;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        shared = PreferenceManager.getDefaultSharedPreferences(this);
        //set the default value for the preferences
        PreferenceManager.setDefaultValues(this, R.xml.prefs, false);

        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //lock orientation on phones
        if (!MainActivity.isTablet(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }

        getFragmentManager().beginTransaction()
                .replace(R.id.frame, new PrefsFragment()).commit();
    }

    /**
     * Only button is back arrow - no need to check
     *
     * @param item - unused
     * @return - unused
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
    }

    /**
     * The fragment that holds the setting
     */
    public static class PrefsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            PreferenceManager.setDefaultValues(getActivity(), R.xml.prefs, false);
            addPreferencesFromResource(R.xml.prefs);

            setSummaries();
            shared.registerOnSharedPreferenceChangeListener(this);
        }

        /**
         * Sets the summaries on start
         */
        protected void setSummaries() {
            setSaveBoxSummary();                                                                  //Save box
        }

        /**
         * Sets the summaries on change
         */
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switch (key) {
                case "saveBox":
                    setSaveBoxSummary();
                    break;                                       //Save box
            }
        }

        /**
         * Sets the summary for the save box
         */
        protected void setSaveBoxSummary() {
            if (isAdded()) {                                                                       //must check if the fragment is added to the activity
                Preference p = findPreference("saveBox");
                if (p != null) {
                    if (shared.getBoolean("saveBox", false)) {
                        p.setSummary(R.string.enabledSaveDesc);
                    } else {
                        p.setSummary(R.string.disabledSaveDesc);
                    }
                }
            }
        }
    }

}
