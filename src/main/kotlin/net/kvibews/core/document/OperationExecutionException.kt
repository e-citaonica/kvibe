package net.kvibews.core.document

import net.kvibews.model.TextOperation

abstract class OperationExecutionException : RuntimeException {
    var operation: TextOperation
        private set
    var docLen: Int = 0
        private set

    constructor(operation: TextOperation, docLen: Int, message: String) : super(message) {
        this.operation = operation
        this.docLen = docLen
    }

    constructor(operation: TextOperation, docLen: Int, message: String, throwable: Throwable) : super(
        message, throwable
    ) {
        this.operation = operation
        this.docLen = docLen
    }

}

class DeleteOperationExecutionException : OperationExecutionException {

    constructor(operation: TextOperation, docLen: Int) : super(operation, docLen, getErrorMessage(operation, docLen))

    constructor(operation: TextOperation, docLen: Int, throwable: Throwable) : super(
        operation, docLen, InsertOperationExecutionException.getErrorMessage(operation, docLen), throwable
    )

    companion object {
        fun getErrorMessage(operation: TextOperation, docLen: Int): String {
            return """Failed to apply delete operation as deletion of length ${operation.position} 
                starting at position ${operation.position} is out of bounds of document of length $docLen"""
        }
    }
}

class InsertOperationExecutionException : OperationExecutionException {

    constructor(operation: TextOperation, docLen: Int) : super(operation, docLen, getErrorMessage(operation, docLen))

    constructor(operation: TextOperation, docLen: Int, throwable: Throwable) : super(
        operation, docLen, getErrorMessage(operation, docLen), throwable
    )

    companion object {
        fun getErrorMessage(operation: TextOperation, docLen: Int): String {
            return """Failed to apply insert operation at position 
                ${operation.position} as ${operation.position} is 
                greater than document length ($docLen)")""".trimIndent()
        }
    }
}