package net.i2p.android.router;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.i2p.android.router.dialog.TextResourceDialog;

public class LicenseFragment extends ListFragment {

    private static final String[] names = {
        "Android Application License", "Apache 2.0",
        "Router License Overview", "Blockfile", "Crypto Filters", "ElGamal / DSA",
        "GPLv2", "LGPLv2.1", "GPLv3", "LGPLv3", "FatCowIcons",
        "Ministreaming",
        "InstallCert", "SHA-256", "SNTP", "Addressbook"};

    private static final int[] files = {
        R.raw.license_app_txt, R.raw.license_apache20_txt,
        R.raw.licenses_txt, R.raw.license_blockfile_txt, R.raw.license_bsd_txt, R.raw.license_elgamaldsa_txt,
        R.raw.license_gplv2_txt, R.raw.license_lgplv2_1_txt, R.raw.license_gplv3_txt, R.raw.license_lgplv3_txt,
        R.raw.license_fatcowicons_txt, R.raw.license_bsd_txt,
        R.raw.license_installcert_txt, R.raw.license_sha256_txt, R.raw.license_sntp_txt, R.raw.license_addressbook_txt};

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setListAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, names));
    }

    @Override
    public void onListItemClick(ListView parent, View view, int pos, long id) {
        TextResourceDialog dialog = new TextResourceDialog();
        Bundle args = new Bundle();
        args.putString(TextResourceDialog.TEXT_DIALOG_TITLE, names[pos]);
        args.putInt(TextResourceDialog.TEXT_RESOURCE_ID, files[pos]);
        dialog.setArguments(args);
        dialog.show(getActivity().getSupportFragmentManager(), "license");
    }
}
