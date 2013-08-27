package net.i2p.android.i2ptunnel.activity;

import android.content.Context;
import android.content.res.Resources;
import net.i2p.android.router.R;
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
        Resources res = mContext.getResources();
        Conditional cTunnelType = new Conditional();
        Conditional cClientType = new Conditional();
        Conditional cServerType = new Conditional();

        return new PageList(
            new BranchPage(this, res.getString(R.string.i2ptunnel_wizard_k_client_server))
                .addBranch(res.getString(R.string.i2ptunnel_wizard_v_client),
                    new SingleFixedChoicePage(this, res.getString(R.string.i2ptunnel_wizard_k_type))
                        .setChoices(
                                res.getString(R.string.i2ptunnel_type_client),
                                res.getString(R.string.i2ptunnel_type_httpclient),
                                res.getString(R.string.i2ptunnel_type_ircclient),
                                res.getString(R.string.i2ptunnel_type_sockstunnel),
                                res.getString(R.string.i2ptunnel_type_socksirctunnel),
                                res.getString(R.string.i2ptunnel_type_connectclient),
                                res.getString(R.string.i2ptunnel_type_streamrclient))
                        .setRequired(true)
                        .makeConditional(cClientType))
                .addBranch(res.getString(R.string.i2ptunnel_wizard_v_server),
                    new SingleFixedChoicePage(this, res.getString(R.string.i2ptunnel_wizard_k_type))
                        .setChoices(
                                res.getString(R.string.i2ptunnel_type_server),
                                res.getString(R.string.i2ptunnel_type_httpserver),
                                res.getString(R.string.i2ptunnel_type_httpbidirserver),
                                res.getString(R.string.i2ptunnel_type_ircserver),
                                res.getString(R.string.i2ptunnel_type_streamrserver))
                        .setRequired(true)
                        .makeConditional(cServerType))
                .setRequired(true)
                .makeConditional(cTunnelType),

            new SingleTextFieldPage(this, res.getString(R.string.i2ptunnel_wizard_k_name))
                .setDescription("The name of the tunnel, for identification in the tunnel list.")
                .setRequired(true),

            new SingleTextFieldPage(this, res.getString(R.string.i2ptunnel_wizard_k_desc))
                .setDescription("A description of the tunnel. This is optional and purely informative."),

            new SingleTextFieldPage(this, res.getString(R.string.i2ptunnel_wizard_k_dest))
                .setDescription("Type in the I2P destination of the service that this client tunnel should connect to. This could be the full base 64 destination key, or an I2P URL from your address book.")
                .setRequired(true)
                .setEqualAnyCondition(cClientType,
                        res.getString(R.string.i2ptunnel_type_client),
                        res.getString(R.string.i2ptunnel_type_ircclient),
                        res.getString(R.string.i2ptunnel_type_streamrclient)),

            new SingleTextFieldPage(this, res.getString(R.string.i2ptunnel_wizard_k_outproxies))
                .setDescription("If you know of any outproxies for this type of tunnel (either HTTP or SOCKS), fill them in. Separate multiple proxies with commas.")
                .setEqualAnyCondition(cClientType,
                        res.getString(R.string.i2ptunnel_type_httpclient),
                        res.getString(R.string.i2ptunnel_type_connectclient),
                        res.getString(R.string.i2ptunnel_type_sockstunnel),
                        res.getString(R.string.i2ptunnel_type_socksirctunnel)),

            // Not set required because a default is specified.
            // Otherwise user would need to edit the field to
            // enable the Next button.
            new SingleTextFieldPage(this, res.getString(R.string.i2ptunnel_wizard_k_target_host))
                .setDefault("127.0.0.1")
                .setDescription("This is the IP that your service is running on, this is usually on the same machine so 127.0.0.1 is autofilled.")
                .setEqualCondition(cClientType,
                        res.getString(R.string.i2ptunnel_type_streamrclient))
                .setEqualAnyCondition(cServerType,
                        res.getString(R.string.i2ptunnel_type_server),
                        res.getString(R.string.i2ptunnel_type_httpserver),
                        res.getString(R.string.i2ptunnel_type_httpbidirserver),
                        res.getString(R.string.i2ptunnel_type_ircserver)),

            new SingleTextFieldPage(this, res.getString(R.string.i2ptunnel_wizard_k_target_port))
                .setDescription("This is the port that the service is accepting connections on.")
                .setRequired(true)
                .setEqualCondition(cTunnelType, res.getString(R.string.i2ptunnel_wizard_v_server)),

            // Not set required because a default is specified.
            new SingleTextFieldPage(this, res.getString(R.string.i2ptunnel_wizard_k_reachable_on))
                .setDefault("127.0.0.1")
                .setDescription("This limits what computers or smartphones can access this tunnel.")
                .setEqualAnyCondition(cClientType,
                        res.getString(R.string.i2ptunnel_type_client),
                        res.getString(R.string.i2ptunnel_type_httpclient),
                        res.getString(R.string.i2ptunnel_type_ircclient),
                        res.getString(R.string.i2ptunnel_type_sockstunnel),
                        res.getString(R.string.i2ptunnel_type_socksirctunnel),
                        res.getString(R.string.i2ptunnel_type_connectclient))
                .setEqualAnyCondition(cServerType,
                        res.getString(R.string.i2ptunnel_type_httpbidirserver),
                        res.getString(R.string.i2ptunnel_type_streamrserver)),

            new SingleTextFieldPage(this, res.getString(R.string.i2ptunnel_wizard_k_binding_port))
                .setDescription("This is the port that the client tunnel will be accessed from locally. This is also the client port for the HTTP bidir server tunnel.")
                .setRequired(true)
                .setEqualCondition(cTunnelType, res.getString(R.string.i2ptunnel_wizard_v_client))
                .setEqualCondition(cServerType, res.getString(R.string.i2ptunnel_type_httpbidirserver)),

            new SingleFixedBooleanPage(this, res.getString(R.string.i2ptunnel_wizard_k_auto_start))
                .setDescription("Should the tunnel automatically start when the router starts?")
                .setRequired(true)
            );
    }

}
