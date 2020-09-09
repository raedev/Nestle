package com.github.raedev.tinker.loader;

import com.github.raedev.tinker.util.SdkTinkerLog;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import dalvik.system.BaseDexClassLoader;

/**
 * dex class loader
 * Created by rae on 2020/9/3.
 * Copyright (c) https://github.com/raedev All rights reserved.
 */
class SdkTinkerDexClassLoader extends BaseDexClassLoader {

    private static final String TAG = "SdkTinker.SdkTinkerDexClassLoader";

    private final ClassLoader mOriginAppClassLoader;

    SdkTinkerDexClassLoader(String dexPath, File optimizedDir, String libraryPath, ClassLoader originAppClassLoader) {
        super(dexPath, optimizedDir, libraryPath, ClassLoader.getSystemClassLoader());
        mOriginAppClassLoader = originAppClassLoader;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> cl = null;
        try {
            cl = super.findClass(name);
        } catch (ClassNotFoundException ignored) {
            cl = null;
        }
        if (cl != null) {
            SdkTinkerLog.i(TAG, "find class exists:" + name);
            return cl;
        } else {
            SdkTinkerLog.w(TAG, "class not found:" + name);
            return mOriginAppClassLoader.loadClass(name);
        }
    }

    @Override
    public URL getResource(String name) {
        // The lookup order we use here is the same as for classes.
        URL resource = Object.class.getClassLoader().getResource(name);
        if (resource != null) {
            return resource;
        }

        resource = findResource(name);
        if (resource != null) {
            return resource;
        }

        return mOriginAppClassLoader.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        @SuppressWarnings("unchecked") final Enumeration<URL>[] resources = (Enumeration<URL>[]) new Enumeration<?>[]{
                Object.class.getClassLoader().getResources(name),
                findResources(name),
                mOriginAppClassLoader.getResources(name)
        };
        return new CompoundEnumeration<>(resources);
    }

    static class CompoundEnumeration<E> implements Enumeration<E> {
        private Enumeration<E>[] enums;
        private int index = 0;

        public CompoundEnumeration(Enumeration<E>[] enums) {
            this.enums = enums;
        }

        @Override
        public boolean hasMoreElements() {
            while (index < enums.length) {
                if (enums[index] != null && enums[index].hasMoreElements()) {
                    return true;
                }
                index++;
            }
            return false;
        }

        @Override
        public E nextElement() {
            if (!hasMoreElements()) {
                throw new NoSuchElementException();
            }
            return enums[index].nextElement();
        }
    }
}
