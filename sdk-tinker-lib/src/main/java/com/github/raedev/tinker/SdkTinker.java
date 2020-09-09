package com.github.raedev.tinker;

import android.app.Application;

import com.github.raedev.tinker.loader.SdkTinkerLoader;

/**
 * SDK热更新修复入口
 * Created by rae on 2020/9/3.
 * Copyright (c) https://github.com/raedev All rights reserved.
 */
public final class SdkTinker {

    public static void inject(Application application, Class<?> aClass) {
        ClassLoader classLoader = aClass.getClassLoader();
        SdkTinkerLoader loader = new SdkTinkerLoader();
        loader.tryLoad(application);
    }

    private SdkTinker() {
        throw new NoSuchMethodError();
    }

}
