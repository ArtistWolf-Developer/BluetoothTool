package com.artist.wolf.bluetoothtool;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.artist.wolf.bluetoothtool.domain.BluetoothDeviceInfo;
import com.artist.wolf.bluetoothtool.exception.BluetoothToolsCallbackException;
import com.artist.wolf.bluetoothtool.exception.BluetoothToolsDeviceObjectException;
import com.artist.wolf.bluetoothtool.exception.BluetoothToolsListenerException;

import java.util.HashMap;
import java.util.List;

/**
 * Created by lukefan on 2018/5/30.
 */

public class BluetoothTools extends BluetoothToolsCallback implements BluetoothToothListener, BluetoothToolsConnectionStatusListener {

    private static final String TAG = "BluetoothTools";
    private static final String ERROR_MESSAGE_NOT_SUPPORT_BLUETOOTH = "It is not support bluetooth";
    private static final String ERROR_MESSAGE_BLUETOOTH_IS_NOT_OPEN = "Bluetooth is not open";
    private static final int DEFAULT_SCAN_TIME = 15;
    public static final int ENABLE_BT_REQ = 0;
    public static final String DEVICE_NAME = "LiteON";//TN01";
    public static final String DEVICE_NAME_NEW = "ST1_";
    public static final int SERVICE_UUID = 0x180A;
    public static final int CHARACTERISTIC_UUID = 0x2A25;

    private static BluetoothTools bluetoothTools = new BluetoothTools();
    private static Object lock = new Object();

    private BluetoothAdapter bluetoothAdapter;
    private Handler handler;
    private BluetoothToolsThread bluetoothToolsThread;

    private HashMap<String, BluetoothDevice> scanResultList;
    private HashMap<String, BluetoothDeviceInfo> bluetoothDeviceInfoHashMap;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {

            } else {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (action) {
                    case BluetoothDevice.ACTION_FOUND:
                        Log.i(TAG, "[onReceive][ACTION_FOUND] Device name : " + bluetoothDevice.getName() + ", Device mac address : " + bluetoothDevice.getAddress());
                        scanResultList.put(bluetoothDevice.getAddress(), bluetoothDevice);
                        break;
                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                        Log.i(TAG, "[onReceive][ACTION_ACL_CONNECTED] Device name : " + bluetoothDevice.getName() + ", Device mac address : " + bluetoothDevice.getAddress());
                        break;
                    case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED:
                        Log.i(TAG, "[onReceive][ACTION_ACL_DISCONNECT_REQUESTED] Device name : " + bluetoothDevice.getName() + ", Device mac address : " + bluetoothDevice.getAddress());
                        break;
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                        Log.i(TAG, "[onReceive][ACTION_ACL_DISCONNECTED] Device name : " + bluetoothDevice.getName() + ", Device mac address : " + bluetoothDevice.getAddress());
                        break;
                    default:
                        break;
                }
            }
        }
    };

    private BluetoothToolsScanCallback bluetoothToolsScanCallback;
    private BluetoothToolsCallback bluetoothToolsCallback;
    private BluetoothToolsConnectionStatusListener bluetoothToolsConnectionStatusListener;

    private BluetoothTools() {
    }

    public static BluetoothTools getInstance() {
        return bluetoothTools;
    }

    public void init() {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (this.bluetoothAdapter == null) {
            onError(ERROR_MESSAGE_NOT_SUPPORT_BLUETOOTH);
        }

        if (!isEnableBluetooth()) {
            onError(ERROR_MESSAGE_BLUETOOTH_IS_NOT_OPEN);
        }

        if (this.bluetoothToolsThread != null && this.bluetoothToolsThread.isAlive()) {
            this.bluetoothToolsThread.quit();
        }
        this.bluetoothToolsThread = new BluetoothToolsThread(this, this);
        this.bluetoothToolsThread.start();
        this.handler = new Handler(this.bluetoothToolsThread.getLooper());
    }

    public boolean isEnableBluetooth() {
        return this.bluetoothAdapter != null && this.bluetoothAdapter.isEnabled();
    }

    public void startScan(Context context, BluetoothToolsScanCallback bluetoothToolsScanCallback) {
        startScan(DEFAULT_SCAN_TIME, context, bluetoothToolsScanCallback);
    }

    public void startScan(int scanTime, final Context context, BluetoothToolsScanCallback bluetoothToolsScanCallback) {
        Log.i(TAG, "[startScan] Start scan");
        if (this.bluetoothAdapter == null) {
            onError(ERROR_MESSAGE_NOT_SUPPORT_BLUETOOTH);
        } else {
            this.bluetoothToolsScanCallback = bluetoothToolsScanCallback;
            if (this.scanResultList == null) {
                this.scanResultList = new HashMap<>();
            }

            this.bluetoothAdapter.startDiscovery();

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            context.getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScan(context);
                }
            }, scanTime * 1000);
        }
    }

    public void stopScan(Context context) {
        Log.i(TAG, "[stopScan] Stop scan");
        bluetoothAdapter.cancelDiscovery();
        try {
            context.getApplicationContext().unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "[stopScan][IllegalArgumentException]", e);
        } catch (Exception e) {
            Log.e(TAG, "[stopScan][Exception]", e);
        } finally {
            if (this.bluetoothToolsScanCallback != null) {
                this.bluetoothToolsScanCallback.onScanResults(this.scanResultList);
            }
        }
    }

    public void connect(final Context context, final BluetoothDevice bluetoothDevice, BluetoothToolsConnectionStatusListener bluetoothToolsConnectionStatusListener) throws BluetoothToolsCallbackException, BluetoothToolsListenerException, BluetoothToolsDeviceObjectException {
        if (bluetoothDevice == null) {
            throw new BluetoothToolsDeviceObjectException("BluetoothDevice can't null");
        }

        if (bluetoothToolsConnectionStatusListener == null) {
            throw new BluetoothToolsListenerException("BluetoothTools Listener is null");
        }

        this.bluetoothToolsCallback = null;
        this.bluetoothToolsConnectionStatusListener = bluetoothToolsConnectionStatusListener;

        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    bluetoothToolsThread.connect(context, bluetoothDevice);

                    synchronized (lock) {
                        lock.wait();
                    }
                } catch (BluetoothToolsDeviceObjectException e) {
                    onError(e.getMessage());
                } catch (BluetoothToolsCallbackException e) {
                    onError(e.getMessage());
                } catch (InterruptedException e) {
                    onError(e.getMessage());
                } catch (BluetoothToolsListenerException e) {
                    onError(e.getMessage());
                }
            }
        });
    }

    public void connect(final Context context, final BluetoothDevice bluetoothDevice, BluetoothToolsCallback bluetoothToolsCallback, BluetoothToolsConnectionStatusListener bluetoothToolsConnectionStatusListener) throws BluetoothToolsCallbackException, BluetoothToolsListenerException, BluetoothToolsDeviceObjectException {
        if (bluetoothDevice == null) {
            throw new BluetoothToolsDeviceObjectException("BluetoothDevice can't null");
        }

        if (bluetoothToolsCallback == null) {
            throw new BluetoothToolsCallbackException("BluetoothTools Callback is null");
        }

        if (bluetoothToolsConnectionStatusListener == null) {
            throw new BluetoothToolsListenerException("BluetoothTools Listener is null");
        }

        this.bluetoothToolsCallback = bluetoothToolsCallback;
        this.bluetoothToolsConnectionStatusListener = bluetoothToolsConnectionStatusListener;

        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    bluetoothToolsThread.connect(context, bluetoothDevice);

                    synchronized (lock) {
                        lock.wait();
                    }
                } catch (BluetoothToolsDeviceObjectException e) {
                    onError(e.getMessage());
                } catch (BluetoothToolsCallbackException e) {
                    onError(e.getMessage());
                } catch (InterruptedException e) {
                    onError(e.getMessage());
                } catch (BluetoothToolsListenerException e) {
                    onError(e.getMessage());
                }
            }
        });
    }

    public void batchConnect(final Context context, final List<BluetoothDevice> bluetoothDeviceList, BluetoothToolsConnectionStatusListener bluetoothToolsConnectionStatusListener) throws BluetoothToolsCallbackException, BluetoothToolsListenerException, BluetoothToolsDeviceObjectException {
        if (bluetoothDeviceList == null) {
            throw new BluetoothToolsDeviceObjectException("BluetoothDevice list can't null");
        }

        if (bluetoothDeviceList != null && bluetoothDeviceList.size() < 1) {
            throw new BluetoothToolsDeviceObjectException("BluetoothDevice list is empty");
        }

        if (bluetoothToolsConnectionStatusListener == null) {
            throw new BluetoothToolsListenerException("BluetoothTools Listener is null");
        }

        this.bluetoothToolsCallback = null;
        this.bluetoothToolsConnectionStatusListener = bluetoothToolsConnectionStatusListener;
        this.bluetoothDeviceInfoHashMap = new HashMap<>();

        handler.post(new Runnable() {
            @Override
            public void run() {
                for (BluetoothDevice bluetoothDevice : bluetoothDeviceList) {
                    try {
                        bluetoothToolsThread.connect(context, bluetoothDevice);

                        synchronized (lock) {
                            lock.wait();
                        }
                    } catch (BluetoothToolsDeviceObjectException e) {
                        onError(e.getMessage());
                    } catch (BluetoothToolsCallbackException e) {
                        onError(e.getMessage());
                    } catch (InterruptedException e) {
                        onError(e.getMessage());
                    } catch (BluetoothToolsListenerException e) {
                        onError(e.getMessage());
                    }
                }

                onBatchRead(bluetoothDeviceInfoHashMap);
            }
        });
    }

    public void batchConnect(final Context context, final List<BluetoothDevice> bluetoothDeviceList, BluetoothToolsCallback bluetoothToolsCallback, BluetoothToolsConnectionStatusListener bluetoothToolsConnectionStatusListener) throws BluetoothToolsCallbackException, BluetoothToolsListenerException, BluetoothToolsDeviceObjectException {
        if (bluetoothDeviceList == null) {
            throw new BluetoothToolsDeviceObjectException("BluetoothDevice list can't null");
        }

        if (bluetoothDeviceList != null && bluetoothDeviceList.size() < 1) {
            throw new BluetoothToolsDeviceObjectException("BluetoothDevice list is empty");
        }

        if (bluetoothToolsCallback == null) {
            throw new BluetoothToolsCallbackException("BluetoothTools Callback is null");
        }

        if (bluetoothToolsConnectionStatusListener == null) {
            throw new BluetoothToolsListenerException("BluetoothTools Listener is null");
        }

        this.bluetoothToolsCallback = bluetoothToolsCallback;
        this.bluetoothToolsConnectionStatusListener = bluetoothToolsConnectionStatusListener;
        this.bluetoothDeviceInfoHashMap = new HashMap<>();

        handler.post(new Runnable() {
            @Override
            public void run() {
                for (BluetoothDevice bluetoothDevice : bluetoothDeviceList) {
                    try {
                        bluetoothToolsThread.connect(context, bluetoothDevice);

                        synchronized (lock) {
                            lock.wait();
                        }
                    } catch (BluetoothToolsDeviceObjectException e) {
                        onError(e.getMessage());
                    } catch (BluetoothToolsCallbackException e) {
                        onError(e.getMessage());
                    } catch (InterruptedException e) {
                        onError(e.getMessage());
                    } catch (BluetoothToolsListenerException e) {
                        onError(e.getMessage());
                    }
                }

                onBatchRead(bluetoothDeviceInfoHashMap);
            }
        });
    }

    @Override
    public void onConnectStatusChange(BluetoothDeviceInfo bluetoothDeviceInfo) {
        if (this.bluetoothToolsConnectionStatusListener != null) {
            this.bluetoothToolsConnectionStatusListener.onConnectStatusChange(bluetoothDeviceInfo);
        }
    }

    @Override
    public BluetoothDeviceInfo onConnectSuccess(BluetoothDeviceInfo bluetoothDeviceInfo) {
        if (this.bluetoothToolsConnectionStatusListener != null) {
            BluetoothDeviceInfo cacheDevice = null;
            if (this.bluetoothDeviceInfoHashMap != null) {
                cacheDevice = this.bluetoothDeviceInfoHashMap.get(bluetoothDeviceInfo.getBluetoothDevice().getAddress());
            }
            if (cacheDevice != null && !TextUtils.isEmpty(cacheDevice.getValue())) {
                bluetoothDeviceInfo.setValue(cacheDevice.getValue());
            }
            BluetoothDeviceInfo device = this.bluetoothToolsConnectionStatusListener.onConnectSuccess(bluetoothDeviceInfo);
            if (device != null) {
                this.bluetoothToolsThread.readData(device);
            } else {
                onRead(bluetoothDeviceInfo);
            }
        }
        return null;
    }

    @Override
    public void onConnectFail(BluetoothDeviceInfo bluetoothDeviceInfo, String message) {
        if (this.bluetoothToolsConnectionStatusListener != null) {
            this.bluetoothToolsConnectionStatusListener.onConnectFail(bluetoothDeviceInfo, message);
        }

        onError(message);
        onRead(bluetoothDeviceInfo);
    }

    @Override
    public void onDisconnect(BluetoothDeviceInfo bluetoothDeviceInfo) {
        if (this.bluetoothToolsConnectionStatusListener != null) {
            BluetoothDeviceInfo cacheDevice = this.bluetoothDeviceInfoHashMap.get(bluetoothDeviceInfo.getBluetoothDevice().getAddress());
            if (cacheDevice != null && !TextUtils.isEmpty(cacheDevice.getValue())) {
                bluetoothDeviceInfo.setValue(cacheDevice.getValue());
            }
            this.bluetoothToolsConnectionStatusListener.onDisconnect(bluetoothDeviceInfo);
        }
    }

    @Override
    public void onError(String message) {
        if (this.bluetoothToolsCallback != null) {
            this.bluetoothToolsCallback.onError(message);
        }
    }

    @Override
    public void onRead(BluetoothDeviceInfo bluetoothDeviceInfo) {
        if (this.bluetoothToolsCallback != null) {
            this.bluetoothToolsCallback.onRead(bluetoothDeviceInfo);
        }

        if (this.bluetoothDeviceInfoHashMap == null) {
            this.bluetoothDeviceInfoHashMap = new HashMap<>();
        }

        BluetoothDeviceInfo device = this.bluetoothDeviceInfoHashMap.get(bluetoothDeviceInfo.getBluetoothDevice().getAddress());
        if (device != null) {
            device.setGattStatus(bluetoothDeviceInfo.getGattStatus());
            device.setProFileStatus(bluetoothDeviceInfo.getProFileStatus());
            this.bluetoothDeviceInfoHashMap.put(bluetoothDeviceInfo.getBluetoothDevice().getAddress(), device);
        } else {
            this.bluetoothDeviceInfoHashMap.put(bluetoothDeviceInfo.getBluetoothDevice().getAddress(), bluetoothDeviceInfo);
        }

        synchronized (lock) {
            lock.notifyAll();
        }
    }

    @Override
    public void onBatchRead(HashMap<String, BluetoothDeviceInfo> bluetoothDeviceInfoHashMap) {
        if (this.bluetoothToolsCallback != null) {
            this.bluetoothToolsCallback.onBatchRead(bluetoothDeviceInfoHashMap);
        }
    }

    public HashMap<String, BluetoothDeviceInfo> getBluetoothDeviceInfoHashMap() {
        if (this.bluetoothDeviceInfoHashMap == null) {
            return new HashMap<String, BluetoothDeviceInfo>();
        } else {
            return this.bluetoothDeviceInfoHashMap;
        }
    }

    public HashMap<String, BluetoothDevice> getScanResultList() {
        return scanResultList;
    }
}
