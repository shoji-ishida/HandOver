package com.example.ishida.handover;

import android.app.Activity;
import android.content.Context;
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

    private HandOver(Activity activity) {
        this.activity = activity;
        this.dictionary = new HashMap<String, Object>();
    }

    public void activityChanged() {

    }


    public void registerCallback(HandOverCallback calllback) {
        this.callback = calllback;
    }

    private IHandOverCallback handOverCallback = new IHandOverCallback.Stub() {
        @Override
        public void handleHandOver() throws RemoteException {

        }
    };



}
