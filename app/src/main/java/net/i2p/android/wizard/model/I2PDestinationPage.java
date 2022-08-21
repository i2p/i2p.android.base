package net.i2p.android.wizard.model;

import java.util.Locale;

import android.support.v4.app.Fragment;

import net.i2p.android.wizard.ui.I2PDestinationFragment;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;

/**
 * A page asking for an I2P Destination.
 * This could be a B64, B32 or Addressbook domain.
 */
public class I2PDestinationPage extends SingleTextFieldPage {
    private static final int BASE32_HASH_LENGTH = 52;   // 1 + Hash.HASH_LENGTH * 8 / 5
    private String mFeedback;

    public I2PDestinationPage(ModelCallbacks callbacks, String title) {
        super(callbacks, title);
    }

    @Override
    public Fragment createFragment() {
        return I2PDestinationFragment.create(getKey());
    }

    @Override
    public boolean isValid() {
        String data = mData.getString(SIMPLE_DATA_KEY);
        if (data.toLowerCase(Locale.US).endsWith(".b32.i2p")) { /* B32 */
            if (data.length() == BASE32_HASH_LENGTH + 8 || data.length() >= BASE32_HASH_LENGTH + 12) {
                mFeedback = "";
                return true;
            }
            mFeedback = "Invalid B32";
            return false;
        } else if (data.endsWith(".i2p")) { /* Domain */
            // Valid
        } else if (data.length() >= 516) { /* B64 */
            try {
                new Destination().fromBase64(data);
            } catch (DataFormatException dfe) {
                mFeedback = "Invalid B64";
                return false;
            }
        } else {
            mFeedback = "Not a valid I2P Destination";
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
