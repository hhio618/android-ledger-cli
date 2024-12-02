package net.ilammy.ledger.api;

import android.support.annotation.NonNull;

import java.nio.charset.StandardCharsets;

/**
 * Session owns an individual journal and provides supporting operations on it.
 */
public final class Global {
    static {
        System.loadLibrary("ledger_jni");
    }

    private static native String runCommand(String command);

}
