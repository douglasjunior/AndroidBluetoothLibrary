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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth connections with other devices. It has a thread
 * that listens for incoming connections, a thread for connecting with a device, and a thread for performing data
 * transmissions when connected.
 */
public class BluetoothClassicService extends BluetoothService {

    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    protected BluetoothClassicService(Context context, String deviceName, Integer bufferSize, Character characterDelimiter) {
        super(context, deviceName, bufferSize, characterDelimiter);
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    protected synchronized void setState(final int state) {
        if (D)
            Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        if (onEventCallback != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onEventCallback.onStateChange(state);
                }
            });

    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D)
            Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
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
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
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
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onEventCallback.onDeviceName(device.getName());
                }
            });

        setState(STATE_CONNECTED);
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
        setState(STATE_NONE);
        mServiceInstance = null;
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

        if (mState != STATE_CONNECTED)
            return;

        r = mConnectedThread;

        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_NONE);

        // Send a failure message back to the Activity
        if (onEventCallback != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onEventCallback.onToast("Não foi possível conectar ao dispositivo");
                }
            });
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(STATE_NONE);

        // Send a failure message back to the Activity

        if (onEventCallback != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onEventCallback.onToast("Conexão perdida com o dispositivo");
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

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "create() failed", e);
            }
            try {
                AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                //For phone speaker(loadspeaker)
                mAudioManager.setMode(AudioManager.MODE_NORMAL);
                mAudioManager.setBluetoothScoOn(false);
                mAudioManager.setSpeakerphoneOn(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
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
            final byte[] buffer = new byte[mBufferSize];
            int i = 0;

            // Keep listening to the InputStream while connected
            while (!canceled) {
                try {
                    // Read from the InputStream
                    temp = (byte) mmInStream.read();
                    //     System.out.println("temp: " + (int) temp);
                    if ((temp == (byte) mCharacterDelimiter || i >= buffer.length) && i > 0) {
                        // Send the obtained bytes to the UI Activity
                        if (onEventCallback != null) {
                            final int finalI = i;
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    onEventCallback.onDataRead(buffer, finalI);
                                }
                            });
                        }
                        i = 0;
                    } else {
                        buffer[i] = temp;
                        i++;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
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
                    handler.post(new Runnable() {
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

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mScanReceiver = new BroadcastReceiver() {
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
                    handler.post(new Runnable() {
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

    @Override
    public void startScan() {
        if (onScanCallback != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onScanCallback.onStartScan();
                }
            });

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mContext.registerReceiver(mScanReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mContext.registerReceiver(mScanReceiver, filter);

        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }

        mAdapter.startDiscovery();
    }

    @Override
    public void stopScan() {
        try {
            mContext.unregisterReceiver(mScanReceiver);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }

        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }

        if (onScanCallback != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onScanCallback.onStopScan();
                }
            });
    }

}
