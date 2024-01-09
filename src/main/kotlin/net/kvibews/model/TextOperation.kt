package net.kvibews.model

import com.fasterxml.jackson.annotation.JsonProperty
import net.kvibews.enum.OperationType

data class TextOperation(
    @JsonProperty("type") val type: OperationType,
    @JsonProperty("operand") val operand: String?,
    @JsonProperty("position") val position: Int,
    @JsonProperty("length") val length: Int
) {
    fun isInsert(): Boolean {
       return type == OperationType.INSERT
    }

    fun isDelete(): Boolean {
        return type == OperationType.DELETE
    }
}