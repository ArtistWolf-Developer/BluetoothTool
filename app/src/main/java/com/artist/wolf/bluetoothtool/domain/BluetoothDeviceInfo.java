package com.artist.wolf.bluetoothtool.domain;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by lukefan on 2018/5/30.
 */

public class BluetoothDeviceInfo implements Parcelable {

    public static final String PROPERTY = "BluetoothDeviceInfo";

    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice bluetoothDevice;
    private int gattStatus;
    private int proFileStatus;
    private String value;
    private int serviceUUID;
    private int characteristicUUID;
    private String deviceId;

    protected BluetoothDeviceInfo(Parcel in) {
        bluetoothDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
        gattStatus = in.readInt();
        proFileStatus = in.readInt();
        value = in.readString();
        serviceUUID = in.readInt();
        characteristicUUID = in.readInt();
        deviceId = in.readString();
    }

    public static final Creator<BluetoothDeviceInfo> CREATOR = new Creator<BluetoothDeviceInfo>() {
        @Override
        public BluetoothDeviceInfo createFromParcel(Parcel in) {
            return new BluetoothDeviceInfo(in);
        }

        @Override
        public BluetoothDeviceInfo[] newArray(int size) {
            return new BluetoothDeviceInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(bluetoothDevice, flags);
        dest.writeInt(gattStatus);
        dest.writeInt(proFileStatus);
        dest.writeString(value);
        dest.writeInt(serviceUUID);
        dest.writeInt(characteristicUUID);
        dest.writeString(deviceId);
    }

    public BluetoothDeviceInfo() {
        this.gattStatus = -1;
        this.proFileStatus = -1;
        this.value = "";
        this.deviceId = "";
    }

    public BluetoothDeviceInfo(BluetoothGatt bluetoothGatt) {
        this();
        this.bluetoothGatt = bluetoothGatt;
        this.bluetoothDevice = bluetoothGatt.getDevice();
    }

    public BluetoothDeviceInfo(BluetoothGatt bluetoothGatt, int gattStatus, int proFileStatus) {
        this(bluetoothGatt);
        this.bluetoothGatt = bluetoothGatt;
        this.bluetoothDevice = bluetoothGatt.getDevice();
        this.gattStatus = gattStatus;
        this.proFileStatus = proFileStatus;
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    public void setBluetoothGatt(BluetoothGatt bluetoothGatt) {
        this.bluetoothGatt = bluetoothGatt;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public int getGattStatus() {
        return gattStatus;
    }

    public void setGattStatus(int gattStatus) {
        this.gattStatus = gattStatus;
    }

    public int getProFileStatus() {
        return proFileStatus;
    }

    public void setProFileStatus(int proFileStatus) {
        this.proFileStatus = proFileStatus;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setServiceUUID(int serviceUUID) {
        this.serviceUUID = serviceUUID;
    }

    public void setCharacteristicUUID(int characteristicUUID) {
        this.characteristicUUID = characteristicUUID;
    }

    public int getServiceUUID() {
        return serviceUUID;
    }

    public int getCharacteristicUUID() {
        return characteristicUUID;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public String toString() {
        return "BluetoothDeviceInfo{" +
                "bluetoothGatt=" + bluetoothGatt +
                ", bluetoothDevice=" + bluetoothDevice +
                ", gattStatus=" + gattStatus +
                ", proFileStatus=" + proFileStatus +
                ", value='" + value + '\'' +
                ", serviceUUID=" + serviceUUID +
                ", characteristicUUID=" + characteristicUUID +
                ", deviceId='" + deviceId + '\'' +
                '}';
    }
}
