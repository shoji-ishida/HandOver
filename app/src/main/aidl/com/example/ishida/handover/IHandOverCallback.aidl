// IHandOverCallback.aidl
package com.example.ishida.handover;

// Declare any non-default types here with import statements

interface IHandOverCallback {

    void handleHandOver();

    void handleRestore(in Map dictionary);
}
