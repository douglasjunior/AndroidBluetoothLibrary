# AndroidBluetoothLibrary

[![Licence MIT](https://img.shields.io/badge/licence-MIT-blue.svg)](https://github.com/douglasjunior/AndroidBluetoothLibrary/blob/master/LICENSE)
[![Release](https://jitpack.io/v/douglasjunior/AndroidBluetoothLibrary.svg)](https://jitpack.io/#douglasjunior/AndroidBluetoothLibrary)
[![Downloads](https://jitpack.io/v/douglasjunior/AndroidBluetoothLibrary/month.svg)](#download)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Android%20Bluetooth%20Library-yellow.svg?style=flat)](http://android-arsenal.com/details/1/5821)

A Library for easy implementation of Serial Bluetooth Classic and Low Energy on Android. 💙

- Bluetooth Classic working from Android 2.1 (API 7)
- Bluetooth Low Energy working from Android 4.3 (API 18)

## Use

### Configuration

#### Bluetooth Classic
```java
BluetoothConfiguration config = new BluetoothConfiguration();
config.context = getApplicationContext();
config.bluetoothServiceClass = BluetoothClassicService.class;
config.bufferSize = 1024;
config.characterDelimiter = '\n';
config.deviceName = "Your App Name";
config.callListenersInMainThread = true;

config.uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // Required

BluetoothService.init(config);
```

#### Bluetooth Low Energy
```java
BluetoothConfiguration config = new BluetoothConfiguration();
config.context = getApplicationContext();
config.bluetoothServiceClass = BluetoothLeService.class;
config.bufferSize = 1024;
config.characterDelimiter = '\n';
config.deviceName = "Your App Name";
config.callListenersInMainThread = true;

config.uuidService = UUID.fromString("e7810a71-73ae-499d-8c15-faa9aef0c3f2"); // Required
config.uuidCharacteristic = UUID.fromString("bef8d6c9-9c21-4c9e-b632-bd58c1009f9f"); // Required
config.transport = BluetoothDevice.TRANSPORT_LE; // Required for dual-mode devices
config.uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // Used to filter found devices. Set null to find all devices.

BluetoothService.init(config);
```

### Getting BluetoothService

```java
BluetoothService service = BluetoothService.getDefaultInstance();
```

### Scanning

```java
service.setOnScanCallback(new BluetoothService.OnBluetoothScanCallback() {
    @Override
    public void onDeviceDiscovered(BluetoothDevice device, int rssi) {
    }

    @Override
    public void onStartScan() {
    }

    @Override
    public void onStopScan() {
    }
});

service.startScan(); // See also service.stopScan();
```

### Connecting

```java
service.setOnEventCallback(new BluetoothService.OnBluetoothEventCallback() {
    @Override
    public void onDataRead(byte[] buffer, int length) {
    }

    @Override
    public void onStatusChange(BluetoothStatus status) {
    }

    @Override
    public void onDeviceName(String deviceName) {
    }

    @Override
    public void onToast(String message) {
    }

    @Override
    public void onDataWrite(byte[] buffer) {
    }
});

service.connect(device); // See also service.disconnect();
```

### Writing

```java
BluetoothWriter writer = new BluetoothWriter(service);

writer.writeln("Your text here");
```

### Complete example

See the [sample project](https://github.com/douglasjunior/AndroidBluetoothLibrary/tree/master/Sample/src/main/java/com/github/douglasjunior/bluetoothsample).

## Download 

1. Add it in your root build.gradle at the end of repositories:
   ```javascript
   allprojects {
     repositories {
       ...
       maven { url "https://jitpack.io" }
     }
   }
   ```

2. Add the dependency

   2.1. Bluetooth Classic
     ```javascript
     dependencies {
       implementation 'com.github.douglasjunior.AndroidBluetoothLibrary:BluetoothClassicLibrary:0.3.5'
     }
     ```
    
   2.2. Bluetooth Low Energy
     ```javascript
     dependencies {
       implementation 'com.github.douglasjunior.AndroidBluetoothLibrary:BluetoothLowEnergyLibrary:0.3.5'
     }
     ```
 
3. Add permission in `AndroidManifest.xml` 

```xml
<manifest ...>
  <uses-permission android:name="android.permission.BLUETOOTH" />
  <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
  <uses-permission android:name="android.permission.BLUETOOTH_PRIVILEGED" />
  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
  ...
</manifest>
```

## Known Issues / Troubleshooting

<span id="known-issues" />

- Scanning will not detect bluetooth devices if the user has denied Location Privacy Permission to your app. This library does not test for the permission and will not raise errors. (Android 6.0+) See: http://stackoverflow.com/a/33045489/2826279

## Contribute

New features, bug fixes and improvements are welcome! For questions and suggestions use the [issues](https://github.com/douglasjunior/AndroidBluetoothLibrary/issues).

Before submit your PR, run the gradle check.
```bash
./gradlew build connectedCheck
```

<a href="https://www.patreon.com/douglasjunior"><img src="http://i.imgur.com/xEO164Z.png" alt="Become a Patron!" width="200" /></a>
[![Donate](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=E32BUP77SVBA2)

## Licence

```
The MIT License (MIT)

Copyright (c) 2015 Douglas Nassif Roma Junior
```

See the full [licence file](https://github.com/douglasjunior/AndroidBluetoothLibrary/blob/master/LICENSE).

