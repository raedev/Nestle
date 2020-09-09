package com.github.raedev.sdkhotfix.sdk.impl;

import android.util.Log;

import com.github.raedev.sdkhotfix.sdk.IDemoTestApi;

/**
 * Created by rae on 2020/9/3.
 * Copyright (c) https://github.com/raedev All rights reserved.
 */
public class DemoTestApi implements IDemoTestApi {

    @Override
    public String hello() {
        String text = "Hello Nestle.";
        Log.d("Nestle.DemoTestApi", text);
        return text;
    }
}
