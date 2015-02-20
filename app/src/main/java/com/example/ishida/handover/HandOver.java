package com.example.ishida.handover;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ishida on 2015/02/16.
 */
public class HandOver {
    public static HandOver getHandOver(Activity activity) {
        return new HandOver(activity);
    }

    private Activity activity;
    private Map<String, Object> dictionary;
    private HandOverCallback callback = null;
    private IHandOverService handOverService;
    private boolean needToSave = false;
    private boolean needToRestore = false;

    private HandOver(Activity activity) {
        this.activity = activity;
        this.dictionary = new HashMap<String, Object>();
    }

    public void activityChanged() {
        if (handOverService == null) { // bind to service first
            Intent intent = new Intent(activity, HandOverService.class);
            activity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            needToSave = true;
        } else {
            try {
                needToSave = false;
                handOverService.activityChanged();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void restore() {
        if (handOverService == null) { // bind to service first
            Intent intent = new Intent(activity, HandOverService.class);
            activity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            needToRestore = true;
        } else if (callback != null) {
            try {
                callback.restoreActivity(handOverService.readDictionary());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


    public void registerCallback(HandOverCallback callback) {
        this.callback = callback;
    }

    public void unbind() {
        if (handOverService != null) {
            activity.unbindService(serviceConnection);
        }
    }

    private IHandOverCallback handOverCallback = new IHandOverCallback.Stub() {
        @Override
        public void handleHandOver() throws RemoteException {
            if (callback != null) {
                dictionary.clear();
                callback.saveActivity(dictionary);
                handOverService.handOver(activity.getComponentName().getClassName(), dictionary);
            }
        }

        @Override
        public void handleRestore(Map dictionary) throws RemoteException {
            if (callback != null) {
                callback.restoreActivity(dictionary);
            }
        }

    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            handOverService = IHandOverService.Stub.asInterface(service);
            try {
                handOverService.registerCallback(handOverCallback);
                if (needToSave) {
                    needToSave = false;
                    handOverService.activityChanged();
                }
                if (needToRestore) {
                    needToRestore = false;
                    restore();
                }

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            try {
                handOverService.unregisterCallback(handOverCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            handOverService = null;
        }
    };

}
