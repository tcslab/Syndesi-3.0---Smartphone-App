package tcslab.syndesiapp.controllers.account;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import tcslab.syndesiapp.controllers.network.RESTInterface;
import tcslab.syndesiapp.models.Account;
import tcslab.syndesiapp.models.PreferenceKey;
import tcslab.syndesiapp.models.SensorData;
import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Manages account information on the app and the server in a singleton controller
 * and provides easy methods to convert to and from JSON.
 *
 * Created by Blaise on 05.05.2015.
 */
public class AccountController {
    private static AccountController mInstance;
    private Context mAppContext;
    private Gson mGson;
    private SharedPreferences mAccountPref;
    private SharedPreferences.Editor mAccountPrefEditor;


    public AccountController(Context context) {
        this.mAppContext = context;
        mGson = new Gson();
        mAccountPref = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        mAccountPrefEditor = mAccountPref.edit();
    }

    public static synchronized AccountController getInstance(Context appContext) {
        if (mInstance == null) {
            mInstance = new AccountController(appContext.getApplicationContext());
        }
        return mInstance;
    }

    public void updateAccount() {
        RESTInterface.getInstance(mAppContext).updateAccount(getJSON());
    }

    public void createAccount(Account account) {
        setAccount(account);
        RESTInterface.getInstance(mAppContext).createAccount(getJSON());
    }

    public void saveAccount(Account account) {
        setAccount(account);
        RESTInterface.getInstance(mAppContext).updateAccount(getJSON());
    }

    public JSONObject formatDataJSON(Float data, String dataType) {
        SensorData sensorData = new SensorData(getAccount().getmId(), data, dataType);
        JSONObject dataJSON = null;
        try {
            dataJSON = new JSONObject(mGson.toJson(sensorData));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return dataJSON;
    }

    public Account getAccount() {
        return mGson.fromJson(mAccountPref.getString(PreferenceKey.PREF_SAVED_ACCOUNT.toString(), ""), Account.class);
    }

    public String getGSON() {
        return mAccountPref.getString(PreferenceKey.PREF_SAVED_ACCOUNT.toString(), "");
    }

    private JSONObject getJSON() {
        JSONObject JSONaccount = null;
        try {
            JSONaccount = new JSONObject(mAccountPref.getString(PreferenceKey.PREF_SAVED_ACCOUNT.toString(), ""));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return JSONaccount;
    }

    public void setAccount(Account account) {
        mAccountPrefEditor.putString(PreferenceKey.PREF_SAVED_ACCOUNT.toString(), mGson.toJson(account)).apply();
    }
}
