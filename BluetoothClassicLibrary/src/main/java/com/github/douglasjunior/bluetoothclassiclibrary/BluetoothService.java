/*
 * MIT License
 *
 * Copyright (c) 2015 Douglas Nassif Roma Junior
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.douglasjunior.bluetoothclassiclibrary;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by douglas on 23/03/15.
 */
public abstract class BluetoothService {
    // Debugging
    private static final String TAG = BluetoothService.class.getSimpleName();
    protected static final boolean D = true;

    protected static BluetoothService mDefaultServiceInstance;
    protected BluetoothConfiguration mConfig;
    protected BluetoothStatus mStatus;

    private final Handler handler;

    protected OnBluetoothEventCallback onEventCallback;

    protected OnBluetoothScanCallback onScanCallback;

    private static BluetoothConfiguration mDefaultConfiguration;

    protected BluetoothService(BluetoothConfiguration config) {
        this.mConfig = config;
        this.mStatus = BluetoothStatus.NONE;
        this.handler = new Handler();
    }

    public void setOnEventCallback(OnBluetoothEventCallback onEventCallback) {
        this.onEventCallback = onEventCallback;
    }

    public void setOnScanCallback(OnBluetoothScanCallback onScanCallback) {
        this.onScanCallback = onScanCallback;
    }

    public BluetoothConfiguration getConfiguration() {
        return mConfig;
    }

    protected synchronized void updateState(final BluetoothStatus status) {
        Log.v(TAG, "updateStatus() " + mStatus + " -> " + status);
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

    protected void runOnMainThread(final Runnable runnable, final long delayMillis) {
        if (mConfig.callListenersInMainThread) {
            if (delayMillis > 0) {
                handler.postDelayed(runnable, delayMillis);
            } else {
                handler.post(runnable);
            }
        } else {
            if (delayMillis > 0) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(delayMillis);
                            runnable.run();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            } else {
                runnable.run();
            }
        }
    }

    protected void runOnMainThread(Runnable runnable) {
        runOnMainThread(runnable, 0);
    }

    protected void removeRunnableFromHandler(Runnable runnable) {
        if (handler != null) {
            handler.removeCallbacks(runnable);
        }
    }

    /**
     * Current BluetoothService status.
     *
     * @return
     */
    public synchronized BluetoothStatus getStatus() {
        return mStatus;
    }

    /**
     * Start scan process and call the {@link OnBluetoothScanCallback}
     */
    public abstract void startScan();

    /**
     * Stop scan process and call the {@link OnBluetoothScanCallback}
     */
    public abstract void stopScan();

    /**
     * Try to connect to the device and call the {@link OnBluetoothEventCallback}
     */
    public abstract void connect(BluetoothDevice device);

    /**
     * Try to disconnect to the device and call the {@link OnBluetoothEventCallback}
     */
    public abstract void disconnect();

    /**
     * Write a array of bytes to the connected device.
     */
    public abstract void write(byte[] bytes);

    /**
     * Stops the BluetoothService and turn it unusable.
     */
    public abstract void stopService();

    /* ====================================
                STATICS METHODS
     ====================================== */

    /**
     * Use {@link BluetoothService#init(BluetoothConfiguration)} instead.
     *
     * @param config
     */
    @Deprecated
    public static void setDefaultConfiguration(BluetoothConfiguration config) {
        init(config);
    }

    /**
     * Configures and initialize the BluetoothService singleton instance.
     *
     * @param config
     */
    public static void init(BluetoothConfiguration config) {
        mDefaultConfiguration = config;
        if (mDefaultServiceInstance != null) {
            mDefaultServiceInstance.stopService();
            mDefaultServiceInstance = null;
        }
        try {
            Constructor<? extends BluetoothService> constructor =
                    (Constructor<? extends BluetoothService>) mDefaultConfiguration.bluetoothServiceClass.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            BluetoothService bluetoothService = constructor.newInstance(mDefaultConfiguration);
            mDefaultServiceInstance = bluetoothService;
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the BluetoothService singleton instance.
     *
     * @return
     */
    public synchronized static BluetoothService getDefaultInstance() {
        if (mDefaultServiceInstance == null) {
            throw new IllegalStateException("BluetoothService is not initialized. Call BluetoothService.init(config).");
        }
        return mDefaultServiceInstance;
    }

    /* ====================================
                    CALLBACKS
     ====================================== */

    public interface OnBluetoothEventCallback {
        void onDataRead(byte[] buffer, int length);

        void onStatusChange(BluetoothStatus status);

        void onDeviceName(String deviceName);

        void onToast(String message);

        void onDataWrite(byte[] buffer);
    }

    public interface OnBluetoothScanCallback {
        void onDeviceDiscovered(BluetoothDevice device, int rssi);

        void onStartScan();

        void onStopScan();
    }
}
