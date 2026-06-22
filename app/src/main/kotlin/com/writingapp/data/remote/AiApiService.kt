package com.writingapp.data.remote

import com.writingapp.data.remote.dto.ChatRequest
import com.writingapp.data.remote.dto.ChatResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface AiApiService {
    @POST
    suspend fun chatCompletion(@Url url: String, @Body request: ChatRequest): Response<ChatResponse>

    @POST
    suspend fun chatCompletionStream(@Url url: String, @Body request: ChatRequest): Response<ResponseBody>
}
