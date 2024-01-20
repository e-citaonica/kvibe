package net.kvibews.enum

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class OperationType(@JsonValue val value: String) {
    INSERT("insert"),
    DELETE("delete");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(value: String): OperationType = entries.first {
            it.value.equals(value, true)
        }
    }
}