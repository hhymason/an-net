/*
 * Copyright (c) 2015 - present Hive-Box.
 */

package com.mason.net.api

import com.mason.net.Request
import com.mason.net.Response
import kotlinx.coroutines.flow.Flow
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

internal interface IApiService {
    @POST(GatewayAuthContract.PATH)
    fun gatewayAuth(@Body request: Request<GatewayAuthContract.Req>): Flow<Response<GatewayAuthContract.Resp>>

    @POST
    fun cosAuth(@Url url: String, @Body request: Request<CosAuthContract.Req>): Flow<Response<CosAuthContract.Resp>>
}
