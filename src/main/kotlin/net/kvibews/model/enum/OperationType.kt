package net.kvibews.model.enum

import com.fasterxml.jackson.annotation.JsonCreator

enum class OperationType(val type: String) {
    INSERT("insert"),
    DELETE("delete");

    @JsonCreator
    fun fromString(value: String): OperationType {
        return if (value == "insert")
            INSERT
        else
            DELETE
    }
}