package com.github.douglasjunior.bluetoothlowenergylibrary;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;


import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService;

import java.util.UUID;

/**
* Created by douglas on 16/03/15.
*/
public class BluetoothLeService extends BluetoothService {

    private static final long SCAN_PERIOD = 10000;
    // Sample Services.
    public static UUID HM_10_CONF = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    // Sample Characteristics.
    public static UUID HM_RX_TX = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private final BluetoothAdapter btAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic characteristicRxTx;

    private final byte[] buffer;
    private int i = 0;

    private Handler handler = new Handler();

    protected BluetoothLeService(Context context, String deviceName, int bufferSize, char characterDelimiter) {
        super(context, deviceName, bufferSize, characterDelimiter);
        BluetoothManager btManager = (BluetoothManager) context.getSystemService(context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        buffer = new byte[bufferSize];
    }

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            final byte[] data = characteristic.getValue();
            Log.v("BluetoothGattCallback", "onCharacteristicChanged: " + new String(characteristic.getValue()));
            readData(data);
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            final byte[] data = characteristic.getValue();
            Log.v("BluetoothGattCallback", "onCharacteristicRead: " + new String(data));
            if (BluetoothGatt.GATT_SUCCESS == status) {
                readData(data);
            } else {
                System.err.println("onCharacteristicRead error " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            final byte[] data = characteristic.getValue();
            Log.v("BluetoothGattCallback", "onCharacteristicWrite: " + new String(data));
            if (BluetoothGatt.GATT_SUCCESS == status) {
                if (onEventCallback != null)
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            onEventCallback.onDataWrite(data);
                        }
                    });
            } else {
                System.err.println("onCharacteristicWrite error " + status);
            }

        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, final int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.v("BluetoothGattCallback", "onConnectionStateChange: " + newState);
            System.err.println("onConnectionStateChange status " + status);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //createBound(gatt.getDevice());
                if (onEventCallback != null)
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            BluetoothDevice device = gatt.getDevice();
                            if (device.getName() == null)
                                device = btAdapter.getRemoteDevice(gatt.getDevice().getAddress());
                            onEventCallback.onDeviceName(device.getName());
                        }
                    });
                setState(STATE_CONNECTED);
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                setState(STATE_CONNECTING);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close();
                if (mState == STATE_NONE || mState == STATE_CONNECTING)
                    makeToast("Não foi possível conectar ao dispositivo");
                else if (mState == STATE_CONNECTED)
                    makeToast("Conexão perdida com o dispositivo");
                setState(STATE_NONE);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.v("BluetoothGattCallback", "onDescriptorRead");
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.v("BluetoothGattCallback", "onDescriptorWrite");
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.v("BluetoothGattCallback", "onReadRemoteRssi");
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.v("BluetoothGattCallback", "onReliableWriteCompleted");
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.v("BluetoothGattCallback", "onServicesDiscovered: " + status);
            if (BluetoothGatt.GATT_SUCCESS == status) {
                for (BluetoothGattService service : gatt.getServices()) {
                    System.out.println("Service: " + service.getUuid());
                    if (HM_10_CONF.equals(service.getUuid())) {
                        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                            System.out.println("Characteristic: " + characteristic.getUuid());
                            if (HM_RX_TX.equals(characteristic.getUuid())) {
                                characteristicRxTx = characteristic;
                                gatt.setCharacteristicNotification(characteristic, true);
                            }
                        }
                    }
                }
            } else {
                System.err.println("onServicesDiscovered error " + status);
            }
        }
    };

//    private void createBound(BluetoothDevice device) {
//        try {
//            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
//                Method method = device.getClass().getMethod("createBond", (Class[]) null);
//                method.invoke(device, (Object[]) null);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private void readData(byte[] data) {
        final byte byteDelimiter = (byte) mCharacterDelimiter;
        for (byte temp : data) {
            /*
            Verifica se está no momento de despachar o buffer
             */
            if (temp == byteDelimiter || i >= buffer.length) {
                if (i > 0) {
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
                }
            }
            /*
            Caso contrário, armazena o byte recebido
             */
            else {
                buffer[i] = temp;
                i++;
            }
        }
    }

    private void makeToast(final String message) {
        if (onEventCallback != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onEventCallback.onToast(message);
                }
            });
    }


    public void connect(BluetoothDevice bluetoothDevice) {
        if (btAdapter != null && btAdapter.isEnabled()) {
            if (bluetoothGatt != null) {
                bluetoothGatt.disconnect();
            }
            setState(STATE_CONNECTING);
            bluetoothGatt = bluetoothDevice.connectGatt(mContext, false, btleGattCallback);
        }
    }

    final Runnable mStopScanRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };

    final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            if (onScanCallback != null)
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onScanCallback.onDeviceDiscovered(device, rssi);
                    }
                });
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

        btAdapter.stopLeScan(mLeScanCallback);

        btAdapter.startLeScan(mLeScanCallback);

        handler.postDelayed(mStopScanRunnable, SCAN_PERIOD);
    }

    @Override
    public void stopScan() {
        handler.removeCallbacks(mStopScanRunnable);
        btAdapter.stopLeScan(mLeScanCallback);

        if (onScanCallback != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onScanCallback.onStopScan();
                }
            });
    }

    public void stopService() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
        bluetoothGatt = null;
    }

    public void write(byte[] data) {
        // Log.v("BluetoothLeService", "write: " + new String(data));
//        System.out.println("Conectados: " + bluetoothGatt.getConnectedDevices());
        if (bluetoothGatt != null && characteristicRxTx != null && mState == STATE_CONNECTED) {
            characteristicRxTx.setValue(data);
            bluetoothGatt.writeCharacteristic(characteristicRxTx);
        }
    }

}
