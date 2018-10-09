package com.artist.wolf.bluetoothtool;

import com.artist.wolf.bluetoothtool.domain.BluetoothDeviceInfo;

/**
 * Created by lukefan on 2018/6/1.
 */

public interface BluetoothToolsConnectionStatusListener {

    public static final String PORPERTY = "BluetoothToolsConnectionStatusListener";

    void onConnectStatusChange(BluetoothDeviceInfo bluetoothDeviceInfo);

    BluetoothDeviceInfo onConnectSuccess(BluetoothDeviceInfo bluetoothDeviceInfo);

    void onConnectFail(BluetoothDeviceInfo bluetoothDeviceInfo, String message);

    void onDisconnect(BluetoothDeviceInfo bluetoothDeviceInfo);

}
