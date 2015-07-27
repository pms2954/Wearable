package com.keti.wearable.definition;

/**
 * Created by ppmmss2229 on 2015-07-27.
 */
public class BTSERV {

    public static final int TYPE = 1;

    // Intent request codes
    public static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    public static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    public static final int REQUEST_ENABLE_BT = 3;

    // Message types sent from the Bluetooth Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key name received from the BluetoothService Handler
    public static final String DEVICE_PATH = "/DEVICE_NAME";
    public static final String TOAST_PATH = "/TOAST";
}
