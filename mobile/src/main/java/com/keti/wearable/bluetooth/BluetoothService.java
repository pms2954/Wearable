package com.keti.wearable.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;

import com.keti.wearable.activity.MainActivity;
import com.keti.wearable.definition.BTSERV;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by ppmmss2229 on 2015-07-07.
 */
public class BluetoothService {
    // Debugging
    private static final String TAG ="BluetoothService";
    private static boolean D = true;

    // Name for the SDP record with creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private static final UUID MY_UUID_SPP_SECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private findSupportingDeive mFindSupportDevice;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    /**
     * Constructor
     */
    public BluetoothService(Context context, Handler handler){
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     */
    public synchronized void setState(int state){
        if(D) Log.d(TAG, "setState()" + mState + "->" + state);
        mState=state;
        // Give the new state to the Handler so the UI Activity can upate
        mHandler.obtainMessage(BTSERV.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current conection state
     */
    public synchronized int getState(){ return mState; }

    /**
     *  Start the chat service. specially start AcceptThread to begin a
     *  session in listening (server) mode. Called by the Activity onResume() .
     *  it is meaning that made phone's blooth to set slave module
     */
    public synchronized void start(){
        if(D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if(mConnectThread != null){mConnectThread.cancel(); mConnectThread = null;}
        if(mConnectedThread != null){mConnectedThread.cancel(); mConnectedThread = null;}

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if(mSecureAcceptThread == null){
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }

        if(mInsecureAcceptThread == null){
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
    }

    /**
     * Start the connectThread to initiate to connection to a remote device.
     * it means that made phone's bluetooth to set master mode.
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security Type - Secure(true), InSecure(false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure){

        if(D) Log.d(TAG, "connected to" + device);

        //Cancel any thread attempting to make a connection
        if(mState == STATE_CONNECTING){
            if(mConnectThread != null){mConnectThread.cancel(); mConnectThread = null;}
        }

        if(mConnectedThread != null){mConnectedThread.cancel(); mConnectedThread = null;}


        // If we're having uuid supporting bluetooth services
        mFindSupportDevice = new findSupportingDeive(device);

        if(mFindSupportDevice.isSupport()) {
            mConnectThread = new ConnectThread(device, secure, mFindSupportDevice.getUuid());
            mConnectThread.start();
            setState(STATE_CONNECTING);
        }
        else{
            connectionFailed();
        }

    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket The BluetoothSocket on which the connection made
     * @param device the BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device ,
                                       final String socketType){
        if(D) Log.d(TAG, "connected, Socket Type: " + socketType);

        //Cancel the thread that completed the connection
        if(mConnectThread != null){mConnectThread.cancel(); mConnectThread = null;}
        if(mConnectedThread != null){mConnectedThread.cancel(); mConnectedThread = null;}

        //Cancel the accept thread becauset we only want to connect to one device
        if(mSecureAcceptThread != null){ mSecureAcceptThread.cancel(); mSecureAcceptThread = null;}
        if(mInsecureAcceptThread != null){ mInsecureAcceptThread.cancel(); mInsecureAcceptThread = null;}

        //Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        //Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(BTSERV.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(BTSERV.DEVICE_PATH, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop(){
        if(D) Log.d(TAG, "stop");

        //Cancel the thread that completed the connection
        if(mConnectThread != null){mConnectThread.cancel(); mConnectThread = null;}
        if(mConnectedThread != null){mConnectedThread.cancel(); mConnectedThread = null;}

        //Cancel the accept thread becauset we only want to connect to one device
        if(mSecureAcceptThread != null){ mSecureAcceptThread.cancel(); mSecureAcceptThread = null;}
        if(mInsecureAcceptThread != null){ mInsecureAcceptThread.cancel(); mInsecureAcceptThread = null;}

        setState(STATE_NONE);
    }

    /**
     * Write to the Connected in an unsynchronized manner
     * @param  out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out){
        //Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectThread
        synchronized(this){
            if(mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity
     */
    private void connectionFailed(){
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BTSERV.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BTSERV.TOAST_PATH, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        //Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity
     */
    private void connectionLost(){
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BTSERV.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BTSERV.TOAST_PATH, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It run until a connection is accepted
     * (or untill cancelled)
     */
    private class AcceptThread extends Thread{
        //The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure){
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure":"Insecure";

            //Create a new listening server socket
            try{
                if(secure){
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
                }
                else{
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_INSECURE,
                            MY_UUID_INSECURE);
                }
            }catch(IOException e){
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed ", e);
            }
            mmServerSocket = tmp;
        }

        public void run(){
            if(D) Log.d(TAG, "SocketType:" + mSocketType + "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            //Listen to the server socket if we're not connected
            while(mState != STATE_CONNECTED){
                try{
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                }catch (IOException e){
                    Log.e(TAG, "Socket Type:" + mSocketType + "accept() failed", e);
                    break;
                }

                //If a connection was accepted
                if(socket != null){
                    synchronized (BluetoothService.this){
                        switch(mState){
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                //Situation normal. Start the connected thread
                                connected(socket, socket.getRemoteDevice(), mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try{
                                    socket.close();
                                }catch(IOException e){
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            if(D) Log.i(TAG, "End mAcceptThread , SocketTpye :"+ mSocketType);
        }

        public void cancel(){
            if(D) Log.d(TAG, "Socket Type" + mSocketType + "cancel" + this);
            try{
                mmServerSocket.close();
            }catch(IOException e){
                Log.e(TAG, "Socket Type"+ mSocketType + "close() of server failed", e);
            }
        }
    }

    /**
     *  This find out the device is supported .
     *  there is many things performing bluetooth communication service.
     *  so this class find out the uuid service in device , then satisfiy to match
     *  this service.
     */
    private class findSupportingDeive {

        // device
        private BluetoothDevice bDevice;
        // active uuid service
        private UUID activeUuid;
        // confirm the permission using bluetooth module
        private boolean isAcess;

        public findSupportingDeive(BluetoothDevice device){

            init(device);

        }

        public void init(BluetoothDevice device){

            bDevice = device;
            // allocate the uuid service
            isAcess = findService();

        }
        /**
         * getter uuid
         * @return  UUID
         */
        public UUID getUuid(){
            return activeUuid;
        }

        /**
         * getter is Acess
         * @return boolean
         */
        public boolean isSupport(){
            return isAcess;
        }

        /**
         * find the proper UUID Service to communicate bluetooth (i.e) HC - 06
         * TODO change support uuid variable structure into the hashTable
         */
        private boolean findService(){
            ParcelUuid[] deviceUuids = bDevice.getUuids();

            if (deviceUuids == null){
                // allocate default uuid Service
                activeUuid = MY_UUID_SPP_SECURE;
                return true;
            }

            for(ParcelUuid uuid : deviceUuids){

                if((uuid.getUuid()).equals(MY_UUID_SPP_SECURE)) {
                    Log.d(TAG, "SUPPORT UUID:" + uuid.getUuid());
                    activeUuid = uuid.getUuid();
                    return true;
                }
                else if((uuid.getUuid()).equals(MY_UUID_SECURE)) {
                    Log.d(TAG, "SUPPORT UUID:" + uuid.getUuid());
                    activeUuid = uuid.getUuid();
                    return true;

                }
            }
            return false;
        }

    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device . It runs straight through; the connection either
     * succeeds or fails
     */
    private class ConnectThread extends Thread{

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mmSokcetType;

        public ConnectThread(BluetoothDevice device , boolean secure, UUID activeUuid){
            mmDevice = device;
            BluetoothSocket tmp = null;
            mmSokcetType = secure? "Secure":"Insecure";

            // Get a BluetoothSocket for a connection with
            // given BluetoothDevice
            try{
                if(secure){
                    tmp = device.createRfcommSocketToServiceRecord(activeUuid);
                }
                else{
                    tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                }
            }catch(IOException e){
                Log.e(TAG, "SokcetType:" + mmSokcetType +"create() failed", e);
            }

            mmSocket = tmp;
        }

        public void run(){
            Log.i(TAG, "BEGIN mConnectThread socketType:" + mmSokcetType);
            setName("ConnectThread" + mmSokcetType);

            //Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            //Make a connection to the BluetoothSocket
            try{
                // This is a blocking call and will only return on a
                // sucessful connection or an exception
                mmSocket.connect();
            }catch(IOException e){
                //close the socket
                try{
                    mmSocket.close();
                }catch(IOException cloE){
                    Log.e(TAG, "unable to close()" + mmSokcetType +
                            " socket during connection failure", cloE);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized(BluetoothService.this){
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mmSokcetType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mmSokcetType + " socket failed", e);
            }
        }

    }

    /**
     * This thread runs during a connection with a device.
     * It handles all incoming and outgoing transmission.
     */
    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType){
            Log.d(TAG, "create ConnectedThread" + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try{
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }catch(IOException e){
                Log.e(TAG, "temp socket not created",e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            byte ch;

            // Keep listening to the InputStream while connected
            while(true){
                try{
                    int bytes = 0;

                    //Read from the InputStream
                    while((ch = (byte)mmInStream.read()) != '\n') {
                        buffer[bytes++] = ch;
                    }
                    buffer[bytes] = '\0';

                    //Send
                    mHandler.obtainMessage(BTSERV.MESSAGE_READ, bytes , -1 , buffer)
                            .sendToTarget();

                }catch(IOException e){
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    //Start the service over to restart listening mode
                    BluetoothService.this.start();
                    break;
                }
            }
        }

        public void write(byte[] buffer){
            try{
                mmOutStream.write(buffer);

                //Share the sent message back to the UI Activity
                mHandler.obtainMessage(BTSERV.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            }catch(IOException e){
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel(){
            try{
                mmSocket.close();
            }catch(IOException e){
                Log.e(TAG, "close() of connection socket failed", e);
            }
        }
    }
}
