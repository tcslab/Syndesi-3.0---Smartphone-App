package tcslab.syndesiapp.views;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.opencv.android.LoaderCallbackInterface;
import tcslab.syndesiapp.R;
import tcslab.syndesiapp.controllers.account.AccountController;
import tcslab.syndesiapp.controllers.localization.LocalizationController;
import tcslab.syndesiapp.controllers.sensor.SensorAdapter;
import tcslab.syndesiapp.controllers.sensor.SensorController;
import tcslab.syndesiapp.controllers.sensor.SensorList;
import tcslab.syndesiapp.controllers.ui.UIReceiver;
import tcslab.syndesiapp.controllers.ui.WifiReceiver;
import tcslab.syndesiapp.models.Account;
import tcslab.syndesiapp.models.BroadcastType;
import tcslab.syndesiapp.models.PreferenceKey;
import tcslab.syndesiapp.models.SensorData;

import java.util.ArrayList;

import org.opencv.android.OpenCVLoader;
import tcslab.syndesiapp.tools.RuntimePermissionChecker;

/**
 * Displays the sensors readings and the server status.
 * Created by Blaise on 27.04.2015.
 */
public class MainActivity extends AppCompatActivity {
    private UIReceiver mUiReceiver;
    private ArrayList<SensorData> mSensorsList;
    private SensorAdapter mSensorsAdapter;
    private AccountController mAccountController;
    private SharedPreferences mPreferences;
    private LocalizationController mLocalizationController;
    private String[] permissionNeeded = new String[] {
            Manifest.permission.INTERNET,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Set default settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        //Set the layout
        setContentView(R.layout.activity_main);
        //Set the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //Set the sensor list
        ListView listView = (ListView) findViewById(R.id.sensor_list);
        mSensorsList = new ArrayList<>();
        mSensorsAdapter = new SensorAdapter(this, mSensorsList);
        listView.setAdapter(mSensorsAdapter);

        //Set the account controller to use with Syndesi server (legacy)
        this.mAccountController = AccountController.getInstance(getApplicationContext());
        Account account = mAccountController.getAccount();
        String id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        Account newAccount = new Account(id, "", "", "", 0, 0, SensorController.getInstance(this).getmAvailableSensors());
        if (account == null) {
            AccountController.getInstance(getApplicationContext()).createAccount(newAccount);
        }else{
            AccountController.getInstance(getApplicationContext()).saveAccount(newAccount);
        }

        // Set the localization controller
        mLocalizationController = LocalizationController.getInstance(this);

        //Creates the broadcast receiver that updates the UI
        mUiReceiver = new UIReceiver(this);

        //Set the preferences listener
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mPreferences.registerOnSharedPreferenceChangeListener(SensorController.getInstance(this));
        mPreferences.registerOnSharedPreferenceChangeListener(LocalizationController.getInstance(this));

        // Runtime permissions for Android 6+
        if (Build.VERSION.SDK_INT >= 23) {
            RuntimePermissionChecker rpc = new RuntimePermissionChecker(this);
            Boolean permission = rpc.getPermissions();
            mPreferences.edit().putBoolean(PreferenceKey.PREF_PERMISION.toString(), permission).apply();
        }else{
            mPreferences.edit().putBoolean(PreferenceKey.PREF_PERMISION.toString(), true).apply();
        }
    }

    public void removeSensors(){
        mSensorsList.clear();
        mSensorsAdapter.notifyDataSetChanged();
    }

    public void addSensor(SensorData sensor){
        Boolean sensorExist = false;
        for(SensorData currentSensor : mSensorsList) {
            if (currentSensor.getmDataType().equals(sensor.getmDataType())) {
                currentSensor.setmData(sensor.getmData());
                sensorExist = true;
            }
        }
        if(!sensorExist){
            mSensorsList.add(sensor);
        }
        mSensorsAdapter.notifyDataSetChanged();
    }

    /**
     *
     * Check that the user did allow the application to use the WiFi and access external storage
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Boolean allPermissionsOk = true;
        for(int i = 0; i < permissions.length; i++){
            if(grantResults[i] == PackageManager.PERMISSION_DENIED){
                allPermissionsOk = false;
            }
        }
        if(allPermissionsOk){
            mPreferences.edit().putBoolean(PreferenceKey.PREF_PERMISION.toString(), true).apply();
        }else{
            ((TextView) findViewById(R.id.loc_display)).setText(R.string.loc_noperm);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle menu clicks
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }else if(id == R.id.action_controller){
            startActivity(new Intent(this, NodesControllerActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Life cycle management
    @Override
    protected void onResume() {
        super.onResume();

        //Register the Broadcast listener
        IntentFilter filter = new IntentFilter();
        for(Integer sensorType : SensorList.sensorUsed){
            filter.addAction(String.valueOf(sensorType));
        }
        filter.addAction(BroadcastType.BCAST_TYPE_SERVER_STATUS.toString());
        filter.addAction(BroadcastType.BCAST_TYPE_CONTROLLER_STATUS.toString());
        filter.addAction(BroadcastType.BCAST_TYPE_LOC_STATUS.toString());
        filter.addAction(BroadcastType.BCAST_TYPE_LOC_POSITION.toString());
        LocalBroadcastManager.getInstance(this).registerReceiver(mUiReceiver, filter);

        //Reset the context on the controllers
        SensorController.getInstance(this).setmActivity(this);
        mLocalizationController.setmAppContext(this);
    }

    public void relocate(View v){
        ((TextView) this.findViewById(R.id.loc_display)).setText(R.string.loc_scanning);
        Toast.makeText(this, "Starting WiFi scan", Toast.LENGTH_SHORT).show();
        ((WifiManager) getSystemService(Context.WIFI_SERVICE)).startScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Unregister the Broadcast listener
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mUiReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d("Restoration", "save");
        savedInstanceState.putString(String.valueOf(R.id.sensors_status), ((TextView) findViewById(R.id.sensors_status)).getText().toString());
        savedInstanceState.putString(String.valueOf(R.id.loc_display), ((TextView) findViewById(R.id.loc_display)).getText().toString());
        savedInstanceState.putString(String.valueOf(R.id.server_display_status), ((TextView) findViewById(R.id.server_display_status)).getText().toString());

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Log.d("Restoration", "restore");
        ((TextView) findViewById(R.id.sensors_status)).setText(savedInstanceState.getString(String.valueOf(R.id.sensors_status)));
        ((TextView) findViewById(R.id.loc_display)).setText(savedInstanceState.getString(String.valueOf(R.id.loc_display)));
        ((TextView) findViewById(R.id.server_display_status)).setText(savedInstanceState.getString(String.valueOf(R.id.server_display_status)));
    }
}
