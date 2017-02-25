package com.github.douglasjunior.bluetoothclassiclibrary;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.support.annotation.RequiresPermission;

/**
 * Created by Douglas on 16/09/2014.
 */
public class BluetoothDeviceDecorator {

    private BluetoothDevice mDevice;

    private int mRSSI;

    public BluetoothDeviceDecorator(BluetoothDevice device) {
        mDevice = device;
    }

    public BluetoothDeviceDecorator(BluetoothDevice device, int RSSI) {
        this(device);
        mRSSI = RSSI;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public String getName() {
        return mDevice.getName() != null && mDevice.getName().length() != 0 ? mDevice.getName() : "Desconhecido...";
    }

    public String getAddress() {
        return mDevice.getAddress();
    }

    public int getRSSI() {
        return mRSSI;
    }

    public void setRSSI(int RSSI) {
        mRSSI = RSSI;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BluetoothDeviceDecorator that = (BluetoothDeviceDecorator) o;

        return mDevice.getAddress().equals(that.mDevice.getAddress());
    }

    @Override
    public int hashCode() {
        return mDevice.getAddress().hashCode();
    }

    public void setDevice(BluetoothDevice device) {
        this.mDevice = device;
    }
}
