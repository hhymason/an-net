/*
 * Copyright (c) 2015 - present Hive-Box.
 */

package com.mason.net.worker

import android.content.Context
import androidx.core.util.PatternsCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mason.net.Consts
import com.mason.net.LogNet
import com.mason.net.Net
import com.mason.net.Net.pingTimeout
import com.mason.net.Net.pingTimes
import com.mason.net.Net.referenceLanHost
import com.mason.net.Net.referenceWlanHost
import com.mason.util.appctx.appCtx
import java.net.URL
import com.mason.util.net.ping as pingUrl

internal class PingWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    companion object {
        private const val WORKER_NAME = "ping_worker"
        const val PING_URL = "ping_url"

        fun start(url: String) {
            if (Net.ping) {
                val request = OneTimeWorkRequestBuilder<PingWorker>()
                    .setInputData(
                        workDataOf(
                            PING_URL to url
                        )
                    )
                    .build()
                WorkManager.getInstance(appCtx)
                    .enqueueUniqueWork(WORKER_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
            }
        }
    }

    override fun doWork(): Result {
        val urlStr = inputData.getString(PING_URL) ?: return Result.failure()
        LogNet.i("ping worker, timeout host = $urlStr")
        if (!PatternsCompat.WEB_URL.toRegex().matches(urlStr)) {
            return Result.failure()
        }
        val url = URL(urlStr)
        val host = url.host
        var result = pingUrl(host, pingTimes, pingTimeout)
        LogNet.ping(host, result)
        if (host.startsWith("192.168")) {
            result = pingUrl(referenceLanHost, pingTimes, pingTimeout)
            LogNet.referencePing(Consts.REFERENCE_LAN_HOST, result)
        } else {
            result = pingUrl(referenceWlanHost, pingTimes, pingTimeout)
            LogNet.referencePing(Consts.REFERENCE_WLAN_HOST, result)
        }
        return Result.success()
    }
}
