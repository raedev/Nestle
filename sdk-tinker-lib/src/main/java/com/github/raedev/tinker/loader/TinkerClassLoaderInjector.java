package com.github.raedev.tinker.loader;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

import com.github.raedev.tinker.loader.shareutil.ShareReflectUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by rae on 2020/9/3.
 * Copyright (c) https://github.com/raedev All rights reserved.
 */
public class TinkerClassLoaderInjector {

    public static ClassLoader inject(Application app, ClassLoader oldClassLoader, File dexOptDir, List<File> patchedDexes) throws Throwable {
        if (oldClassLoader instanceof SdkTinkerDexClassLoader) return oldClassLoader;
        final String[] patchedDexPaths = new String[patchedDexes.size()];
        for (int i = 0; i < patchedDexPaths.length; ++i) {
            patchedDexPaths[i] = patchedDexes.get(i).getAbsolutePath();
        }
        final ClassLoader newClassLoader = createNewClassLoader(app, oldClassLoader, dexOptDir, patchedDexPaths);
        doInject(app, newClassLoader);
        return newClassLoader;
    }

    private static ClassLoader createNewClassLoader(Context context, ClassLoader oldClassLoader,
                                                    File dexOptDir,
                                                    String... patchDexPaths) throws Throwable {
        final Field pathListField = findField(
                Class.forName("dalvik.system.BaseDexClassLoader", false, oldClassLoader),
                "pathList");
        final Object oldPathList = pathListField.get(oldClassLoader);

        final StringBuilder dexPathBuilder = new StringBuilder();
        final boolean hasPatchDexPaths = patchDexPaths != null && patchDexPaths.length > 0;
        if (hasPatchDexPaths) {
            for (int i = 0; i < patchDexPaths.length; ++i) {
                if (i > 0) {
                    dexPathBuilder.append(File.pathSeparator);
                }
                dexPathBuilder.append(patchDexPaths[i]);
            }
        }
        final String combinedDexPath = dexPathBuilder.toString();
        final ClassLoader result = new SdkTinkerDexClassLoader(combinedDexPath, dexOptDir, null, oldClassLoader);
        // hook pathList class loader.
        findField(oldPathList.getClass(), "definingContext").set(oldPathList, result);
        return result;
    }

    private static void doInject(Application app, ClassLoader classLoader) throws Throwable {
        Thread.currentThread().setContextClassLoader(classLoader);

        final Context baseContext = (Context) findField(app.getClass(), "mBase").get(app);
        final Object basePackageInfo = findField(baseContext.getClass(), "mPackageInfo").get(baseContext);
        findField(basePackageInfo.getClass(), "mClassLoader").set(basePackageInfo, classLoader);

        if (Build.VERSION.SDK_INT < 27) {
            final Resources res = app.getResources();
            try {
                findField(res.getClass(), "mClassLoader").set(res, classLoader);

                final Object drawableInflater = findField(res.getClass(), "mDrawableInflater").get(res);
                if (drawableInflater != null) {
                    findField(drawableInflater.getClass(), "mClassLoader").set(drawableInflater, classLoader);
                }
            } catch (Throwable ignored) {
                // Ignored.
            }
        }
    }

    private static Field findField(Class<?> clazz, String name) throws Throwable {
        return ShareReflectUtil.findField(clazz, name);
    }

}
