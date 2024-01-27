package net.kvibews.core.document

import net.kvibews.model.TextOperation
import net.kvibews.model.enum.OperationType

class InvalidOperationException(
    message: String
) : RuntimeException(message)

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
        if (operation.position < 0) {
            throw InvalidOperationException("Insert operation starting index (${operation.position}) is less than 0")
        } else if (operation.position > buffer.length) {
            throw InvalidOperationException("Insert operation starting index (${operation.position}) is greater than document length (${operation.length})")
        } else if (buffer.length == operation.position) {
            buffer.append(operation.operand)
        } else {
            buffer.insert(operation.position, operation.operand)
        }
    }

    private fun applyDelete(operation: TextOperation) {
        if (operation.length > buffer.length) {
            throw InvalidOperationException("Attempted to delete section of length ${operation.position} that is greater than length of document")
        } else if (operation.position > buffer.length || operation.position + operation.length > buffer.length) {
            throw InvalidOperationException("Attempted to remove section out of document bounds")
        } else {
            buffer.delete(operation.position, operation.position + operation.length)
        }
    }
}