package com.github.raedev.nestle;

import android.content.Context;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

/**
 * hook system class loader : PathClassLoader
 * Created by rae on 2020/9/4.
 * Copyright (c) https://github.com/raedev All rights reserved.
 */
final class ClassLoaderHooker {

    public static ClassLoader hook(Context context, ClassLoader oldClassLoader, File dexOptDir, List<File> patchedDexes) throws Throwable {
        final String[] patchedDexPaths = new String[patchedDexes.size()];
        for (int i = 0; i < patchedDexPaths.length; ++i) {
            patchedDexPaths[i] = patchedDexes.get(i).getAbsolutePath();
        }
        final ClassLoader newClassLoader = createNewClassLoader(context, oldClassLoader, dexOptDir, patchedDexPaths);
        doInject(context, newClassLoader);
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

        final ClassLoader result = new NestleClassLoader(combinedDexPath, dexOptDir, oldClassLoader);
        findField(oldPathList.getClass(), "definingContext").set(oldPathList, result);
        return result;
    }


    private static void doInject(Context baseContext, ClassLoader classLoader) throws Throwable {
        Thread.currentThread().setContextClassLoader(classLoader);
        final Object basePackageInfo = findField(baseContext.getClass(), "mPackageInfo").get(baseContext);
        findField(basePackageInfo.getClass(), "mClassLoader").set(basePackageInfo, classLoader);
    }


    private static Field findField(Class<?> clazz, String name) throws Throwable {
        Class<?> currClazz = clazz;
        while (true) {
            try {
                final Field result = currClazz.getDeclaredField(name);
                result.setAccessible(true);
                return result;
            } catch (Throwable ignored) {
                if (currClazz == Object.class) {
                    throw new NoSuchFieldException("Cannot find field "
                            + name + " in class " + clazz.getName() + " and its super classes.");
                } else {
                    currClazz = currClazz.getSuperclass();
                }
            }
        }
    }

}
