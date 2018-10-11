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

package com.github.douglasjunior.bluetoothsamplekotlin

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast

import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothDeviceDecorator
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus

import java.util.Arrays

class MainActivity : AppCompatActivity(), BluetoothService.OnBluetoothScanCallback, BluetoothService.OnBluetoothEventCallback, DeviceItemAdapter.OnAdapterItemClickListener {

    private var pgBar: ProgressBar? = null
    private var mMenu: Menu? = null
    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: DeviceItemAdapter? = null

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mService: BluetoothService? = null
    private var mScanning: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        pgBar = findViewById<View>(R.id.pg_bar) as ProgressBar
        pgBar!!.visibility = View.GONE

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        mRecyclerView = findViewById<View>(R.id.rv) as RecyclerView
        val lm = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        mRecyclerView!!.layoutManager = lm

        mAdapter = DeviceItemAdapter(this, mBluetoothAdapter!!.bondedDevices)
        mAdapter!!.setOnAdapterItemClickListener(this)
        mRecyclerView!!.adapter = mAdapter

        mService = BluetoothService.getDefaultInstance()

        mService!!.setOnScanCallback(this)
        mService!!.setOnEventCallback(this)
    }

    override fun onResume() {
        super.onResume()
        mService!!.setOnEventCallback(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        mMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_scan) {
            startStopScan()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun startStopScan() {
        if (!mScanning) {
            mService!!.startScan()
        } else {
            mService!!.stopScan()
        }
    }

    override fun onDeviceDiscovered(device: BluetoothDevice, rssi: Int) {
        Log.d(TAG, "onDeviceDiscovered: " + device.name + " - " + device.address + " - " + Arrays.toString(device.uuids))
        val dv = BluetoothDeviceDecorator(device, rssi)
        val index = mAdapter!!.devices.indexOf(dv)
        if (index < 0) {
            mAdapter!!.devices.add(dv)
            mAdapter!!.notifyItemInserted(mAdapter!!.devices.size - 1)
        } else {
            mAdapter!!.devices[index].device = device
            mAdapter!!.devices[index].rssi = rssi
            mAdapter!!.notifyItemChanged(index)
        }
    }

    override fun onStartScan() {
        Log.d(TAG, "onStartScan")
        mScanning = true
        pgBar!!.visibility = View.VISIBLE
        mMenu!!.findItem(R.id.action_scan).setTitle(R.string.action_stop)
    }

    override fun onStopScan() {
        Log.d(TAG, "onStopScan")
        mScanning = false
        pgBar!!.visibility = View.GONE
        mMenu!!.findItem(R.id.action_scan).setTitle(R.string.action_scan)
    }

    override fun onDataRead(buffer: ByteArray, length: Int) {
        Log.d(TAG, "onDataRead")
    }

    override fun onStatusChange(status: BluetoothStatus) {
        Log.d(TAG, "onStatusChange: $status")
        Toast.makeText(this, status.toString(), Toast.LENGTH_SHORT).show()

        if (status == BluetoothStatus.CONNECTED) {
            val colors = arrayOf<CharSequence>("Try text", "Try picture")

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select")
            builder.setItems(colors) { dialog, which ->
                if (which == 0) {
                    startActivity(Intent(this@MainActivity, DeviceActivity::class.java))
                } else {
                    startActivity(Intent(this@MainActivity, BitmapActivity::class.java))
                }
            }
            builder.setCancelable(false)
            builder.show()
        }

    }

    override fun onDeviceName(deviceName: String) {
        Log.d(TAG, "onDeviceName: $deviceName")
    }

    override fun onToast(message: String) {
        Log.d(TAG, "onToast")
    }

    override fun onDataWrite(buffer: ByteArray) {
        Log.d(TAG, "onDataWrite")
    }

    override fun onItemClick(device: BluetoothDeviceDecorator, position: Int) {
        mService!!.connect(device.device)
    }

    companion object {

        val TAG = "BluetoothExampleKotlin"
    }
}
