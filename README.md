# AndroidBluetoothLibrary

A Library for easy implementation Bluetooth Classic and Low Energy on Android.

## Use

### Bluetooth Classic

Soon

### Bluetooth Low Energy

Soon


## Install 

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
       compile 'com.github.douglasjunior.AndroidBluetoothLibrary:BluetoothClassicLibrary:0.3.0'
     }
     ```
    
   2.2. Bluetooth Low Energy
     ```javascript
     dependencies {
       compile 'com.github.douglasjunior.AndroidBluetoothLibrary:BluetoothLowEnergyLibrary:0.3.0'
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

## Known issues

- Location needs to be enabled for Bluetooth Low Energy Scanning on Android 6.0 http://stackoverflow.com/a/33045489/2826279
 
## Contribute

New features, bug fixes and improvements in the translation are welcome! For questions and suggestions use the [issues](https://github.com/douglasjunior/AndroidBluetoothLibrary/issues).

Before submit your PR, run the gradle check.
```bash
./gradlew build connectedCheck
```

## Donate

[![Donate](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=ZJ6TCL3EVUDDL)


## Licence

```
The MIT License (MIT)

Copyright (c) 2015 Douglas Nassif Roma Junior
```

See the full [licence file](https://github.com/douglasjunior/AndroidBluetoothLibrary/blob/master/LICENSE).

