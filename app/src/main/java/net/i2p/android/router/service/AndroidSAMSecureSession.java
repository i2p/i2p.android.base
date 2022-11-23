package net.i2p.android.router.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import net.i2p.android.I2PActivity;
import net.i2p.android.I2PActivityBase;
import net.i2p.android.router.R;
import net.i2p.android.router.util.Notifications;
import net.i2p.android.router.util.Util;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.sam.*;
import net.i2p.sam.SAMException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Implements SAMSecureSessionInterface on Android platforms using a Toast
 * as the interactive channel.
 *
 * @since 1.8.0
 */
public class AndroidSAMSecureSession extends AppCompatActivity implements SAMSecureSessionInterface {
    private static final String URI_I2P_ANDROID = "net.i2p.android";
    private final Context mCtx;
    private final RouterService _routerService;
    private final StatusBar _statusBar;
    static private Map<String, Integer> results = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public static void affirmResult(String clientId) {
        Util.d("Affirmed result for: " + clientId);
        results.put(clientId, 1);
    }

    public AndroidSAMSecureSession(Context ctx, RouterService rCtx, StatusBar statusBar) {
        mCtx = ctx;
        _routerService = rCtx;
        _statusBar = statusBar;
    }

    private void waitForResult(String clientId) {
        for (int i=0;i<60;i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Util.e("SAMSecureSession Error", e);
            }
            Integer result = results.get(clientId);
            if (result == null)
                continue;
            if (result != -1)
                break;
            Util.d("Waiting on user to approve SAM connection for: "+clientId);
        }
    }

    private boolean isResult(String clientId) {
        waitForResult(clientId);
        final Integer finResult = results.get(clientId);
        if (finResult == null)
            return false;
        _routerService.updateStatus();
        if (finResult == 0) {
            Util.w("SAM connection cancelled by user request");
            return false;
        }
        if (finResult == 1) {
            Util.w("SAM connection allowed by user action");
            return true;
        }
        Util.w("SAM connection denied by timeout.");
        return false;
    }

    private boolean checkResult(String clientId) {
        Intent intent = new Intent("net.i2p.android.router.service.APPROVE_SAM", null, mCtx, I2PActivity.class );
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage(URI_I2P_ANDROID);
        Bundle bundle = new Bundle();
        bundle.putBoolean("approveSAMConnection", true);
        bundle.putString("ID", clientId);
        intent.putExtras(bundle);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(
                    mCtx, 7656,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE,
                    bundle);
        } else {
            pendingIntent = PendingIntent.getActivity(
                    mCtx, 7656,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT,
                    bundle);
        }
        String dlgText = mCtx.getString(R.string.settings_confirm_sam) + "\n";//""</br>";
        dlgText += mCtx.getString(R.string.settings_confirm_sam_id) + clientId + "\n";//""</br>";
        dlgText += mCtx.getString(R.string.settings_confirm_allow_sam) + "\n";//""</br>";
        dlgText += mCtx.getString(R.string.settings_confirm_deny_sam) + "\n";//""</br>";


        _statusBar.replaceIntent(StatusBar.ICON_ACTIVE, dlgText, pendingIntent);
        return isResult(clientId);
    }

    @Override
    public boolean approveOrDenySecureSession(Properties i2cpProps, Properties props) throws SAMException {
        String ID = props.getProperty("USER");
        if (ID == null)
            ID = i2cpProps.getProperty("inbound.nickname");
        if (ID == null)
            ID = i2cpProps.getProperty("outbound.nickname");
        if (ID == null)
            ID = props.getProperty("ID");
        if (ID == null) {
            MessageDigest messageDigest = null;
            try {
                messageDigest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                ID = "No_ID_Present";
            }
            if (messageDigest != null) {
                String combinedProps = i2cpProps.toString() + props.toString();
                messageDigest.update(combinedProps.getBytes());
                ID = messageDigest.digest().toString();
            }else{
                ID = "No_ID_Present";
            }
        }
        return checkResult(ID);
    }
}
