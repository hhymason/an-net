package com.mason.logic.config

import org.gradle.api.JavaVersion

object Apps {
    /** 主版本号. */
    const val MAJOR = 1

    /** 子版本号. */
    const val MINOR = 0

    /** 修正版本号. */
    const val PATCH = 1

    const val APPLICATION_ID = "com.mason.net.sample"

    const val LIB_ID = "com.mason.net"

    /** SDK 编译版本 */
    const val COMPILE_SDK = 33

    /** 最小 SDK 版本. */
    const val MIN_SDK = 21

    /** 目标 SDK 版本 */
    const val TARGET_SDK = 33

    /** lib 版本名采用 GNU 风格，主版本号.子版本号.修正版本号，例如 1.2.10. */
    const val VERSION_NAME = "$MAJOR.$MINOR.$PATCH"

    /** 版本号由版本名映射，主版本号 * 10000 + 子版本号 * 100 + 修正版本号，例如 1.2.10 -> 10210. */
    const val VERSION_CODE = MAJOR * 10000 + MINOR * 100 + PATCH

    const val GROUP_ID = "com.github.hhymason"

    const val ARTIFACT_ID = "an-net"

    val JAVA_VERSION = JavaVersion.VERSION_17
}
