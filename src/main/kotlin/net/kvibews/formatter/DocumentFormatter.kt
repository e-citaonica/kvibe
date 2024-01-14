package net.kvibews.formatter

import net.kvibews.model.TextOperation
import net.kvibews.enum.OperationType


class DocumentFormatter(initialContent: String?) {

    private val buffer = StringBuffer(initialContent ?: "")

    fun getValue() = buffer.toString()
    fun apply(operation: TextOperation) {
        when (operation.type) {
            OperationType.INSERT -> applyInsert(operation)
            OperationType.DELETE -> applyDelete(operation)
        }
    }

    private fun applyInsert(operation: TextOperation) {
        if (buffer.length == operation.position) {
            buffer.append(operation.operand)
        } else {
            buffer.insert(operation.position, operation.operand)
        }
    }

    private fun applyDelete(operation: TextOperation) {
        buffer.delete(operation.position, operation.position + operation.length)
    }
}