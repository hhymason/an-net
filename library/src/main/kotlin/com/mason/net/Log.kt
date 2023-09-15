/*
 * Copyright (c) 2015 - present Hive-Box.
 */

package com.mason.net

import com.mason.net.Net.log
import com.mason.util.exception.msg

interface INetLog {
    fun d(message: String, vararg args: Any?)
    fun i(message: String, vararg args: Any?)
    fun w(message: String, vararg args: Any?)
    fun e(message: String, vararg args: Any?)
}

internal object LogNet {
    private const val TAG = "[网络]"
    private const val INFO = "$TAG %s"
    private const val ERR = "$TAG %s\n%s"
    private const val REQ = "$TAG [请求] --> %s %s netMsgId = %s\n%s\n%s\n--> END"
    private const val RESP = "$TAG [响应] <-- %s %s %s ms netMsgId = %s\n\n%s\n<-- END"
    private const val PING = "$TAG [ping] host = %s\n%s"
    private const val REFERENCE_PING = "$TAG [reference ping] host = %s\n%s"

    fun d(message: String?) {
        log.d(INFO, message)
    }

    fun i(message: String?) {
        log.i(INFO, message)
    }

    fun e(throwable: Throwable? = null, message: String? = null) {
        if (throwable != null) {
            log.e(ERR, message ?: "", throwable.msg)
        } else {
            log.e(ERR, message ?: "", "")
        }
    }

    fun req(
        method: String?,
        url: String?,
        requestId: String?,
        headers: String?,
        body: String?
    ) {
        log.i(REQ, method, url, requestId, headers, body)
    }

    fun resp(
        httpCode: Int,
        url: String?,
        millis: Long,
        requestId: String?,
        body: String?
    ) {
        log.i(RESP, httpCode, url, millis, requestId, body)
    }

    fun ping(host: String, result: String) {
        log.i(PING, host, result)
    }

    fun referencePing(host: String, result: String) {
        log.i(REFERENCE_PING, host, result)
    }
}
