/*
 * Copyright (c) 2015 - present Hive-Box.
 */

package com.mason.net

import com.mason.util.config.Encoding.CHARSET_UTF_8
import com.mason.util.os.languageAndCountry
import okhttp3.Interceptor
import okhttp3.internal.http.promisesBody
import okio.Buffer
import java.nio.charset.Charset
import java.util.Locale

internal class HeaderRequestInterceptor(
    private val block: (builder: OkHttpRequestBuilder, url: String) -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): OkHttpResponse {
        val originRequest = chain.request()
        val requestBuilder = originRequest.newBuilder()
            .header("Content-Type", "application/json")
            .header("Accept-Language", languageAndCountry)

        block(requestBuilder, originRequest.url.toString())
        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}

internal class LogRequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): OkHttpResponse {
        val originRequest = chain.request()
        val originRequestBody = originRequest.body
        val hasRequestBody = originRequestBody != null
        var requestBodyText = ""
        if (hasRequestBody) {
            val requestBodyBuffer = Buffer()
            originRequestBody?.writeTo(requestBodyBuffer)
            var charset: Charset = CHARSET_UTF_8
            val contentType = originRequestBody?.contentType()
            if (contentType != null) {
                charset = contentType.charset(CHARSET_UTF_8) ?: charset
            }

            if (requestBodyBuffer.isPlaintext()) {
                requestBodyText = requestBodyBuffer.readString(charset)
            }
        }

        val firstRegex =
            """
                "requestId":"((?!").)+",
            """
                .trimIndent()
                .toRegex()
        val requestId = firstRegex.find(requestBodyText)?.let {
            val origin = it.value
            val secondRegex =
                """
                    "((?!").)+"
                """
                    .trimIndent()
                    .toRegex()
            secondRegex.find(origin, 12)?.value ?: ""
        } ?: ""
        requestBodyText = firstRegex.replace(requestBodyText, "")

        val requestBuilder = originRequest.newBuilder()
            .header("Request-ID", requestId)
        val request = requestBuilder.build()
        val headers = request.headers
        var requestHeaderText = ""
        for (i in 0 until headers.size) {
            when (headers.name(i).lowercase(Locale.US)) {
                "content-type",
                "request-id",
                "transfer-encoding",
                "content-length" -> {
                    // ignored
                }

                else -> {
                    requestHeaderText += "${headers.name(i)}: ${headers.value(i)}\n"
                }
            }
        }
        LogNet.req(
            request.method,
            request.url.toString(),
            requestId,
            requestHeaderText,
            requestBodyText
        )
        return chain.proceed(request)
    }
}

internal class GzipRequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): OkHttpResponse {
        val originRequest = chain.request()
        val requestBody = originRequest.body
        val hasRequestBody = requestBody != null
        val requestBuilder = originRequest.newBuilder()
        if (hasRequestBody) {
            requestBuilder.header("Accept-Encoding", "gzip")
                .header("Content-Encoding", "gzip")
                .method(originRequest.method, requestBody?.gzip())
        }
        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}

internal class GzipResponseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): OkHttpResponse {
        val request = chain.request()
        var response = chain.proceed(request)
        var responseBody = response.body
        val responseHeaders = response.headers
        if (response.promisesBody() && responseBody != null &&
            responseHeaders["Content-Encoding"].equals("gzip", ignoreCase = true)
        ) {
            responseBody = responseBody.unGzip()
            response = response.newBuilder()
                .body(responseBody)
                .build()
        }
        return response
    }
}

internal class LogResponseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): OkHttpResponse {
        val request = chain.request()
        val headers = request.headers
        var requestId = ""
        for (i in 0 until headers.size) {
            when (headers.name(i)) {
                "Request-ID" -> {
                    requestId = headers.value(i)
                }

                else -> {
                    // ignored
                }
            }
        }
        val start = System.currentTimeMillis()
        val response = chain.proceed(request)
        val millis = System.currentTimeMillis() - start
        var responseBodyText = ""
        val responseBody = response.body
        if (response.promisesBody() && responseBody != null) {
            responseBodyText = response.peekBody(java.lang.Long.MAX_VALUE).string()
        }
        LogNet.resp(
            response.code,
            request.url.toString(),
            millis,
            requestId,
            responseBodyText
        )
        return response
    }
}
