package com.keti.wearable.googleService;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by ppmmss2229 on 2015-07-27.
 * Class to encapsulate connecting apps to wearable devices
 * and sending messags back and forth.
 */
public class WearableConnector {

    private static final String TAG = "WearableConncetor";
    private GoogleApiClient mGoogleApiclient;

    /**
     *  Constructor
     *  @param ctx Context
     *  @param connectionCallbacks add connection callbacks
     *  @param OnConnectionFailedListener connection failure callback
     */
    public WearableConnector(Context ctx,
                             GoogleApiClient.ConnectionCallbacks connectionCallbacks,
                             GoogleApiClient.OnConnectionFailedListener connectionFailedListener){

        mGoogleApiclient = new GoogleApiClient.Builder(ctx)
                    .addConnectionCallbacks(connectionCallbacks)
                    .addOnConnectionFailedListener(connectionFailedListener)
                    .addApi(Wearable.API)
                    .build();
    }


    /**
     * Connect mobile app & wearable app to one another
     */
    public void connect(){
        if(!mGoogleApiclient.isConnected()){
            mGoogleApiclient.connect();
        }
    }

    /**
     * Disconnect moblie app & wearable app.
     */
    public void disconnect(){
        if(mGoogleApiclient.isConnected()) {
            mGoogleApiclient.disconnect();
        }
    }

    /**
     *  Add a message listener
     *  @param listener MessageApi.MessageListener object
     */
    public void addListener(MessageApi.MessageListener listener){
        if(mGoogleApiclient.isConnected()){
            Wearable.MessageApi.addListener(mGoogleApiclient, listener);
        }
    }


    /**
     *  Remove a message listener
     *  @param listener MessageApi.MessageListener object
     */
    public void removeListener(MessageApi.MessageListener listener){
        if(mGoogleApiclient.isConnected()){
            Wearable.MessageApi.removeListener(mGoogleApiclient, listener);
        }
    }

    /**
     * Send a message to connected devices
     * @param path Message path
     * @param message Message to send
     */

    public void sendMessage(String path, String message){
        if(mGoogleApiclient.isConnected()){
            Log.i(TAG, "Google Api is conneted");
            new WearableMessageSender(path, message, mGoogleApiclient).start();
        }
        else{
            Log.w(TAG , "Attempted to send message when not connected to google Api Client");
        }
    }


}
