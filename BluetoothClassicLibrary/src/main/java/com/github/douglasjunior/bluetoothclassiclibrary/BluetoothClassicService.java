/*
 * Copyright (C) 2009 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.github.douglasjunior.bluetoothclassiclibrary;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Looper;
import android.support.annotation.RequiresPermission;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class does all the work for setting up and managing Bluetooth connections with other devices. It has a thread
 * that listens for incoming connections, a thread for connecting with a device, and a thread for performing data
 * transmissions when connected.
 */
public class BluetoothClassicService extends BluetoothService {

    private static final String TAG = BluetoothClassicService.class.getSimpleName();

    // Unique UUID for this application
    //private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    protected BluetoothClassicService(BluetoothConfiguration config) {
        super(config);
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mStatus = BluetoothStatus.NONE;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param status An integer defining the current connection state
     */
    protected synchronized void updateStatus(final BluetoothStatus status) {
        if (D)
            Log.d(TAG, "updateStatus() " + mStatus + " -> " + status);
        mStatus = status;

        // Give the new state to the Handler so the UI Activity can update
        if (onEventCallback != null)
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    onEventCallback.onStatusChange(status);
                }
            });

    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public synchronized void connect(BluetoothDevice device) {
        if (D)
            Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mStatus == BluetoothStatus.CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        updateState(BluetoothStatus.CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public synchronized void connected(BluetoothSocket socket, final BluetoothDevice device) {
        if (D)
            Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        if (onEventCallback != null)
            runOnMainThread(new Runnable() {

                @RequiresPermission(Manifest.permission.BLUETOOTH)
                @Override
                public void run() {
                    onEventCallback.onDeviceName(device.getName());
                }

            });

        updateState(BluetoothStatus.CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stopService() {
        if (D)
            Log.d(TAG, "stop");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (getStatus() != BluetoothStatus.NONE)
            updateState(BluetoothStatus.NONE);
        if (BluetoothService.mDefaultServiceInstance == this)
            BluetoothService.mDefaultServiceInstance = null;
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see
     */
    public synchronized void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread

        if (mStatus != BluetoothStatus.CONNECTED)
            return;

        r = mConnectedThread;

        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        updateState(BluetoothStatus.NONE);

        // Send a failure message back to the Activity
        if (onEventCallback != null)
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    onEventCallback.onToast("Não foi possível conectar ao dispositivo.");
                }
            });
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        updateState(BluetoothStatus.NONE);

        // Send a failure message back to the Activity

        if (onEventCallback != null)
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    onEventCallback.onToast("Conexão perdida com o dispositivo.");
                }
            });
    }

    /**
     * This thread runs while attempting to make an outgoing connection with a device. It runs straight through; the
     * connection either succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        @RequiresPermission(Manifest.permission.BLUETOOTH)
        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(mConfig.uuid);
            } catch (Exception e) {
                Log.e(TAG, "create() failed", e);
            }
            try {
                AudioManager mAudioManager = (AudioManager) mConfig.context.getSystemService(Context.AUDIO_SERVICE);
                //For phone speaker(loadspeaker)
                mAudioManager.setMode(AudioManager.MODE_NORMAL);
                mAudioManager.setBluetoothScoOn(false);
                mAudioManager.setSpeakerphoneOn(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            mmSocket = tmp;
        }

        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN})
        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            if (mAdapter.isDiscovering())
                mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (Exception e) {
                e.printStackTrace();
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (Exception e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothClassicService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
            try {
                interrupt();
            } catch (Exception e) {
                Log.e(TAG, "interrupt() of Thread failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device. It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private boolean canceled = false;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (Exception e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte temp;
            final byte[] buffer = new byte[mConfig.bufferSize];
            int i = 0;
            byte byteDelimiter = (byte) mConfig.characterDelimiter;
            // Keep listening to the InputStream while connected
            while (!canceled) {
                try {
                    // Read from the InputStream
                    int read = mmInStream.read();
                    temp = (byte) read;

                    if (temp == byteDelimiter) {
                        if (i > 0) {
                            dispatchBuffer(buffer, i);
                            i = 0;
                        }
                        continue;
                    }
                    if (i == buffer.length - 1) {
                        dispatchBuffer(buffer, i);
                        i = 0;
                    }
                    buffer[i] = temp;
                    i++;
                    //System.out.println("read: " + new String(buffer, 0 , i));
                } catch (Exception e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }

        }

        private void dispatchBuffer(byte[] buffer, int i) {
            final byte[] data = new byte[i];
            System.arraycopy(buffer, 0, data, 0, i);
            if (onEventCallback != null) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        onEventCallback.onDataRead(data, data.length);
                    }
                });
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(final byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                mmOutStream.flush();

                if (onEventCallback != null)
                    runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            onEventCallback.onDataWrite(buffer);
                        }
                    });
            } catch (Exception e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            canceled = true;
            try {
                mmSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
            try {
                mmInStream.close();
            } catch (Exception e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
            try {
                interrupt();
            } catch (Exception e) {
                Log.e(TAG, "interrupt() of Thread failed", e);
            }
        }
    }

    private void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mScanReceiver = new BroadcastReceiver() {
        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH})
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed
                // already
                final int RSSI = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                if (onScanCallback != null)
                    runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            onScanCallback.onDeviceDiscovered(device, RSSI);
                        }
                    });
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                stopScan();
            }
        }
    };

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH})
    @Override
    public void startScan() {
        if (onScanCallback != null)
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    onScanCallback.onStartScan();
                }
            });

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mConfig.context.registerReceiver(mScanReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mConfig.context.registerReceiver(mScanReceiver, filter);

        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }

        mAdapter.startDiscovery();
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH})
    @Override
    public void stopScan() {
        try {
            mConfig.context.unregisterReceiver(mScanReceiver);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }

        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }

        if (onScanCallback != null)
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    onScanCallback.onStopScan();
                }
            });
    }

}
