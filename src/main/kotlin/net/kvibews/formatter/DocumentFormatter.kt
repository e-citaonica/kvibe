package net.kvibews.formatter

import net.kvibews.model.TextOperation
import net.kvibews.model.enum.OperationType
import org.springframework.stereotype.Component


class DocumentFormatter {

    private var buffer = StringBuffer()

    fun applyOperation(operation: TextOperation): String {
        return when (operation.opType) {
            OperationType.INSERT -> applyInsert(operation)
            OperationType.DELETE -> applyDelete(operation)
        }
    }

    fun getText(): String {
        return buffer.toString()
    }

    fun reset(value: String) {
        buffer = StringBuffer()
        buffer.append(value)
    }

    private fun applyInsert(operation: TextOperation): String {
        if (buffer.length == operation.position) {
            buffer.append(operation.operand)
        } else {
            buffer.insert(operation.position, operation.operand)
        }
        return buffer.toString()
    }

    private fun applyDelete(operation: TextOperation): String {
        val start = operation.position
        val end = start + operation.operand.length
        buffer.delete(start, end)
        return buffer.toString()
    }
}