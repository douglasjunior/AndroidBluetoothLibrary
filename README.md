# AndroidBluetoothLibrary
A Library for implementation Bluetooth Classic and Low Energy on Android.

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
        compile 'com.github.douglasjunior.AndroidBluetoothLibrary:BluetoothClassicLibrary:v0.2.1'
    }
    ```
    
    2.1. Bluetooth Low Energy

    ```javascript
    dependencies {
        compile 'com.github.douglasjunior.AndroidBluetoothLibrary:BluetoothLowEnergyLibrary:v0.2.1'
    }
    ```
 
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

