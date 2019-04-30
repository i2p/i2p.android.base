package net.i2p.android.router.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import net.i2p.android.router.R;
import net.i2p.android.wizard.model.Page;
import net.i2p.client.naming.NamingService;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.router.RouterContext;

import java.net.IDN;
import java.util.List;
import java.util.Locale;

public class NamingServiceUtil {
    private static final String DEFAULT_NS = "BlockfileNamingService";

    public static boolean addFromWizard(
            Context ctx, NamingService ns, Bundle data, boolean replace) {
        boolean success = false;

        // Get the Bundle keys
        Resources res = ctx.getResources();

        String kHostName = res.getString(R.string.addressbook_add_wizard_k_name);
        String kDest = res.getString(R.string.addressbook_add_wizard_k_destination);

        String hostName = data.getBundle(kHostName).getString(Page.SIMPLE_DATA_KEY);
        String host;
        try {
            // Already validated, won't throw IAE
            // ^^^ NOT TRUE ticket #2489 ^^^
            host = toASCII(res, hostName);
        } catch (IllegalArgumentException iae) {
            Toast.makeText(ctx,
                           iae.getMessage(),
                           Toast.LENGTH_LONG).show();
            return false;
        }
        String displayHost = host.equals(hostName) ? hostName :
                                                     hostName + " (" + host + ')';

        String destB64 = data.getBundle(kDest).getString(Page.SIMPLE_DATA_KEY);
        Destination dest = new Destination();
        try {
            dest.fromBase64(destB64);
        } catch (DataFormatException e) {} // Already validated

        // Check if already in addressbook
        Destination oldDest = ns.lookup(host);
        if (oldDest != null) {
            if (destB64.equals(oldDest.toBase64()))
                Toast.makeText(ctx,
                        "Host name " + displayHost + " is already in address book, unchanged.",
                        Toast.LENGTH_LONG).show();
            else if (!replace)
                Toast.makeText(ctx,
                        "Host name " + displayHost + " is already in address book with a different Destination.",
                        Toast.LENGTH_LONG).show();
        } else {
            // Put the new host name
            success = ns.put(host, dest);
            if (!success)
                Toast.makeText(ctx,
                        "Failed to add Destination " + displayHost + " to naming service " + ns.getName(),
                        Toast.LENGTH_LONG).show();
        }

        return success;
    }

    /** @return the NamingService for the current file name, or the root NamingService */
    public static NamingService getNamingService(RouterContext ctx, String book)
    {
        NamingService root = ctx.namingService();
        NamingService rv = searchNamingService(root, book);
        return rv != null ? rv : root;
    }

    /** depth-first search */
    private static NamingService searchNamingService(NamingService ns, String srch)
    {
        String name = ns.getName();
        if (name.equals(srch) || basename(name).equals(srch) || name.equals(DEFAULT_NS))
            return ns;
        List<NamingService> list = ns.getNamingServices();
        if (list != null) {
            for (NamingService nss : list) {
                NamingService rv = searchNamingService(nss, srch);
                if (rv != null)
                    return rv;
            }
        }
        return null;
    }

    private static String basename(String filename) {
        int slash = filename.lastIndexOf('/');
        if (slash >= 0)
            filename = filename.substring(slash + 1);
        return filename;
    }

    private static final char DOT = '.';
    private static final char DOT2 = 0x3002;
    private static final char DOT3 = 0xFF0E;
    private static final char DOT4 = 0xFF61;

    /**
     * Ref: java.net.IDN and RFC 3940
     * @param host will be converted to lower case
     * @return name converted to lower case and punycoded if necessary
     * @throws java.lang.IllegalArgumentException on various errors or if IDN is needed but not available
     * @since 0.8.7
     */
    @SuppressLint("NewApi")
    static String toASCII(Resources res, String host) throws IllegalArgumentException {
        host = host.toLowerCase(Locale.US);

        boolean needsIDN = false;
        // Here we do easy checks and throw translated exceptions.
        // We do checks on the whole host name, not on each "label", so
        // we allow '.', and some untranslated errors will be thrown by IDN.toASCII()
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if (c <= 0x2c ||
                    c == 0x2f ||
                    c >= 0x3a && c <= 0x40 ||
                    c >= 0x5b && c <= 0x60 ||
                    c >= 0x7b && c <= 0x7f) {
                String bad = "\"" + c + "\" (0x" + Integer.toHexString(c) + ')';
                throw new IllegalArgumentException(
                        res.getString(R.string.nsu_iae_illegal_char, host, bad));
            }
            if (c == DOT2)
                host = host.replace(DOT2, DOT);
            else if (c == DOT3)
                host = host.replace(DOT3, DOT);
            else if (c == DOT4)
                host = host.replace(DOT4, DOT);
            else if (c > 0x7f)
                needsIDN = true;
        }
        if (host.startsWith("-"))
            throw new IllegalArgumentException(
                    res.getString(R.string.nsu_iae_cannot_start_with, "-"));
        if (host.startsWith("."))
            throw new IllegalArgumentException(
                    res.getString(R.string.nsu_iae_cannot_start_with, "."));
        if (host.endsWith("-"))
            throw new IllegalArgumentException(
                    res.getString(R.string.nsu_iae_cannot_end_with, "-"));
        if (host.endsWith("."))
            throw new IllegalArgumentException(
                    res.getString(R.string.nsu_iae_cannot_end_with, "."));
        if (needsIDN) {
            if (host.startsWith("xn--"))
                throw new IllegalArgumentException(
                        res.getString(R.string.nsu_iae_cannot_start_with, "xn--"));
            if (host.contains(".xn--"))
                throw new IllegalArgumentException(
                        res.getString(R.string.nsu_iae_cannot_contain, ".xn--"));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
                return IDN.toASCII(host, IDN.ALLOW_UNASSIGNED);
            else
                throw new IllegalArgumentException(
                        res.getString(R.string.nsu_iae_requires_conversion, host));
        }
        return host;
    }
}
