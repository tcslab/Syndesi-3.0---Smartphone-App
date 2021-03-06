package tcslab.syndesiapp.controllers.localization;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import tcslab.syndesiapp.tools.WifiCallback;

/**
 * Receive the system broadcast to update the values of the RSSI to the WiFi service.
 *
 * Created by Blaise on 30.11.2016.
 */
public class WifiReceiver extends BroadcastReceiver {
    private WifiCallback callback;
    private Context mAppContext;

    public WifiReceiver(WifiCallback callback, Context appContext) {
        this.callback = callback;
        this.mAppContext = appContext;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
            callback.sendResults(((WifiManager) mAppContext.getSystemService(Service.WIFI_SERVICE)).getScanResults());
        }
    }
}
