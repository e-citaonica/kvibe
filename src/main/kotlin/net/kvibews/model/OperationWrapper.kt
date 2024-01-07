package net.kvibews.model

import com.fasterxml.jackson.annotation.JsonProperty


data class OperationWrapper(
    @JsonProperty("docId") val docId: String,
    @JsonProperty("revision") val revision: Int,
    @JsonProperty("performedBy") val performedBy: String,
    @JsonProperty("operation") val operation: TextOperation
)