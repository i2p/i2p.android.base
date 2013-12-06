package net.i2p.android.i2ptunnel;

import net.i2p.android.router.R;
import net.i2p.android.wizard.model.AbstractWizardModel;
import net.i2p.android.wizard.ui.AbstractWizardActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class TunnelWizardActivity extends AbstractWizardActivity {
    @Override
    protected AbstractWizardModel onCreateModel() {
        return new TunnelWizardModel(this);
    }

    @Override
    protected DialogFragment onGetFinishWizardDialog() {
        return new DialogFragment() {
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                return new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.i2ptunnel_wizard_submit_confirm_message)
                        .setPositiveButton(R.string.i2ptunnel_wizard_submit_confirm_button,
                                new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent result = new Intent();
                                        result.putExtra(TunnelListFragment.TUNNEL_WIZARD_DATA, mWizardModel.save());
                                        setResult(Activity.RESULT_OK, result);
                                        dialog.dismiss();
                                        finish();
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
            }
        };
    }
}
