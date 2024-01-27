package net.kvibews.dto

import com.fasterxml.jackson.annotation.JsonProperty


object DocumentDTO {

    data class Create(
        val name: String,
        val language: String,
    )

    data class UserInfo(
        @JsonProperty("docId") val docId: String,
        @JsonProperty("sessionId") val sessionId: String,
        @JsonProperty("username") val username: String
    )
}
