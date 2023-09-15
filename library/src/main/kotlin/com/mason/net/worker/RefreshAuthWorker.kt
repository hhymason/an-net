/*
 * Copyright (c) 2015 - present Hive-Box.
 */

package com.mason.net.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mason.net.LogNet
import com.mason.net.Net.apiService
import com.mason.net.Sp
import com.mason.net.api.GatewayAuthContract
import com.mason.util.appctx.appCtx
import java.util.concurrent.TimeUnit

internal class RefreshAuthWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    companion object {
        private const val WORKER_NAME = "refresh_auth_worker"

        fun start() {
            LogNet.i("start automatic refresh gateway auth")
            val request =
                PeriodicWorkRequestBuilder<RefreshAuthWorker>(15, TimeUnit.MINUTES)
                    .setInitialDelay(10, TimeUnit.MINUTES)
                    .build()

            WorkManager.getInstance(appCtx)
                .enqueueUniquePeriodicWork(WORKER_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun stop() {
            LogNet.i("stop automatic refresh gateway auth")
            WorkManager.getInstance(appCtx)
                .cancelUniqueWork(WORKER_NAME)
        }
    }

    override suspend fun doWork(): Result {
        var result: Result = Result.failure()
        apiService
            .gatewayAuth(
                GatewayAuthContract.Req(
                    grantType = "refresh_token",
                    refreshToken = Sp.gatewayRefreshAuth
                )
            )
            .collect {
                result = Result.success()
            }
        return result
    }
}
