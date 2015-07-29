package com.keti.wearable.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.keti.wearable.definiton.GGSERV;
import com.keti.wearable.googleSevice.WearableConnector;

import java.util.ArrayList;

public class MyActivity extends Activity{

    private final String TAG = "MyActivity";

    //UI
    private TextView mTextView;

    //google Service
    private WearableConnector mWearableConn;
    private BroadcastReceiver mLocalMessageReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        initWearableConn();
        initUI();

    }

    @Override // Activity
    protected void onStart() {
        Log.e(TAG, "++ ON START ++");
        super.onStart();

        mWearableConn.connect();
    }

    @Override // Activity
    protected void onStop() {
        Log.e(TAG, "-- ON STOP --");
        super.onStop();

        mWearableConn.disconnect();
    }

    /**
     * intialize the wearableConnection
     */
    public void initWearableConn(){

        //GoogleApiClient Listener
        GoogleApiClient.OnConnectionFailedListener wearableConnectionFailedListener =
                new GoogleApiClient.OnConnectionFailedListener(){
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed :" + result);
                    }

        };

        GoogleApiClient.ConnectionCallbacks wearableConnetionCallbacks =
                new GoogleApiClient.ConnectionCallbacks(){
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG , "-- Connected Google Service --");
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG , "-- Suspended Google Service --");
                    }
        };

        //make the GoogleApiClient instance
        mWearableConn = new WearableConnector(this, wearableConnetionCallbacks,
                wearableConnectionFailedListener );

        //Regitster a local broadcast receiver to handle message that
        //have been received by the wearable message listening service.
        mLocalMessageReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                String msg = intent.getStringExtra(GGSERV.INTENT_PATH);
                Log.d(TAG, "onReceivce : broadcast Message received -> " + msg);
                //set the text
                mTextView.setText(msg);
            }
        };

        IntentFilter msgFilter = new IntentFilter(Intent.ACTION_SEND);
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalMessageReceiver, msgFilter);

    }

    public void initUI(){
        mTextView = (TextView)findViewById(R.id.sensorInfo);
    }



}
