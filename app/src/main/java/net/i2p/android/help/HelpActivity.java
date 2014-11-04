package net.i2p.android.help;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;

import net.i2p.android.router.LicenseActivity;
import net.i2p.android.router.R;
import net.i2p.android.router.dialog.TextResourceDialog;

import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.lang.reflect.Field;

public class HelpActivity extends ActionBarActivity implements
        BrowserAdapter.OnBrowserSelectedListener {
    public static final String CATEGORY = "help_category";
    public static final int CAT_MAIN = 0;
    public static final int CAT_CONFIGURE_BROWSER = 1;
    public static final int CAT_ADDRESSBOOK = 2;
    public static final int CAT_I2PTUNNEL = 3;

    private Spinner mSpinner;

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

        mSpinner = (Spinner) findViewById(R.id.main_spinner);

        mSpinner.setAdapter(ArrayAdapter.createFromResource(this,
                R.array.help_categories, android.R.layout.simple_spinner_dropdown_item));

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                showCategory(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        if (findViewById(R.id.detail_fragment) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        int category;
        if (savedInstanceState != null) {
            int selected = savedInstanceState.getInt(CATEGORY);
            mSpinner.setSelection(selected);
        } else {
            category = getIntent().getIntExtra(CATEGORY, CAT_MAIN);
            // TODO remove when addressbook and I2PTunnel help added
            if (category > CAT_CONFIGURE_BROWSER)
                category = CAT_MAIN;
            mSpinner.setSelection(category);
            showCategory(category);
        }
    }

    private void showCategory(int category) {
        Fragment f;
        switch (category) {
            case CAT_CONFIGURE_BROWSER:
                f = new BrowserListFragment();
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
            int padH = getResources().getDimensionPixelOffset(R.dimen.activity_horizontal_margin);
            int padV = getResources().getDimensionPixelOffset(R.dimen.activity_vertical_margin);
            text.setPadding(padH, padV, padH, padV);
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CATEGORY, mSpinner.getSelectedItemPosition());
    }

    // BrowserAdapter.OnBrowserSelected

    @Override
    public void onBrowserSelected(Browser browser) {
        int file;
        if (browser.isKnown) {
            if (browser.isSupported) {
                // Check for embedded browser
                if (browser.packageName.startsWith("net.i2p.android"))
                    file = R.raw.help_embedded_browser;
                else {
                    // Load the configuration guide for this browser
                    try {
                        String name = "help_" + browser.packageName.replace('.', '_');
                        Class res = R.raw.class;
                        Field field = res.getField(name);
                        file = field.getInt(null);
                    } catch (Exception e) {
                        file = R.raw.help_unknown_browser;
                    }
                }
            } else
                file = R.raw.help_unsupported_browser;
        } else
            file = R.raw.help_unknown_browser;
        HelpHtmlFragment configFrag = HelpHtmlFragment.newInstance(file);
        if (mTwoPane) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_fragment, configFrag)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment, configFrag)
                    .addToBackStack("config" + browser.packageName)
                    .commit();
        }
    }
}
