package com.mason.logic.config

import com.android.build.api.dsl.ApplicationExtension

internal fun configureApplication(
    commonExtension: ApplicationExtension,
) {
    commonExtension.apply {
        compileSdk = Apps.COMPILE_SDK
        namespace = Apps.APPLICATION_ID
        defaultConfig {
            applicationId = Apps.APPLICATION_ID
            minSdk = Apps.MIN_SDK
            targetSdk = Apps.TARGET_SDK
            versionCode = Apps.VERSION_CODE
            versionName = Apps.VERSION_NAME
        }
    }
}