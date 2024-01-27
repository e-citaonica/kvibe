package net.kvibews.core.document

import net.kvibews.model.TextOperation
import net.kvibews.model.enum.OperationType

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
            try {
                buffer.insert(operation.position, operation.operand)
            } catch (e: StringIndexOutOfBoundsException) {
                throw InsertOperationExecutionException(operation, buffer.length, e)
            }
        }
    }

    private fun applyDelete(operation: TextOperation) {
        try {
            buffer.delete(operation.position, operation.position + operation.length)
        } catch (e: StringIndexOutOfBoundsException) {
            throw DeleteOperationExecutionException(operation, buffer.length, e)
        }
    }
}