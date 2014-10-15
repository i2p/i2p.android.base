package net.i2p.android.help;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import net.i2p.android.router.LicenseActivity;
import net.i2p.android.router.R;
import net.i2p.android.router.dialog.TextResourceDialog;

import org.sufficientlysecure.htmltextview.HtmlTextView;

public class HelpActivity extends ActionBarActivity implements
        BrowserConfigFragment.OnBrowserSelectedListener {
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

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.help_categories, android.R.layout.simple_spinner_dropdown_item);

        ActionBar.OnNavigationListener navigationListener = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int i, long l) {
                showCategory(i);
                return true;
            }
        };

        actionBar.setListNavigationCallbacks(spinnerAdapter, navigationListener);

        if (savedInstanceState == null) {
            int category = getIntent().getIntExtra(CATEGORY, CAT_MAIN);
            // TODO remove when addressbook and I2PTunnel help added
            if (category > CAT_CONFIGURE_BROWSER)
                category = CAT_MAIN;
            actionBar.setSelectedNavigationItem(category);
            showCategory(category);
        }

        if (findViewById(R.id.detail_fragment) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
    }

    private void showCategory(int category) {
        Fragment f;
        switch (category) {
            case CAT_CONFIGURE_BROWSER:
                //f = new BrowserConfigFragment();
                f = HelpHtmlFragment.newInstance(R.raw.help_configure_browser);
                break;

            case CAT_ADDRESSBOOK:
                f = HelpHtmlFragment.newInstance(R.raw.help_addressbook);
                break;

            case CAT_I2PTUNNEL:
                f = HelpHtmlFragment.newInstance(R.raw.help_i2ptunnel);
                break;

            case CAT_MAIN:
            default:
                f = HelpHtmlFragment.newInstance(R.raw.help_main);
                break;
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.main_fragment, f);
        if (mTwoPane)
            ft.remove(getSupportFragmentManager().findFragmentById(R.id.detail_fragment));
        ft.commit();
    }

    public static class HelpHtmlFragment extends Fragment {
        public static final String ARG_HTML_FILE = "htmlFile";

        static HelpHtmlFragment newInstance(int htmlFile) {
            HelpHtmlFragment f = new HelpHtmlFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_HTML_FILE, htmlFile);
            f.setArguments(args);
            return f;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            ScrollView scroller = new ScrollView(getActivity());
            HtmlTextView text = new HtmlTextView(getActivity());
            scroller.addView(text);
            text.setHtmlFromRawResource(getActivity(), getArguments().getInt(ARG_HTML_FILE), true);
            return scroller;
        }
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

    // BrowserConfigFragment.OnBrowserSelectedListener

    @Override
    public void onBrowserSelected(String browserPackage) {
        // TODO
    }
}
