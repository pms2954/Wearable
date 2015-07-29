package com.keti.wearable.googleSevice;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by ppmmss2229 on 2015-07-27.
 *
 * WearableMessageSender is responsible for sending messages
 * between apps and wearable devices. It manages this work
 * on its own thread.
 */
public class WearableMessageSender extends Thread{

    private static final String TAG = "WearableMessageSender";
    private String mPath, mMessage;
    private GoogleApiClient mGoogleApiClient;

    /**
     * Constructor to send a message to the data layer
     * @param path Message path
     * @param msg Message contents
     * @param googleApiClient GoogleApiClient object
     */
    public WearableMessageSender(String path, String msg, GoogleApiClient googleApiClient){
        if(path == null || msg == null || googleApiClient == null){
            Log.e(TAG, "Invalid parameter(s) passed to wearableMessageSender");
            throw new IllegalArgumentException("Invalid parameter(s) passed to wearableSender");
        }

        mPath = path;
        mMessage = msg;
        mGoogleApiClient = googleApiClient;
    }

    /**
     * Sending Message using MessageApi
     */
    public void run(){
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi
                .getConnectedNodes(mGoogleApiClient).await();

        for(Node node : nodes.getNodes()){
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient,
                    node.getId(),
                    mPath,
                    mMessage.getBytes()
            ).await();


            if(result.getStatus().isSuccess()){
                Log.d(TAG , "Message :{" + mMessage + "} successfully sent to :"
                    + node.getDisplayName());
            }
            else{
                Log.e(TAG, "Faied to send message to deivce");
            }
        }
    }
}
