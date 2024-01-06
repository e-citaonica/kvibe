package net.kvibews.operation_transformations.impl

import net.kvibews.model.TextOperation
import net.kvibews.operation_transformations.OperationTransformations
import org.springframework.stereotype.Component

@Component
class SimpleCharacterOperationTransformations: OperationTransformations {

    override fun transform(op1: TextOperation, op2: TextOperation): List<TextOperation> {
        return if (op1.operationIsInsert() && op2.operationIsInsert()) {
            listOf(transformII(op1, op2))
        } else if (op1.operationIsInsert() && op2.operationIsDelete()) {
            listOf(transformID(op1, op2))
        } else if (op1.operationIsDelete() && op2.operationIsInsert()) {
            listOf(transformDI(op1, op2))
        } else if (op1.operationIsDelete() && op2.operationIsDelete()) {
            transformDD(op1, op2)?.let {
                return listOf(it)
            } ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun transformII(op1: TextOperation, op2: TextOperation): TextOperation {
        val newPos = if (op1.position < op2.position) op1.position else op1.position + 1
        return TextOperation(op1.opType, op1.operand, newPos)
    }

    fun transformID(op1: TextOperation, op2: TextOperation): TextOperation {
        val newPos = if (op1.position <= op2.position) op1.position else op1.position - 1
        return TextOperation(op1.opType, op1.operand, newPos)
    }

    fun transformDI(op1: TextOperation, op2: TextOperation): TextOperation {
        val newPos = if (op1.position < op2.position) op1.position else op1.position + 1
        return TextOperation(op1.opType, op1.operand, newPos)
    }

    fun transformDD(op1: TextOperation, op2: TextOperation): TextOperation? {
        val newPos = if (op1.position < op2.position) op1.position
        else if (op1.position > op2.position) op1.position - 1
        else return null
        return TextOperation(op1.opType, op1.operand, newPos)
    }
}

