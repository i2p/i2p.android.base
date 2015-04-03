package net.i2p.android;

import android.test.ActivityInstrumentationTestCase2;

import net.i2p.android.router.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

public class I2PActivityTest extends ActivityInstrumentationTestCase2<I2PActivity> {
    public I2PActivityTest() {
        super(I2PActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // For each test method invocation, the Activity will not actually be created
        // until the first time this method is called.
        getActivity();
    }

    public void testMainTabs() {
        onView(withId(R.id.router_onoff_button)).check(matches(isDisplayed()));

        // Press "Addresses" tab
        onView(withText(R.string.label_addresses)).perform(click());
        onView(withId(R.id.router_onoff_button)).check(matches(not(isDisplayed())));
        onView(withText(R.string.label_router)).check(matches(isDisplayed()));

        // Press "Tunnels" tab
        onView(allOf(withText(R.string.label_tunnels),
                not(isDescendantOfA(withId(R.id.main_scrollview))))).perform(click());
        onView(withText(R.string.label_router)).check(matches(not(isDisplayed())));
        onView(withText(R.string.label_i2ptunnel_client)).check(matches(isDisplayed()));

        // Press "Console" tab
        onView(withText(R.string.label_console)).perform(click());
        // Tunnels fragment should have been destroyed
        onView(withText(R.string.label_i2ptunnel_client)).check(doesNotExist());
        onView(withId(R.id.router_onoff_button)).check(matches(isDisplayed()));
    }

    public void testMainSwipe() {
        onView(withId(R.id.router_onoff_button)).check(matches(isDisplayed()));

        onView(allOf(withId(R.id.pager), hasSibling(withId(R.id.main_toolbar)))).perform(swipeLeft());
        onView(withId(R.id.router_onoff_button)).check(matches(not(isDisplayed())));
        onView(withText(R.string.label_router)).check(matches(isDisplayed()));

        onView(allOf(withId(R.id.pager), hasSibling(withId(R.id.main_toolbar)))).perform(swipeLeft());
        // TODO: test addressbook ViewPager
        onView(allOf(withId(R.id.pager), hasSibling(withId(R.id.main_toolbar)))).perform(swipeLeft());
        onView(withText(R.string.label_router)).check(matches(not(isDisplayed())));
        onView(withText(R.string.label_i2ptunnel_client)).check(matches(isDisplayed()));
        // TODO: test addressbook ViewPager
    }

    public void testConsoleSubToolbar() {
        onView(withText(R.string.label_news)).perform(click());
        onView(withText(R.string.label_news)).check(matches(isDisplayed()));
        onView(withId(R.id.router_onoff_button)).check(doesNotExist());

        pressBack();
        onView(withId(R.id.router_onoff_button)).check(matches(isDisplayed()));

        onView(withText(R.string.label_logs)).perform(click());
        onView(withId(R.id.router_onoff_button)).check(doesNotExist());

        pressBack();
        onView(withText(R.string.label_error_logs)).check(doesNotExist());
        onView(withId(R.id.router_onoff_button)).check(matches(isDisplayed()));
    }
}
