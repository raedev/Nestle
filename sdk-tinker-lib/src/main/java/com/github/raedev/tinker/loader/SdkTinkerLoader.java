package com.github.raedev.tinker.loader;

import android.app.Application;
import android.content.Intent;
import android.os.SystemClock;

import androidx.multidex.MultiDex;

import com.github.raedev.tinker.loader.shareutil.ShareConstants;
import com.github.raedev.tinker.loader.shareutil.ShareIntentUtil;
import com.github.raedev.tinker.util.SdkTinkerLog;
import com.github.raedev.tinker.util.SdkTinkerPatchFileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于TinkerLoader原理修改,应用补丁
 * Created by rae on 2020/9/3.
 * Copyright (c) https://github.com/raedev All rights reserved.
 */
public final class SdkTinkerLoader {
    private static final String TAG = "SdkTinker.SdkTinkerLoader";

    public Intent tryLoad(Application application) {
        Intent intent = new Intent();
        long begin = SystemClock.elapsedRealtime();
        tryLoadPatchFilesInternal(application, intent);
        long cost = SystemClock.elapsedRealtime() - begin;
        ShareIntentUtil.setIntentPatchCostTime(intent, cost);

        return intent;
    }

    private void tryLoadPatchFilesInternal(Application application, Intent intent) {
        SdkTinkerLog.d(TAG, "try load patch files...");

        // 检查补丁路径
        File patchDirectoryFile = SdkTinkerPatchFileUtil.getPatchDirectory(application);
        if (patchDirectoryFile == null || !patchDirectoryFile.exists()) {
            SdkTinkerLog.w(TAG, "PatchDirectory not exist:" + patchDirectoryFile);
            ShareIntentUtil.setIntentReturnCode(intent, ShareConstants.ERROR_LOAD_PATCH_DIRECTORY_NOT_EXIST);
            return;
        }
        SdkTinkerLog.d(TAG, "Patch directory: " + patchDirectoryFile);

        // 检查补丁文件: /sdkTinker/patch.info

        File patchFile = new File(patchDirectoryFile, "sdk.patch.dex");
        SdkTinkerLog.d(TAG, "patch file: " + patchFile);

        // 准备class loader
        List<File> patchedDexes = new ArrayList<>();
        patchedDexes.add(patchFile);

        try {
            File optimizeDir = new File(patchDirectoryFile, ShareConstants.DEFAULT_DEX_OPTIMIZE_PATH);

            SystemClassLoaderAdder.installDexes(application, application.getClassLoader(), optimizeDir, patchedDexes, false);


        } catch (Throwable throwable) {
            SdkTinkerLog.e(TAG, "Class loader Exception:" + throwable.getMessage(), throwable);
        }

        SdkTinkerLog.d(TAG, "this class loader:" + application.getClassLoader().toString());

    }


}
