package com.example.ishida.handover;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;

/**
 * Created by ishida on 2015/02/19.
 */
class HandOverGattServer {
    private static final String TAG = HandOverGattServer.class.getSimpleName();

    private static final UUID service_uuid = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    private static final UUID field1_characteristic_uuid = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    private static final UUID field2_characteristic_uuid = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    private static final UUID field3_characteristic_uuid = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");


    private AdvertiseCallback advCallback;
    private BluetoothGattServerCallback gattCallback;
    private BluetoothManager bTManager;
    private BluetoothAdapter bTAdapter;
    private BluetoothGattServer gattServer;
    private Context context;
    private String activityName;


    public HandOverGattServer(Context context, BluetoothManager manager, BluetoothAdapter adapter, String activity) {
        this.context = context;
        this.bTManager = manager;
        this.bTAdapter = adapter;
        this.activityName = activity;

        init();
    }

    private void init() {
        // BLE check
        if (!isBLESupported()) {
            Toast.makeText(context, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            Log.d(TAG, context.getString(R.string.ble_not_supported));
            return;
        }

        // BT check
        if (bTManager != null) {
            bTAdapter = bTManager.getAdapter();
        }
        if (bTAdapter == null) {
            Toast.makeText(context, R.string.bt_unavailable, Toast.LENGTH_SHORT).show();
            Log.d(TAG, context.getString(R.string.bt_unavailable));
            return;
        }

        // BT LE adv check
        //if (!bTAdapter.isMultipleAdvertisementSupported()) {
        //    Toast.makeText(context, R.string.ble_adv_not_supported, Toast.LENGTH_SHORT).show();
        //    Log.d(TAG, context.getString(R.string.ble_adv_not_supported));
        //    return;
        //}

        //advCallback = new AdvertiseCallback() {
        //    @Override
        //    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
        //        if (settingsInEffect != null) {
        //            Log.d(TAG, settingsInEffect.toString());
        //        } else {
        //            Log.d(TAG, "onStartSuccess: settingInEffect = null");
        //        }
        //    }

        //    @Override
        //    public void onStartFailure(int errorCode) {
        //        Log.d(TAG, "onStartFailure: errorCode = " + errorCode);
        //    }
        //};

        gattCallback = new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                Log.d(TAG, "onConnectionStateChange: " + device.getName() + " status=" + status + "->" + newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                }

            }

            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "onServiceAdded: status=GATT_SUCCESS service="
                            + service.getUuid().toString());
                } else {
                    Log.d(TAG, "onServiceAdded: status!=GATT_SUCCESS");
                }

            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                Log.d(TAG, "onCharacteristicReadRequest: requestId=" + requestId + " offset=" + offset);
                Log.d(TAG, "uuid: " + characteristic.getUuid().toString());
                if (characteristic.getUuid().equals(field1_characteristic_uuid)) {
                    Log.d(TAG, device.getName() + " is reading characteristic field1");
                    characteristic.setValue("Vow");
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
                } else if (characteristic.getUuid().equals(field2_characteristic_uuid)) {
                    Log.d(TAG, device.getName() + " is reading characteristic field2");
                    characteristic.setValue("Meow");
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
                }
            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                Log.d(TAG, "onCharacteristicWriteRequest: requestId=" + requestId + " preparedWrite="
                        + Boolean.toString(preparedWrite) + " responseNeeded="
                        + Boolean.toString(responseNeeded) + " offset=" + offset);



            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                Log.d(TAG, "onDescriptorReadRequest: ");
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                Log.d(TAG, "onDescriptorWriteRequest: ");
            }

            @Override
            public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
                Log.d(TAG, "onExecuteWrite: ");
            }

            @Override
            public void onNotificationSent(BluetoothDevice device, int status) {
                Log.d(TAG, "onNotificationSent: " + device.getName());
            }
        };

        Log.d(TAG,context.getString(R.string.ble_initialized));
    }

    /** check if BLE Supported device */
    private boolean isBLESupported() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public void startGattServer() {
        gattServer = bTManager.openGattServer(context, gattCallback);

        BluetoothGattService gs = new BluetoothGattService(
                service_uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY
        );

        BluetoothGattCharacteristic gc1 = new BluetoothGattCharacteristic(
                field1_characteristic_uuid, BluetoothGattCharacteristic.PROPERTY_READ|BluetoothGattCharacteristic.PROPERTY_NOTIFY|BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ|BluetoothGattCharacteristic.PERMISSION_WRITE
        );

        BluetoothGattCharacteristic gc2 = new BluetoothGattCharacteristic(
                field2_characteristic_uuid, BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
        );

        BluetoothGattCharacteristic gc3 = new BluetoothGattCharacteristic(
                field3_characteristic_uuid, BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
        );

        gs.addCharacteristic(gc1);
        gs.addCharacteristic(gc2);
        gs.addCharacteristic(gc3);
        gattServer.addService(gs);
    }

    public void stopGattServer() {
        if (gattServer != null) {
            gattServer.clearServices();
            gattServer.close();
            gattServer = null;
        }
    }
}
