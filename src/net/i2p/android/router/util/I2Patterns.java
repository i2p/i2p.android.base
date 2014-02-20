package net.i2p.android.router.util;

import java.util.regex.Pattern;

import android.util.Patterns;

public class I2Patterns {
    /**
     * The double-parentheses are needed because the included
     * pattern has an additional closing parenthesis.
     */
    public static final String I2P_TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL =
            "(?:("
            + Patterns.TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL
            + "|i2p))";

    /**
     *  Regular expression pattern to match most part of RFC 3987
     *  Internationalized URLs, aka IRIs.  Commonly used Unicode characters are
     *  added.
     *  Copied from android.util.Patterns
     */
    public static final Pattern I2P_WEB_URL = Pattern.compile(
        "((?:(http|https|Http|Https|rtsp|Rtsp):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
        + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
        + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
        + "((?:(?:[" + Patterns.GOOD_IRI_CHAR + "][" + Patterns.GOOD_IRI_CHAR + "\\-]{0,64}\\.)+"   // named host
        + I2P_TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL
        + "|(?:(?:25[0-5]|2[0-4]" // or ip address
        + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]"
        + "|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1]"
        + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
        + "|[1-9][0-9]|[0-9])))"
        + "(?:\\:\\d{1,5})?)" // plus option port number
        + "(\\/(?:(?:[" + Patterns.GOOD_IRI_CHAR + "\\;\\/\\?\\:\\@\\&\\=\\#\\~"  // plus option query params
        + "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
        + "(?:\\b|$)"); // and finally, a word boundary or end of
                        // input.  This is to stop foo.sure from
                        // matching as foo.su

    private I2Patterns() {
    }
}
