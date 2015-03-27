package com.github.douglasjunior.bluetoothclassiclibrary;

public class BluetoothWriter {
    private final BluetoothService service;

    public BluetoothWriter(BluetoothService service) {
        this.service = service;
    }

    static long milis = 0;

    public void write(String msg) {
        long current = System.currentTimeMillis();
        long dif = current - milis;
        //     System.out.println(msg + " -> " + dif);
        milis = current;
        if (service != null)
            service.write(msg.getBytes());
    }

    public void write(int number) {
        write(number + "");
    }

    public void writeln(String msg) {
        write(msg + service.getCharacterDelimiter());
    }
}
