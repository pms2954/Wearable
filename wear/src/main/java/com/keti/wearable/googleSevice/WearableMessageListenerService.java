package com.keti.wearable.googleSevice;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;
import com.keti.wearable.definiton.GGSERV;

/**
 * Created by ppmmss2229 on 2015-07-27.
 *
 * WearableMessageListenerService listens for message
 * and issues local broadcasts containing the new message data
 */

public class WearableMessageListenerService extends WearableListenerService {

    private static final String TAG = "WearableMessageListenerService";

    /**
     * is performing when paired with wearable device
     * @param node
     */
    @Override // NodeApi.NodeListener
    public void onPeerConnected(Node node) {
        Toast.makeText(getApplication(), "Peer Connected", Toast.LENGTH_SHORT).show();
    }

    /**
     * is performing when paried device is lost
     * @param node
     */
    @Override // NodeApi.NodeListener
    public void onPeerDisconnected(Node node) {
        Toast.makeText(getApplication(), "Peer Disconnected", Toast.LENGTH_SHORT).show();
    }

    /**
     * is performing when data which is made of text is arrived
     * @param messageEvent
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, " onMessageRecevied : incoming message...." );

        if (messageEvent.getPath().equals(GGSERV.MESSAGE_PATH)) {
            final String msg = new String(messageEvent.getData());
            Log.d(TAG, "onMessageRecevied : PATH ->" + messageEvent.getPath());
            Log.d(TAG, "onMessageRecevied : Message received ->" + msg);

            //broad cast this message to other app
            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra(GGSERV.INTENT_PATH, msg);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
            Log.d(TAG, "onMessageRecevied : Message send with braodcasting ->" + msg);
        }
    }

    /**
     * performing when arrived data is changed compread prior data
     * @param dataEvents
     */
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

    }
}

