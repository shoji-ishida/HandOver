package com.example.ishida.handover;

import android.app.Activity;
import android.content.Context;

/**
 * Created by ishida on 2015/02/16.
 */
public class HandOver {
    public static HandOver getHandOver(Activity activity) {
        return new HandOver(activity);
    }

    private Activity activity;

    private HandOver(Activity activity) {
        this.activity = activity;
    }

    public void activityChanged() {

    }


}
