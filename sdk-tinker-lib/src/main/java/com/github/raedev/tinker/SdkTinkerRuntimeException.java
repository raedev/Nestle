/*
 * Tencent is pleased to support the open source community by making Tinker available.
 *
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.raedev.tinker;

/**
 * 热更新异常
 * Created by rae on 2020/9/3.
 * Copyright (c) https://github.com/raedev All rights reserved.
 */
public class SdkTinkerRuntimeException extends RuntimeException {
    private static final String TINKER_RUNTIME_EXCEPTION_PREFIX = "SdkTinker Exception:";
    private static final long serialVersionUID = 1L;

    public SdkTinkerRuntimeException(String detailMessage) {
        super(TINKER_RUNTIME_EXCEPTION_PREFIX + detailMessage);
    }

    public SdkTinkerRuntimeException(String detailMessage, Throwable throwable) {
        super(TINKER_RUNTIME_EXCEPTION_PREFIX + detailMessage, throwable);
    }

}
