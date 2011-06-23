package net.i2p.android.router.activity;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.i2p.android.router.R;

public class LicenseActivity extends ListActivity {

    private static final String[] names = {
        "License Overview", "Blockfile", "BSD", "ElGamal / DSA",
        "GPLv2", "LGPLv2.1", "GPLv3", "LGPLv3",
        "InstallCert", "SHA-256", "SNTP", "Addressbook"};

    private static final int[] files = {
        R.raw.licenses_txt, R.raw.license_blockfile_txt, R.raw.license_bsd_txt, R.raw.license_elgamaldsa_txt,
        R.raw.license_gplv2_txt, R.raw.license_lgplv2_1_txt, R.raw.license_gplv3_txt, R.raw.license_lgplv3_txt,
        R.raw.license_installcert_txt, R.raw.license_sha256_txt, R.raw.license_sntp_txt, R.raw.license_addressbook_txt};

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names));
        ListView lv = getListView();

        // set the callback
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int pos, long id) {
                Intent intent = new Intent(view.getContext(), TextResourceActivity.class);
                intent.putExtra(TextResourceActivity.TEXT_RESOURCE_ID, files[pos]);
                startActivity(intent);
            }
        });
    }
}
