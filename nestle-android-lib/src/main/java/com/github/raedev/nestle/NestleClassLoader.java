package com.github.raedev.nestle;

import java.io.File;

import dalvik.system.BaseDexClassLoader;

/**
 * Created by rae on 2020/9/4.
 * Copyright (c) https://github.com/raedev All rights reserved.
 */
class NestleClassLoader extends BaseDexClassLoader {

    private final ClassLoader mOriginAppClassLoader;

    /**
     * Constructs an instance.
     *
     * @param dexPath            the list of jar/apk files containing classes and
     *                           resources, delimited by {@code File.pathSeparator}, which
     *                           defaults to {@code ":"} on Android
     * @param optimizedDirectory directory where optimized dex files
     *                           should be written; may be {@code null}
     * @param parent             the parent class loader
     */
    public NestleClassLoader(String dexPath, File optimizedDirectory, ClassLoader parent) {
        super(dexPath, optimizedDirectory, null, parent);
        mOriginAppClassLoader = parent;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> cl = null;
        try {
            cl = super.findClass(name);
        } catch (ClassNotFoundException ignored) {
        }
        if (cl != null) {
            return cl;
        } else {
            return mOriginAppClassLoader.loadClass(name);
        }
    }

}
