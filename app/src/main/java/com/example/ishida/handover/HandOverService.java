package com.example.ishida.handover;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.Map;

public class HandOverService extends Service implements HandOverGattServerCallback{

    private static final String TAG = HandOverService.class.getSimpleName();

    // assumes BT MAC addrs are exchanged wither by Nfc or BLE adv.
    private static final String addrs[] = {
        "F0:6B:CA:35:96:EC", // Galaxy S4
        "50:A4:C8:93:5C:CE", // Galaxy S3

    };

    private BluetoothManager bTManager;
    private BluetoothAdapter bTAdapter;
    private String myAddr;
    private IHandOverCallback callback = null;

    private HandOverGattServer gattServer;
    private HandOverGatt bluetoothGatt;

    private BroadcastReceiver screenStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Receive screen off
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                // OFF
                // stop scanning
                Log.d(TAG, "Screen off");
                stopScan();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                // ON
                // start scanning;
                Log.d(TAG, "Screen on");
                startScan();
            }
        }
    };

    private IHandOverService.Stub stub = new IHandOverService.Stub() {
        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

        }

        @Override
        public void registerCallback(IHandOverCallback callback) throws RemoteException {
            HandOverService.this.callback = callback;
        }

        @Override
        public void unregisterCallback(IHandOverCallback callback) throws RemoteException {
            if (callback != null && callback.equals(callback)) {
                HandOverService.this.callback = null;
            }
        }

        @Override
        public void handOver(String activityName, Map dictionary) throws RemoteException {
            Log.d(TAG, activityName + ": " + dictionary);
            gattServer.setData(activityName, dictionary);

        }

        @Override
        public void activityChanged() throws RemoteException {
            // start GATT server
            gattServer = new HandOverGattServer(HandOverService.this, bTManager, bTAdapter);
            gattServer.startGattServer();
        }

        @Override
        public Map readDictionary() throws RemoteException {
            if (bluetoothGatt != null) {
                return bluetoothGatt.dictionary;
            }
            return null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // set broadcast receiver to receive screen on/off
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenStatusReceiver, filter);

        // initialize
        bTManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bTAdapter = bTManager.getAdapter();
        myAddr = bTAdapter.getAddress();

        Log.d(TAG, "Initialized");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "start command");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroyed");
        unregisterReceiver(screenStatusReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return stub;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // should clean up gatt server
        return super.onUnbind(intent);
    }

    @Override
    public void gattServerReady() {
        if (callback != null) {
            try {
                callback.handleHandOver();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void startScan() {
        // scan HandOver host
        // scan BLE adv. or paired device
        bluetoothGatt = new HandOverGatt(this, bTManager, bTAdapter);
        for (String mac : addrs) {
            if (mac.equals(myAddr)) continue;
            BluetoothDevice device = bTAdapter.getRemoteDevice(mac);
            bluetoothGatt.connectGatt(device);
        }
    }

    private void stopScan() {
        // stop on going scan
        bluetoothGatt.closeGatt();
        bluetoothGatt = null;
    }
}
