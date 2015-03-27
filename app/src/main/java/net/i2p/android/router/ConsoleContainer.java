package net.i2p.android.router;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import net.i2p.android.help.HelpActivity;
import net.i2p.android.router.dialog.AboutDialog;
import net.i2p.android.router.dialog.TextResourceDialog;
import net.i2p.android.router.service.State;

public class ConsoleContainer extends Fragment {
    MainFragment mMainFragment = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.container_console, container, false);
        // Start with the home view
        if (savedInstanceState == null && getChildFragmentManager().findFragmentById(R.id.main_fragment) == null) {
            mMainFragment = new MainFragment();
            mMainFragment.setArguments(getActivity().getIntent().getExtras());
            getChildFragmentManager().beginTransaction()
                    .add(R.id.main_fragment, mMainFragment).commit();
        }
        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_main_actions, menu);
        inflater.inflate(R.menu.activity_base_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_settings:
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
            return true;

        case R.id.menu_about:
            AboutDialog dialog = new AboutDialog();
            dialog.show(getFragmentManager(), "about");
            return true;

        case R.id.menu_help:
            Intent hi = new Intent(getActivity(), HelpActivity.class);
            startActivity(hi);
            return true;

        case R.id.menu_help_release_notes:
            TextResourceDialog rDdialog = new TextResourceDialog();
            Bundle args = new Bundle();
            args.putString(TextResourceDialog.TEXT_DIALOG_TITLE,
                    getResources().getString(R.string.label_release_notes));
            args.putInt(TextResourceDialog.TEXT_RESOURCE_ID, R.raw.releasenotes_txt);
            rDdialog.setArguments(args);
            rDdialog.show(getFragmentManager(), "release_notes");
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void updateState(State state) {
        MainFragment f = (MainFragment) getChildFragmentManager().findFragmentById(R.id.main_fragment);
        if (f != null)
            f.updateState(state);
    }
}
