package net.i2p.android.router;

import net.i2p.android.router.R;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

public class HelpActivity extends I2PActivityBase {
    public static final String REFERRER = "help_referrer";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDrawerToggle.setDrawerIndicatorEnabled(false);
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
