package net.i2p.android.help;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import net.i2p.android.router.LicenseActivity;
import net.i2p.android.router.R;
import net.i2p.android.router.dialog.TextResourceDialog;

public class HelpActivity extends ActionBarActivity implements
        HelpListFragment.OnEntrySelectedListener {
    public static final String CATEGORY = "help_category";
    public static final int CAT_MAIN = 0;
    public static final int CAT_CONFIGURE_BROWSER = 1;
    public static final int CAT_ADDRESSBOOK = 2;
    public static final int CAT_I2PTUNNEL = 3;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        // Set the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        if (findViewById(R.id.detail_fragment) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_fragment, new HelpListFragment())
                    .commit();
        }

        int category = getIntent().getIntExtra(CATEGORY, -1);
        if (category >= 0)
            showCategory(category);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_help_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_help_licenses:
                Intent lic = new Intent(HelpActivity.this, LicenseActivity.class);
                startActivity(lic);
                return true;
            case R.id.menu_help_release_notes:
                TextResourceDialog dialog = new TextResourceDialog();
                Bundle args = new Bundle();
                args.putString(TextResourceDialog.TEXT_DIALOG_TITLE,
                        getResources().getString(R.string.label_release_notes));
                args.putInt(TextResourceDialog.TEXT_RESOURCE_ID, R.raw.releasenotes_txt);
                dialog.setArguments(args);
                dialog.show(getSupportFragmentManager(), "release_notes");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // HelpListFragment.OnEntrySelectedListener

    @Override
    public void onEntrySelected(int entry) {
        if (entry == CAT_CONFIGURE_BROWSER) {
            Intent i = new Intent(this, BrowserConfigActivity.class);
            startActivity(i);
        } else
            showCategory(entry);
    }

    private void showCategory(int category) {
        int file;
        switch (category) {
            case CAT_ADDRESSBOOK:
                file = R.raw.help_addressbook;
                break;

            case CAT_I2PTUNNEL:
                file = R.raw.help_i2ptunnel;
                break;

            case CAT_MAIN:
            default:
                file = R.raw.help_main;
                break;
        }
        HelpHtmlFragment f = HelpHtmlFragment.newInstance(file);
        if (mTwoPane) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_fragment, f).commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment, f)
                    .addToBackStack("help"+category)
                    .commit();
        }
    }
}
