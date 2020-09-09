package com.github.raedev.tinker.lib;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.github.raedev.tinker.loader.SdkTinkerLoader;
import com.github.raedev.tinker.util.SdkTinkerLog;

/**
 * 热更新Application代理类
 * Created by rae on 2020/9/3.
 * Copyright (c) https://github.com/raedev All rights reserved.
 */
public class SdkTinkerApplicationProxy {
    private static final String TAG = "SdkTinker.ApplicationProxy";
    // 当前应用程序ClassLoader
    private ClassLoader mCurrentClassLoader;
    private final Application mApplication;

    // Tinker Loader
    private final SdkTinkerLoader mTinkerLoader = new SdkTinkerLoader();

    public SdkTinkerApplicationProxy(Application application) {
        mApplication = application;
    }

    // 加载Tinker
    public Intent loadTinker() {
        return mTinkerLoader.tryLoad(mApplication);
    }

    public void onCreate() {
        SdkTinkerLog.d(TAG, "onCreate");
    }

    public void attachBaseContext(Context base) {
        SdkTinkerLog.d(TAG, "attachBaseContext");
//        Intent intent = this.loadTinker();
//        mCurrentClassLoader = base.getClassLoader();
    }

    public ClassLoader getClassLoader(ClassLoader superClassLoader) {
        return superClassLoader;
    }

}
