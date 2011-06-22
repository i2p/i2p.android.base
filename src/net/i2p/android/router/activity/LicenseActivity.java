package net.i2p.android.router.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import net.i2p.android.router.R;

public class LicenseActivity extends I2PActivityBase {

    private static final int[] buttons = {
        R.id.license_main, R.id.license_bf, R.id.license_bsd, R.id.license_elg,
        R.id.license_gplv2, R.id.license_lgplv21, R.id.license_gplv3, R.id.license_lgplv3,
        R.id.license_cert, R.id.license_sha256, R.id.license_sntp};

    private static final int[] files = {
        R.raw.licenses_txt, R.raw.license_blockfile_txt, R.raw.license_bsd_txt, R.raw.license_elgamaldsa_txt,
        R.raw.license_gplv2_txt, R.raw.license_lgplv2_1_txt, R.raw.license_gplv3_txt, R.raw.license_lgplv3_txt,
        R.raw.license_installcert_txt, R.raw.license_sha256_txt, R.raw.license_sntp_txt};

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.license);

        for (int i = 0; i < buttons.length; i++) {
            Button b = (Button) findViewById(buttons[i]);
            b.setOnClickListener(new LicenseClick(files[i]));
        }
    }

    private class LicenseClick implements View.OnClickListener {
        private final int resource;

        public LicenseClick(int r) {
            resource = r;
        }

        public void onClick(View view) {
            Intent intent = new Intent(view.getContext(), TextResourceActivity.class);
            intent.putExtra(TextResourceActivity.TEXT_RESOURCE_ID, resource);
            startActivity(intent);
        }
    }
}
