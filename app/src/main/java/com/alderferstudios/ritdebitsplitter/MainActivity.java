package com.alderferstudios.ritdebitsplitter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.text.DecimalFormat;

/**
 * Main window of the Budget Splitter
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
    private int startYear, startMonth, startDay;
    /**
     * The ending date
     */
    private int endYear, endMonth, endDay;
    /**
     * week and day difference between dates
     */
    private int weekDiff, dayDiff, currentWeekDiff, currentDayDiff;
    /**
     * Results cards
     */
    private CardView initialCard, currentCard;
    /**
     * The results TextViews
     * <p/>
     * 0 - Initial Daily
     * 1 - Initial Weekly
     * <p/>
     * 2 - Diff
     * 3 - Current Daily
     * 4 - Current Weekly
     */
    private TextView[] tvs = new TextView[5];
    /**
     * Start and end date TextViews
     */
    private TextView startDateText, endDateText;
    /**
     * The EditText fields
     */
    private EditText initialBalanceEditText, currentBalanceEditText, totalDaysOffEditText, pastDaysOffEditText;
    /**
     * The text input in the fields
     */
    private String initialBalance = "", currentBalance = "", totalDaysOff = "", pastDaysOff = "";
    /**
     * Whether the fields have been entered
     */
    private boolean initialBalanceIsEntered, currentBalanceIsEntered, currentDateIsInRange, totalDaysOffIsEntered, pastDaysOffIsEntered;
    /**
     * Either start or end date
     */
    private String dateBeingSet;
    /**
     * Context reference for later
     */
    private Context c;

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

        c = this;

        if (!isTablet(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }

        shared = PreferenceManager.getDefaultSharedPreferences(this);
        editor = shared.edit();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initialBalanceEditText = (EditText) findViewById(R.id.initialBalanceText);
        currentBalanceEditText = (EditText) findViewById(R.id.currentBalanceText);
        startDateText = (TextView) findViewById(R.id.startDate);
        endDateText = (TextView) findViewById(R.id.endDate);
        totalDaysOffEditText = (EditText) findViewById(R.id.totalDaysOffText);
        pastDaysOffEditText = (EditText) findViewById(R.id.pastDaysOffText);

        tvs[0] = (TextView) findViewById(R.id.initDailyText);
        tvs[1] = (TextView) findViewById(R.id.initWeeklyText);

        tvs[2] = (TextView) findViewById(R.id.diffText);
        tvs[3] = (TextView) findViewById(R.id.currentDailyText);
        tvs[4] = (TextView) findViewById(R.id.currentWeeklyText);

        initialCard = (CardView) findViewById(R.id.initialCard);
        currentCard = (CardView) findViewById(R.id.currentCard);

        clearResults();
        addTextListeners();

        restoreValues();
    }

    /**
     * Check to restore values on resume also
     * since it could be changed in the settings
     */
    @Override
    protected void onResume() {
        super.onResume();
        restoreValues();
    }

    /**
     * Sets up the menu (none for now)
     *
     * @param menu - the menu to set up
     * @return true (useless)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Adds a listener to the EditTexts
     * Text is saved to reduce method calls
     * Updates the results if possible
     */
    private void addTextListeners() {
        initialBalanceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                initialBalance = initialBalanceEditText.getText().toString();
                //if it ends with a "." remove the "." before getting the number
                if (initialBalance.length() > 0 && initialBalance.charAt(0) != '.' && initialBalance.substring(initialBalance.length() - 1, initialBalance.length()).equals(".")) {
                    initialBalance = initialBalance.substring(0, initialBalance.length());
                }
                initialBalanceIsEntered = !initialBalance.equals("") && (initialBalance.length() > 1 || initialBalance.charAt(0) != '.');

                attemptUpdate();
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
                currentBalanceIsEntered = !currentBalance.equals("") && (currentBalance.length() > 1 || currentBalance.charAt(0) != '.');
                if (currentBalanceIsEntered) {
                    //if it ends with a "." remove the "." before getting the number
                    if (currentBalance.length() > 0 && currentBalance.charAt(0) != '.' && currentBalance.substring(currentBalance.length() - 1, currentBalance.length()).equals(".")) {
                        currentBalance = currentBalance.substring(0, currentBalance.length());
                    }
                    //if current balance > initial, fix that
                    if (!initialBalance.equals("") && !currentBalance.equals("") &&
                            Double.parseDouble(currentBalance) > Double.parseDouble(initialBalance)) {
                        currentBalance = initialBalance;
                        currentBalanceEditText.setText(currentBalance);
                        Toast.makeText(c, R.string.remainingGreaterThanInitial, Toast.LENGTH_LONG).show();
                    }
                }

                attemptUpdate();
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
                totalDaysOffIsEntered = true;
                attemptUpdate();
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
                    Toast.makeText(c, R.string.totalNotEntered, Toast.LENGTH_LONG).show();
                }

                // if current days off > total days off, change it to the total
                if (!totalDaysOff.equals("") && !pastDaysOff.equals("") && Integer.parseInt(pastDaysOff) > Integer.parseInt(totalDaysOff)) {
                    pastDaysOff = totalDaysOff;
                    pastDaysOffEditText.setText(totalDaysOff);
                    Toast.makeText(c, R.string.pastGreaterThanTotal, Toast.LENGTH_LONG).show();
                }

                attemptUpdate();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /**
     * Saves the entered values for later
     */
    private void saveValues() {
        //save entered values
        if (initialBalanceIsEntered && !initialBalance.equals("")) {
            editor.putFloat("initialBalance", Float.parseFloat(initialBalance));
        }
        if (currentBalanceIsEntered && !currentBalance.equals("")) {
            editor.putFloat("currentBalance", Float.parseFloat(currentBalance));
        }
        if (totalDaysOffIsEntered && !totalDaysOff.equals("")) {
            editor.putInt("totalDaysOff", Integer.parseInt(totalDaysOff));
        }
        if (pastDaysOffIsEntered && !pastDaysOff.equals("")) {
            editor.putInt("pastDaysOff", Integer.parseInt(pastDaysOff));
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
    }

    /**
     * Restores the previous values
     */
    private void restoreValues() {
        //only restore values if they want to
        if (shared.getBoolean("saveBox", false)) {
            //restore start date
            startMonth = shared.getInt("startMonth", Integer.parseInt(getString(R.string.startMonth)));
            startDay = shared.getInt("startDay", Integer.parseInt(getString(R.string.startDay)));
            startYear = shared.getInt("startYear", Integer.parseInt(getString(R.string.startYear)));

            //restore end date
            endMonth = shared.getInt("endMonth", Integer.parseInt(getString(R.string.endMonth)));
            endDay = shared.getInt("endDay", Integer.parseInt(getString(R.string.endDay)));
            endYear = shared.getInt("endYear", Integer.parseInt(getString(R.string.endYear)));

            //update date field texts
            startDateText.setText(startMonth + "/" + startDay + "/" + startYear);
            endDateText.setText(endMonth + "/" + endDay + "/" + endYear);

            //restore entered values
            float initial = shared.getFloat("initialBalance", 0.0f);
            if (initial != 0.0f) {
                initialBalance = initial + "";
                initialBalanceEditText.setText(initialBalance);
            }

            float current = shared.getFloat("currentBalance", 0.0f);
            if (current != 0.0f) {
                currentBalance = current + "";
                currentBalanceEditText.setText(currentBalance);
            }

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
            startMonth = Integer.parseInt(getResources().getString(R.string.startMonth));
            startDay = Integer.parseInt(getResources().getString(R.string.startDay));
            startYear = Integer.parseInt(getResources().getString(R.string.startYear));
            endMonth = Integer.parseInt(getResources().getString(R.string.endMonth));
            endDay = Integer.parseInt(getResources().getString(R.string.endDay));
            endYear = Integer.parseInt(getResources().getString(R.string.endYear));

            clearFields();
        }
    }

    /**
     * Checks if results can be updated
     * If possible, update
     * If not, clear results
     */
    private void attemptUpdate() {
        if (initialBalanceIsEntered) {
            updateResults();
        } else {
            clearResults();
        }
    }

    /**
     * Updates the results text
     */
    private void updateResults() {
        calculateDateDiff(findViewById(R.id.initialBalanceText));
        saveValues();

        initialCard.setVisibility(View.VISIBLE);

        DecimalFormat twoDecimal = new DecimalFormat("0.00");
        double initial = Double.parseDouble(initialBalance);

        double daily, weekly;
        if (weekDiff > 0) {
            daily = initial / ((weekDiff * 7) + dayDiff);
            weekly = daily * 7;
        } else {    //only 1 week or less
            weekly = initial;
            daily = weekly / currentDayDiff;
        }

        tvs[0].setText(twoDecimal.format(daily));
        tvs[1].setText(twoDecimal.format(weekly));

        if (currentDateIsInRange && currentBalanceIsEntered) {
            currentCard.setVisibility(View.VISIBLE);

            double curBalance = Double.parseDouble(currentBalance);

            // current balance - amount that should be left initially
            double diff = curBalance - (weekly * currentWeekDiff + daily * currentDayDiff);

            double currentWeekly, currentDaily;
            if (currentWeekDiff > 0) {
                currentDaily = curBalance / ((currentWeekDiff * 7) + currentDayDiff);
                currentWeekly = currentDaily * 7;
            } else {    //only 1 week or less
                currentWeekly = curBalance;
                currentDaily = currentWeekly / currentDayDiff;
            }

            if (diff > 0) {
                tvs[2].setText("+" + twoDecimal.format(diff));
            } else {
                tvs[2].setText(twoDecimal.format(diff));
            }

            tvs[3].setText(twoDecimal.format(currentDaily));
            tvs[4].setText(twoDecimal.format(currentWeekly));
        } else { //if it can't be displayed, make sure its hidden
            currentCard.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * If the results cannot be updated, then nothing should be displayed
     * Makes all TextViews invisible
     */
    private void clearResults() {
        initialCard.setVisibility(View.INVISIBLE);
        currentCard.setVisibility(View.INVISIBLE);
    }

    /**
     * Clears all user input fields
     * Note - only clears fields, not any saved data
     */
    private void clearFields() {
        initialBalanceEditText.setText("");
        currentBalanceEditText.setText("");
        totalDaysOffEditText.setText("");
        pastDaysOffEditText.setText("");
        initialBalanceIsEntered = false;
        currentBalanceIsEntered = false;
        totalDaysOffIsEntered = false;
        pastDaysOffIsEntered = false;

        //this will be called after restoring default dates
        startDateText.setText(startMonth + "/" + startDay + "/" + startYear);
        endDateText.setText(endMonth + "/" + endDay + "/" + endYear);
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
     * @param resultCode  - useless
     * @param data        - the Intent with the date
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_CANCELED) {    //only gets info if they didn't press back
            if (dateBeingSet.equals("start")) {
                startYear = data.getIntExtra("year", 2016);
                startMonth = data.getIntExtra("month", 1) + 1;
                startDay = data.getIntExtra("day", 25);
                startDateText.setText(startMonth + "/" + startDay + "/" + startYear);
                attemptUpdate();
            } else {
                endYear = data.getIntExtra("year", 2016);
                endMonth = data.getIntExtra("month", 1) + 1;
                endDay = data.getIntExtra("day", 25);
                endDateText.setText(endMonth + "/" + endDay + "/" + endYear);
                attemptUpdate();
            }
        }
    }

    /**
     * Calculates the difference in the two dates
     */
    public void calculateDateDiff(View v) {
        DateTimeZone Eastern = DateTimeZone.forID("America/New_York");
        DateTime start = new DateTime(startYear, startMonth, startDay, 0, 0, 0, Eastern);
        DateTime end = new DateTime(endYear, endMonth, endDay, 0, 0, 0, Eastern);
        dayDiff = Days.daysBetween(start.toLocalDate(), end.toLocalDate()).getDays();

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
            if (endMonth > 12) {
                endMonth -= 12;
                ++endYear;
            }

            endDay = startDay;
            endDateText.setText(endMonth + "/" + endDay + "/" + endYear);
            Toast.makeText(this, R.string.endDateBeforeStart, Toast.LENGTH_LONG).show();
            attemptUpdate();
            return;
        }

        weekDiff = dayDiff / 7;
        dayDiff -= weekDiff * 7;

        LocalDate localDate = new LocalDate();
        String currentDate = localDate.toString();
        String currentYear = currentDate.substring(0, 4);
        String currentMonth = currentDate.substring(5, 7);
        String currentDay = currentDate.substring(8, 10);
        int year = 2016, month = 1, day = 25;
        if (!currentYear.equals("")) {
            year = Integer.parseInt(currentYear);
        }
        if (!currentMonth.equals("")) {
            month = Integer.parseInt(currentMonth);
        }
        if (!currentDay.equals("")) {
            day = Integer.parseInt(currentDay);
        }

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

        if (currentDateIsInRange) {
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
        } else if (currentBalanceIsEntered) {
            Toast.makeText(this, getString(R.string.dateOutOfRangeMessage), Toast.LENGTH_LONG).show();
        }
    }
}