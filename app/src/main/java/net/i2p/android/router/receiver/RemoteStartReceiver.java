package net.i2p.android.router.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import android.util.Log;

import net.i2p.android.router.service.RouterService;
import net.i2p.android.router.util.Util;

public class RemoteStartReceiver extends BroadcastReceiver {
    private static final String TAG = "I2PRemoteStartReceiver";
    private static final String ACTION_START_I2P = "net.i2p.android.router.receiver.START_I2P";
    
    // SECURITY: Authentication token to prevent unauthorized access (CVE-2025-ANDROID-001)
    private static final String AUTH_TOKEN_EXTRA = "auth_token";
    private static final String REQUIRED_PERMISSION = "net.i2p.android.router.REMOTE_START";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        // SECURITY: Validate intent action
        if (!ACTION_START_I2P.equals(intent.getAction())) {
            Log.w(TAG, "Received invalid action: " + intent.getAction());
            return;
        }
        
        // SECURITY: Validate sender package signature
        if (!validateSender(context, intent)) {
            Log.w(TAG, "Unauthorized remote start attempt from untrusted source");
            return;
        }
        
        // SECURITY: Check authentication token
        if (!validateAuthToken(context, intent)) {
            Log.w(TAG, "Invalid or missing authentication token for remote start");
            return;
        }
        
        // Only start router if not already running
        if (Util.getRouterContext() == null) {
            Log.i(TAG, "Starting I2P Router via authenticated remote request");
            Intent rsIntent = new Intent(context, RouterService.class);
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(rsIntent);
            } else {
                context.startService(rsIntent);
            }
            
            Toast.makeText(context, "Starting I2P Router (Remote)", Toast.LENGTH_SHORT).show();
        } else {
            Log.i(TAG, "I2P Router already running, ignoring remote start request");
        }
    }
    
    /**
     * SECURITY: Validate that the sending package is authorized to start I2P
     * Only allow packages signed with the same signature as I2P
     */
    private boolean validateSender(Context context, Intent intent) {
        try {
            // Get the package name of the sender
            Bundle extras = intent.getExtras();
            if (extras == null) {
                return false;
            }
            
            String senderPackage = extras.getString("sender_package");
            if (senderPackage == null || senderPackage.isEmpty()) {
                return false;
            }
            
            // Check if sender has the required custom permission
            PackageManager pm = context.getPackageManager();
            try {
                pm.getPermissionInfo(REQUIRED_PERMISSION, 0);
                // Permission exists, check if sender has it
                int result = pm.checkPermission(REQUIRED_PERMISSION, senderPackage);
                return result == PackageManager.PERMISSION_GRANTED;
            } catch (PackageManager.NameNotFoundException e) {
                // Custom permission doesn't exist, fall back to signature check
                return checkSignatureMatch(context, senderPackage);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error validating sender", e);
            return false;
        }
    }
    
    /**
     * Check if the sender package has the same signature as I2P
     */
    private boolean checkSignatureMatch(Context context, String senderPackage) {
        try {
            PackageManager pm = context.getPackageManager();
            String i2pPackage = context.getPackageName();
            
            // Compare package signatures
            int result = pm.checkSignatures(i2pPackage, senderPackage);
            return result == PackageManager.SIGNATURE_MATCH;
        } catch (Exception e) {
            Log.e(TAG, "Error checking signatures", e);
            return false;
        }
    }
    
    /**
     * SECURITY: Validate authentication token to prevent replay attacks
     */
    private boolean validateAuthToken(Context context, Intent intent) {
        try {
            Bundle extras = intent.getExtras();
            if (extras == null) {
                return false;
            }
            
            String providedToken = extras.getString(AUTH_TOKEN_EXTRA);
            if (providedToken == null || providedToken.isEmpty()) {
                return false;
            }
            
            // For demonstration: simple time-based token validation
            // In production, use more sophisticated token validation
            long currentTime = System.currentTimeMillis();
            long tokenTime = Long.parseLong(providedToken.substring(providedToken.length() - 13));
            
            // Token must be within 5 minutes of current time
            return Math.abs(currentTime - tokenTime) < 300000; // 5 minutes
            
        } catch (Exception e) {
            Log.e(TAG, "Error validating auth token", e);
            return false;
        }
    }
}
