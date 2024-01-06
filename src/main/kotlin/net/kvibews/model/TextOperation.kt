package net.kvibews.model

import net.kvibews.model.enum.OperationType

class TextOperation(
    val opType: OperationType,
    val operand: String,
    val position: Int
) {
    fun operationIsInsert(): Boolean {
       return opType == OperationType.INSERT
    }

    fun operationIsDelete(): Boolean {
        return opType == OperationType.DELETE
    }
}