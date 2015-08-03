package com.example.ishida.handover;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.util.Map;

public class HandOverService extends Service {

    private static final String TAG = HandOverService.class.getSimpleName();

    // assumes BT MAC addrs are exchanged wither by Nfc or BLE adv.
    // must use capital A-F
    private static final String addrs[] = {
        "58:3F:54:4A:9F:B0", // Nexus 5
        "D4:0B:1A:16:89:91", // Nexus 9

    };

    private BluetoothManager bTManager;
    private BluetoothAdapter bTAdapter;
    private String myAddr;
    private IHandOverCallback callback = null;

    private HandOverGattServer gattServer;
    private HandOverGatt bluetoothGatt;

    private Handler handler;

    private RemoteCallbackList<IHandOverCallback> remoteCallbackList = new RemoteCallbackList<IHandOverCallback>();

    //ACTION_SCREEN receiver which triggers to discover HandOver GATT service
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

    // 
    private IHandOverService.Stub stub = new IHandOverService.Stub() {

        @Override
        public void registerCallback(IHandOverCallback callback) throws RemoteException {
            Log.d(TAG, "A callback registered");
            //HandOverService.this.remoteCallbackList.register(callback);
            HandOverService.this.callback = callback;
        }

        @Override
        public void unregisterCallback(IHandOverCallback callback) throws RemoteException {
            Log.d(TAG, "A callback unregistered");
            //HandOverService.this.remoteCallbackList.unregister(callback);
            if (HandOverService.this.callback.equals(callback)) {
                HandOverService.this.callback = null;
            }
        }

        @Override
        public void handOver(ComponentName componentName, Map dictionary) throws RemoteException {
            Log.d(TAG, componentName + ": " + dictionary);
            gattServer.setData(componentName, dictionary);
        }

        @Override
        public void activityChanged() throws RemoteException {
            // start GATT server
            if (gattServer == null) {
                gattServer = new HandOverGattServer(HandOverService.this, bTManager, bTAdapter);
                gattServer.startGattServer();
            } else { // gatt server already running
                Log.d(TAG, "Gatt server is already running");
                gattServerReady();
            }
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
        gattServer = new HandOverGattServer(HandOverService.this, bTManager, bTAdapter);
        gattServer.startGattServer();

        myAddr = bTAdapter.getAddress();
        Log.d(TAG, "MyAddr: " + myAddr);
        Log.d(TAG, "Initialized");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "start command:" + intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroyed");
        unregisterReceiver(screenStatusReceiver);
        // should clean up gatt server
        if (gattServer != null) {
            gattServer.stopGattServer();
            gattServer = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: " + intent);
        return stub;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: " + intent);
        // should clean up gatt server
        //if (gattServer != null) {
        //    gattServer.stopGattServer();
        //    gattServer = null;
        //}

        return super.onUnbind(intent);
    }

    void gattServerReady() {
        if (callback != null) {
            try {
                Log.d(TAG, "gattServerReady: " + Thread.currentThread().toString());
                callback.handleHandOver();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


    void restoreReady(Map<String, Object> dictionary) {
        if (callback != null) {
            Log.d(TAG, "call handleRestore");
            try {
                callback.handleRestore(dictionary);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "callback is null");
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
        if (bluetoothGatt != null) {
            bluetoothGatt.closeGatt();
            bluetoothGatt = null;
        }
    }
}
