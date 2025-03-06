package net.i2p.android.router.addressbook;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
//import android.support.v4.app.DialogFragment;
import androidx.fragment.app.DialogFragment;
//import android.support.v7.app.AlertDialog;
import androidx.appcompat.app.AlertDialog;

import net.i2p.android.wizard.model.AbstractWizardModel;
import net.i2p.android.wizard.ui.AbstractWizardActivity;

public class AddressbookAddWizardActivity extends AbstractWizardActivity {
    @Override
    protected AbstractWizardModel onCreateModel() {
        return new AddressbookAddWizardModel(this);
    }

    @Override
    protected DialogFragment onGetFinishWizardDialog() {
        return FinishWizardDialogFragment.newInstance();
    }

    public void onFinishWizard() {
        Intent result = new Intent();
        result.putExtra(AddressbookContainer.ADD_WIZARD_DATA, mWizardModel.save());
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    public static class FinishWizardDialogFragment extends DialogFragment {
        AddressbookAddWizardActivity mListener;

        public static DialogFragment newInstance() {
            return new FinishWizardDialogFragment();
        }

        public void onAttach(Context context) {
            super.onAttach(context);
            // Verify that the host fragment implements the callback interface
            try {
                // Instantiate the AddressbookAddWizardActivity so we can send events to the host
                mListener = (AddressbookAddWizardActivity) context;
            } catch (ClassCastException e) {
                // The fragment doesn't implement the interface, throw exception
                throw new ClassCastException(context.toString()
                        + " must be AddressbookAddWizardActivity");
            }
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage("Add to private addressbook?")
                    .setPositiveButton("Add",
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    mListener.onFinishWizard();
                                }
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        }
    }
}
