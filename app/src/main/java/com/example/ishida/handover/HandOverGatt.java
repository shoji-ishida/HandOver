package com.example.ishida.handover;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by ishida on 2015/02/19.
 */
public class HandOverGatt {
    private static final String TAG = HandOverGatt.class.getSimpleName();

    private BluetoothManager bTManager;
    private BluetoothAdapter bTAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCallback gattCallback;
    private Context context;
    private String activityName;
    private String packageName;
    private boolean activityNameRead = false;
    private boolean packageNameRead = false;
    private int longValue;
    private boolean longValueFlag = false;

    Map<String, Object> dictionary = new HashMap<String, Object>();

    private String fieldName;
    private HandOverGattServer.DataType fieldDataType;

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
                    Log.i(TAG, "Connected to GATT server. " + gatt.getDevice().getName());
                    bluetoothGatt = gatt;
                    //gatt.requestMtu(256);
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from GATT server. " + gatt.getDevice().getName());
                }
            }

            @Override
            public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
                Log.d(TAG, "onServicesDiscovered: ");
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BluetoothGattService service = gatt.getService(HandOverGattServer.service_uuid);
                    if (service != null) {
                        Log.d(TAG, "Found HandOver service");
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(HandOverGattServer.field1_characteristic_uuid);
                        if (characteristic != null) {
                            Log.d(TAG, "Found Characteristic 1");
                            boolean ret = gatt.readCharacteristic(characteristic);
                            if (!ret) {
                                Log.d(TAG, "Reading characteristic failed: " + characteristic);
                            }
                            int prop = characteristic.getProperties();
                            if ((prop | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                gatt.setCharacteristicNotification(characteristic, true);
                            }
                        }
                    } else {
                        Log.d(TAG, "Closing Gatt");
                        gatt.disconnect();
                        gatt.close();
                    }

                } else {
                    Log.d(TAG, "stat = " + status);
                }
            }

            /*
            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d(TAG, "onCharacteristicRead: ");
                if (characteristic.getUuid().equals(HandOverGattServer.field1_characteristic_uuid)) {
                    String str = characteristic.getStringValue(0);
                    Log.d(TAG, str);
                    activityName = str;
                    readCharacteristics(gatt, HandOverGattServer.field2_characteristic_uuid);
                } else if (characteristic.getUuid().equals(HandOverGattServer.field2_characteristic_uuid)) {
                    String str = characteristic.getStringValue(0);
                    Log.d(TAG, str);
                    dictionary.put("edit_text", str);
                    readCharacteristics(gatt, HandOverGattServer.field3_characteristic_uuid);
                } else if (characteristic.getUuid().equals(HandOverGattServer.field3_characteristic_uuid)) {
                    Integer i = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    boolean sw = (i.intValue() == 1) ? true : false;
                    Log.d(TAG, Boolean.valueOf(sw).toString());
                    dictionary.put("switch", sw);
                    postNotification();
                }
            }
            */

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                //Log.d(TAG, "onCharacteristicRead: ");
                if (characteristic.getUuid().equals(HandOverGattServer.field1_characteristic_uuid)) {
                    String str = characteristic.getStringValue(0);
                    if (str.equals("DONE")) {
                        activityNameRead = packageNameRead = false;
                        Log.d(TAG, "dictionary = " + dictionary);
                        if (packageName != null) {
                            postNotification();
                        }
                        Log.d(TAG, "Closing Gatt");
                        gatt.disconnect();
                        gatt.close();
                        return;
                    }
                    if (!activityNameRead) {
                        Log.d(TAG, "activityName = " + str);
                        activityName = str;
                        activityNameRead = true;
                        readCharacteristics(gatt, HandOverGattServer.field1_characteristic_uuid);
                    } else if (!packageNameRead) {
                        Log.d(TAG, "packageName = " + str);
                        packageName = str;
                        packageNameRead = true;
                        readCharacteristics(gatt, HandOverGattServer.field1_characteristic_uuid);
                    } else {
                        Log.d(TAG, "Field name = " + str);
                        fieldName = str;
                        readCharacteristics(gatt, HandOverGattServer.field2_characteristic_uuid);
                    }
                } else if (characteristic.getUuid().equals(HandOverGattServer.field2_characteristic_uuid)) {
                    Integer i = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    fieldDataType = HandOverGattServer.DataType.valueOf(i);
                    Log.d(TAG, "fieldDataType = " + fieldDataType);
                    readCharacteristics(gatt, HandOverGattServer.field3_characteristic_uuid);
                } else if (characteristic.getUuid().equals(HandOverGattServer.field3_characteristic_uuid)) {
                    UUID uuid = readCharacteristicDataField(characteristic);
                    readCharacteristics(gatt, uuid);
                } else if (characteristic.getUuid().equals(HandOverGattServer.field4_characteristic_uuid)) {
                    UUID uuid = readCharacteristicDataField(characteristic);
                    readCharacteristics(gatt, uuid);                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d(TAG, "onCharacteristicWrite: " + status + ", " + characteristic.getStringValue(0));
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                Log.d(TAG, "onCharacteristicChanged: ");
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Mtu is set to " + mtu);
                } else {
                    Log.d(TAG, "Failed to set Mtu to " + mtu);
                }
            }
        };
    }

    private void readCharacteristics(BluetoothGatt gatt, UUID uuid) {
        BluetoothGattService service = gatt.getService(HandOverGattServer.service_uuid);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuid);
            if (characteristic != null) {
                boolean ret = gatt.readCharacteristic(characteristic);
                if (!ret) {
                    Log.d(TAG, "Reading characteristic failed: " + characteristic);
                }
            } else {
                Log.d(TAG, "Specified UUID " + uuid.toString() + " does not exist");
            }
        } else {
            Log.d(TAG, "Specified Service does not exist");
        }
    }

    private UUID readCharacteristicDataField(BluetoothGattCharacteristic characteristic) {
        Object obj = null;
        UUID uuid = HandOverGattServer.field1_characteristic_uuid;
        switch (fieldDataType) {
            case BOOLEAN:
                Integer i = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                boolean bool = (i.intValue() == 1) ? true : false;
                Log.d(TAG, fieldName + "=" + Boolean.toString(bool));
                obj = bool;
                break;
            case SHORT:
                i = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                short s = i.shortValue();
                Log.d(TAG, fieldName + "=" + s);
                obj = s;
                break;
            case INT:
                i = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
                Log.d(TAG, fieldName + "=" + i);
                obj = (int)i;
                break;
            case LONG:
                i = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
                if (!longValueFlag) { //Low bits
                    longValue = i;
                    uuid = HandOverGattServer.field4_characteristic_uuid;
                    longValueFlag = true;
                } else { // high bits
                    ByteBuffer bb = ByteBuffer.allocate(Long.SIZE);
                    bb.putInt(i);
                    bb.putInt(longValue);
                    bb.rewind();

                    long l = bb.getLong();
                    Log.d(TAG, fieldName + "=" + l);
                    obj = l;
                    longValueFlag = false;
                }
                break;
            case STRING:
                obj = characteristic.getStringValue(0);
                Log.d(TAG, fieldName + "=" + obj);
                break;
            case FLOAT:
                i = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
                Float f = Float.intBitsToFloat(i);
                obj = f.floatValue();
                Log.d(TAG, fieldName + "=" + f);
                break;
            case DOUBLE:
                i = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
                if (!longValueFlag) { //Low bits
                    longValue = i;
                    uuid = HandOverGattServer.field4_characteristic_uuid;
                    longValueFlag = true;
                    //Log.d(TAG, "Double value lower bits " + Integer.toHexString(i));
                } else { // high bits
                    //Log.d(TAG, "Double value higher bits " + Integer.toHexString(i));
                    ByteBuffer bb = ByteBuffer.allocate(Double.SIZE);
                    bb.putInt(i);
                    bb.putInt(longValue);
                    bb.rewind();

                    double d = bb.getDouble();
                    Log.d(TAG, fieldName + "=" + d);
                    obj = d;
                    longValueFlag = false;
                }
                break;
            case UNKNOWN:
            default:
                Log.d(TAG, "Error! Something going wrong data object type mismatch");
        }
        if (obj != null) {
            dictionary.put(fieldName, obj);
        }
        return uuid;
    }

    public void connectGatt(final BluetoothDevice device) {
        Log.d(TAG, "Connecting to " + device.getAddress() + ":" + device.getName());
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }

    public void closeGatt() {
        if (bluetoothGatt != null) {
            Log.d(TAG, "Closing gatt " + bluetoothGatt);
            bluetoothGatt.close();
        }
    }

    int mId;
    private void postNotification() {
        // Creates an explicit intent for an Activity in your app

        Intent resultIntent = new Intent();
        resultIntent.setClassName(packageName, activityName);
        resultIntent.setAction("com.example.ishida.handover.RECOVER");

        // see if a target app is foreground
        // we must handle app is foreground but Activity here
        // in such case directory send Intent
        // otherwise Post Notification
        PackageManager pm = context.getPackageManager();
        ComponentName componentName = resultIntent.resolveActivity(pm);
        if (checkRunningAppProcess(componentName.getPackageName())) {
            ((HandOverService)context).restoreReady(dictionary);
            return;
        }

        Drawable icon = null;
        CharSequence label = null;
        Bitmap bitmap = null;
        // Should add Icon and Label of activity to launch
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            icon = pm.getApplicationIcon(appInfo);
            bitmap = drawableToBitmap(icon);
            label = pm.getApplicationLabel(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        //stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_stat_av_repeat)
                        .setLargeIcon(bitmap)
                        .setAutoCancel(true)
                        .setSound(uri)
                        .setTicker("HandOver")
                        .setContentTitle("HandOver from " + bluetoothGatt.getDevice().getName())
                        .setContentText("Launch " + label); // should replace with App label
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(mId, mBuilder.build());
    }

    private boolean checkRunningAppProcess(String target){

        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processInfoList = am.getRunningAppProcesses();
        for( ActivityManager.RunningAppProcessInfo info : processInfoList){
            if(info.processName.equals(target)){
                if( info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND){
                    // app is FOREGROUND
                    Log.d(TAG,"app is FOREGROUND");
                    return true;
                }
            }
        }
        return false;
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
