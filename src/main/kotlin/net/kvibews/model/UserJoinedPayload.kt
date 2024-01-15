package net.kvibews.model

import com.fasterxml.jackson.annotation.JsonProperty

data class UserJoinedPayload (
    @JsonProperty("socketId") val socketId: String,
    @JsonProperty("username")  val username: String
)