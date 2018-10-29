package com.alderferstudios.ritdebitsplitter.base;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.DatePicker;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Popup calendar
 *
 * @author Ben Alderfer
 */
public class CalendarDialog extends AppCompatActivity {

    private DatePicker calendar;

    /**
     * Creates the CalendarDialog
     *
     * @param savedInstanceState - the previous state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar_dialog);
        calendar = findViewById(R.id.calendar);
        int month = getIntent().getIntExtra("month", 3);
        int day = getIntent().getIntExtra("day", 1);
        int year = getIntent().getIntExtra("year", 2016);
        //remove 1 from month since java 7 dates start at 0 and jodatime starts at 1
        calendar.updateDate(year, month - 1, day);
    }

    /**
     * Before closing the popup, return date
     */
    private void close() {

        Intent result = new Intent();
        result.putExtra("year", calendar.getYear());
        result.putExtra("month", calendar.getMonth());
        result.putExtra("day", calendar.getDayOfMonth());
        setResult(1, result);
        finish();
        super.onStop();
    }

    /**
     * When they touch outside, save info and close
     *
     * @param event - the MotionEvent
     * @return false - useless
     */
    public boolean onTouchEvent(MotionEvent event) {
        close();
        return false;
    }
}