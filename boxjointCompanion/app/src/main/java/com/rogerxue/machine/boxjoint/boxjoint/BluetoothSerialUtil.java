package com.rogerxue.machine.boxjoint.boxjoint;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothSerialUtil implements Serializable {
    private static final String TAG = "BluetoothSerialUtil";
    private static final UUID SERIAL_UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String ASCII_ENCODE = "US-ASCII";

    private static BluetoothSerialUtil mInstance;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private OutputStream mmOutputStream;
    private InputStream mmInputStream;
    private Thread workerThread;
    private byte[] readBuffer;
    private int readBufferPosition;
    private volatile boolean stopWorker;

    private boolean connected = false;

    private final List<BtSerialListener> mListeners = new ArrayList<>();


    public interface BtSerialListener {
        void onConnect(boolean connected);
        void onDataRead(String data);
    }

    public static BluetoothSerialUtil getInstance() {
        if (mInstance == null) {
            mInstance = new BluetoothSerialUtil();
        }
        return mInstance;
    }

    private BluetoothSerialUtil() {
    }

    public void addListener(BtSerialListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(BtSerialListener listener) {
        mListeners.remove(listener);
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * @return true if connected
     */
    public boolean connect(String btDeviceName) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null) {
            Log.w(TAG, "No bluetooth adapter available");
            for (BtSerialListener listener : mListeners) {
                listener.onConnect(false);
            }
            connected = false;
            return isConnected();
        }

        if(!mBluetoothAdapter.isEnabled()) {
            Log.w(TAG, "BT not enabled");
            for (BtSerialListener listener : mListeners) {
                listener.onConnect(false);
            }
            connected = false;
            return isConnected();
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0) {
            for(BluetoothDevice device : pairedDevices) {
                if(device.getName().equals(btDeviceName)) {
                    mmDevice = device;
                    Log.d(TAG, "Bluetooth Device Found " + btDeviceName);
                    try {
                        mmSocket = mmDevice.createRfcommSocketToServiceRecord(SERIAL_UUID);
                        mmSocket.connect();
                        mmOutputStream = mmSocket.getOutputStream();
                        mmInputStream = mmSocket.getInputStream();
                    } catch (IOException e) {
                        Log.e(TAG, "unable to connect to bt.", e);
                        connected = false;
                        return isConnected();
                    }
                    for (BtSerialListener listener : mListeners) {
                        listener.onConnect(true);
                    }
                    connected = true;
                    listenForData();
                    return isConnected();
                }
            }
            Log.d(TAG, "Bluetooth Device NOT Found " + btDeviceName);
            for (BtSerialListener listener : mListeners) {
                listener.onConnect(false);
            }
            connected = true;
            return isConnected();
        }
        return isConnected();
    }

    private void listenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
                while(!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if(b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, ASCII_ENCODE);
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        public void run() {
                                        for (BtSerialListener listener : mListeners) {
                                            listener.onDataRead(data);
                                        }
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    public boolean sendData(String data) {
        if (!isConnected()) {
            Log.d(TAG, "not connected");
            return false;
        }
        data += "\n";
        try {
            mmOutputStream.write(data.getBytes());
        } catch (IOException e) {
            Log.d(TAG, "can't send data." + e);
            return false;
        }
        Log.d(TAG, "Data Sent");
        return true;
    }

    public void stop() {
        try {
            if (mmOutputStream != null) {
                mmOutputStream.close();
            }
            if (mmInputStream != null) {
                mmInputStream.close();
            }
            if (mmSocket != null) {
                mmSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "can't close bt properly." + e);
        }
    }
}