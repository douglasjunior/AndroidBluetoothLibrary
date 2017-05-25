package com.github.douglasjunior.bluetoothsample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothWriter;

/**
 * Created by dougl on 10/04/2017.
 */

public class DeviceActivity extends AppCompatActivity implements BluetoothService.OnBluetoothEventCallback, View.OnClickListener {

    private static final String TAG = "DeviceActivity";

    private FloatingActionButton mFab;
    private EditText mEdRead;
    private EditText mEdWrite;

    private BluetoothService mService;
    private BluetoothWriter mWriter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(this);

        mEdRead = (EditText) findViewById(R.id.ed_read);
        mEdWrite = (EditText) findViewById(R.id.ed_write);

        mService = BluetoothService.getDefaultInstance();
        mWriter = new BluetoothWriter(mService);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mService.setOnEventCallback(this);
    }

    @Override
    public void onDataRead(byte[] buffer, int length) {
        Log.d(TAG, "onDataRead: " + new String(buffer, 0, length));
        mEdRead.append("< " + new String(buffer, 0, length) + "\n");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mService.disconnect();
    }

    @Override
    public void onStatusChange(BluetoothStatus status) {
        Log.d(TAG, "onStatusChange: " + status);
    }

    @Override
    public void onDeviceName(String deviceName) {
        Log.d(TAG, "onDeviceName: " + deviceName);
    }

    @Override
    public void onToast(String message) {
        Log.d(TAG, "onToast");
    }

    @Override
    public void onDataWrite(byte[] buffer) {
        Log.d(TAG, "onDataWrite");
        mEdRead.append("> " + new String(buffer));
    }

    @Override
    public void onClick(View v) {
        mWriter.writeln(mEdWrite.getText().toString());
        mEdWrite.setText("");
    }
}
