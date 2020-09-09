package com.github.raedev.sdkhotfix.sdk;

import android.app.Application;

import com.github.raedev.sdkhotfix.sdk.impl.DemoTestApi;

/**
 * Created by rae on 2020/9/3.
 * Copyright (c) https://github.com/raedev All rights reserved.
 */
public final class DemoSdk implements IDemoSdkProvider {

    public DemoSdk(Application application) {
    }

    @Override
    public IDemoTestApi getDemoTestApi() {
        return new DemoTestApi();
//        return new DemoTestPatchApi();
    }

}
