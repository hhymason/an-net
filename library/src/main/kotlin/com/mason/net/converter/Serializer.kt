/*
 * Copyright (c) 2015 - present Hive-Box.
 */

package com.mason.net.converter

import com.mason.net.ApiException
import com.mason.net.BaseApiCode
import com.mason.net.Net.json
import com.mason.net.Response
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.serializer
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import java.lang.reflect.Type

internal sealed class Serializer {
    abstract fun <T> fromResponseBody(loader: DeserializationStrategy<T>, body: ResponseBody): T
    abstract fun <T> toRequestBody(
        contentType: MediaType,
        saver: SerializationStrategy<T>,
        value: T
    ): RequestBody

    @OptIn(ExperimentalSerializationApi::class) // Experimental is only for subtypes.
    protected abstract val format: SerialFormat

    @ExperimentalSerializationApi // serializer(Type) is not stable.
    fun serializer(type: Type): KSerializer<Any> = format.serializersModule.serializer(type)

    @OptIn(ExperimentalSerializationApi::class) // Experimental is only for subtypes.
    class FromString(override val format: StringFormat) : Serializer() {
        override fun <T> fromResponseBody(
            loader: DeserializationStrategy<T>,
            body: ResponseBody
        ): T {
            val string = body.string()
            json.decodeFromString<Response<Stub>>(string)
                .also {
                    if (it.code != BaseApiCode.PI_SUCCESS.value) {
                        throw ApiException(it.code, it.msg)
                    }
                }
            return format.decodeFromString(loader, string)
        }

        override fun <T> toRequestBody(
            contentType: MediaType,
            saver: SerializationStrategy<T>,
            value: T
        ): RequestBody {
            val string = format.encodeToString(saver, value)
            return RequestBody.create(contentType, string)
        }
    }

    @Serializable
    class Stub

    @OptIn(ExperimentalSerializationApi::class) // Experimental is only for subtypes.
    class FromBytes(override val format: BinaryFormat) : Serializer() {
        override fun <T> fromResponseBody(
            loader: DeserializationStrategy<T>,
            body: ResponseBody
        ): T {
            val bytes = body.bytes()
            return format.decodeFromByteArray(loader, bytes)
        }

        override fun <T> toRequestBody(
            contentType: MediaType,
            saver: SerializationStrategy<T>,
            value: T
        ): RequestBody {
            val bytes = format.encodeToByteArray(saver, value)
            return RequestBody.create(contentType, bytes)
        }
    }
}
