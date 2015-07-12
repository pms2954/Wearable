package contents.keti.org.wearable;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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


public class MainActivity extends ActionBarActivity{

    // Debugging
    private static final String TAG = "BluetoothCommunication";
    private static final boolean D = true;

    // Buffer
    private static StringBuilder rBuffer;

    // Message types sent from the Bluetooth Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key name received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;

    // Name of  the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing message
    private StringBuffer mOutStringBuffer;
    // Local Blutooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat Services
    private BluetoothChatService mChatService = null;

    private final void setStatus(int resId){
        //final ActionBar actionBar = getActionBar();
        //actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        //final ActionBar actionBar = getActionBar();
        //actionBar.setSubtitle(subTitle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the read buffer
        rBuffer = new StringBuilder();

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if(mBluetoothAdapter == null){
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (D) Log.e(TAG, "++ ON START ++");

        // If BT is not on , the request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume(){
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // on Resume() will be called when ACTION_REQUEST_ENABLE activity  returns.
        if(mChatService != null){
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if(mChatService.getState() == BluetoothChatService.STATE_NONE){
                mChatService.start();
            }
        }
    }

    @Override
    public synchronized void onPause(){
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop(){
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        //stop the bluetooth chat services
        if(mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
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
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            case R.id.insecure_connect_scan:
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
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
        if(D) Log.d(TAG, "ensure discoverable");
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
                sendMessage(msg);
            }
        });

        //Intialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        //Intialize the buffer for outgoing message
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message){
        if(D) Log.d(TAG, "sendMessage()");
        // Check that we're actually connected before trying anything
        if(mChatService.getState() != BluetoothChatService.STATE_CONNECTED){
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        if(message.length() > 0){
            // Get the msg bytes and tell the BluetoothService to write
            byte[] snd = message.getBytes();
            mChatService.write(snd);

            // Reset out String buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     *  Result fot Activity
     *  @param requestCode  request for bluetooth
     *  @param resultCode result from bluetooth
     *  @param data result data Map from bluetooth
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch(requestCode){
            case REQUEST_CONNECT_DEVICE_SECURE :
                if(resultCode == Activity.RESULT_OK){
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE :
                if(resultCode == Activity.RESULT_OK){
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT :
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
        if(D) Log.d(TAG, "connectDevice()");
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to device
        mChatService.connect(device, secure);
    }

    /**
     *  The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler(){

        private final String NEWLINE = "\n";

        @Override
        public void handleMessage(Message msg){
            switch(msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE : " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                            break;
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMsg = new String(writeBuf);
                    mConversationArrayAdapter.add("Me: " + writeMsg);
                    break;
                case MESSAGE_READ:
                    int index = 0;

                    byte[] readBuf = (byte[]) msg.obj;

                    //construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    String allMessage = null;
                    String remains = null;

                    /*
                    rBuffer.append(readMessage);

                    if( readMessage.contains(NEWLINE)) {
                        index = rBuffer.lastIndexOf(NEWLINE);
                        allMessage = rBuffer.substring(0, index);
                        remains = rBuffer.substring( index+1 , rBuffer.length());
                        mConversationArrayAdapter.add(allMessage);
                        rBuffer.setLength(0);
                        rBuffer.append(remains);
                    }
                    */
                    mConversationArrayAdapter.add(readMessage);

                    break;
                case MESSAGE_DEVICE_NAME:
                    //save the connected deive's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST)
                            , Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    /**
     *  Call back Method for key Event
     */
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener(){
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event){
                    //If the action is a key-up event on the return key , send the message
                    if(actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP){
                        String msg = view.getText().toString();
                        sendMessage(msg);
                    }
                    if(D) Log.i(TAG, "END onEditorAction");
                    return true;
                }
    };


}
