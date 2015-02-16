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

    private HandOver(Activity activity) {
        this.activity = activity;
        this.dictionary = new HashMap<String, Object>();
    }

    public void activityChanged() {
        if (handOverService == null) { // bind to service first
            Intent intent = new Intent(HandOverService.class.getName());
            activity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }


    public void registerCallback(HandOverCallback callback) {
        this.callback = callback;
    }

    private IHandOverCallback handOverCallback = new IHandOverCallback.Stub() {
        @Override
        public void handleHandOver() throws RemoteException {
            if (callback != null) {
                callback.saveActivity(dictionary);
                handOverService.handOver(activity.getLocalClassName(), dictionary);
            }
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            handOverService = IHandOverService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            handOverService = null;
        }
    };

}
