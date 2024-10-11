package net.i2p.android;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.i2p.android.router.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class I2PActivityTest {

    private ActivityScenario<I2PActivity> scenario;

    @Before
    public void setUp() {
        scenario = ActivityScenario.launch(I2PActivity.class);
    }

    @Test
    public void testMainTabs() {
        onView(withId(R.id.router_onoff_button)).check(matches(isDisplayed()));

        // Press "Tunnels" tab
        onView(allOf(withText(R.string.label_tunnels),
                not(isDescendantOfA(withId(R.id.main_scrollview))))).perform(click());
        onView(withId(R.id.router_onoff_button)).check(matches(not(isDisplayed())));
        onView(withText(R.string.label_i2ptunnel_client)).check(matches(isDisplayed()));

        // Press "Addresses" tab
        onView(withText(R.string.label_addresses)).perform(click());
        onView(withText(R.string.label_i2ptunnel_client)).check(matches(not(isDisplayed())));
        onView(withText(R.string.label_router)).check(matches(isDisplayed()));

        // Press "Console" tab
        onView(withText(R.string.label_console)).perform(click());
        // Addressbook fragment should have been destroyed
        onView(withText(R.string.label_router)).check(doesNotExist());
        onView(withId(R.id.router_onoff_button)).check(matches(isDisplayed()));
    }

    @Test
    public void testMainSwipe() {
        onView(withId(R.id.router_onoff_button)).check(matches(isDisplayed()));

        onView(allOf(withId(R.id.pager), withParent(hasSibling(withId(R.id.main_toolbar))))).perform(swipeLeft());
        onView(withId(R.id.router_onoff_button)).check(matches(not(isDisplayed())));
        onView(withText(R.string.label_i2ptunnel_client)).check(matches(isDisplayed()));

        onView(allOf(withId(R.id.pager), withParent(hasSibling(withId(R.id.main_toolbar))))).perform(swipeLeft());
        // TODO: test tunnels ViewPager
        onView(allOf(withId(R.id.pager), withParent(hasSibling(withId(R.id.main_toolbar))))).perform(swipeLeft());
        onView(withText(R.string.label_i2ptunnel_client)).check(matches(not(isDisplayed())));
        onView(withText(R.string.label_router)).check(matches(isDisplayed()));
        // TODO: test addressbook ViewPager
    }

    @Test
    public void testSettingsNavigation() {
        // Open settings menu
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.menu_settings)).perform(click());

        // Open bandwidth page
        onView(withText(R.string.settings_label_bandwidth_net)).perform(click());
        onView(withText(R.string.settings_label_startOnBoot)).check(matches(isDisplayed()));
        pressBack();

        // Open graphs page
        onView(withText(R.string.label_graphs)).perform(click());
        onView(withText(R.string.router_not_running)).check(matches(isDisplayed()));
        pressBack();

        // Open logging page
        onView(withText(R.string.settings_label_logging)).perform(click());
        onView(withText(R.string.settings_label_default_log_level)).check(matches(isDisplayed()));
        pressBack();

        // Open addressbook page
        onView(withText(R.string.label_addressbook)).perform(click());
        onView(withText("Subscriptions")).check(matches(isDisplayed()));
        closeSoftKeyboard();
        pressBack();

        // Open graphs page
        onView(withText(R.string.settings_label_advanced)).perform(click());
        onView(withText(R.string.settings_label_transports)).check(matches(isDisplayed()));
        pressBack();

        // Check back exits settings
        onView(withText(R.string.settings_label_advanced)).check(matches(isDisplayed()));
        pressBack();
        onView(withText(R.string.settings_label_advanced)).check(doesNotExist());
    }
}