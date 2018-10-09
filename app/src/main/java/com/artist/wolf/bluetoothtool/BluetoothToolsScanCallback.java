package com.artist.wolf.bluetoothtool;

import android.bluetooth.BluetoothDevice;

import java.util.HashMap;

/**
 * Created by lukefan on 2018/6/1.
 */

public abstract class BluetoothToolsScanCallback implements BluetoothToothListener {

    public abstract void onScanResults(HashMap<String, BluetoothDevice> bluetoothDeviceList);
}
