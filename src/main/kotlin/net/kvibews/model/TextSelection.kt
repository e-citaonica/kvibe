package net.kvibews.model

import com.fasterxml.jackson.annotation.JsonProperty

data class TextSelection(
    @JsonProperty("docId") val docId: String,
    @JsonProperty("revision") val revision: Int,
    @JsonProperty("from") val from: Int,
    @JsonProperty("to") val to: Int,
    @JsonProperty("performedBy") val performedBy: String
)
