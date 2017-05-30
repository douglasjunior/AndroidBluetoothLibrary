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

        return mDevice.equals(that.mDevice);
    }

    @Override
    public int hashCode() {
        return mDevice.hashCode();
    }

    public void setDevice(BluetoothDevice device) {
        this.mDevice = device;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }
}
