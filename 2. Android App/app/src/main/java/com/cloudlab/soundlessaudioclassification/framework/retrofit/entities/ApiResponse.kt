package com.cloudlab.soundlessaudioclassification.framework.retrofit.entities

import com.google.gson.annotations.SerializedName

data class ApiResponse(
    @SerializedName("data") val data: Any?,
    @SerializedName("log") val log: List<String>,
    @SerializedName("message") val message: String,
    @SerializedName("statusCode") val statusCode: Int
)