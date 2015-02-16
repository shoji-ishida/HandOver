// IHandOverService.aidl
package com.example.ishida.handover;

// Declare any non-default types here with import statements
import com.example.ishida.handover.IHandOverCallback;

interface IHandOverService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);

    oneway void registerCallback(IHandOverCallback callback);

    oneway void unregisterCallback(IHandOverCallback callback);

    oneway void handOver(String activityName, in Map dictionary);

}
