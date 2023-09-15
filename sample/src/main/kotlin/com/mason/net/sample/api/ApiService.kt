/*
 * Copyright (c) 2015 - present Hive-Box.
 */

package com.mason.net.sample.api

import com.mason.net.BaseService
import com.mason.net.Net
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import java.util.concurrent.TimeUnit

val apiService by lazy {
    Net.apply {
        url = { "https://cloud-gateway-sit6.fcbox.com/" }
    }
    ApiService()
}

class ApiService : BaseService() {
    private val service: IApiService = buildService()

    private fun buildService(): IApiService {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        return buildService(
            IApiService::class,
            json,
            headerBuilderBlock = { _, _ ->
            },
            okHttpClientBuilderBlock = { builder ->
                with(builder) {
                    connectTimeout(5, TimeUnit.SECONDS)
                    readTimeout(5, TimeUnit.SECONDS)
                }
            }
        )
    }

    fun downloadFromUrl(url: String): Flow<ResponseBody> {
        return service.downloadFromUrl(url)
    }

    fun sendMailVerificationCode(request: SendMailVerificationCodeContract.Req): Flow<SendMailVerificationCodeContract.Resp> {
        return service.sendMailVerificationCode(request.buildRequest()).applyNetStrategy()
    }

    fun gatewayAuth(request: GatewayAuthContract.Req): Flow<GatewayAuthContract.Resp> {
        return service.gatewayAuth(request.buildRequest()).applyNetStrategy(authRetry = false)
    }
}
