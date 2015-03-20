package net.i2p.android.i2ptunnel;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import net.i2p.android.router.R;

public class EditTunnelActivity extends ActionBarActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_onepane);

        // Set the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            int tunnelId = getIntent().getIntExtra(TunnelDetailFragment.TUNNEL_ID, 0);
            EditTunnelFragment editFrag = EditTunnelFragment.newInstance(tunnelId);
            getSupportFragmentManager().beginTransaction()
                .add(R.id.main_fragment, editFrag).commit();
        }
    }
}
