package net.i2p.android.router.addressbook;

import android.content.Context;
import android.content.res.Resources;
import net.i2p.android.router.R;
import net.i2p.android.wizard.model.AbstractWizardModel;
import net.i2p.android.wizard.model.I2PB64DestinationPage;
import net.i2p.android.wizard.model.PageList;
import net.i2p.android.wizard.model.SingleTextFieldPage;

public class AddressbookAddWizardModel extends AbstractWizardModel {
    public AddressbookAddWizardModel(Context context) {
        super(context);
    }

    @Override
    protected PageList onNewRootPageList() {
        Resources res = mContext.getResources();

        return new PageList(
            new SingleTextFieldPage(this, res.getString(R.string.addressbook_add_wizard_k_name))
                .setDescription(res.getString(R.string.addressbook_add_wizard_desc_name))
                .setRequired(true),

            new I2PB64DestinationPage(this, res.getString(R.string.addressbook_add_wizard_k_destination))
                .setDescription(res.getString(R.string.addressbook_add_wizard_desc_destination))
                .setRequired(true)
            );
    }

}
