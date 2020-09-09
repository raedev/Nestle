package com.github.raedev.tinker.util;


import com.github.raedev.tinker.loader.shareutil.ShareTinkerLog;

/**
 * Created by rae on 2020/9/3.
 * Copyright (c) https://github.com/raedev All rights reserved.
 */
public class SdkTinkerLog {

    public static void v(final String tag, final String fmt, final Object... values) {
        ShareTinkerLog.getDefaultImpl().v(tag, fmt, values);
    }

    public static void d(final String tag, final String fmt, final Object... values) {
        ShareTinkerLog.getDefaultImpl().d(tag, fmt, values);
    }

    public static void i(final String tag, final String fmt, final Object... values) {
        ShareTinkerLog.getDefaultImpl().i(tag, fmt, values);
    }

    public static void w(final String tag, final String fmt, final Object... values) {
        ShareTinkerLog.getDefaultImpl().w(tag, fmt, values);
    }

    public static void e(final String tag, final String fmt, final Object... values) {
        ShareTinkerLog.getDefaultImpl().e(tag, fmt, values);
    }
}
