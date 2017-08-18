package net.i2p.android.router;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import net.i2p.android.ext.floatingactionbutton.FloatingActionsMenu;
import net.i2p.android.router.dialog.AboutDialog;
import net.i2p.android.router.dialog.TextResourceDialog;
import net.i2p.android.router.log.LogActivity;
import net.i2p.android.router.netdb.NetDbActivity;
import net.i2p.android.router.stats.PeersActivity;
import net.i2p.android.router.stats.RateGraphActivity;
import net.i2p.android.router.util.Util;

public class ConsoleContainer extends Fragment {
    MainFragment mMainFragment = null;
    FloatingActionsMenu mConsoleMenu;

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

        mConsoleMenu = (FloatingActionsMenu) v.findViewById(R.id.console_action_menu);
        mConsoleMenu.findViewById(R.id.action_news).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent news = new Intent(getActivity(), NewsActivity.class);
                startActivity(news);
            }
        });
        mConsoleMenu.findViewById(R.id.action_logs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent log = new Intent(getActivity(), LogActivity.class);
                startActivity(log);
            }
        });
        mConsoleMenu.findViewById(R.id.action_graphs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent graphs = new Intent(getActivity(), RateGraphActivity.class);
                startActivity(graphs);
            }
        });
//        mConsoleMenu.findViewById(R.id.action_peers).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent peers = new Intent(getActivity(), PeersActivity.class);
//                startActivity(peers);
//            }
//        });
        mConsoleMenu.findViewById(R.id.action_netdb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent netdb = new Intent(getActivity(), NetDbActivity.class);
                startActivity(netdb);
            }
        });
        setMenuVisibility();

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_main_actions, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        setMenuVisibility();
    }

    private void setMenuVisibility() {
        boolean routerRunning = Util.getRouterContext() != null;
        mConsoleMenu.findViewById(R.id.action_logs).setVisibility(routerRunning ? View.VISIBLE : View.GONE);
        mConsoleMenu.findViewById(R.id.action_graphs).setVisibility(routerRunning ? View.VISIBLE : View.GONE);

        if (getActivity() != null) {
            boolean advanced = PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getBoolean("i2pandroid.main.showStats", false);
//            mConsoleMenu.findViewById(R.id.action_peers).setVisibility(
//                    advanced && routerRunning ? View.VISIBLE : View.GONE);
            mConsoleMenu.findViewById(R.id.action_netdb).setVisibility(
                    advanced && routerRunning ? View.VISIBLE : View.GONE);
        }
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
}
