/*
 * Copyright (c) 2015 - present Hive-Box.
 */
package com.mason.net

import com.mason.net.Consts.NET_RETRY_TIMES
import com.mason.net.Consts.RETRY_BASE_DELAY
import com.mason.net.Net.appVersionName
import com.mason.net.Net.gzip
import com.mason.net.Net.uicNotLogin
import com.mason.net.Net.url
import com.mason.net.adpater.FlowCallAdapterFactory
import com.mason.net.converter.asConverterFactory
import com.mason.net.worker.PingWorker.Companion.start
import com.mason.util.exception.msg
import com.mason.util.resource.appStr
import com.mason.util.time.isoUtcNowPair
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.math.pow
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

open class ApiException : RuntimeException {
    val code: String

    constructor(code: String, msg: String) : super(msg) {
        this.code = code
    }

    constructor(code: String, msg: String, throwable: Throwable) : super(msg, throwable) {
        this.code = code
    }
}

class NetworkTimeoutException(throwable: Throwable, networkDesc: String) : ApiException(
    BaseApiCode.CS_HTTP_TIMEOUT.value,
    appStr(R.string.net_http_timeout).format(networkDesc)
)

class NetworkErrorException : ApiException {
    constructor(code: String, msg: String) : super(code, msg)

    constructor(code: String, msg: String, throwable: Throwable) : super(code, msg, throwable) {
        stackTrace = throwable.stackTrace + stackTrace
    }
}

abstract class BaseService {
    @OptIn(ExperimentalSerializationApi::class)
    fun <T : Any> buildService(
        clazz: KClass<T>,
        json: Json,
        headerBuilderBlock: ((builder: OkHttpRequestBuilder, url: String) -> Unit)? = null,
        requestInterceptors: LinkedHashSet<Interceptor>? = null,
        responseInterceptors: LinkedHashSet<Interceptor>? = null,
        okHttpClientBuilderBlock: ((builder: OkHttpClient.Builder) -> Unit)? = null
    ): T {
        val contentType = "application/json; charset=utf-8".toMediaType()
        val retrofitBuilder = Retrofit.Builder()
            .baseUrl(url())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(json.asConverterFactory(contentType))
            .addCallAdapterFactory(FlowCallAdapterFactory.instance)
        val client = buildClient {
            val headerInterceptor = HeaderRequestInterceptor { builder, url ->
                headerBuilderBlock?.invoke(builder, url)
            }
            addInterceptor(headerInterceptor)
            requestInterceptors?.let {
                it.forEach { interceptor ->
                    addInterceptor(interceptor)
                }
            }
            addInterceptor(LogRequestInterceptor())
            if (gzip) {
                addInterceptor(GzipRequestInterceptor())
            }
            responseInterceptors?.let {
                it.forEach { interceptor ->
                    addInterceptor(interceptor)
                }
            }
            addInterceptor(LogResponseInterceptor())
            if (gzip) {
                addInterceptor(GzipResponseInterceptor())
            }
            okHttpClientBuilderBlock?.invoke(this)
        }
        val retrofit = retrofitBuilder
            .client(client)
            .build()
        return retrofit.create(clazz.java)
    }

    fun <T> T.buildRequest(): Request<T> {
        val pair = isoUtcNowPair
        return Request(
            pair.first,
            Net.genNetMsgId(),
            "android",
            appVersionName.invoke(),
            this
        )
    }

    fun <T> Flow<Response<T>>.applyNetStrategy(
        timeoutRetry: Boolean = Net.timeoutRetry,
        authRetry: Boolean = true
    ): Flow<T> {
        return map { it.data!! }
            .catch { e -> throw e.transformToApiException() }
            .retryWhen { cause, attempt ->
                val apiException = cause as ApiException
                when (apiException.code) {
                    BaseApiCode.CS_HTTP_TIMEOUT.value -> {
                        if (timeoutRetry && attempt < NET_RETRY_TIMES) {
                            val seq = attempt + 1
                            val powDelay = RETRY_BASE_DELAY.pow(seq.toInt()).toInt()
                            val message =
                                appStr(R.string.net_connection_timeout_retry).format(powDelay, seq)
                            LogNet.e(message = message)
                            delay(powDelay.toDuration(DurationUnit.SECONDS))
                            true
                        } else false
                    }

                    BaseApiCode.GW_AUTH_TOKEN_EXPIRED.value -> {
                        if (authRetry && attempt < NET_RETRY_TIMES) {
                            var authSuccessful = false
//                            apiService
//                                .gatewayAuth(
//                                    GatewayAuthContract.Req(
//                                        username.invoke(),
//                                        password.invoke().encodeRsa(rsaPublicKey),
//                                        "password",
//                                        null
//                                    )
//                                )
//                                .collect {
//                                    authSuccessful = true
//                                }
                            authSuccessful
                        } else false
                    }

                    BaseApiCode.UIC_AUTH_NOT_LOGIN.value -> {
                        uicNotLogin?.invoke()
                        false
                    }

                    else -> {
                        false
                    }
                }
            }
            .onCompletion { e ->
                e?.let {
                    if (it is CancellationException) LogNet.e(message = appStr(R.string.net_cancel_request))
                }
            }
    }

    private fun buildClient(
        block: OkHttpClient.Builder.() -> Unit = {}
    ): OkHttpClient {
        val sslClient = SslClient.create(null, null)
        val builder = OkHttpClient.Builder()
        if (sslClient?.socketFactory != null && sslClient.trustManager != null) {
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            builder.sslSocketFactory(sslClient.socketFactory!!, sslClient.trustManager!!)
        }
        builder.apply(block)
        return builder.build()
    }

    private fun Throwable.transformToApiException(): ApiException {
        val apiException: ApiException = this.let {
            when (it) {
                is ApiException,
                is NetworkErrorException ->
                    it as ApiException

                is ConnectException,
                is SocketTimeoutException -> {
                    start(url())
                    NetworkTimeoutException(it, url().networkDesc())
                }

                is SerializationException -> {
                    NetworkErrorException(
                        BaseApiCode.CS_SERVER_SIDE_BREAK_PROTO_CONTRACT.value,
                        appStr(R.string.net_network_error)
                            .format(
                                appStr(R.string.net_server_side_break_proto_contract),
                                it.message
                            ),
                        it
                    )
                }

                is UnknownHostException ->
                    NetworkErrorException(
                        BaseApiCode.CS_DNS_FAILURE.value,
                        appStr(R.string.net_network_error)
                            .format(appStr(R.string.net_dns_failure), url().networkDesc()),
                        it
                    )

                else ->
                    NetworkErrorException(
                        BaseApiCode.CS_OTHER_ERROR.value,
                        appStr(R.string.net_network_error)
                            .format(it.msg, url().networkDesc()),
                        it
                    )
            }
        }
        LogNet.e(message = "api code = {${apiException.code}} msg = {${apiException.msg}} ")
        return apiException
    }
}

internal fun <T> handleResponse(
    request: OkHttpRequest,
    retrofitResponse: RetrofitResponse<T>
): Response<*> {
    if (!retrofitResponse.isSuccessful) {
        throw NetworkErrorException(
            BaseApiCode.CS_HTTP_NON_200.value,
            appStr(R.string.net_network_error)
                .format(
                    appStr(R.string.net_http_non_200)
                        .format(retrofitResponse.code()),
                    request.url.toString().networkDesc()
                )
        )
    }

    val body = retrofitResponse.body()
    checkNotNull(body) // 如果数据有问题会抛出异常，不会出现 body 为空的情况
    val apiResponse = body as Response<*>
    return Response(
        apiResponse.code,
        apiResponse.msg,
        apiResponse.data
    )
}
