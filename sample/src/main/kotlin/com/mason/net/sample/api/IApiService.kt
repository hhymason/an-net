/*
 * Copyright (c) 2015 - present Hive-Box.
 */

package com.mason.net.sample.api

import com.mason.net.Request
import com.mason.net.Response
import kotlinx.coroutines.flow.Flow
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

interface IApiService {
    @POST(SendMailVerificationCodeContract.path)
    fun sendMailVerificationCode(@Body request: Request<SendMailVerificationCodeContract.Req>): Flow<Response<SendMailVerificationCodeContract.Resp>>

    @POST(GatewayAuthContract.path)
    fun gatewayAuth(@Body request: Request<GatewayAuthContract.Req>): Flow<Response<GatewayAuthContract.Resp>>

    @GET
    @Streaming
    fun downloadFromUrl(@Url url: String): Flow<ResponseBody>
}
