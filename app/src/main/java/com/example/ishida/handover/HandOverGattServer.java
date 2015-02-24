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
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by ishida on 2015/02/19.
 */
class HandOverGattServer {
    private static final String TAG = HandOverGattServer.class.getSimpleName();

    static final UUID service_uuid = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    static final UUID field1_characteristic_uuid = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb"); // activity or name of dictionary
    static final UUID field2_characteristic_uuid = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb"); // data type
    static final UUID field3_characteristic_uuid = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb"); // data

    enum DataType {
        UNKNOWN(0),
        BOOLEAN(1),
        SHORT(2),
        INT(3),
        LONG(4),
        FLOAT(5),
        DOUBLE(6),
        STRING(7);

        private final int id;

        private DataType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static DataType valueOf(int id) {
            // values() で、列挙したオブジェクトをすべて持つ配列が得られる
            for (DataType num : values()) {
                if (num.getId() == id) { // id が一致するものを探す
                    return num;
                }
            }

            return UNKNOWN;
        }
    }


    private AdvertiseCallback advCallback;
    private BluetoothGattServerCallback gattCallback;
    private BluetoothManager bTManager;
    private BluetoothAdapter bTAdapter;
    private BluetoothGattServer gattServer;
    private Context context;
    private String activityName;
    private Map<String, Object> dictionary;
    private Set<String> keySet = null;
    private Iterator<String> iterator = null;
    private String dictKey;
    private Handler handler;


    public HandOverGattServer(Context context, BluetoothManager manager, BluetoothAdapter adapter) {
        this.context = context;
        this.handler = new Handler(context.getMainLooper());
        this.bTManager = manager;
        this.bTAdapter = adapter;

        init();
    }

    public void setData(String activity, Map<String, Object> map) {
        this.activityName = activity;
        this.dictionary = map;
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

        // This should be enabled when BLE adv above Lollipop is supported

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
                    // if someone connects then we should stop BLE adv here
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // maybe need to clean up staff
                }

            }

            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "onServiceAdded: status=GATT_SUCCESS service="
                            + service.getUuid().toString());
                    // call callback here
                    ((HandOverService)context).gattServerReady();
                } else {
                    Log.d(TAG, "onServiceAdded: status!=GATT_SUCCESS");
                }

            }

            /*
            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                Log.d(TAG, "onCharacteristicReadRequest: requestId=" + requestId + " offset=" + offset);
                Log.d(TAG, "uuid: " + characteristic.getUuid().toString());
                if (characteristic.getUuid().equals(field1_characteristic_uuid)) {
                    Log.d(TAG, device.getName() + " is reading characteristic field1");
                    characteristic.setValue(activityName);
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
                } else if (characteristic.getUuid().equals(field2_characteristic_uuid)) {
                    Log.d(TAG, device.getName() + " is reading characteristic field2");
                    String str = (String) dictionary.get("edit_text");
                    Log.d(TAG, "edit_text = " + str);
                    characteristic.setValue(str);
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
                } else if (characteristic.getUuid().equals(field3_characteristic_uuid)) {
                    Log.d(TAG, device.getName() + " is reading characteristic field3");
                    boolean sw = (boolean) dictionary.get("switch");
                    int i = sw ? 1 : 0;
                    Log.d(TAG, "switch = " + i);
                    characteristic.setValue(i, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
                }
            }
            */

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                Log.d(TAG, "onCharacteristicReadRequest: requestId=" + requestId + " offset=" + offset);
                Log.d(TAG, "uuid: " + characteristic.getUuid().toString());
                if (characteristic.getUuid().equals(field1_characteristic_uuid)) {
                    Log.d(TAG, device.getName() + " is reading characteristic field1");
                    if (keySet == null ) { // we should return activity here
                        Log.d(TAG, "Set activity: " + activityName);
                        characteristic.setValue(activityName);
                        keySet = dictionary.keySet();
                        iterator = keySet.iterator();
                    } else {
                        if (iterator.hasNext()) { // there's more entry
                            dictKey = iterator.next();
                            Log.d(TAG, "Set entry name: " + dictKey);
                            characteristic.setValue(dictKey);
                        } else { // all entry in dictionary is sent
                            Log.d(TAG, "Set DONE");
                            characteristic.setValue("DONE");
                            keySet = null;
                            iterator = null;
                        }
                    }
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
                } else if (characteristic.getUuid().equals(field2_characteristic_uuid)) {
                    Log.d(TAG, device.getName() + " is reading characteristic field2");
                    Object obj = dictionary.get(dictKey);
                    Log.d(TAG, "Set object type: " + obj.getClass().getName());
                    int i = getDataType(obj).getId();
                    characteristic.setValue(i, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
                } else if (characteristic.getUuid().equals(field3_characteristic_uuid)) {
                    Log.d(TAG, device.getName() + " is reading characteristic field3");
                    setCharacteristicDataField(characteristic);
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

    private void setCharacteristicDataField(BluetoothGattCharacteristic characteristic) {
        Object obj = dictionary.get(dictKey);
        switch (getDataType(obj)) {
            case BOOLEAN:
                boolean bool = (boolean)obj;
                Log.d(TAG, Boolean.toString(bool));
                int i = bool ? 1 : 0;
                characteristic.setValue(i, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                break;
            case SHORT:
                short s = (short)obj;
                Log.d(TAG, Short.toString(s));
                characteristic.setValue(s, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                break;
            case INT:
                i = (int)obj;
                Log.d(TAG, Integer.toString(i));
                characteristic.setValue(i, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
                break;
            case LONG:
                Log.d(TAG, "Long not supported yet");
                break;
            case STRING:
                String str = (String)obj;
                Log.d(TAG, str);
                characteristic.setValue(str);
                break;
            case FLOAT:
                float f = (float)obj;
                Log.d(TAG, Float.toString(f));
                i = Float.floatToIntBits(f);
                characteristic.setValue(i, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
                break;
            case DOUBLE:
                Log.d(TAG, "Double not supported yet");
                break;
            default:
                Log.d(TAG, "Error! Something going wrong data object type mismatch");
        }
    }

    private DataType getDataType(Object obj) {
        DataType ret = DataType.UNKNOWN;
        if (obj instanceof Boolean) {
            ret = DataType.BOOLEAN;
        } else if (obj instanceof Short) {
            ret = DataType.SHORT;
        } else if (obj instanceof  Integer) {
            ret = DataType.INT;
        } else if (obj instanceof Long) {
            ret = DataType.LONG;
        } else if (obj instanceof String) {
            ret = DataType.STRING;
        } else if (obj instanceof Float) {
            ret = DataType.FLOAT;
        } else if (obj instanceof Double) {
            ret = DataType.DOUBLE;
        }
        return ret;
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
