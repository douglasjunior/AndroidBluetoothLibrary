package com.github.douglasjunior.bluetoothclassiclibrary;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by douglas on 23/03/15.
 */
public abstract class BluetoothService {
    // Debugging
    protected static final String TAG = "BluetoothService";
    protected static final boolean D = true;

    protected static BluetoothService mServiceInstance;
    protected final char mCharacterDelimiter;
    protected final Context mContext;
    protected final int mBufferSize;
    protected final String mDeviceName;
    protected int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0x0; // we're doing nothing
    public static final int STATE_CONNECTING = 0x1; // now initiating an outgoing
    public static final int STATE_CONNECTED = 0x2; // now connected to a remote

    protected final Handler handler = new Handler();

    protected OnBluetoothEventCallback onEventCallback;

    protected OnBluetoothScanCallback onScanCallback;

    protected BluetoothService(Context context, String deviceName, Integer bufferSize, Character characterDelimiter) {
        this.mCharacterDelimiter = characterDelimiter;
        this.mContext = context;
        this.mBufferSize = bufferSize;
        this.mDeviceName = deviceName;
        this.mState = STATE_NONE;
    }

    public static BluetoothService getInstance() {
        if (mServiceInstance == null) {
            throw new NullPointerException("O serviço não foi inciado, chame createServiceInstance() para iniciar.");
        }
        return mServiceInstance;
    }

    public static void createServiceInstance(Class<? extends BluetoothService> bluetoothServiceClass, Context context, String deviceName, int bufferSize, char characterDelimiter) {
        try {
            Constructor<? extends BluetoothService> constructor = bluetoothServiceClass.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            BluetoothService bluetoothService = constructor.newInstance(context, deviceName, bufferSize, characterDelimiter);
            mServiceInstance = bluetoothService;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void setOnEventCallback(OnBluetoothEventCallback onEventCallback) {
        this.onEventCallback = onEventCallback;
    }

    public void setOnScanCallback(OnBluetoothScanCallback onScanCallback) {
        this.onScanCallback = onScanCallback;
    }

    public abstract void write(byte[] bytes);

    public char getCharacterDelimiter() {
        return mCharacterDelimiter;
    }

    protected synchronized void setState(final int state) {
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

    public synchronized int getState() {
        return mState;
    }

    public abstract void stopService();

    public abstract void connect(BluetoothDevice device);

    public abstract void startScan();

    public abstract void stopScan();

    public interface OnBluetoothEventCallback {
        void onDataRead(byte[] buffer, int length);

        void onStateChange(int state);

        void onDeviceName(String deviceName);

        void onToast(String message);

        void onDataWrite(byte[] buffer);
    }

    public interface OnBluetoothScanCallback{
        void onDeviceDiscovered(BluetoothDevice device, int rssi);

        void onStartScan();

        void onStopScan();
    }
}
