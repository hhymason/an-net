/*
 * Copyright (c) 2015 - present Hive-Box.
 */

package com.mason.net.api

import com.mason.net.BaseService
import com.mason.net.Net.json
import com.mason.net.Sp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

internal class ApiService : BaseService() {
    private val service: IApiService = buildService(
        IApiService::class,
        json
    )

    fun gatewayAuth(request: GatewayAuthContract.Req): Flow<GatewayAuthContract.Resp> {
        return service.gatewayAuth(request.buildRequest()).applyNetStrategy(authRetry = false)
            .onEach {
                Sp.gatewayAuth = it.value
                Sp.gatewayRefreshAuth = it.refreshToken.value
            }
    }


}
