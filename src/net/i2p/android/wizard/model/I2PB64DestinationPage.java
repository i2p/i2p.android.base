package net.i2p.android.wizard.model;

import android.support.v4.app.Fragment;

import net.i2p.android.wizard.ui.I2PB64DestinationFragment;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;

/**
 * A page asking for an I2P Destination.
 * This must be the B64 representation of a Destination.
 */
public class I2PB64DestinationPage extends SingleTextFieldPage {
    private String mFeedback;

    public I2PB64DestinationPage(ModelCallbacks callbacks, String title) {
        super(callbacks, title);
    }

    @Override
    public Fragment createFragment() {
        return I2PB64DestinationFragment.create(getKey());
    }

    @Override
    public boolean isValid() {
        String data = mData.getString(SIMPLE_DATA_KEY);
        try {
            new Destination().fromBase64(data);
        } catch (DataFormatException dfe) {
            mFeedback = "Invalid B64";
            return false;
        }
        mFeedback = "";
        return true;
    }

    @Override
    public boolean showFeedback() {
        return true;
    }

    @Override
    public String getFeedback() {
        return mFeedback;
    }
}
