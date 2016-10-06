package com.alderferstudios.ritdebitsplitter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Main window
 *
 * @author Ben Alderfer
 *         Alderfer Studios
 */
public class MainActivity extends AppCompatActivity {

    /**
     * SharedPreferences objects for saving values
     */
    protected static SharedPreferences shared;
    protected static SharedPreferences.Editor editor;

    /**
     * The starting date
     */
    private int startYear = 2016, startMonth = 1, startDay = 1;

    /**
     * The ending date
     */
    private int endYear = 2016, endMonth = 2, endDay = 2;

    /**
     * week and day difference between dates
     */
    private int weekDiff, dayDiff, currentWeekDiff, currentDayDiff;

    /**
     * Default dates, derived from the date range string
     */
    private int defaultStartMonth, defaultStartDay, defaultStartYear,
            defaultEndMonth, defaultEndDay, defaultEndYear;

    /**
     * Results cards
     */
    private CardView summaryCard, tableCard;

    /**
     * The results TextViews
     * <p/>
     * 0 - summary amount - "you can spend"
     * <p/>
     * 1 - Average Daily
     * 2 - Average Weekly
     * 3 - Current Daily
     * 4 - Current Weekly
     * 5 - Difference Daily
     * 6 - Difference Weekly
     */
    private TextView[] tvs = new TextView[7];

    /**
     * Start and end date TextViews
     */
    private TextView startDateText, endDateText;

    /**
     * The dropdowns for meal options and terms
     */
    private Spinner mealOptionSpinner;

    /**
     * The EditText fields
     */
    private EditText customDiningEditText, rolloverEditText, currentBalanceEditText, totalDaysOffEditText, pastDaysOffEditText;

    /**
     * The text input in the fields
     */
    private String customDining = "", currentBalance = "", totalDaysOff = "", pastDaysOff = "",
            rollOver = "", selectedMealPlan = "", lastPlanSelected = "";

    /**
     * Whether the fields have been entered
     */
    private boolean isEnteringDate;

    /**
     * Either start or end date
     */
    private String dateBeingSet;

    /**
     * The total initial debit
     * Meal plan + rollover
     */
    private double totalInitial;

    /**
     * Flag to prevent overwriting days off on boot
     * Will only overwrite if nothing was entered
     */
    private boolean isBooting;

    /**
     * if the snackbar is currently showing
     */
    private boolean snackbarIsShowing = false;

    /**
     * If the user is manually calculating (or on boot)
     */
    private boolean isManuallyCalculating = false;

    /**
     * Formats a number with 2 decimal points
     */
    private DecimalFormat twoDecimal;

    /**
     * Checks if the device is a tablet
     *
     * @param context the Context
     * @return true if a tablet
     */
    protected static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * Sets up the toolbar
     * Saves the Views for later
     * Hides the results headers
     * Adds listeners to the fields and buttons
     *
     * @param savedInstanceState - the previous state (not used)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        JodaTimeAndroid.init(this);

        if (!isTablet(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }

        shared = PreferenceManager.getDefaultSharedPreferences(this);
        editor = shared.edit();
        //set the default value for the preferences
        PreferenceManager.setDefaultValues(this, R.xml.prefs, false);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        isBooting = true;

        mealOptionSpinner = (Spinner) findViewById(R.id.mealOption);
        customDiningEditText = (EditText) findViewById(R.id.customDiningText);
        rolloverEditText = (EditText) findViewById(R.id.rolloverBalanceText);
        currentBalanceEditText = (EditText) findViewById(R.id.currentBalanceText);
        startDateText = (TextView) findViewById(R.id.startDate);
        endDateText = (TextView) findViewById(R.id.endDate);
        totalDaysOffEditText = (EditText) findViewById(R.id.totalDaysOffText);
        pastDaysOffEditText = (EditText) findViewById(R.id.pastDaysOffText);

        tvs[0] = (TextView) findViewById(R.id.summaryAmount);

        tvs[1] = (TextView) findViewById(R.id.initDailyText);
        tvs[2] = (TextView) findViewById(R.id.initWeeklyText);
        tvs[3] = (TextView) findViewById(R.id.currentDailyText);
        tvs[4] = (TextView) findViewById(R.id.currentWeeklyText);
        tvs[5] = (TextView) findViewById(R.id.differenceDailyText);
        tvs[6] = (TextView) findViewById(R.id.differenceWeeklyText);

        summaryCard = (CardView) findViewById(R.id.summaryCard);
        tableCard = (CardView) findViewById(R.id.tableCard);

        String startDate = getResources().getString(R.string.startDate);
        String endDate = getResources().getString(R.string.endDate);

        defaultStartMonth = Integer.parseInt(startDate.substring(0, 2));
        defaultStartDay = Integer.parseInt(startDate.substring(3, 5));
        defaultStartYear = Integer.parseInt(startDate.substring(6, 10));

        defaultEndMonth = Integer.parseInt(endDate.substring(0, 2));
        defaultEndDay = Integer.parseInt(endDate.substring(3, 5));
        defaultEndYear = Integer.parseInt(endDate.substring(6, 10));

        twoDecimal = new DecimalFormat("0.00");

        initializeSpinner();

        hideResults();
        setDateDefaults();
        addListeners();
    }

    /**
     * Restore values when the app resumes
     * Do not change anything if they are just resuming from a date picker
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (!isEnteringDate) {
            restoreValues();
            updateResultsManual(currentBalanceEditText);
        } else {
            isEnteringDate = false;
        }
    }

    /**
     * Save entered values when app pauses
     */
    @Override
    protected void onPause() {
        super.onPause();
        saveValues();
    }

    /**
     * Sets up the menu
     *
     * @param menu - the menu to set up
     * @return true (useless)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    /**
     * Handles menu option clicks
     *
     * @param item - the MenuItem that was clicked
     * @return either true or handed off to super class (useless)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent settingsActivity = new Intent(this, SettingsActivity.class);
                startActivity(settingsActivity);
                return true;
            case R.id.tigerbucks:
                Intent tigerbucks = new Intent(Intent.ACTION_VIEW, Uri.parse("https://tigerbucks.rit.edu"));
                tigerbucks.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                tigerbucks.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                tigerbucks.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                Bundle b = new Bundle();
                b.putBoolean("new_window", true);
                tigerbucks.putExtras(b);
                startActivity(tigerbucks);
                return true;
            case R.id.eservices:
                Intent eservices = new Intent(Intent.ACTION_VIEW, Uri.parse("https://eservices.rit.edu/eServices"));
                startActivity(eservices);
                return true;
            case R.id.dining_services:
                Intent diningServices = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.rit.edu/fa/diningservices"));
                startActivity(diningServices);
                return true;
            case R.id.help:
                Intent helpActivity = new Intent(this, HelpActivity.class);
                startActivity(helpActivity);
                return true;
            case R.id.about:
                Intent aboutActivity = new Intent(this, AboutActivity.class);
                startActivity(aboutActivity);
                return true;
            case R.id.reset:
                resetDefaults();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets the date defaults based on resource strings
     */
    private void setDateDefaults() {
        //on boot, do not overwrite any input
        if (isBooting) {
            //set the days off if none were entered
            if (totalDaysOff.equals("")) {
                totalDaysOff = getResources().getString(R.string.daysOff);
            }
            isBooting = false;
        }

        //set the days off if none were entered or the other term's days were entered
        if (totalDaysOff.equals("")) {
            totalDaysOff = getResources().getString(R.string.daysOff);
        }

        //parse the dates
        startMonth = defaultStartMonth;
        startDay = defaultStartDay;
        startYear = defaultStartYear;

        endMonth = defaultEndMonth;
        endDay = defaultEndDay;
        endYear = defaultEndYear;

        //update date fields
        startDateText.setText(startMonth + "/" + startDay + "/" + startYear);
        endDateText.setText(endMonth + "/" + endDay + "/" + endYear);

        //set days off after date change to prevent wrong toasts
        totalDaysOffEditText.setText(totalDaysOff);

        //call to updateResults not needed since updating other fields will do that
    }

    /**
     * Adds a listener to the EditTexts
     * Text is saved to reduce method calls
     * Updates the results if possible
     */
    private void addListeners() {
        //field listeners
        mealOptionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //saves previous meal plan before checking new one
                //previous used to figure out if ViewSwitcher should switch
                lastPlanSelected = selectedMealPlan;
                updateResults();

                //switches fields if needed
                switchTotalCustom();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        customDiningEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateResults();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        rolloverEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                rollOver = rolloverEditText.getText().toString();
                updateResults();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        currentBalanceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentBalance = currentBalanceEditText.getText().toString();
                if (currentBalanceIsEntered()) {
                    //if it ends with a "." remove the "." before getting the number
                    if (currentBalance.length() > 0 && currentBalance.charAt(0) != '.' && currentBalance.substring(currentBalance.length() - 1, currentBalance.length()).equals(".")) {
                        currentBalance = currentBalance.substring(0, currentBalance.length());
                    }
                    //if current balance > initial, fix that
                    //Log.d("current before reset", currentBalance + "");
                    //Log.d("initial before reset", totalInitial + "");
                    if (!currentBalance.equals("") && Double.parseDouble(currentBalance) > totalInitial) {
                        //Log.d("reseting current", "current balance was > initial");
                        currentBalance = totalInitial + "";
                        currentBalanceEditText.setText(currentBalance);
                        try {
                            showSnackbar(getResources().getString(R.string.remainingGreaterThanInitial), true);
                        } catch (NullPointerException e) {
                            Log.e("remain. > initial error", e.getMessage());
                        }
                    }
                }

                updateResults();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        totalDaysOffEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                totalDaysOff = totalDaysOffEditText.getText().toString();
                updateResults();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        pastDaysOffEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                pastDaysOff = pastDaysOffEditText.getText().toString();

                //if total days off is blank and current days off is entered, change total to equal current
                if (totalDaysOff.equals("") && !pastDaysOff.equals("")) {
                    totalDaysOff = pastDaysOff;
                    totalDaysOffEditText.setText(totalDaysOff);
                    try {
                        showSnackbar(getResources().getString(R.string.totalNotEntered), false);
                    } catch (NullPointerException e) {
                        Log.e("total missing error", e.getMessage());
                    }
                }

                // if current days off > total days off, change it to the total
                if (!totalDaysOff.equals("") && !pastDaysOff.equals("") && Integer.parseInt(pastDaysOff) > Integer.parseInt(totalDaysOff)) {
                    pastDaysOff = totalDaysOff;
                    pastDaysOffEditText.setText(totalDaysOff);
                    try {
                        showSnackbar(getResources().getString(R.string.pastGreaterThanTotal), true);
                    } catch (NullPointerException e) {
                        Log.e("past days > total error", e.getMessage());
                    }
                }

                updateResults();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /**
     * Sets the items in the meal option dropdown menu
     */
    private void initializeSpinner() {
        String[] items = new String[]{getString(R.string.mealOption1), getString(R.string.mealOption2),
                getString(R.string.mealOption3), getString(R.string.mealOption4),
                getString(R.string.mealOption5), getString(R.string.mealOption6),
                getString(R.string.mealOption7), getString(R.string.mealOption8),
                getString(R.string.mealOption9), getString(R.string.mealOptionCustom)};
        ArrayAdapter<String> mealPlanAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, items);
        mealOptionSpinner.setAdapter(mealPlanAdapter);
    }

    /**
     * Returns the value of the selected plan
     * Switch cannot be used since the strings are not constants
     *
     * @return the value of the selected plan
     */
    private float getPlanValue() {
        if (mealOptionSpinner.getSelectedItem() != null) {
            selectedMealPlan = mealOptionSpinner.getSelectedItem().toString();
            if (selectedMealPlan.equals(getString(R.string.mealOption1))) {
                return Float.parseFloat(getString(R.string.mealOptionValue1));
            } else if (selectedMealPlan.equals(getString(R.string.mealOption2))) {
                return Float.parseFloat(getString(R.string.mealOptionValue2));
            } else if (selectedMealPlan.equals(getString(R.string.mealOption3))) {
                return Float.parseFloat(getString(R.string.mealOptionValue3));
            } else if (selectedMealPlan.equals(getString(R.string.mealOption4))) {
                return Float.parseFloat(getString(R.string.mealOptionValue4));
            } else if (selectedMealPlan.equals(getString(R.string.mealOption5))) {
                return Float.parseFloat(getString(R.string.mealOptionValue5));
            } else if (selectedMealPlan.equals(getString(R.string.mealOption6))) {
                return Float.parseFloat(getString(R.string.mealOptionValue6));
            } else if (selectedMealPlan.equals(getString(R.string.mealOption7))) {
                return Float.parseFloat(getString(R.string.mealOptionValue7));
            } else if (selectedMealPlan.equals(getString(R.string.mealOption8))) {
                return Float.parseFloat(getString(R.string.mealOptionValue8));
            } else if (selectedMealPlan.equals(getString(R.string.mealOption9))) {
                return Float.parseFloat(getString(R.string.mealOptionValue9));
            } else if (selectedMealPlan.equals(getString(R.string.mealOptionCustom))) { //custom dining
                customDining = customDiningEditText.getText().toString();
                if (!customDining.equals("")) {
                    return Float.parseFloat(customDining);
                }
            }
        }
        return 0;
    }

    /**
     * Saves the entered values for later
     */
    private void saveValues() {
        editor.putInt("mealPlan", mealOptionSpinner.getSelectedItemPosition());

        if (!customDining.equals("")) {
            editor.putFloat("customDining", Float.parseFloat(customDining));
        } else { //save 0 to overwrite previous value
            editor.putFloat("customDining", 0.0f);
        }

        if (!rollOver.equals("")) {
            editor.putFloat("rollOver", Float.parseFloat(rollOver));
        } else { //save 0 to overwrite previous value
            editor.putFloat("rollOver", 0.0f);
        }

        //Log.i("current before save", currentBalance);
        if (currentBalanceIsEntered()) {
            editor.putFloat("currentBalance", Float.parseFloat(currentBalance));
        } else { //save 0 to overwrite previous value
            editor.putFloat("currentBalance", 0.0f);
        }

        if (totalDaysOffIsEntered()) {
            editor.putInt("totalDaysOff", Integer.parseInt(totalDaysOff));
        } else { //save 0 to overwrite previous value
            editor.putInt("totalDaysOff", 0);
        }

        if (!pastDaysOff.equals("")) {
            editor.putInt("pastDaysOff", Integer.parseInt(pastDaysOff));
        } else { //save 0 to overwrite previous value
            editor.putInt("pastDaysOff", 0);
        }

        //save start date
        editor.putInt("startMonth", startMonth);
        editor.putInt("startDay", startDay);
        editor.putInt("startYear", startYear);

        //save end date
        editor.putInt("endMonth", endMonth);
        editor.putInt("endDay", endDay);
        editor.putInt("endYear", endYear);

        editor.commit();

        //Log.i("current after save", String.valueOf(shared.getFloat("currentBalance", 0.0f)));
    }

    /**
     * Restores the previous values
     */
    private void restoreValues() {
        //only restore values if they want to
        if (shared.getBoolean("saveBox", true)) {

            //restore start date
            startMonth = shared.getInt("startMonth", defaultStartMonth);
            startDay = shared.getInt("startDay", defaultStartDay);
            startYear = shared.getInt("startYear", defaultStartYear);

            //restore end date
            endMonth = shared.getInt("endMonth", defaultEndMonth);
            endDay = shared.getInt("endDay", defaultEndDay);
            endYear = shared.getInt("endYear", defaultEndYear);

            //update date field texts
            startDateText.setText(startMonth + "/" + startDay + "/" + startYear);
            endDateText.setText(endMonth + "/" + endDay + "/" + endYear);

            mealOptionSpinner.setSelection(shared.getInt("mealPlan", 0));
            totalInitial = getPlanValue();
            //Log.i("meal option", mealOptionSpinner.getSelectedItem().toString());
            //Log.i("current before", currentBalanceEditText.getText().toString());

            float customDiningRestored = shared.getFloat("customDining", 0.0f);
            if (customDiningRestored != 0.0f) {
                customDiningEditText.setText(String.format(Locale.US, "%.02f", customDiningRestored));
            }

            float rollOver = shared.getFloat("rollOver", 0.0f);
            if (rollOver != 0.0f) {
                rolloverEditText.setText(String.format(Locale.US, "%.02f", rollOver));
            }

            //Log.i("current restored", String.valueOf(shared.getFloat("currentBalance", 0.0f)));
            float current = shared.getFloat("currentBalance", 0.0f);
            //Log.i("current restored var", String.valueOf(current));
            if (current != 0.0f) {
                //Log.i("inside current", "Hey - I got inside");
                currentBalanceEditText.setText(String.format(Locale.US, "%.02f", current));
            }

            //Log.d("current after", currentBalanceEditText.getText().toString());

            int totalDays = shared.getInt("totalDaysOff", 0);
            if (totalDays != 0) {
                totalDaysOff = totalDays + "";
                totalDaysOffEditText.setText(totalDaysOff);
            }

            int pastDays = shared.getInt("pastDaysOff", 0);
            if (pastDays != 0) {
                pastDaysOff = pastDays + "";
                pastDaysOffEditText.setText(pastDaysOff);
            }
        } else {    //dates must be initialized and fields cleared
            resetDefaults();
        }
    }

    /**
     * Formats given number with leading sign and $
     * @param number the number to format
     * @return the formatted number
     */
    private String formatNumber(double number) {
        if (number >= 0) {
            return "+$" + twoDecimal.format(number);
        } else {
            return "-$" + twoDecimal.format(Math.abs(number));
        }
    }

    /**
     * Manually update results
     * Separate method needed so main method
     * doesn't need the useless view
     * @param v useless
     */
    public void updateResultsManual(View v) {
        isManuallyCalculating = true;
        updateResults();
    }

    /**
     * Updates the results text
     */
    private void updateResults() {
        calculateDateDiff();

        totalInitial = getPlanValue();
        if (!rollOver.equals("")) {
            totalInitial += Float.parseFloat(rollOver);
        }

        //only update total text if its showing, doesn't show with custom
        if (!selectedMealPlan.equals(getString(R.string.mealOptionCustom))) {
            try {
                ((TextView) findViewById(R.id.totalInitialText)).setText(twoDecimal.format(totalInitial));
            } catch (NullPointerException e) {
                Log.e("set total initial error", e.getMessage());
            }
        }

        if (currentBalanceIsEntered()) {
            summaryCard.setVisibility(View.VISIBLE);
            tableCard.setVisibility(View.VISIBLE);

            double averageDaily, averageWeekly;
            if (weekDiff > 1 || (weekDiff == 1 && dayDiff > 1)) {
                averageDaily = totalInitial / ((weekDiff * 7) + dayDiff);
                averageWeekly = averageDaily * 7;
            } else {    //1 week or less
                averageWeekly = totalInitial;
                averageDaily = totalInitial / dayDiff;
            }

            double curBalance = Double.parseDouble(currentBalance);

            // current balance - amount that should be left initially
            double diff = curBalance - (averageWeekly * currentWeekDiff + averageDaily * currentDayDiff);
            tvs[0].setText(formatNumber(diff));

            double currentWeekly, currentDaily;
            if (currentWeekDiff > 1 || (currentWeekDiff == 1 && currentDayDiff > 1)) {
                currentDaily = curBalance / ((currentWeekDiff * 7) + currentDayDiff);
                currentWeekly = currentDaily * 7;
            } else {     //1 week or less
                currentWeekly = curBalance;
                currentDaily = curBalance / ((currentWeekDiff * 7) + currentDayDiff);
            }

            //set average calculations
            tvs[1].setText(formatNumber(averageDaily));
            tvs[2].setText(formatNumber(averageWeekly));

            //set current calculations
            tvs[3].setText(formatNumber(currentDaily));
            tvs[4].setText(formatNumber(currentWeekly));

            //set difference calculations
            tvs[5].setText(formatNumber(currentDaily - averageDaily));
            tvs[6].setText(formatNumber(currentWeekly - averageWeekly));

        } else { //if it can't be displayed, make sure its hidden
            showSnackbar(getResources().getString(R.string.remainingBalanceNotEntered), false);
            hideResults();
        }
    }

    /**
     * If the results cannot be updated, then nothing should be displayed
     * Makes all TextViews invisible
     */
    private void hideResults() {
        summaryCard.setVisibility(View.GONE);
        tableCard.setVisibility(View.GONE);
    }

    /**
     * Resets all saved values
     * Hides results
     */
    private void resetDefaults() {
        startMonth = defaultStartMonth;
        startDay = defaultStartDay;
        startYear = defaultStartYear;

        endMonth = defaultEndMonth;
        endDay = defaultEndDay;
        endYear = defaultEndYear;

        clearFields();
    }

    /**
     * Clears all user input fields
     * Note - only clears fields, not any saved data
     */
    private void clearFields() {
        rolloverEditText.setText("");
        currentBalanceEditText.setText("");
        totalDaysOffEditText.setText("");
        pastDaysOffEditText.setText("");

        //this will be called after restoring default dates
        startDateText.setText(startMonth + "/" + startDay + "/" + startYear);
        endDateText.setText(endMonth + "/" + endDay + "/" + endYear);

        totalDaysOff = getResources().getString(R.string.daysOff);
        totalDaysOffEditText.setText(totalDaysOff);
    }

    /**
     * Sets the start date
     */
    public void setStartDate(View v) {
        dateBeingSet = "start";
        Intent popUp = new Intent(this, CalendarDialog.class);
        popUp.putExtra("month", startMonth);
        popUp.putExtra("day", startDay);
        popUp.putExtra("year", startYear);
        startActivityForResult(popUp, 1);
    }

    /**
     * Sets the end date
     */
    public void setEndDate(View v) {
        dateBeingSet = "end";
        Intent popUp = new Intent(this, CalendarDialog.class);
        popUp.putExtra("month", endMonth);
        popUp.putExtra("day", endDay);
        popUp.putExtra("year", endYear);
        startActivityForResult(popUp, 1);
    }

    /**
     * When the pop up returns a date
     * Months are reported as 1 off so 1 added
     *
     * @param requestCode - useless
     * @param resultCode - useless
     * @param data - the Intent with the date
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //flag to prevent the default values from being applied when resuming from date picker
        isEnteringDate = true;

        if (resultCode != RESULT_CANCELED) {    //only gets info if they didn't press back
            if (dateBeingSet.equals("start")) {
                startYear = data.getIntExtra("year", defaultStartYear);
                startMonth = data.getIntExtra("month", defaultStartMonth) + 1;
                startDay = data.getIntExtra("day", defaultStartDay);
                startDateText.setText(startMonth + "/" + startDay + "/" + startYear);
                updateResults();
            } else {
                endYear = data.getIntExtra("year", defaultEndYear);
                endMonth = data.getIntExtra("month", defaultEndMonth) + 1;
                endDay = data.getIntExtra("day", defaultEndDay);
                endDateText.setText(endMonth + "/" + endDay + "/" + endYear);
                updateResults();
            }
        }
    }

    /**
     * Calculates the difference in the two dates
     */
    public void calculateDateDiff() {

        DateTimeZone Eastern = DateTimeZone.forID("America/New_York");
        DateTime start = new DateTime(startYear, startMonth, startDay, 0, 0, 0, Eastern);
        DateTime end = new DateTime(endYear, endMonth, endDay, 0, 0, 0, Eastern);
        dayDiff = Days.daysBetween(start.toLocalDate(), end.toLocalDate()).getDays();
        ++dayDiff;  //increment dayDiff by 1 so the end date is included

        int totalDaysOffNumber = 0;
        if (!totalDaysOff.equals("")) {
            totalDaysOffNumber = Integer.parseInt(totalDaysOff);
            dayDiff -= totalDaysOffNumber;
        }

        /**
         * if end date is not at least 1 day after the start after removing days off
         * change the end date to be 1 month after start + number of months in days off
         */
        if (dayDiff < 1) {
            endYear = startYear;
            endMonth = startMonth + 1 + (totalDaysOffNumber / 29);
            while (endMonth > 12) {
                endMonth -= 12;
                ++endYear;
            }

            endDay = startDay;
            endDateText.setText(endMonth + "/" + endDay + "/" + endYear);

            try {
                showSnackbar(getResources().getString(R.string.endDateBeforeStart), true);
            } catch (NullPointerException e) {
                Log.e("end date before start", e.getMessage());
            }

            updateResults();
            return;
        }

        weekDiff = dayDiff / 7;
        dayDiff -= weekDiff * 7;

        LocalDate localDate = new LocalDate();
        String currentDate = localDate.toString();
        String currentYear = currentDate.substring(0, 4);
        String currentMonth = currentDate.substring(5, 7);
        String currentDay = currentDate.substring(8, 10);

        int year = defaultStartYear;
        int month = defaultStartMonth;
        int day = defaultStartDay;
        if (!currentYear.equals("")) {
            year = Integer.parseInt(currentYear);
        }
        if (!currentMonth.equals("")) {
            month = Integer.parseInt(currentMonth);
        }
        if (!currentDay.equals("")) {
            day = Integer.parseInt(currentDay);
        }

        boolean currentDateIsInRange;
        //check year
        if (year <= endYear && year >= startYear) {
            //check month
            if (month <= endMonth && month >= startMonth) {
                //if month not equal, no need to check day (default)
                currentDateIsInRange = true;

                //if a month is equal, check day
                if (month == endMonth) {
                    if (day > endDay) {
                        currentDateIsInRange = false;
                    }
                } else if (month == startMonth) {
                    if (day < startDay) {
                        currentDateIsInRange = false;
                    }
                }
            } else {
                currentDateIsInRange = false;
            }
        } else {
            currentDateIsInRange = false;
        }

        //calculate current values
        currentDayDiff = Days.daysBetween(localDate, end.toLocalDate()).getDays();

        //subtract the total days off like before but add back any days that have passed
        if (!totalDaysOff.equals("")) {
            currentDayDiff -= Integer.parseInt(totalDaysOff);
        }
        if (!pastDaysOff.equals("")) {
            currentDayDiff += Integer.parseInt(pastDaysOff);
        }

        currentWeekDiff = currentDayDiff / 7;
        currentDayDiff -= currentWeekDiff * 7;

        //if outside the date range but current is entered
        if (!currentDateIsInRange && currentBalanceIsEntered()) {
            try {
                showSnackbar(getResources().getString(R.string.dateOutOfRangeMessage), false);
            } catch (NullPointerException e) {
                Log.e("date out of range error", e.getMessage());
            }
        }
    }

    /**
     * Checks if the current balance is entered
     * @return true if the current balance is entered
     */
    boolean currentBalanceIsEntered() {
        //Log.i("balance is entered", String.valueOf(!currentBalance.equals("") && currentBalance.charAt(0) != '.'));
        return !currentBalance.equals("") && currentBalance.charAt(0) != '.';
    }

    /**
     * Checks if the total days off is entered
     * @return true if the total days off is entered
     */
    private boolean totalDaysOffIsEntered() {
        return !totalDaysOff.equals("");
    }

    /**
     * Shows a snackbar with given message
     * Colors the text and button for readability
     */
    private void showSnackbar(String textToShow, boolean overrideSuppress) throws NullPointerException {
        //only show error messages on boot and manual calculate
        //auto-calculate does not show error messages
        if (isManuallyCalculating || overrideSuppress) {
            final Snackbar snack = Snackbar.make(findViewById(R.id.display), textToShow, Snackbar.LENGTH_LONG);

            //color dismiss button to fit theme (default is greenish)
            snack.setAction(getString(R.string.dismiss), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    snack.dismiss();
                }
            });
            snack.setActionTextColor(ContextCompat.getColor(this, R.color.orangePrimary));

            //set text color to white
            View view = snack.getView();
            TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
            tv.setTextColor(Color.WHITE);

            //set callbacks to know if snackbar is showing
            snack.setCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                    super.onDismissed(snackbar, event);
                    snackbarIsShowing = false;
                }

                @Override
                public void onShown(Snackbar snackbar) {
                    snackbarIsShowing = true;
                }
            });

            //only show if none is currently showing
            if (!snackbarIsShowing) {
                snack.show();
            }

            isManuallyCalculating = false;
        }
    }

    /**
     * Switches between total initial and custom dining based on spinner
     * Switches when custom selected or last was custom and another selected
     */
    public void switchTotalCustom() {
        if (selectedMealPlan.equals(getString(R.string.mealOptionCustom)) ||
                lastPlanSelected.equals(getString(R.string.mealOptionCustom)) &&
                !selectedMealPlan.equals(getString(R.string.mealOptionCustom))) {

            ((ViewSwitcher) findViewById(R.id.totalCustomSwitcher)).showNext();
        }
    }
}