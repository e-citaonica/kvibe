package net.kvibews.dto

import com.fasterxml.jackson.annotation.JsonProperty
import net.kvibews.model.DocumentOverview
import java.time.ZonedDateTime


object DocumentDTO {

    data class Create(
        val name: String,
        val language: String,
        val username: String
    )

    data class UserInfo(
        @JsonProperty("docId") val docId: String,
        @JsonProperty("sessionId") val sessionId: String,
        @JsonProperty("username") val username: String
    )
}
