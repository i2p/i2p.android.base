package net.i2p.android.i2ptunnel.activity;

import android.content.Context;
import net.i2p.android.wizard.model.AbstractWizardModel;
import net.i2p.android.wizard.model.BranchPage;
import net.i2p.android.wizard.model.Conditional;
import net.i2p.android.wizard.model.PageList;
import net.i2p.android.wizard.model.SingleFixedBooleanPage;
import net.i2p.android.wizard.model.SingleFixedChoicePage;
import net.i2p.android.wizard.model.SingleTextFieldPage;

public class TunnelWizardModel extends AbstractWizardModel {
    public TunnelWizardModel(Context context) {
        super(context);
    }

    @Override
    protected PageList onNewRootPageList() {
        Conditional cTunnelType = new Conditional();
        Conditional cClientType = new Conditional();
        Conditional cServerType = new Conditional();
        return new PageList(
            new BranchPage(this, "Client or Server")
                .addBranch("Client tunnel",
                    new SingleFixedChoicePage(this, "Tunnel type")
                        .setChoices("Standard", "HTTP", "IRC", "SOCKS 4/4a/5", "SOCKS IRC", "CONNECT", "Streamr")
                        .setRequired(true)
                        .makeConditional(cClientType))
                .addBranch("Server tunnel",
                    new SingleFixedChoicePage(this, "Tunnel type")
                        .setChoices("Standard", "HTTP", "HTTP bidir", "IRC", "Streamr")
                        .setRequired(true)
                        .makeConditional(cServerType))
                .setRequired(true)
                .makeConditional(cTunnelType),
            new SingleTextFieldPage(this, "Name")
                .setDescription("The name of the tunnel, for identification in the tunnel list.")
                .setRequired(true),
            new SingleTextFieldPage(this, "Description")
                .setDescription("A description of the tunnel. This is optional and purely informative."),
            new SingleTextFieldPage(this, "Destination")
                .setDescription("Type in the I2P destination of the service that this client tunnel should connect to. This could be the full base 64 destination key, or an I2P URL from your address book.")
                .setEqualAnyCondition(cClientType, "Standard", "IRC", "Streamr"),
            new SingleTextFieldPage(this, "Outproxies")
                .setDescription("If you know of any outproxies for this type of tunnel (either HTTP or SOCKS), fill them in. Separate multiple proxies with commas.")
                .setEqualAnyCondition(cClientType, "HTTP", "CONNECT", "SOCKS 4/4a/5", "SOCKS IRC"),
            new SingleTextFieldPage(this, "Target host")
                .setDefault("127.0.0.1")
                .setDescription("This is the IP that your service is running on, this is usually on the same machine so 127.0.0.1 is autofilled.")
                .setEqualCondition(cClientType, "Streamr")
                .setEqualAnyCondition(cServerType, "Standard", "HTTP", "HTTP bidir", "IRC"),
            new SingleTextFieldPage(this, "Target port")
                .setDescription("This is the port that the service is accepting connections on.")
                .setEqualCondition(cTunnelType, "Server tunnel"),
            new SingleTextFieldPage(this, "Reachable on")
                .setDefault("127.0.0.1")
                .setDescription("This limits what computers or smartphones can access this tunnel.")
                .setEqualAnyCondition(cClientType, "Standard", "HTTP", "IRC", "SOCKS 4/4a/5", "SOCKS IRC", "CONNECT")
                .setEqualAnyCondition(cServerType, "HTTP bidir", "Streamr"),
            new SingleTextFieldPage(this, "Binding port")
                .setDescription("This is the port that the client tunnel will be accessed from locally. This is also the client port for the HTTP bidir server tunnel.")
                .setEqualCondition(cTunnelType, "Client tunnel")
                .setEqualCondition(cServerType, "HTTP bidir"),
            new SingleFixedBooleanPage(this, "Auto start")
                .setDescription("Should the tunnel automatically start when the router starts?")
                .setRequired(true)
            );
    }

}
