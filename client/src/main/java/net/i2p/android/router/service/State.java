package net.i2p.android.router.service;

/**
 * Extracted from RouterService because Enums should be avoided on Android.
 * <p/>
 * https://developer.android.com/training/articles/memory.html#Overhead
 *
 * @author str4d
 * @since 0.9.14
 */
public class State {
    // These states persist even if we died... Yuck, it causes issues.
    public static final int INIT = 0;
    public static final int WAITING = 1;
    public static final int STARTING = 2;
    public static final int RUNNING = 3;
    public static final int ACTIVE = 4;
    // unplanned (router stopped itself), next: killSelf()
    public static final int STOPPING = 5;
    public static final int STOPPED = 6;
    // button, don't kill service when stopped, stay in MANUAL_STOPPED
    public static final int MANUAL_STOPPING = 7;
    public static final int MANUAL_STOPPED = 8;
    // button, DO kill service when stopped, next: killSelf()
    public static final int MANUAL_QUITTING = 9;
    public static final int MANUAL_QUITTED = 10;
    // Stopped by listener (no network), next: WAITING (spin waiting for network)
    public static final int NETWORK_STOPPING = 11;
    public static final int NETWORK_STOPPED = 12;
}
