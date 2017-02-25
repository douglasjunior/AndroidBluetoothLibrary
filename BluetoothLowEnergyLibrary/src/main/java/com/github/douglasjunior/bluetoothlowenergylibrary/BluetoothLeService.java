package com.github.douglasjunior.bluetoothlowenergylibrary;

import android.Manifest;
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
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresPermission;
import android.util.Log;


import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Created by douglas on 16/03/15.
 */
public class BluetoothLeService extends BluetoothService {

    private static final String TAG = BluetoothLeService.class.getSimpleName();

    private static final long SCAN_PERIOD = 10000;

    private final BluetoothAdapter btAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic characteristicRxTx;

    private final byte[] buffer;
    private int i = 0;

    private Handler handler = new Handler();

    protected BluetoothLeService(BluetoothConfiguration config) {
        super(config);
        BluetoothManager btManager = (BluetoothManager) config.context.getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        buffer = new byte[config.bufferSize];
    }

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            final byte[] data = characteristic.getValue();
            Log.v(TAG, "onCharacteristicChanged: " + new String(characteristic.getValue()));
            readData(data);
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            final byte[] data = characteristic.getValue();
            Log.v(TAG, "onCharacteristicRead: " + new String(data));
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
            Log.v(TAG, "onCharacteristicWrite: " + new String(data));
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

        @RequiresPermission(Manifest.permission.BLUETOOTH)
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, final int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.v(TAG, "onConnectionStateChange: status: " + status + " newState: " + newState);
            if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close();
                if (mStatus == BluetoothStatus.NONE || mStatus == BluetoothStatus.CONNECTING)
                    makeToast("Não foi possível conectar ao dispositivo");
                else if (mStatus == BluetoothStatus.CONNECTED)
                    makeToast("Conexão perdida com o dispositivo");
                updateState(BluetoothStatus.NONE);
            } else {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    //createBound(gatt.getDevice());
                    updateDeviceName(gatt.getDevice());
                    updateState(BluetoothStatus.CONNECTED);
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                    updateState(BluetoothStatus.CONNECTING);
                }
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.v(TAG, "onDescriptorRead");
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.v(TAG, "onDescriptorWrite");
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.v(TAG, "onReadRemoteRssi");
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.v(TAG, "onReliableWriteCompleted");
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.v(TAG, "onServicesDiscovered: " + status);
            if (BluetoothGatt.GATT_SUCCESS == status) {
                for (BluetoothGattService service : gatt.getServices()) {
                    Log.v(TAG, "Service: " + service.getUuid());
                    if (mConfig.uuidService.equals(service.getUuid())) {
                        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                            Log.v(TAG, "Characteristic: " + characteristic.getUuid());
                            if (mConfig.uuidCharacteristic.equals(characteristic.getUuid())) {
                                characteristicRxTx = characteristic;
                                gatt.setCharacteristicNotification(characteristic, true);
                            }
                        }
                    }
                }
            } else {
                Log.e(TAG, "onServicesDiscovered error " + status);
            }
        }
    };

    @RequiresPermission(Manifest.permission.BLUETOOTH)
    private void updateDeviceName(final BluetoothDevice device) {
        if (onEventCallback != null)
            handler.post(new Runnable() {
                @RequiresPermission(Manifest.permission.BLUETOOTH)
                @Override
                public void run() {
                    BluetoothDevice dev = device;
                    if (dev.getName() == null)
                        dev = btAdapter.getRemoteDevice(dev.getAddress());
                    onEventCallback.onDeviceName(dev.getName());
                }
            });
    }

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
        final byte byteDelimiter = (byte) mConfig.characterDelimiter;
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

    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public void connect(BluetoothDevice bluetoothDevice) {
        if (btAdapter != null && btAdapter.isEnabled()) {
            if (bluetoothGatt != null) {
                bluetoothGatt.disconnect();
            }
            updateState(BluetoothStatus.CONNECTING);
            bluetoothGatt = bluetoothDevice.connectGatt(mConfig.context, false, btleGattCallback);
        }
    }

    final Runnable mStopScanRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };


    final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH)
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            List<UUID> uuids = parseUUIDs(scanRecord);
            Log.v(TAG, "onLeScan " + device.getName() + " " + new String(scanRecord) + " -> uuids: " + uuids);
            if (onScanCallback != null && uuids.contains(mConfig.uuid)) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onScanCallback.onDeviceDiscovered(device, rssi);
                    }
                });
            }
        }

    };

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
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

    private List<UUID> parseUUIDs(final byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<>();

        int offset = 0;
        while (offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset++];
            if (len == 0)
                break;

            int type = advertisedData[offset++];
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (len > 1) {
                        int uuid16 = advertisedData[offset++];
                        uuid16 += (advertisedData[offset++] << 8);
                        len -= 2;
                        uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                    }
                    break;
                case 0x06:// Partial list of 128-bit UUIDs
                case 0x07:// Complete list of 128-bit UUIDs
                    // Loop through the advertised 128-bit UUID's.
                    while (len >= 16) {
                        try {
                            // Wrap the advertised bits and order them.
                            ByteBuffer buffer = ByteBuffer.wrap(advertisedData,
                                    offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                            long mostSignificantBit = buffer.getLong();
                            long leastSignificantBit = buffer.getLong();
                            uuids.add(new UUID(leastSignificantBit,
                                    mostSignificantBit));
                        } catch (IndexOutOfBoundsException e) {
                            // Defensive programming.
                            Log.e(TAG, e.toString());
                            continue;
                        } finally {
                            // Move the offset to read the next uuid.
                            offset += 15;
                            len -= 16;
                        }
                    }
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }

        return uuids;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
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
        // Log.v(TAG, "write: " + new String(data));
        if (bluetoothGatt != null && characteristicRxTx != null && mStatus == BluetoothStatus.CONNECTED) {
            characteristicRxTx.setValue(data);
            bluetoothGatt.writeCharacteristic(characteristicRxTx);
        }
    }

}
