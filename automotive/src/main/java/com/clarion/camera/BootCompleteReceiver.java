package com.clarion.camera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import javax.security.auth.login.LoginException;

public class BootCompleteReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompleteReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent SensorServiceIntent = new Intent(context,
                SensorManagerMonitorService.class);
        Log.i(TAG, "onReceive: Starting SensorManagerMonitorService");
        context.startService(SensorServiceIntent);
    }
}
