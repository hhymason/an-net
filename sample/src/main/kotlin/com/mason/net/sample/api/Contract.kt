/*
 * Copyright (c) 2015 - present Hive-Box.
 */

package com.mason.net.sample.api

import kotlinx.serialization.Serializable

object SendMailVerificationCodeContract {
    const val path = "i18n-saas-locker-server/user/check/send/mail/verification/code"

    @Serializable
    class Resp()

    @Serializable
    data class Req(val mail: String)
}

object GatewayAuthContract {
    const val path = "i18n-saas-oauth2-server/oauth/token"

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
