package com.github.raedev.sdkhotfix.sample;

import android.app.Application;
import android.content.Context;

import com.github.raedev.nestle.Nestle;


/**
 * Created by rae on 2020/9/3.
 * Copyright (c) https://github.com/raedev All rights reserved.
 */
public class SampleApplication extends Application {

    public SampleApplication() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Nestle.install(base);
    }
}
