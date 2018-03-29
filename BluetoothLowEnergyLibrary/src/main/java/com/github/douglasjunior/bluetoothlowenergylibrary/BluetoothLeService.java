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
import android.support.annotation.RequiresApi;
import android.support.annotation.RequiresPermission;
import android.util.Log;

import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothConfiguration;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
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

    private final byte[] readBuffer;
    private int readBufferIndex = 0;
    private byte[][] writeBuffer;
    private int writeBufferIndex = 0;

    private int maxTransferBytes = 20;

    protected BluetoothLeService(BluetoothConfiguration config) {
        super(config);
        BluetoothManager btManager = (BluetoothManager) config.context.getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        readBuffer = new byte[config.bufferSize];
    }

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.v(TAG, "onMtuChanged: " + mtu + " status: " + status);
            // Receive the requested MTU size.
            // See also https://stackoverflow.com/questions/24135682/android-sending-data-20-bytes-by-ble
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // It discounts 3 bytes of metadata.
                maxTransferBytes = mtu - 3;
            }
        }

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
            //Log.v(TAG, "onCharacteristicWrite status: " + status + " data: " + new String(data));
            Log.v(TAG, "onCharacteristicWrite status: " + status + " data: " + data.length);
            if (BluetoothGatt.GATT_SUCCESS == status || status == 11) {
                if (onEventCallback != null)
                    runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            onEventCallback.onDataWrite(data);
                        }
                    });
                writeCharacteristic();
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
                    makeToast("Unable to connect to device");
                else if (mStatus == BluetoothStatus.CONNECTED)
                    makeToast("Connection lost");
                updateState(BluetoothStatus.NONE);
            } else {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    //createBound(gatt.getDevice());
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

        @RequiresPermission(Manifest.permission.BLUETOOTH)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.v(TAG, "onServicesDiscovered: " + status);

            if (BluetoothGatt.GATT_SUCCESS == status) {
                for (BluetoothGattService service : gatt.getServices()) {
                    Log.v(TAG, "Service: " + service.getUuid());
                    if (mConfig.uuidService == null || service.getUuid().equals(mConfig.uuidService)) {
                        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                            final int props = characteristic.getProperties();
                            Log.v(TAG, "Characteristic: " + characteristic.getUuid() +
                                    " PROPERTY_WRITE: " + (props & BluetoothGattCharacteristic.PROPERTY_WRITE) +
                                    " PROPERTY_WRITE_NO_RESPONSE: " + (props & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE));
                            if (characteristic.getUuid().equals(mConfig.uuidCharacteristic)) {
                                characteristicRxTx = characteristic;
                                gatt.setCharacteristicNotification(characteristic, true);

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    // Request the MTU size to device.
                                    // See also https://stackoverflow.com/questions/24135682/android-sending-data-20-bytes-by-ble
                                    boolean requestMtu = bluetoothGatt.requestMtu(512);
                                    Log.v(TAG, "requestMtu: " + requestMtu);

                                    // Request a specific connection priority.
                                    // CONNECTION_PRIORITY_BALANCED is the default value if no connection parameter update is requested
                                    if (mConfig.connectionPriority != BluetoothGatt.CONNECTION_PRIORITY_BALANCED)
                                        requestConnectionPriority(mConfig.connectionPriority);
                                }

                                updateDeviceName(gatt.getDevice());
                                updateState(BluetoothStatus.CONNECTED);
                                return;
                            }
                        }
                    }
                }
                Log.e(TAG, "Could not find uuidService:" + mConfig.uuidService + " and uuidCharacteristic:" + mConfig.uuidCharacteristic);
            } else {
                Log.e(TAG, "onServicesDiscovered error " + status);
            }

            // If arrived here, no service or characteristic has been found.
            gatt.disconnect();
        }
    };

    @RequiresPermission(Manifest.permission.BLUETOOTH)
    private void updateDeviceName(final BluetoothDevice device) {
        if (onEventCallback != null)
            runOnMainThread(new Runnable() {
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

            if (temp == byteDelimiter) {
                if (readBufferIndex > 0) {
                    dispatchBuffer(readBuffer, readBufferIndex);
                    readBufferIndex = 0;
                }
                continue;
            }
            if (readBufferIndex == readBuffer.length - 1) {
                dispatchBuffer(readBuffer, readBufferIndex);
                readBufferIndex = 0;
            }
            readBuffer[readBufferIndex] = temp;
            readBufferIndex++;

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

    private void makeToast(final String message) {
        if (onEventCallback != null)
            runOnMainThread(new Runnable() {
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

            /*
            About this issue:
            https://code.google.com/p/android/issues/detail?id=92949
            http://stackoverflow.com/q/27633680/2826279
             */

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // If android verion is greather or equal to Android M (23), then call the connectGatt with TRANSPORT_LE.
                bluetoothGatt = bluetoothDevice.connectGatt(mConfig.context, false, btleGattCallback, mConfig.transport);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // From Android LOLLIPOP (21) the transport types exists, but them are hide for use,
                // so is needed to use relfection to get the value
                try {
                    Method connectGattMethod = bluetoothDevice.getClass().getDeclaredMethod("connectGatt", Context.class, boolean.class, BluetoothGattCallback.class, int.class);
                    connectGattMethod.setAccessible(true);
                    bluetoothGatt = (BluetoothGatt) connectGattMethod.invoke(bluetoothDevice, mConfig.context, false, btleGattCallback, mConfig.transport);
                } catch (Exception ex) {
                    Log.d(TAG, "Error on call BluetoothDevice.connectGatt with reflection.", ex);
                }
            }

            // If any try is fail, then call the connectGatt without transport
            if (bluetoothGatt == null) {
                bluetoothGatt = bluetoothDevice.connectGatt(mConfig.context, false, btleGattCallback);
            }
        }
    }

    @Override
    public void disconnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
    final Runnable mStopScanRunnable = new Runnable() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
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
            if (onScanCallback != null && (mConfig.uuid == null || uuids.contains(mConfig.uuid))) {
                runOnMainThread(new Runnable() {
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
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    onScanCallback.onStartScan();
                }
            });

        btAdapter.stopLeScan(mLeScanCallback);

        btAdapter.startLeScan(mLeScanCallback);

        runOnMainThread(mStopScanRunnable, SCAN_PERIOD);
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
        removeRunnableFromHandler(mStopScanRunnable);
        btAdapter.stopLeScan(mLeScanCallback);

        if (onScanCallback != null)
            runOnMainThread(new Runnable() {
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void requestConnectionPriority(int connectionPriority) {
        if (bluetoothGatt != null) {
            if (connectionPriority >= BluetoothGatt.CONNECTION_PRIORITY_BALANCED
                    && connectionPriority <= BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER) {
                boolean requestConnectionPriority = bluetoothGatt.requestConnectionPriority(connectionPriority);
                Log.v(TAG, "requestConnectionPriority("+connectionPriority+"): " + requestConnectionPriority);
            } else
                Log.e(TAG, "requestConnectionPriority("+connectionPriority+"): ERROR - connectionPriority not within valid range");
        }
    }

    /**
     * Splits the bytes into packets according to the MTU size of the device, and writes the packets sequentially.
     *
     * See also https://stackoverflow.com/questions/24135682/android-sending-data-20-bytes-by-ble
     *
     * @param data
     */
    public void write(byte[] data) {
        Log.v(TAG, "write: " + data.length);
        if (bluetoothGatt != null && characteristicRxTx != null && mStatus == BluetoothStatus.CONNECTED) {
            if (data.length <= maxTransferBytes) {
                writeBufferIndex = 0;
                writeBuffer = new byte[1][data.length];
                writeBuffer[0] = data;
            } else {
                writeBufferIndex = 0;
                int bufferSize = (data.length / maxTransferBytes) + 1;
                writeBuffer = new byte[bufferSize][maxTransferBytes];

                for (int i = 0; i < writeBuffer.length; i++) {
                    int start = i * maxTransferBytes;
                    int end = start + maxTransferBytes;
                    if (start >= data.length)
                        break;
                    if (end > data.length)
                        end = data.length;
                    writeBuffer[i] = Arrays.copyOfRange(data, start, end);
                }
            }
            writeCharacteristic();
        }
    }

    /**
     * Writes next packet to the Characteristic.
     *
     */
    private void writeCharacteristic() {
        Log.v(TAG, "writeCharacteristic " + writeBufferIndex);
        if (writeBufferIndex >= writeBuffer.length)
            return;

        byte[] bytes = writeBuffer[writeBufferIndex];

        boolean setValue = characteristicRxTx.setValue(bytes);
        Log.v(TAG, "setValue: " + setValue);

        boolean writeCharacteristic = bluetoothGatt.writeCharacteristic(characteristicRxTx);
        Log.v(TAG, "writeCharacteristic: " + writeCharacteristic);

        writeBufferIndex++;
    }

}
