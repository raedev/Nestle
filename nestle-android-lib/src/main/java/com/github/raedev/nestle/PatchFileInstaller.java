package com.github.raedev.nestle;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.zip.ZipFile;

import dalvik.system.DexFile;

/**
 * 补丁文件安装器
 * Created by rae on 2020/9/4.
 * Copyright (c) https://github.com/raedev All rights reserved.
 */
final class PatchFileInstaller {

    private static String TAG = "Nestle.PatchFileInstaller";

    private static final String DEX_PATH_NAME = "nestle";
    private static final String OLD_SECONDARY_FOLDER_NAME = "nestle_secondary_dexes";
    private static final String CODE_CACHE_SECONDARY_FOLDER_NAME = "nestle_secondary_dexes";
    private static final String DEX_FILE_NAME = "nestle.dex";
    public static final String DEFAULT_DEX_OPTIMIZE_PATH = "odex";

    private PatchFileInstaller() {
    }


    /**
     * install dex file
     */
    public static void install(Context mainContext) throws NestleRuntimeException {
        ApplicationInfo applicationInfo = getApplicationInfo(mainContext);
        ClassLoader classLoader = getDexClassloader(mainContext);
        if (classLoader == null) {
            throw new NestleRuntimeException("Not found DexClassLoader.");
        }
        if (applicationInfo == null) {
            throw new NestleRuntimeException("Application is null.");
        }

        try {
            clearOldDexDir(mainContext);
        } catch (Throwable t) {
            Log.w(TAG, "Something went wrong when trying to clear old MultiDex extraction, "
                    + "continuing without cleaning.", t);
        }

        File dataDir = new File(applicationInfo.dataDir);

        try {
            // data_dir/nestle/nestle_secondary_dexes
            File dexDir = getDexDir(mainContext, dataDir, CODE_CACHE_SECONDARY_FOLDER_NAME);
            // data_dir/nestle/odex
            File optDir = getDexDir(mainContext, dataDir, DEFAULT_DEX_OPTIMIZE_PATH);
            // data_dir/nestle/nestle.dex
            File dexFile = new File(dexDir, DEX_FILE_NAME);
            if (!dexFile.exists()) {
                throw new NestleRuntimeException("not found dex file: " + dexFile);
            }

            // apply patch file
            applyPatchFile(mainContext, classLoader, optDir, Collections.singletonList(dexFile));
            Log.i(TAG, "last class loader:" + mainContext.getClassLoader());
        } catch (IOException ex) {
            throw new NestleRuntimeException("create dex directory error: " + ex.getMessage(), ex);
        } catch (Throwable ex) {
            throw new NestleRuntimeException("apply patch error: " + ex.getMessage(), ex);
        }
    }

    public static void applyPatchFile(Context context, ClassLoader loader, File optDir, List<File> files)
            throws IllegalArgumentException, IllegalAccessException,
            NoSuchFieldException, InvocationTargetException, NoSuchMethodException,
            IOException, InstantiationException, ClassNotFoundException, Throwable {

        // dex2opt
        if (Build.VERSION.SDK_INT < 24) {
            dex2opt(files, optDir);
        }

        if (!files.isEmpty()) {
            if (Build.VERSION.SDK_INT >= 24) {
                ClassLoaderHooker.hook(context, loader, optDir, files);
            } else if (Build.VERSION.SDK_INT >= 23) {
                V23.install(loader, files, optDir);
            } else if (Build.VERSION.SDK_INT >= 19) {
                V19.install(loader, files, optDir);
            } else if (Build.VERSION.SDK_INT >= 14) {
                V14.install(loader, optDir, files);
            } else {
                V4.install(loader, files);
            }
        }
    }

    private static void dex2opt(List<File> files, File optDir) {
        for (File file : files) {
            try {
                DexFile.loadDex(file.getAbsolutePath(), optDir.getAbsolutePath(), 0);
            } catch (IOException e) {
                Log.e(TAG, "dex2opt Error:" + e.getMessage(), e);
            }
        }
    }

    private static File mkdirChecked(File dir) throws IOException {
        if (dir.exists()) return dir;
        dir.mkdir();
        if (!dir.isDirectory()) {
            File parent = dir.getParentFile();
            if (parent == null) {
                Log.e(TAG, "Failed to create dir " + dir.getPath() + ". Parent file is null.");
            } else {
                Log.e(TAG, "Failed to create dir " + dir.getPath() +
                        ". parent file is a dir " + parent.isDirectory() +
                        ", a file " + parent.isFile() +
                        ", exists " + parent.exists() +
                        ", readable " + parent.canRead() +
                        ", writable " + parent.canWrite());
            }
            throw new IOException("Failed to create directory " + dir.getPath());
        }
        return dir;
    }

    private static ApplicationInfo getApplicationInfo(Context context) {
        try {
            /* Due to package install races it is possible for a process to be started from an old
             * apk even though that apk has been replaced. Querying for ApplicationInfo by package
             * name may return information for the new apk, leading to a runtime with the old main
             * dex file and new secondary dex files. This leads to various problems like
             * ClassNotFoundExceptions. Using context.getApplicationInfo() should result in the
             * process having a consistent view of the world (even if it is of the old world). The
             * package install races are eventually resolved and old processes are killed.
             */
            return context.getApplicationInfo();
        } catch (RuntimeException e) {
            /* Ignore those exceptions so that we don't break tests relying on Context like
             * a android.test.mock.MockContext or a android.content.ContextWrapper with a null
             * base Context.
             */
            Log.w(TAG, "Failure while trying to obtain ApplicationInfo from Context. " +
                    "Must be running in test mode. Skip patching.", e);
            return null;
        }
    }

    private static void clearOldDexDir(Context context) throws Exception {
        File dexDir = new File(context.getFilesDir(), OLD_SECONDARY_FOLDER_NAME);
        if (dexDir.isDirectory()) {
            Log.i(TAG, "Clearing old secondary dex dir (" + dexDir.getPath() + ").");
            File[] files = dexDir.listFiles();
            if (files == null) {
                Log.w(TAG, "Failed to list secondary dex dir content (" + dexDir.getPath() + ").");
                return;
            }
            for (File oldFile : files) {
                Log.i(TAG, "Trying to delete old file " + oldFile.getPath() + " of size "
                        + oldFile.length());
                if (!oldFile.delete()) {
                    Log.w(TAG, "Failed to delete old file " + oldFile.getPath());
                } else {
                    Log.i(TAG, "Deleted old file " + oldFile.getPath());
                }
            }
            if (!dexDir.delete()) {
                Log.w(TAG, "Failed to delete secondary dex dir " + dexDir.getPath());
            } else {
                Log.i(TAG, "Deleted old secondary dex dir " + dexDir.getPath());
            }
        }
    }

    private static File getDexDir(Context context, File dataDir, String secondaryFolderName)
            throws IOException {
        File cache = new File(dataDir, DEX_PATH_NAME);
        try {
            mkdirChecked(cache);
        } catch (IOException e) {
            /* If we can't emulate code_cache, then store to filesDir. This means abandoning useless
             * files on disk if the device ever updates to android 5+. But since this seems to
             * happen only on some devices running android 2, this should cause no pollution.
             */
            cache = new File(context.getFilesDir(), DEX_PATH_NAME);
            mkdirChecked(cache);
        }
        File dexDir = new File(cache, secondaryFolderName);
        mkdirChecked(dexDir);
        return dexDir;
    }

    /**
     * Returns a {@link ClassLoader} from the {@link Context} that is capable of reading dex
     * bytecode or null if the Classloader is not dex-capable e.g: when running on a JVM testing
     * environment such as Robolectric.
     */
    private static ClassLoader getDexClassloader(Context context) {
        ClassLoader loader;
        try {
            loader = context.getClassLoader();
        } catch (RuntimeException e) {
            /* Ignore those exceptions so that we don't break tests relying on Context like
             * a android.test.mock.MockContext or a android.content.ContextWrapper with a
             * null base Context.
             */
            Log.w(TAG, "Failure while trying to obtain Context class loader. "
                    + "Must be running in test mode. Skip patching.", e);
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (loader instanceof dalvik.system.BaseDexClassLoader) {
                return loader;
            }
        } else if (loader instanceof dalvik.system.DexClassLoader
                || loader instanceof dalvik.system.PathClassLoader) {
            return loader;
        }
        Log.e(TAG, "Context class loader is null or not dex-capable. "
                + "Must be running in test mode. Skip patching.");
        return null;
    }


    /**
     * Installer for platform versions 23.
     */
    private static final class V23 {

        private static void install(ClassLoader loader, List<File> additionalClassPathEntries,
                                    File optimizedDirectory)
                throws IllegalArgumentException, IllegalAccessException,
                NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IOException {
            /* The patched class loader is expected to be a descendant of
             * dalvik.system.BaseDexClassLoader. We modify its
             * dalvik.system.DexPathList pathList field to append additional DEX
             * file entries.
             */
            Field pathListField = findField(loader, "pathList");
            Object dexPathList = pathListField.get(loader);
            ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
            expandFieldArray(dexPathList, "dexElements", makePathElements(dexPathList,
                    new ArrayList<File>(additionalClassPathEntries), optimizedDirectory,
                    suppressedExceptions));
            if (suppressedExceptions.size() > 0) {
                for (IOException e : suppressedExceptions) {
                    Log.w(TAG, "Exception in makePathElement", e);
                    throw e;
                }

            }
        }

        /**
         * A wrapper around
         * {@code private static final dalvik.system.DexPathList#makePathElements}.
         */
        private static Object[] makePathElements(
                Object dexPathList, ArrayList<File> files, File optimizedDirectory,
                ArrayList<IOException> suppressedExceptions)
                throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

            Method makePathElements;
            try {
                makePathElements = findMethod(dexPathList, "makePathElements", List.class, File.class,
                        List.class);
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "NoSuchMethodException: makePathElements(List,File,List) failure");
                try {
                    makePathElements = findMethod(dexPathList, "makePathElements", ArrayList.class, File.class, ArrayList.class);
                } catch (NoSuchMethodException e1) {
                    Log.e(TAG, "NoSuchMethodException: makeDexElements(ArrayList,File,ArrayList) failure");
                    try {
                        Log.e(TAG, "NoSuchMethodException: try use v19 instead");
                        return V19.makeDexElements(dexPathList, files, optimizedDirectory, suppressedExceptions);
                    } catch (NoSuchMethodException e2) {
                        Log.e(TAG, "NoSuchMethodException: makeDexElements(List,File,List) failure");
                        throw e2;
                    }
                }
            }

            return (Object[]) makePathElements.invoke(dexPathList, files, optimizedDirectory, suppressedExceptions);
        }
    }


    /**
     * Installer for platform versions 19.
     */
    private static final class V19 {

        static void install(ClassLoader loader,
                            List<? extends File> additionalClassPathEntries,
                            File optimizedDirectory)
                throws IllegalArgumentException, IllegalAccessException,
                NoSuchFieldException, InvocationTargetException, NoSuchMethodException,
                IOException {
            /* The patched class loader is expected to be a descendant of
             * dalvik.system.BaseDexClassLoader. We modify its
             * dalvik.system.DexPathList pathList field to append additional DEX
             * file entries.
             */
            Field pathListField = findField(loader, "pathList");
            Object dexPathList = pathListField.get(loader);
            ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
            expandFieldArray(dexPathList, "dexElements", makeDexElements(dexPathList,
                    new ArrayList<File>(additionalClassPathEntries), optimizedDirectory,
                    suppressedExceptions));
            if (suppressedExceptions.size() > 0) {
                for (IOException e : suppressedExceptions) {
                    Log.w(TAG, "Exception in makeDexElement", e);
                }
                Field suppressedExceptionsField =
                        findField(dexPathList, "dexElementsSuppressedExceptions");
                IOException[] dexElementsSuppressedExceptions =
                        (IOException[]) suppressedExceptionsField.get(dexPathList);

                if (dexElementsSuppressedExceptions == null) {
                    dexElementsSuppressedExceptions =
                            suppressedExceptions.toArray(
                                    new IOException[suppressedExceptions.size()]);
                } else {
                    IOException[] combined =
                            new IOException[suppressedExceptions.size() +
                                    dexElementsSuppressedExceptions.length];
                    suppressedExceptions.toArray(combined);
                    System.arraycopy(dexElementsSuppressedExceptions, 0, combined,
                            suppressedExceptions.size(), dexElementsSuppressedExceptions.length);
                    dexElementsSuppressedExceptions = combined;
                }

                suppressedExceptionsField.set(dexPathList, dexElementsSuppressedExceptions);

                IOException exception = new IOException("I/O exception during makeDexElement");
                exception.initCause(suppressedExceptions.get(0));
                throw exception;
            }
        }

        /**
         * A wrapper around
         * {@code private static final dalvik.system.DexPathList#makeDexElements}.
         */
        private static Object[] makeDexElements(
                Object dexPathList, ArrayList<File> files, File optimizedDirectory,
                ArrayList<IOException> suppressedExceptions)
                throws IllegalAccessException, InvocationTargetException,
                NoSuchMethodException {
            Method makeDexElements =
                    findMethod(dexPathList, "makeDexElements", ArrayList.class, File.class,
                            ArrayList.class);

            return (Object[]) makeDexElements.invoke(dexPathList, files, optimizedDirectory,
                    suppressedExceptions);
        }
    }

    /**
     * Installer for platform versions 14, 15, 16, 17 and 18.
     */
    private static final class V14 {

        private interface ElementConstructor {
            Object newInstance(File file, DexFile dex)
                    throws IllegalArgumentException, InstantiationException,
                    IllegalAccessException, InvocationTargetException, IOException;
        }

        /**
         * Applies for ICS and early JB (initial release and MR1).
         */
        private static class ICSElementConstructor implements ElementConstructor {
            private final Constructor<?> elementConstructor;

            ICSElementConstructor(Class<?> elementClass)
                    throws SecurityException, NoSuchMethodException {
                elementConstructor =
                        elementClass.getConstructor(File.class, ZipFile.class, DexFile.class);
                elementConstructor.setAccessible(true);
            }

            @Override
            public Object newInstance(File file, DexFile dex)
                    throws IllegalArgumentException, InstantiationException,
                    IllegalAccessException, InvocationTargetException, IOException {
                return elementConstructor.newInstance(file, new ZipFile(file), dex);
            }
        }

        /**
         * Applies for some intermediate JB (MR1.1).
         * <p>
         * See Change-Id: I1a5b5d03572601707e1fb1fd4424c1ae2fd2217d
         */
        private static class JBMR11ElementConstructor implements ElementConstructor {
            private final Constructor<?> elementConstructor;

            JBMR11ElementConstructor(Class<?> elementClass)
                    throws SecurityException, NoSuchMethodException {
                elementConstructor = elementClass
                        .getConstructor(File.class, File.class, DexFile.class);
                elementConstructor.setAccessible(true);
            }

            @Override
            public Object newInstance(File file, DexFile dex)
                    throws IllegalArgumentException, InstantiationException,
                    IllegalAccessException, InvocationTargetException {
                return elementConstructor.newInstance(file, file, dex);
            }
        }

        /**
         * Applies for latest JB (MR2).
         * <p>
         * See Change-Id: Iec4dca2244db9c9c793ac157e258fd61557a7a5d
         */
        private static class JBMR2ElementConstructor implements ElementConstructor {
            private final Constructor<?> elementConstructor;

            JBMR2ElementConstructor(Class<?> elementClass)
                    throws SecurityException, NoSuchMethodException {
                elementConstructor = elementClass
                        .getConstructor(File.class, Boolean.TYPE, File.class, DexFile.class);
                elementConstructor.setAccessible(true);
            }

            @Override
            public Object newInstance(File file, DexFile dex)
                    throws IllegalArgumentException, InstantiationException,
                    IllegalAccessException, InvocationTargetException {
                return elementConstructor.newInstance(file, Boolean.FALSE, file, dex);
            }
        }

        private final ElementConstructor elementConstructor;

        static void install(ClassLoader loader,
                            File optimizedDirectory,
                            List<? extends File> additionalClassPathEntries)
                throws IOException, SecurityException, IllegalArgumentException,
                ClassNotFoundException, NoSuchMethodException, InstantiationException,
                IllegalAccessException, InvocationTargetException, NoSuchFieldException {
            /* The patched class loader is expected to be a descendant of
             * dalvik.system.BaseDexClassLoader. We modify its
             * dalvik.system.DexPathList pathList field to append additional DEX
             * file entries.
             */
            Field pathListField = findField(loader, "pathList");
            Object dexPathList = pathListField.get(loader);
            Object[] elements = new V14().makeDexElements(additionalClassPathEntries, optimizedDirectory);
            try {
                expandFieldArray(dexPathList, "dexElements", elements);
            } catch (NoSuchFieldException e) {
                // dexElements was renamed pathElements for a short period during JB development,
                // eventually it was renamed back shortly after.
                Log.w(TAG, "Failed find field 'dexElements' attempting 'pathElements'", e);
                expandFieldArray(dexPathList, "pathElements", elements);
            }
        }

        private V14() throws ClassNotFoundException, SecurityException, NoSuchMethodException {
            ElementConstructor constructor;
            Class<?> elementClass = Class.forName("dalvik.system.DexPathList$Element");
            try {
                constructor = new ICSElementConstructor(elementClass);
            } catch (NoSuchMethodException e1) {
                try {
                    constructor = new JBMR11ElementConstructor(elementClass);
                } catch (NoSuchMethodException e2) {
                    constructor = new JBMR2ElementConstructor(elementClass);
                }
            }
            this.elementConstructor = constructor;
        }

        /**
         * An emulation of {@code private static final dalvik.system.DexPathList#makeDexElements}
         * accepting only extracted secondary dex files.
         * OS version is catching IOException and just logging some of them, this version is letting
         * them through.
         */
        private Object[] makeDexElements(List<? extends File> files, File optimizedDirectory)
                throws IOException, SecurityException, IllegalArgumentException,
                InstantiationException, IllegalAccessException, InvocationTargetException {
            Object[] elements = new Object[files.size()];
            for (int i = 0; i < elements.length; i++) {
                File file = files.get(i);
                elements[i] = elementConstructor.newInstance(
                        file,
                        DexFile.loadDex(file.getPath(), optimizedDirectory.getAbsolutePath(), 0));
            }
            return elements;
        }
    }


    /**
     * Installer for platform versions 4 to 13.
     */
    private static final class V4 {
        static void install(ClassLoader loader,
                            List<? extends File> additionalClassPathEntries)
                throws IllegalArgumentException, IllegalAccessException,
                NoSuchFieldException, IOException {
            /* The patched class loader is expected to be a descendant of
             * dalvik.system.DexClassLoader. We modify its
             * fields mPaths, mFiles, mZips and mDexs to append additional DEX
             * file entries.
             */
            int extraSize = additionalClassPathEntries.size();

            Field pathField = findField(loader, "path");

            StringBuilder path = new StringBuilder((String) pathField.get(loader));
            String[] extraPaths = new String[extraSize];
            File[] extraFiles = new File[extraSize];
            ZipFile[] extraZips = new ZipFile[extraSize];
            DexFile[] extraDexs = new DexFile[extraSize];
            for (ListIterator<? extends File> iterator = additionalClassPathEntries.listIterator();
                 iterator.hasNext(); ) {
                File additionalEntry = iterator.next();
                String entryPath = additionalEntry.getAbsolutePath();
                path.append(':').append(entryPath);
                int index = iterator.previousIndex();
                extraPaths[index] = entryPath;
                extraFiles[index] = additionalEntry;
                extraZips[index] = new ZipFile(additionalEntry);
                extraDexs[index] = DexFile.loadDex(entryPath, entryPath + ".dex", 0);
            }

            pathField.set(loader, path.toString());
            expandFieldArray(loader, "mPaths", extraPaths);
            expandFieldArray(loader, "mFiles", extraFiles);
            expandFieldArray(loader, "mZips", extraZips);
            expandFieldArray(loader, "mDexs", extraDexs);
        }
    }

    /**
     * Replace the value of a field containing a non null array, by a new array containing the
     * elements of the original array plus the elements of extraElements.
     *
     * @param instance      the instance whose field is to be modified.
     * @param fieldName     the field to modify.
     * @param extraElements elements to append at the end of the array.
     */
    private static void expandFieldArray(Object instance, String fieldName,
                                         Object[] extraElements) throws NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException {
        Field jlrField = findField(instance, fieldName);
        Object[] original = (Object[]) jlrField.get(instance);
        Object[] combined = (Object[]) Array.newInstance(
                original.getClass().getComponentType(), original.length + extraElements.length);


        System.arraycopy(extraElements, 0, combined, 0, extraElements.length);
        System.arraycopy(original, 0, combined, extraElements.length, original.length);

        jlrField.set(instance, combined);
    }

    /**
     * Locates a given field anywhere in the class inheritance hierarchy.
     *
     * @param instance an object to search the field into.
     * @param name     field name
     * @return a field object
     * @throws NoSuchFieldException if the field cannot be located
     */
    private static Field findField(Object instance, String name) throws NoSuchFieldException {
        for (Class<?> clazz = instance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            try {
                Field field = clazz.getDeclaredField(name);
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }

                return field;
            } catch (NoSuchFieldException e) {
                // ignore and search next
            }
        }
        throw new NoSuchFieldException("Field " + name + " not found in " + instance.getClass());
    }

    /**
     * Locates a given method anywhere in the class inheritance hierarchy.
     *
     * @param instance       an object to search the method into.
     * @param name           method name
     * @param parameterTypes method parameter types
     * @return a method object
     * @throws NoSuchMethodException if the method cannot be located
     */
    private static Method findMethod(Object instance, String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        for (Class<?> clazz = instance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            try {
                Method method = clazz.getDeclaredMethod(name, parameterTypes);
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                return method;
            } catch (NoSuchMethodException e) {
                // ignore and search next
            }
        }
        throw new NoSuchMethodException("Method " + name + " with parameters " +
                Arrays.asList(parameterTypes) + " not found in " + instance.getClass());
    }
}
