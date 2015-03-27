package com.github.douglasjunior.bluetoothclassiclibrary;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Douglas on 16/09/2014.
 */
public class BluetoothDeviceDecoratorNull extends BluetoothDeviceDecorator {

    private BluetoothDeviceDecoratorNull(BluetoothDevice device) {
        super(device);
    }

    public static BluetoothDeviceDecoratorNull getInstance() {
        return new BluetoothDeviceDecoratorNull(null);
    }

    @Override
    public String getName() {
        return "Nenhum dispositivo encontrado.";
    }

    @Override
    public String getAddress() {
        return "";
    }

    @Override
    public int getRSSI() {
        return 9999;
    }

    @Override
    public void setRSSI(int RSSI) {
    }
}
