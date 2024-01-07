package net.kvibews.enum

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class OperationType(val value: String) {
    INSERT("insert"),
    DELETE("delete");

    @JsonValue fun getSerialized() = value
    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(value: String): OperationType = entries.first {
            it.value.equals(value, true)
        }
    }
}