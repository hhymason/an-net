/*
 * Copyright (c) 2015 - present Hive-Box.
 */

package com.mason.net.api

import kotlinx.serialization.Serializable

internal object GatewayAuthContract {
    const val PATH = "i18n-saas-oauth2-server/oauth/token"

    @Serializable
    data class Req(
        val username: String? = null,
        val password: String? = null,
        val grantType: String,
        val refreshToken: String? = null
    )

    @Serializable
    data class Resp(
        val expiresIn: Long,
        val expired: Boolean,
        val tokenType: String,
        val value: String,
        val refreshToken: RefreshToken
    ) {
        @Serializable
        data class RefreshToken(
            val expiration: String,
            val value: String
        )
    }
}

internal object CosAuthContract {
    const val CABINET_PATH = "i18n-saas-locker-server/qcloud/cos/token"
    const val CABINET_IOT_PATH = "i18n-saas-iot-server/qcloud/cos/token"
    const val MOBILE_PATH = "i18n-saas-mobile-server/qcloud/cos/token"

    @Serializable
    class Req

    @Serializable
    data class Resp(
        val region: String,
        val bucket: String,
        val credentials: Credentials,
        val expiredDate: String,
        val timezone: String,
    ) {
        @Serializable
        data class Credentials(
            val sessionToken: String,
            val tmpSecretId: String,
            val tmpSecretKey: String
        )
    }
}
