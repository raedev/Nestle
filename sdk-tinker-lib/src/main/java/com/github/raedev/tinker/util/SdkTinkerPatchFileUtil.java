package com.github.raedev.tinker.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;


import java.io.File;

/**
 * Created by rae on 2020/9/3.
 * Copyright (c) https://github.com/raedev All rights reserved.
 */
public class SdkTinkerPatchFileUtil {

    private static final String TAG = "SdkTinker.SdkTinkerPatchFileUtil";

    /**
     * 获取补丁路径，如：/data/data/tinker.sample.android/sdkTinker
     */
    public static File getPatchDirectory(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        if (applicationInfo == null) {
            // Looks like running on a test Context, so just return without patching.
            return null;
        }
        return new File(applicationInfo.dataDir, SdkTinkerConstants.PATCH_DIRECTORY_NAME);
    }

    public static File getPatchInfoFile(String patchDirectory) {
        return new File(patchDirectory + "/" + SdkTinkerConstants.PATCH_INFO_NAME);
    }

}
