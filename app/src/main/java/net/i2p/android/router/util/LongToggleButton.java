package net.i2p.android.router.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ToggleButton;

public class LongToggleButton extends ToggleButton {
    public LongToggleButton(Context context) {
        super(context);
    }

    public LongToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LongToggleButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean performClick() {
        /* Cancel out toggle */
        toggle();
        return super.performClick();
    }

    @Override
    public boolean performLongClick() {
        /* When clicked, toggle the state */
        toggle();
        return super.performLongClick();
    }
}
