package com.dinuscxj.refresh;

import android.util.Log;

public final class RefreshLogger {
    private static final String TAG = "RefreshLayout";

    private static boolean mEnableDebug = false;

    public static void setEnableDebug(boolean enableDebug) {
        mEnableDebug = enableDebug;
    }

    public static void i(String msg) {
        if (mEnableDebug) {
            Log.i(TAG, msg);
        }
    }

    public static void v(String msg) {
        if (mEnableDebug) {
            Log.v(TAG, msg);
        }
    }

    public static void d(String msg) {
        if (mEnableDebug) {
            Log.d(TAG, msg);
        }
    }

    public static void w(String msg) {
        if (mEnableDebug) {
            Log.w(TAG, msg);
        }
    }

    public static void e(String msg) {
        if (mEnableDebug) {
            Log.e(TAG, msg);
        }
    }
}
