package net.i2p.android.router;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import net.i2p.android.router.dialog.AboutDialog;
import net.i2p.android.router.dialog.TextResourceDialog;
import net.i2p.android.router.log.LogActivity;
import net.i2p.android.router.netdb.NetDbActivity;
import net.i2p.android.router.service.State;
import net.i2p.android.router.stats.PeersActivity;
import net.i2p.android.router.stats.RateGraphActivity;

public class ConsoleContainer extends Fragment {
    MainFragment mMainFragment = null;
    Toolbar mSubToolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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

        mSubToolbar = (Toolbar) v.findViewById(R.id.sub_toolbar);
        mSubToolbar.inflateMenu(R.menu.container_console_sub_actions);
        setAdvancedVisibility();
        mSubToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_news:
                        Intent news = new Intent(getActivity(), NewsActivity.class);
                        startActivity(news);
                        return true;
                    case R.id.action_logs:
                        Intent log = new Intent(getActivity(), LogActivity.class);
                        startActivity(log);
                        return true;
                    case R.id.action_graphs:
                        Intent graphs = new Intent(getActivity(), RateGraphActivity.class);
                        startActivity(graphs);
                        return true;
                    case R.id.action_peers:
                        Intent peers = new Intent(getActivity(), PeersActivity.class);
                        startActivity(peers);
                        return true;
                    case R.id.action_netdb:
                        Intent netdb = new Intent(getActivity(), NetDbActivity.class);
                        startActivity(netdb);
                        return true;
                    default:
                        return false;
                }
            }
        });

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_main_actions, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        setAdvancedVisibility();
    }

    private void setAdvancedVisibility() {
        boolean advanced = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(
                "i2pandroid.main.showStats", false);

        mSubToolbar.getMenu().findItem(R.id.action_graphs).setVisible(advanced);
        mSubToolbar.getMenu().findItem(R.id.action_peers).setVisible(advanced);
        mSubToolbar.getMenu().findItem(R.id.action_netdb).setVisible(advanced);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
                AboutDialog dialog = new AboutDialog();
                dialog.show(getFragmentManager(), "about");
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
