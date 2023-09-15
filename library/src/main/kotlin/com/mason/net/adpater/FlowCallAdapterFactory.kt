/*
 * Copyright (c) 2015 - present Hive-Box.
 */

package com.mason.net.adpater

import kotlinx.coroutines.flow.Flow
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class FlowCallAdapterFactory : CallAdapter.Factory() {
    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {
        if (getRawType(returnType) != Flow::class.java) {
            return null
        }
        val observableType =
            getParameterUpperBound(
                0,
                returnType as ParameterizedType
            )
        return FlowCallAdapter<Any?>(
            observableType
        )
    }
//    = when (getRawType(returnType)) {
//        Flow::class.java -> {
//            check(returnType is ParameterizedType) { "Flow return type must be parameterized as Flow<Foo>" }
//            val responseType = getParameterUpperBound(0, returnType)
//            check(responseType is ParameterizedType && getRawType(responseType) == Response::class.java) { "Response must be parameterized as Response<Foo>" }
//            FlowCallAdapter<Any>(responseType)
//        }
//        else -> null
//    }

    companion object {
        val instance by lazy { FlowCallAdapterFactory() }
    }
}
