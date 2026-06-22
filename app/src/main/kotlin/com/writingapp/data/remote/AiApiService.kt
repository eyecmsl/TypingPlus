package com.writingapp.data.remote

import com.writingapp.data.remote.dto.ChatRequest
import com.writingapp.data.remote.dto.ChatResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AiApiService {
    @POST("chat/completions")
    suspend fun chatCompletion(@Body request: ChatRequest): Response<ChatResponse>

    @POST("chat/completions")
    suspend fun chatCompletionStream(@Body request: ChatRequest): Response<ResponseBody>
}
