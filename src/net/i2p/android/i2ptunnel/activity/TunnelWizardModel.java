package net.i2p.android.i2ptunnel.activity;

import android.content.Context;
import net.i2p.android.wizard.model.AbstractWizardModel;
import net.i2p.android.wizard.model.BranchPage;
import net.i2p.android.wizard.model.MultipleFixedChoicePage;
import net.i2p.android.wizard.model.PageList;
import net.i2p.android.wizard.model.SingleFixedChoicePage;
import net.i2p.android.wizard.model.TextFieldPage;

public class TunnelWizardModel extends AbstractWizardModel {
    public TunnelWizardModel(Context context) {
        super(context);
    }

    @Override
    protected PageList onNewRootPageList() {
        return new PageList(
            new BranchPage(this, "Client or Server")
                .addBranch("Client tunnel",
                    new SingleFixedChoicePage(this, "Tunnel type")
                        .setChoices("Standard", "HTTP", "IRC", "SOCKS 4/4a/5", "SOCKS IRC", "CONNECT", "Streamr")
                        .setRequired(true))
                .addBranch("Server tunnel",
                    new SingleFixedChoicePage(this, "Tunnel type")
                        .setChoices("Standard", "HTTP", "HTTP bidir", "IRC", "Streamr")
                        .setRequired(true))
                .setRequired(true),
            new TextFieldPage(this, "Name")
                .setDescription("The name of the tunnel, for identification in the tunnel list.")
                .setRequired(true),
            new TextFieldPage(this, "Description")
                .setDescription("A description of the tunnel. This is optional and purely informative.")
            );
    }

}
