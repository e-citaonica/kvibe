package net.kvibews.model

import com.fasterxml.jackson.annotation.JsonProperty
import net.kvibews.enum.OperationType

data class TextOperation(
    @JsonProperty("type") val opType: OperationType,
    @JsonProperty("operand") val operand: String,
    @JsonProperty("position") val position: Int
) {
    fun operationIsInsert(): Boolean {
       return opType == OperationType.INSERT
    }

    fun operationIsDelete(): Boolean {
        return opType == OperationType.DELETE
    }
}