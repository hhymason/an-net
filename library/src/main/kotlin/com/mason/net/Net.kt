/*
 * Copyright (c) 2015 - present Hive-Box.
 */

package com.mason.net

import android.util.Log
import android.util.Patterns.WEB_URL
import com.mason.net.Consts.PING_TIMEOUT
import com.mason.net.Consts.PING_TIMES
import com.mason.net.Consts.REFERENCE_LAN_HOST
import com.mason.net.Consts.REFERENCE_WLAN_HOST
import com.mason.net.Consts.UNICODE_LENGTH
import com.mason.net.api.ApiService
import com.mason.net.worker.RefreshAuthWorker
import com.mason.util.exception.unexpectedValue
import com.mason.util.os.appVerName
import com.mason.util.os.guid
import com.mason.util.resource.appStr
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.internal.http.RealResponseBody
import okio.*
import java.io.EOFException
import java.net.URL

typealias OkHttpRequest = okhttp3.Request
typealias OkHttpRequestBuilder = okhttp3.Request.Builder
typealias OkHttpResponse = okhttp3.Response
typealias RetrofitResponse<T> = retrofit2.Response<T>

@Suppress("unused")
object Net {
    const val TAG = "Net"
    internal val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    internal val apiService: ApiService by lazy {
        ApiService()
    }
    lateinit var url: () -> String
    var appVersionName: () -> String = {
        appVerName().substringBeforeLast('.', "unknown version")
    }

    var autoRefreshAuth: Boolean = false
        set(value) {
            if (value) RefreshAuthWorker.start() else RefreshAuthWorker.stop()
            field = value
        }
    var gzip = false
    var ping = true
    var timeoutRetry = true
    var referenceWlanHost = REFERENCE_WLAN_HOST
    var referenceLanHost = REFERENCE_LAN_HOST
    var pingTimes = PING_TIMES
    var pingTimeout = PING_TIMEOUT
    var log: INetLog = object : INetLog {
        override fun d(message: String, vararg args: Any?) {
            Log.d(TAG, message.format(*args))
        }

        override fun i(message: String, vararg args: Any?) {
            Log.i(TAG, message.format(*args))
        }

        override fun w(message: String, vararg args: Any?) {
            Log.w(TAG, message.format(*args))
        }

        override fun e(message: String, vararg args: Any?) {
            Log.e(TAG, message.format(*args))
        }
    }
    var uicNotLogin: (suspend () -> Unit)? = null


    internal fun genNetMsgId(): String {
        synchronized(this) {
            val max = 10000000
            var netIdSeq = Sp.netIdSeq
            netIdSeq = netIdSeq % (max - 1) + 1
            Sp.netIdSeq = netIdSeq
            return "$guid${netIdSeq.toString().padStart(7, '0')}"
        }
    }
}

internal fun Buffer.isPlaintext(): Boolean {
    try {
        val prefix = Buffer()
        val byteCount = if (size < 64) size else 64
        copyTo(prefix, 0, byteCount)

        for (i in 1..UNICODE_LENGTH) {
            if (prefix.exhausted()) {
                break
            }
            val codePoint = prefix.readUtf8CodePoint()
            if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                return false
            }
        }
        return true
    } catch (e: EOFException) {
        // Truncated UTF-8 sequence.
        return false
    }
}

internal fun RequestBody.gzip(): RequestBody {
    val it = this
    return object : RequestBody() {
        override fun contentType(): MediaType? {
            return it.contentType()
        }

        override fun writeTo(sink: BufferedSink) {
            val gzipSink = GzipSink(sink).buffer()
            it.writeTo(gzipSink)
            gzipSink.close()
        }
    }
}

internal fun ResponseBody.unGzip(): ResponseBody {
    val it = this
    return RealResponseBody(
        it.contentType().toString(),
        it.contentLength(),
        GzipSource(it.source()).buffer()
    )
}

internal fun String.networkDesc(): String {
    if (!WEB_URL.toRegex().matches(this)) {
        return ""
    }
    val url = URL(this)
    val host = url.host
    return if (host.startsWith("192.168")) {
        appStr(R.string.net_lan_desc)
    } else {
        ""
    }
}

@Suppress("unused")
private fun RequestBody.forceContentLength(): RequestBody {
    val it = this
    val buffer = Buffer()
    it.writeTo(buffer)
    return object : RequestBody() {
        override fun contentType(): MediaType? {
            return it.contentType()
        }

        override fun writeTo(sink: BufferedSink) {
            sink.write(buffer.snapshot())
        }
    }
}

@Serializable
data class Request<T>(
    val requestTime: String,
    var requestId: String,
    val os: String,
    val appVersion: String,
    val data: T
)

@Serializable
data class Response<T>(
    val code: String,
    val msg: String,
    val data: T? = null
)

@Suppress("unused")
enum class BaseApiCode(val value: String) {
    /* 客户端探测: CS (client side) - 负数 */
    CS_DNS_FAILURE("-1"), // 域名解析失败
    CS_SERVER_SIDE_BREAK_PROTO_CONTRACT("-2"), // 服务端违反协议约定，无法解析该数据
    CS_HTTP_TIMEOUT("-3"), // HTTP 超时，客户端网络问题
    CS_HTTP_NON_200("-4"), // HTTP 非 2XX 问题
    CS_OTHER_ERROR("-5"), // 其他错误

    /* 平台无关: PI (platform independence) - 000 */
    PI_SUCCESS("000000000"), // 操作成功
    PI_FAILURE("000000001"), // 操作失败，客户端的请求正常，服务器出错
    PI_INVALID_PARAM("000000002"), // 无效参数，服务器验证客户端的参数无效
    PI_SERVER_SIDE_SYSTEM_BUSY("000000003"), // 服务端系统繁忙
    PI_SERVER_SIDE_EXCEPTION("000000004"), // 服务端的服务器异常
    PI_SERVER_SIDE_SYSTEM_EXCEPTION("000000005"), // 服务端的系统异常

    /* 用户中心: UIC (user interface center) - 103 */
    UIC_AUTH_WRONG_ACCOUNT_OR_PASSWORD("103000002"), // 用户账号或密码错误
    UIC_AUTH_NOT_LOGIN("103000005"), // 用户未登录
    UIC_AUTH_UNAUTHORIZED("103000006"), // 用户未授权
    UIC_AUTH_QRCODE_EXPIRED("103020001"), // 用户登录二维码过期
    UIC_AUTH_QRCODE_UNUSED("103020002"), // 用户登录二维码尚未被使用，可以继续轮询

    /* 网关: GW (gateway) - 200 */
    GW_AUTH_WRONG_KEY_OR_SECRET("200000000"), // APP KEY 或者 APP SECRET 错误
    GW_AUTH_TOKEN_EXPIRED("200000001"); // 服务 token 过期

    companion object {
        private val map = values().associateBy { it.value }
        fun fromValue(type: String) = map[type] ?: unexpectedValue(type)
    }
}

@Suppress("unused")
enum class PlatformId(val value: String) {
    CABINET("004"),
    MOBILE_COURIER("005"),
    MOBILE_CONSUMER("006"),
    CABINET_IOT("008"),
}
