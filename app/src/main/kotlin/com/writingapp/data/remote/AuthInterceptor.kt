package com.writingapp.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    @Volatile
    var apiKey: String = ""

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val modified = if (apiKey.isNotBlank()) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
        } else request
        return chain.proceed(modified)
    }
}
