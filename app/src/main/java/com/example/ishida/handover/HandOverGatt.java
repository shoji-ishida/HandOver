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
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    Map<String, Object> dictionary = new HashMap<String, Object>();

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
                    BluetoothGattService service = gatt.getService(HandOverGattServer.service_uuid);
                    if (service != null) {
                        Log.d(TAG, "Found HandOver service");
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

                } else {
                    Log.d(TAG, "stat = " + status);
                }
            }

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

    private void readCharacteristics(BluetoothGatt gatt, UUID uuid) {
        BluetoothGattService service = gatt.getService(HandOverGattServer.service_uuid);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuid);
            if (characteristic != null) {
                gatt.readCharacteristic(characteristic);
            }
        }
    }

    public void connectGatt(final BluetoothDevice device) {
        Log.d(TAG, "Connecting to " + device.getAddress());
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }

    public void closeGatt() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }
    }

    int mId;
    private void postNotification() {
        // Creates an explicit intent for an Activity in your app

        Intent resultIntent = new Intent();
        resultIntent.setClassName(context, activityName);
        resultIntent.setAction("com.example.ishida.handover.RECOVER");
        PackageManager pm = context.getPackageManager();
        ComponentName componentName = resultIntent.resolveActivity(pm);
        if (checkRunningAppProcess(componentName.getPackageName())) {

            return;
        }

        Drawable icon = null;
        CharSequence chars = null;
        // Should add Icon and Label of activity to launch


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
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setAutoCancel(true)
                        .setSound(uri)
                        .setContentTitle("HandOver")
                        .setContentText("タップで起動");
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
}
