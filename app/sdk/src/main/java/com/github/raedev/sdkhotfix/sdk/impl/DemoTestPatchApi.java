package com.github.raedev.sdkhotfix.sdk.impl;

import android.util.Log;

import com.github.raedev.sdkhotfix.sdk.IDemoTestApi;

/**
 * Created by rae on 2020/9/3.
 * Copyright (c) https://github.com/raedev All rights reserved.
 */
public class DemoTestPatchApi implements IDemoTestApi {
    @Override
    public String hello() {
        String text = "Patch.Patch.Patch";
        Log.d("SdkTinker.DemoTestApi", text);
        return text;
    }
}
