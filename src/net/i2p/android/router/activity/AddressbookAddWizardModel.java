package net.i2p.android.router.activity;

import android.content.Context;
import android.content.res.Resources;
import net.i2p.android.wizard.model.AbstractWizardModel;
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
            new SingleTextFieldPage(this, "Name")
                .setDescription("The name")
                .setRequired(true),

            new SingleTextFieldPage(this, "Destination")
                .setDescription("The destination")
                .setRequired(true)
            );
    }

}
