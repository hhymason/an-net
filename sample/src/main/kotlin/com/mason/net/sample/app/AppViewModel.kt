package com.mason.net.sample.app

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mason.net.sample.api.apiService
import com.mason.util.appctx.appCtx
import com.mason.util.toast.toast
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream


lateinit var appContext: Application

fun setApplicationContext(context: Application) {
    appContext = context
}

inline fun <reified VM : ViewModel> Fragment.getAppViewModel(): VM {
    (requireActivity().application as? App).let {
        if (it == null) {
            throw NullPointerException("application dose not extends from App?")
        } else {
            return it.getAppViewModelProvider()[VM::class.java]
        }
    }
}

inline fun <reified VM : ViewModel> AppCompatActivity.getAppViewModel(): VM {
    (application as? App).let {
        if (it == null) {
            throw NullPointerException("application dose not extends from App?")
        } else {
            return it.getAppViewModelProvider()[VM::class.java]
        }
    }
}

class AppViewModel : ViewModel() {
    private val url = "https://lmg.jj20.com/up/allimg/1113/031920120534/200319120534-7-1200.jpg"
    var count = 0
    var isStop = false
    private var job: Job? = null
    private var coroutineScope = CoroutineScope(Dispatchers.IO)
    fun init() {
        // start
        isStop = false
        count = 0
//        viewModelScope.ensureActive()
        if (!coroutineScope.isActive) {
            coroutineScope = CoroutineScope(Dispatchers.IO)
        }
        download()
    }

    fun stop() {
        isStop = true
        coroutineScope.cancel()
        job?.cancel()
    }

    fun getWeb() {
        viewModelScope.launch {
            val url = "https://qg.wealthyman.cn/h5/test.txt"
            apiService.getWeb(url)
                .onEach {
                    toast(it)
                }
                .catch {
                    it.printStackTrace()
                }
                .collect {}
        }
    }

    private fun download() {
        viewModelScope.launch {
            job = coroutineScope.launch {

                flowOf(url)
                    .flowOn(Dispatchers.IO)
                    .flatMapConcat {
                        apiService.downloadFromUrl(url)
                    }
                    .map { saveResource(it) }
                    .catch {
                        Log.e("download", "error ${it.message}")
                    }
                    .onCompletion {
                        Log.i("download", "download completion ${++count}")
                        if (coroutineScope.isActive) {
                            download()
                        }
                    }
                    .flowOn(Dispatchers.IO)
                    .collect {}
            }
        }

    }

    private fun saveResource(body: ResponseBody) {
        val file = File(appCtx.filesDir, "download-test.temp")
        // 创建 FileOutputStream，并写入文件
        val outputStream = FileOutputStream(file)
        outputStream.write(body.bytes())
        outputStream.close()
        body.close()
    }

}