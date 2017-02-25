package com.github.douglasjunior.bluetoothclassiclibrary;

public class BluetoothWriter {
    private final BluetoothService service;

    public BluetoothWriter(BluetoothService service) {
        this.service = service;
    }

    public void write(String msg) {
        if (service != null)
            service.write(msg.getBytes());
    }

    public void write(Integer number) {
        write(number.toString());
    }

    public void writeln(String msg) {
        write(msg + service.getConfiguration().characterDelimiter);
    }

    public void write(Character c){
        write(c.toString());
    }
}
