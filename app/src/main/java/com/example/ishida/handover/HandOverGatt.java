package com.example.ishida.handover;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.List;

/**
 * Created by ishida on 2015/02/19.
 */
public class HandOverGatt {
    private static final String TAG = HandOverGatt.class.getSimpleName();

    private BluetoothManager bTManager;
    private BluetoothAdapter bTAdapter;
    private BluetoothGatt bluetoothGatt;
    BluetoothGattCallback gattCallback;
    private Context context;

    public HandOverGatt(Context context, BluetoothManager manager, BluetoothAdapter adapter) {
        this.context = context;
        this.bTManager = manager;
        this.bTAdapter = adapter;

        init();
    }

    private void init() {
        gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.d(TAG, "onConnectionStateChange: " + status + "->" + newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Connected to GATT server.");
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from GATT server.");
                }
            }

            @Override
            public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
                Log.d(TAG, "onServicesDiscovered: ");
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    List<BluetoothGattService> services = gatt.getServices();
                    for (BluetoothGattService service: services) {
                        Log.d(TAG, service.toString());
                        if (service.getUuid().equals(HandOverGattServer.service_uuid)) {
                            BluetoothGattCharacteristic characteristic = service.getCharacteristic(HandOverGattServer.field1_characteristic_uuid);
                            if (characteristic != null) {
                                Log.d(TAG, "Found Characteristic 1");
                                gatt.readCharacteristic(characteristic);
                                int prop = characteristic.getProperties();
                                if ((prop | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                    gatt.setCharacteristicNotification(characteristic, true);
                                }
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "stat = " + status);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d(TAG, "onCharacteristicRead: ");
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d(TAG, "onCharacteristicWrite: " + status + ", " + characteristic.getStringValue(0));
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                Log.d(TAG, "onCharacteristicChanged: ");
            }
        };
    }
}
