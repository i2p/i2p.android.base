package net.i2p.android.help;

import net.i2p.android.router.LicenseActivity;
import net.i2p.android.router.R;
import net.i2p.android.router.dialog.TextResourceDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

public class HelpActivity extends ActionBarActivity {
    public static final String CATEGORY = "help_category";
    public static final int CAT_MAIN = 0;
    public static final int CAT_CONFIGURE_BROWSER = 1;
    public static final int CAT_ADDRESSBOOK = 2;
    public static final int CAT_I2PTUNNEL = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_help);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.help_categories, android.R.layout.simple_spinner_dropdown_item);

        ActionBar.OnNavigationListener navigationListener = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int i, long l) {
                return false;
            }
        };

        actionBar.setListNavigationCallbacks(spinnerAdapter, navigationListener);

        int category = getIntent().getIntExtra(CATEGORY, CAT_MAIN);
        actionBar.setSelectedNavigationItem(category);

        /*if (savedInstanceState == null) {
            HelpFragment f = new HelpFragment();
            f.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_fragment, f).commit();
        }*/
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
}
