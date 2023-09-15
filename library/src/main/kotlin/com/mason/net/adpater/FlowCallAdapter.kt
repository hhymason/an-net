/*
 * Copyright (c) 2015 - present Hive-Box.
 */

package com.mason.net.adpater

import com.mason.net.OkHttpRequest
import com.mason.net.Response
import com.mason.net.handleResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.awaitResponse
import java.lang.reflect.Type

class FlowCallAdapter<T>(private val responseType: Type) :
    CallAdapter<T, Flow<T?>> {
    override fun responseType(): Type = responseType

    override fun adapt(call: Call<T>): Flow<T?> =
        flow {
            val execute = call.execute()
            val body = execute.body()
            emit(body)
        }.flowOn(Dispatchers.IO)
//        flow {
//            val clonedCall = call.clone()
//            val okHttpRequest: OkHttpRequest = clonedCall.request()
//            val retrofitResponse = clonedCall.awaitResponse()
//            emit(handleResponse(okHttpRequest, retrofitResponse))
//        }
}
