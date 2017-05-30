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
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.UUID;

/**
 * Created by dougl on 10/04/2017.
 */
public class BluetoothConfiguration {

    private static final String TAG = BluetoothConfiguration.class.getSimpleName();

    /**
     * Class reference for the {@link BluetoothService} implementation.
     *
     * @see BluetoothClassicService
     * @see BluetoothLeService
     */
    public Class<? extends BluetoothService> bluetoothServiceClass;

    /**
     * {@link android.app.Application} context reference.
     */
    public Context context;

    /**
     * Name of your application or device.
     */
    public String deviceName;

    /**
     * Maximum of bytes to keep in the buffer before call the
     * {@link com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService.OnBluetoothEventCallback#onDataRead(byte[], int)}
     */
    public int bufferSize;

    /**
     * Character delimiter to know if a data is received completly and call the
     * {@link com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService.OnBluetoothEventCallback#onDataRead(byte[], int)}
     */
    public char characterDelimiter;

    /**
     * Required in {@link BluetoothClassicService}, is the UUID of the device that will connect in serial mode. <br/>
     * Optional in {@link BluetoothLeService}, is the UUID of the device that will be filtered in scan.
     * Set {@link null} if you want to scan all devices.
     */
    public UUID uuid;

    /**
     * Required for {@link BluetoothLeService} <br/>
     * UUID of bluetooth service.
     */
    public UUID uuidService;

    /**
     * Required for {@link BluetoothLeService} <br/>
     * UUID of bluetooth characteristic.
     */
    public UUID uuidCharacteristic;

    /**
     * Preferred transport for GATT connections to remote dual-mode devices
     * {@link BluetoothDevice#TRANSPORT_AUTO} or
     * {@link BluetoothDevice#TRANSPORT_BREDR} or {@link BluetoothDevice#TRANSPORT_LE}
     */
    public int transport;

    /**
     * Whether to call the listener only in Main Thread (true)
     * or call in the Thread where the event occurs (false).
     */
    public boolean callListenersInMainThread = true;

    public BluetoothConfiguration() {
        setDefaultTransport();
    }

    /**
     * Set the default value for {@link BluetoothConfiguration#transport}.
     */
    private void setDefaultTransport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            transport = BluetoothDevice.TRANSPORT_LE;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // From Android LOLLIPOP (21) the transport types exists, but them are hide for use,
            // so is needed to use relfection to get the value
            try {
                transport = BluetoothDevice.class.getDeclaredField("TRANSPORT_LE").getInt(null);
            } catch (Exception ex) {
                Log.d(TAG, "Error on get BluetoothDevice.TRANSPORT_LE with reflection.", ex);
            }
        } else {
            transport = -1;
        }
    }
}