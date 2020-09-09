package com.github.raedev.nestle;

import android.content.Context;
import android.util.Log;

/**
 * Created by rae on 2020/9/4.
 * Copyright (c) https://github.com/raedev All rights reserved.
 */
public final class Nestle {

    private static final String TAG = "Nestle";

    public static void install(Context context) {
        Log.i(TAG, "INSTALL...");
        try {
            PatchFileInstaller.install(context);
        } catch (NestleRuntimeException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }
    }
}
