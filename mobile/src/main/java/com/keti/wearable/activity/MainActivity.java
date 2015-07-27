package com.keti.wearable.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.keti.wearable.bluetooth.BluetoothService;
import com.keti.wearable.definition.BTSERV;
import com.keti.wearable.definition.GGSERV;
import com.keti.wearable.googleService.WearableConnector;


public class MainActivity extends ActionBarActivity {

    // Debugging
    private static final String TAG = "BluetoothCommunication";

    // Layout Views
    private ListView mConversationView = null;
    private EditText mOutEditText = null;
    private Button mSendButton = null;

    // BluetoothService
    private BluetoothAdapter mBluetoothAdapter = null;      // Local Blutooth adapte
    private BluetoothService mBluetoothService = null;      // Member object for the chat Services
    private String mConnectedDeviceName = null;             // Name of  the connected device
    private ArrayAdapter<String> mConversationArrayAdapter  // Array adapter for the conversation thread
                                        = null;
    private StringBuffer mOutStringBuffer = null;           // String buffer for outgoing message

    // Google Api Service for communicating Watch
    private WearableConnector mWearableConn;
    private BroadcastReceiver mLocalMessageReceiver;


    private void setStatus(int resId){
        //final ActionBar actionBar = getActionBar();
        //actionBar.setSubtitle(resId);
    }

    private void setStatus(CharSequence subTitle) {
        //final ActionBar actionBar = getActionBar();
        //actionBar.setSubtitle(subTitle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if(mBluetoothAdapter == null){
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initWearableConn();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.e(TAG, "++ ON START ++");

        // If BT is not on , the request that it be enabled.
        // setupChat() will then be called during onActivityResult

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, BTSERV.REQUEST_ENABLE_BT);
        } else {
            if (mBluetoothService == null) setupChat();
        }

        mWearableConn.connect();
    }

    @Override
    public synchronized void onResume(){
        Log.e(TAG, "+ ON RESUME +");
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // on Resume() will be called when ACTION_REQUEST_ENABLE activity  returns.
        if(mBluetoothService != null){
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if(mBluetoothService.getState() == BluetoothService.STATE_NONE){
                mBluetoothService.start();
            }
        }

    }

    @Override
    public synchronized void onPause(){
        Log.e(TAG, "- ON PAUSE -");
        super.onPause();
    }

    @Override
    public void onStop(){
        Log.e(TAG, "-- ON STOP --");
        super.onStop();

        // disconnect the google service
        mWearableConn.disconnect();
    }

    @Override
    public void onDestroy(){
        Log.e(TAG, "--- ON DESTROY ---");
        super.onDestroy();

        //stop the bluetooth chat services
        if(mBluetoothService != null) mBluetoothService.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;

        switch(item.getItemId()){
            case R.id.secure_connect_scan:
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, BTSERV.REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            case R.id.insecure_connect_scan:
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, BTSERV.REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            case R.id.discoverable:
                ensureDiscoverable();
                return true;
        }
        return false;

    }

    /**
     * Move to device list
     */
    private void ensureDiscoverable(){
        Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){

            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }


    /**
     *  Intialize the chat program
     */
    private void setupChat(){
        Log.d(TAG , "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView)findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText)findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        //Initialize the send button with a listener that for click events
        mSendButton = (Button)findViewById(R.id.button_send);

        mSendButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                //Send a message using content of the edit text widget
                TextView view = (TextView)findViewById(R.id.edit_text_out);
                String msg = view.getText().toString();
                mWearableConn.sendMessage(GGSERV.MESSAGE_PATH, msg);
            }
        });

        //Intialize the BluetoothService to perform bluetooth connections
        mBluetoothService = new BluetoothService(this, mHandler);

        //Intialize the buffer for outgoing message
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * intialize the wearableConnection
     */
    public void initWearableConn(){

        //GoogleApuClient Listener
        GoogleApiClient.OnConnectionFailedListener wearableConnectionFailedListener =
                new GoogleApiClient.OnConnectionFailedListener() {

                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                };

        //GoogleApuClient Listener
        GoogleApiClient.ConnectionCallbacks connectionCallbacks =
                new GoogleApiClient.ConnectionCallbacks(){
                    @Override
                    public void onConnected(Bundle connectionHint){
                        Log.d(TAG , "-- Connected Google Service --");
                    }

                    @Override
                    public void onConnectionSuspended(int cause){
                        Log.d(TAG , "-- Suspended Google Service --");
                    }
                };

        //make the GoogleApiClient instance
        mWearableConn = new WearableConnector(this, connectionCallbacks,
                wearableConnectionFailedListener);


        //Regitster a local broadcast receiver to handle message that
        //have been received by the wearable message listening service.
        mLocalMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getStringExtra(GGSERV.INTENT_PATH);
                Log.d(TAG, "onReceivce : broadcast Message received -> " + message);
            }
        };

    }

    /**
     *  Call back Method for key Event
     */
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener(){
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event){
                    //If the action is a key-up event on the return key , send the message
                    if(actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP){
                        String msg = view.getText().toString();
                        sndMessage(msg, GGSERV.TYPE);
                    }
                    Log.i(TAG, "END onEditorAction");
                    return true;
                }
    };

    /**
     *  Result fot Activity
     *  @param requestCode  request for bluetooth
     *  @param resultCode result from bluetooth
     *  @param data result data Map from bluetooth
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        Log.d(TAG, "onActivityResult " + resultCode);
        switch(requestCode){
            case BTSERV.REQUEST_CONNECT_DEVICE_SECURE :
                if(resultCode == Activity.RESULT_OK){
                    connectDevice(data, true);
                }
                break;
            case BTSERV.REQUEST_CONNECT_DEVICE_INSECURE :
                if(resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case BTSERV.REQUEST_ENABLE_BT :
                if(resultCode == Activity.RESULT_OK){
                    setupChat();
                }
                else{
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    /**
     * Connect Device using bluetooth
     * @param data bluetooth Info in Server (MAC address)
     * @param secure choice the secure
     */
    private void connectDevice(Intent data, boolean secure){
        Log.d(TAG, "connectDevice()");
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to device
        mBluetoothService.connect(device, secure);
    }

    /**
     *  The Handler that gets information back from the BluetoothService
     */
    private final Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg){
            switch(msg.what) {
                case BTSERV.MESSAGE_STATE_CHANGE:
                    Log.i(TAG, "MESSAGE_STATE_CHANGE : " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                            break;
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case BTSERV.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMsg = new String(writeBuf);
                    mConversationArrayAdapter.add("Me: " + writeMsg);
                    break;
                case BTSERV.MESSAGE_READ:

                    byte[] readBuf = (byte[]) msg.obj;

                    //construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    mConversationArrayAdapter.add(readMessage);
                    sndMessage(readMessage, GGSERV.TYPE);

                    break;
                case BTSERV.MESSAGE_DEVICE_NAME:
                    //save the connected deive's name
                    mConnectedDeviceName = msg.getData().getString(BTSERV.DEVICE_PATH);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case BTSERV.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(BTSERV.TOAST_PATH)
                            , Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    /**
     * choice the type of sending message.
     * @param message  A string of text to send.
     * @param type what type of message
     */
    private void sndMessage(String message , int type){

        if(message.length() <= 0)
            return;

        switch(type){
            case BTSERV.TYPE :
                Log.d(TAG, "sendMessage : bluetooth type ");
                // Check that we're actually connected before trying anything
                if(mBluetoothService.getState() != BluetoothService.STATE_CONNECTED){
                    Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                    return;
                }
                mBluetoothService.write(message.getBytes());
                break;
            case GGSERV.TYPE :
                Log.d(TAG, "sendMessage : googleService type ");
                sndMessage(message, GGSERV.TYPE);
                break;
            default :
                Log.d(TAG, "sendMessage : none type");
        }

        mOutStringBuffer.setLength(0);
        mOutEditText.setText(mOutStringBuffer);
    }


}
