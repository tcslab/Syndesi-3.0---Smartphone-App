package tcslab.syndesiapp.controllers.network;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import tcslab.syndesiapp.R;
import tcslab.syndesiapp.controllers.account.AccountController;
import tcslab.syndesiapp.tools.NodeCallback;
import tcslab.syndesiapp.controllers.sensor.SensorList;
import tcslab.syndesiapp.models.NodeDevice;
import tcslab.syndesiapp.models.NodeType;
import tcslab.syndesiapp.models.PreferenceKey;
import tcslab.syndesiapp.views.NodesControllerActivity;

import java.util.ArrayList;

/**
 * Implements a REST service in a singleton class to send data to the Syndesi server.
 *
 * Created by Blaise on 04.05.2015.
 */
public class RESTInterfaceSyndesiOld extends RESTInterface {
    private static RESTInterfaceSyndesiOld mInstance;
    private Context mAppContext;
    private RequestQueue mRequestQueue;
    private AccountController mAccountController;

    public RESTInterfaceSyndesiOld(Context appContext) {
        this.mAppContext = appContext;
        mRequestQueue = getRequestQueue();
        mAccountController = AccountController.getInstance(mAppContext);
    }

    public static synchronized RESTInterfaceSyndesiOld getInstance(Context appContext) {
        if (mInstance == null) {
            mInstance = new RESTInterfaceSyndesiOld(appContext.getApplicationContext());
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mAppContext.getApplicationContext());
        }
        return mRequestQueue;
    }

    /**
     * Sends data to the server
     *
     * @param data     the data to send
     * @param dataType the type of sensor used to collect the data
     */
    public void sendData(Float data, int dataType) {
        String server_url = PreferenceManager.getDefaultSharedPreferences(mAppContext).getString(PreferenceKey.PREF_SYNDESI_URL.toString(), "");

        if (!server_url.equals("")) {
            // Instantiate the RequestQueue.
            if (server_url.length() > 7 && !server_url.substring(0, 7).equals("http://")) {
                server_url = "http://" + server_url;
            }

            final String url = server_url + "/ero2proxy/crowddata";

            if(mAccountController.getAccount() != null) {
                JSONObject dataJSON = mAccountController.formatDataJSON(data, SensorList.getStringType(dataType));

                // Request a string response from the provided URL.
                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, dataJSON,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                Log.d("HTTP", response.toString());
                                //Send broadcast to update the UI if the app is active
                                RESTInterface.sendServerStatusBcast(mAppContext, response.toString());
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("HTTP", "Error connecting to server address " + url);
                        //Send broadcast to update the UI if the app is active
                        RESTInterface.sendServerStatusBcast(mAppContext, mAppContext.getString(R.string.connection_error) + ": " + url);
                    }
                });

                mRequestQueue.add(request);
            }else{
                Log.e("Account", "No account set");
            }
        } else {
            RESTInterface.sendServerStatusBcast(mAppContext, mAppContext.getString(R.string.connection_no_server_set));
        }
    }

    /**
     * Get all the nodes registered on the server
     */
    public void fetchNodes(final NodeCallback callback) {
        // Get the sever address from the preferences
        String server_url = PreferenceManager.getDefaultSharedPreferences(mAppContext).getString(PreferenceKey.PREF_SYNDESI_URL.toString(), "");

        if (!server_url.equals("")) {

            // Instantiate the RequestQueue.
            if (server_url.length() > 7 && !server_url.substring(0, 7).equals("http://")) {
                server_url = "http://" + server_url;
            }

            final String url = server_url + "/ero2proxy/service";

            StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d("HTTP", response);

                    try {
                        // Convert the string response to JSON
                        JSONObject jsonResponse = new JSONObject(response);

                        // Get all the nodes
                        JSONArray nl = jsonResponse.getJSONArray("services");

                        //Store the nodes
                        ArrayList<NodeDevice> nodesList = new ArrayList<>();

                        for (int i = 0; i < nl.length(); i++) {
                            JSONObject ns = nl.getJSONObject(i);
                            JSONArray nr = ns.getJSONArray("resources");
                            JSONObject n = nr.getJSONObject(2);

                            // Add the node to the UI
                            String NID = n.getString("node_id");
                            String device = n.getJSONObject("resourcesnode").getString("name");
                            NodeType nodeType = NodeType.getType(device);
                            NodeDevice newNode = new NodeDevice(NID, nodeType, nodeType.getStatus(device), NID, n.getJSONObject("resourcesnode").getString("path"));
                            nodesList.add(newNode);
                        }

                        //Send nodes to the caller
                        callback.addNodesCallback(nodesList);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    //Update the UI with the error message
                    Log.d("HTTP", "Error connecting to server address " + url);
                    RESTInterface.sendControllerStatusBcast(mAppContext, mAppContext.getString(R.string.connection_error) + ": " + url);
                }
            });

            mRequestQueue.add(request);

        } else {
            RESTInterface.sendControllerStatusBcast(mAppContext, mAppContext.getString(R.string.connection_no_server_set));
        }
    }

    /**
     * Toggle the node given in attribute
     */
    public void toggleNode(final NodeDevice node, final NodeCallback callback) {
        // Get the sever address from the preferences
        String server_url = PreferenceManager.getDefaultSharedPreferences(mAppContext).getString(PreferenceKey.PREF_SYNDESI_URL.toString(), "");

        if (!server_url.equals("")) {
            // Instantiate the RequestQueue.
            if (server_url.length() > 7 && !server_url.substring(0, 7).equals("http://")) {
                server_url = "http://" + server_url;
            }

            final String url = server_url + node.getPath().replace("monitor", "mediate") + "&resource=" + node.getmType() + "&status=" + node.getmType().getToggleStatus(node.getmStatus());
            Log.d("URL", url);

            StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d("SYNDESI", response);

                    if (response.equals("ERROR")) {
                        RESTInterface.sendControllerStatusBcast(mAppContext, "Error toggling the state of the node");
                    } else {
                        ((NodesControllerActivity) mAppContext).addNode(new NodeDevice(node.getmNID(), node.getmType(), NodeType.parseResponse(response), node.getmOffice(), node.getPath()));
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    //Update the UI with the error message
                    Log.e("HTTP", "Error connecting to server address " + url);
                    RESTInterface.sendControllerStatusBcast(mAppContext, mAppContext.getString(R.string.connection_error) + ": " + url);
                }
            });

            mRequestQueue.add(request);
        }
    }

    /**
     * Creates the current user account on the server
     *
     * @param account the account to create
     */
    public void createAccount(JSONObject account) {
        String server_url = PreferenceManager.getDefaultSharedPreferences(mAppContext).getString(PreferenceKey.PREF_SYNDESI_URL.toString(), "");

        if (!server_url.equals("")) {
            // Instantiate the RequestQueue.
            if (server_url.length() > 7 && !server_url.substring(0, 7).equals("http://")) {
                server_url = "http://" + server_url;
            }

            final String url = server_url + "/ero2proxy/crowdusers";

            //Initiate the JSON request
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, account,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d("HTTP", response.toString());
                            RESTInterface.sendServerStatusBcast(mAppContext, response.toString());
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("HTTP", "Error connecting to server " + url);
                    RESTInterface.sendServerStatusBcast(mAppContext, mAppContext.getString(R.string.connection_error) + ": " + url);
                }
            });

            mRequestQueue.add(request);
        } else {
            RESTInterface.sendServerStatusBcast(mAppContext, mAppContext.getString(R.string.connection_no_server_set));
        }
    }

    /**
     * Updates the current account on the server
     *
     * @param account the account to update
     */
    public void updateAccount(JSONObject account) {
        String server_url = PreferenceManager.getDefaultSharedPreferences(mAppContext).getString(PreferenceKey.PREF_SYNDESI_URL.toString(), "");

        if (!server_url.equals("")) {
            // Instantiate the RequestQueue.
            if (server_url.length() > 7 && !server_url.substring(0, 7).equals("http://")) {
                server_url = "http://" + server_url;
            }

            final String url = server_url + "/ero2proxy/crowdusers";

            //Initiate the JSON request
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, account,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d("HTTP", response.toString());
                            RESTInterface.sendServerStatusBcast(mAppContext, response.toString());
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("HTTP", "Error connecting to server " + url);
                    RESTInterface.sendServerStatusBcast(mAppContext, mAppContext.getString(R.string.connection_error) + ": " + url);
                }
            });

            mRequestQueue.add(request);
        } else {
            RESTInterface.sendServerStatusBcast(mAppContext, mAppContext.getString(R.string.connection_no_server_set));
        }
    }

    public void setmAppContext(Context appContext){
        this.mAppContext = appContext;
    }
}
