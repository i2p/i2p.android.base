package net.i2p.android.i2ptunnel;

import android.content.Context;
import android.content.res.Resources;
import net.i2p.android.router.R;
import net.i2p.android.wizard.model.AbstractWizardModel;
import net.i2p.android.wizard.model.BranchPage;
import net.i2p.android.wizard.model.Conditional;
import net.i2p.android.wizard.model.I2PDestinationPage;
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
                .setDescription(res.getString(R.string.i2ptunnel_wizard_desc_name))
                .setRequired(true),

            new SingleTextFieldPage(this, res.getString(R.string.i2ptunnel_wizard_k_desc))
                .setDescription(res.getString(R.string.i2ptunnel_wizard_desc_desc)),

            new I2PDestinationPage(this, res.getString(R.string.i2ptunnel_wizard_k_dest))
                .setDescription(res.getString(R.string.i2ptunnel_wizard_desc_dest))
                .setRequired(true)
                .setEqualAnyCondition(cClientType,
                        res.getString(R.string.i2ptunnel_type_client),
                        res.getString(R.string.i2ptunnel_type_ircclient),
                        res.getString(R.string.i2ptunnel_type_streamrclient)),

            new SingleTextFieldPage(this, res.getString(R.string.i2ptunnel_wizard_k_outproxies))
                .setDescription(res.getString(R.string.i2ptunnel_wizard_desc_outproxies))
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
                .setDescription(res.getString(R.string.i2ptunnel_wizard_desc_target_host))
                .setEqualCondition(cClientType,
                        res.getString(R.string.i2ptunnel_type_streamrclient))
                .setEqualAnyCondition(cServerType,
                        res.getString(R.string.i2ptunnel_type_server),
                        res.getString(R.string.i2ptunnel_type_httpserver),
                        res.getString(R.string.i2ptunnel_type_httpbidirserver),
                        res.getString(R.string.i2ptunnel_type_ircserver)),

            new SingleTextFieldPage(this, res.getString(R.string.i2ptunnel_wizard_k_target_port))
                .setDescription(res.getString(R.string.i2ptunnel_wizard_desc_target_port))
                .setNumeric(true)
                .setRequired(true)
                .setEqualCondition(cTunnelType, res.getString(R.string.i2ptunnel_wizard_v_server)),

            // Not set required because a default is specified.
            new SingleTextFieldPage(this, res.getString(R.string.i2ptunnel_wizard_k_reachable_on))
                .setDefault("127.0.0.1")
                .setDescription(res.getString(R.string.i2ptunnel_wizard_desc_reachable_on))
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
                .setDescription(res.getString(R.string.i2ptunnel_wizard_k_binding_port))
                .setNumeric(true)
                .setRequired(true)
                .setEqualCondition(cTunnelType, res.getString(R.string.i2ptunnel_wizard_v_client))
                .setEqualCondition(cServerType, res.getString(R.string.i2ptunnel_type_httpbidirserver)),

            new SingleFixedBooleanPage(this, res.getString(R.string.i2ptunnel_wizard_k_auto_start))
                .setDescription(res.getString(R.string.i2ptunnel_wizard_desc_auto_start))
                .setRequired(true)
            );
    }

}
