package net.i2p.android.i2ptunnel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import net.i2p.android.router.R;
import net.i2p.android.wizard.model.AbstractWizardModel;
import net.i2p.android.wizard.ui.AbstractWizardActivity;

public class TunnelWizardActivity extends AbstractWizardActivity {
    @Override
    protected AbstractWizardModel onCreateModel() {
        return new TunnelWizardModel(this);
    }

    @Override
    protected DialogFragment onGetFinishWizardDialog() {
        return new DialogFragment() {
            @NonNull
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                return new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.i2ptunnel_wizard_submit_confirm_message)
                        .setPositiveButton(R.string.i2ptunnel_wizard_submit_confirm_button,
                                new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent result = new Intent();
                                        result.putExtra(TunnelsContainer.TUNNEL_WIZARD_DATA, mWizardModel.save());
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
