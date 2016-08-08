package com.alderferstudios.ritdiningplanner;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * About section
 *
 * @author Ben Alderfer
 *         Alderfer Studios
 */
public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //set the version text to the current version
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            ((TextView) findViewById(R.id.version)).setText(getString(R.string.version) + " " + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //make links clickable
        try {
            ((TextView) findViewById(R.id.opensource)).setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) findViewById(R.id.translate)).setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) findViewById(R.id.contact)).setMovementMethod(LinkMovementMethod.getInstance());
        } catch (NullPointerException e) {
            Log.e("Clickable link error", e.getMessage());
        }
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
}
