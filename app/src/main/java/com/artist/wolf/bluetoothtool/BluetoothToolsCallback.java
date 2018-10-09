package com.artist.wolf.bluetoothtool;

import com.artist.wolf.bluetoothtool.domain.BluetoothDeviceInfo;

import java.util.HashMap;

/**
 * Created by lukefan on 2018/6/1.
 */

public abstract class BluetoothToolsCallback implements BluetoothToothListener {

    public abstract void onRead(BluetoothDeviceInfo bluetoothDeviceInfo);

    public abstract void onBatchRead(HashMap<String, BluetoothDeviceInfo> bluetoothDeviceInfoHashMap);
}
