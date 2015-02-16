package com.example.ishida.handover;

import java.util.Map;

/**
 * Created by ishida on 2015/02/16.
 */
public interface HandOverCallback {
    public void saveActivity(Map<String, Object> dictionary);
    public void restoreActivity(Map<String, Object> dictionary);
}
