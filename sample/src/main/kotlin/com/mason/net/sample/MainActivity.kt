/*
 * Copyright (c) 2015 - present Hive-Box.
 */

package com.mason.net.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mason.net.sample.databinding.ActivityMainBinding
import com.mason.net.sample.app.AppViewModel
import com.mason.net.sample.app.getAppViewModel

class MainActivity : AppCompatActivity() {


    companion object {
        const val LOADING = "loading..."
    }

    private lateinit var binding: ActivityMainBinding
    var refreshToken: String = ""

    private val appViewModel: AppViewModel by lazy { getAppViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.apply {

            btnStart.setOnClickListener {
                appViewModel.getWeb()
            }
            btnStop.setOnClickListener {
                appViewModel.stop()
            }
        }
    }
}

