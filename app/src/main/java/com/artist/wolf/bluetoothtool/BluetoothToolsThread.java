package com.artist.wolf.bluetoothtool;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.HandlerThread;
import android.util.Log;

import com.artist.wolf.bluetoothtool.domain.BluetoothDeviceInfo;
import com.artist.wolf.bluetoothtool.exception.BluetoothToolsCallbackException;
import com.artist.wolf.bluetoothtool.exception.BluetoothToolsDeviceObjectException;
import com.artist.wolf.bluetoothtool.exception.BluetoothToolsListenerException;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * Created by lukefan on 2018/5/30.
 */

public class BluetoothToolsThread extends HandlerThread {
    private static final String THREAD_NAME = "BluetoothToolsThread";

    private BluetoothDeviceInfo bluetoothDeviceInfo;
    private BluetoothToolsCallback bluetoothToolsCallback;
    private BluetoothToolsConnectionStatusListener bluetoothToolsConnectionStatusListener;

    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            BluetoothDeviceInfo bluetoothDeviceInfo = new BluetoothDeviceInfo(gatt, status, newState);

            bluetoothToolsConnectionStatusListener.onConnectStatusChange(bluetoothDeviceInfo);
            switch (bluetoothDeviceInfo.getGattStatus()) {
                case BluetoothGatt.GATT_SUCCESS:
                    switch (bluetoothDeviceInfo.getProFileStatus()) {
                        case BluetoothProfile.STATE_CONNECTED:
                            bluetoothToolsConnectionStatusListener.onConnectSuccess(bluetoothDeviceInfo);
                            break;
                        case BluetoothProfile.STATE_DISCONNECTED:
                            gatt.close();
                            bluetoothToolsConnectionStatusListener.onDisconnect(bluetoothDeviceInfo);
                            break;
                        default:
                            gatt.close();
                            bluetoothToolsConnectionStatusListener.onConnectFail(bluetoothDeviceInfo,
                                    String.format("[onConnectionStateChange] BluetoothGatt is GATT_SUCCESS, Device %s(%s), Gatt status : %d, Profile status : %d",
                                    bluetoothDeviceInfo.getBluetoothDevice().getName(),
                                            bluetoothDeviceInfo.getBluetoothDevice().getAddress(),
                                            status,
                                            newState));
                            break;
                    }
                    break;
                case 8:
                    gatt.close();
                    bluetoothToolsConnectionStatusListener.onDisconnect(bluetoothDeviceInfo);
                    break;
                case 133:
                    gatt.close();
                    bluetoothToolsConnectionStatusListener.onConnectFail(bluetoothDeviceInfo, String.format("Device %s(%s), Gatt status : %d", bluetoothDeviceInfo.getBluetoothDevice().getName(), bluetoothDeviceInfo.getBluetoothDevice().getAddress(), status));
                    break;
                default:
                    gatt.close();
                    bluetoothToolsConnectionStatusListener.onConnectFail(bluetoothDeviceInfo,  String.format("Device %s(%s), Gatt status : %d", bluetoothDeviceInfo.getBluetoothDevice().getName(), bluetoothDeviceInfo.getBluetoothDevice().getAddress(), status));
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            try {
                BluetoothGattService bluetoothGattService = gatt.getService(convertFromInteger(bluetoothDeviceInfo.getServiceUUID()));
                if (bluetoothGattService == null) {
                    bluetoothToolsCallback.onError("BluetoothService is null, Please check service UUID");
                    bluetoothToolsCallback.onRead(bluetoothDeviceInfo);
                } else {
                    BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(convertFromInteger(bluetoothDeviceInfo.getCharacteristicUUID()));
                    if (bluetoothGattCharacteristic == null) {
                        bluetoothToolsCallback.onError("BluetoothGattCharacteristic is null, Please check characteristic UUID");
                        bluetoothToolsCallback.onRead(bluetoothDeviceInfo);
                    } else {
                        gatt.readCharacteristic(bluetoothGattCharacteristic);
                    }
                }
            } catch (NullPointerException e) {
                bluetoothToolsCallback.onError(e.getMessage());
                bluetoothToolsCallback.onRead(bluetoothDeviceInfo);
            } catch (Exception e) {
                bluetoothToolsCallback.onError(e.getMessage());
                bluetoothToolsCallback.onRead(bluetoothDeviceInfo);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            String value = "";
            try {
                value = new String(characteristic.getValue(), "UTF-8");
                Log.i(THREAD_NAME, "[onCharacteristicRead] " + value);
            } catch (UnsupportedEncodingException e) {
                bluetoothToolsCallback.onError(e.getMessage());
            } catch (Exception e) {
                bluetoothToolsCallback.onError(e.getMessage());
            } finally {
                bluetoothDeviceInfo.setValue(value);
                bluetoothToolsCallback.onRead(bluetoothDeviceInfo);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }
    };

    public BluetoothToolsThread() {
        super(THREAD_NAME);
    }

    public BluetoothToolsThread(BluetoothToolsCallback bluetoothToolsCallback, BluetoothToolsConnectionStatusListener bluetoothToolsConnectionStatusListener) {
        super(THREAD_NAME);
        this.bluetoothToolsCallback = bluetoothToolsCallback;
        this.bluetoothToolsConnectionStatusListener = bluetoothToolsConnectionStatusListener;
    }

    public void connect(Context context, BluetoothDevice bluetoothDevice) throws BluetoothToolsDeviceObjectException, BluetoothToolsCallbackException, BluetoothToolsListenerException {
        if (bluetoothDevice == null) {
            throw new BluetoothToolsDeviceObjectException("BluetoothDevice can't null");
        }

        if (bluetoothToolsCallback == null) {
            throw new BluetoothToolsCallbackException("BluetoothTools Callback can't null");
        }

        if (this.bluetoothToolsConnectionStatusListener == null) {
            throw new BluetoothToolsListenerException("BluetoothTools Listener can't null");
        }

        bluetoothDevice.connectGatt(context, false, bluetoothGattCallback);
    }

    public void readData(BluetoothDeviceInfo bluetoothDeviceInfo) {
        this.bluetoothDeviceInfo = bluetoothDeviceInfo;
        bluetoothDeviceInfo.getBluetoothGatt().discoverServices();
    }

    private UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }
}
