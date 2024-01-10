package net.kvibews.operation_transformations.impl

import net.kvibews.enum.OperationType
import net.kvibews.model.TextOperation
import net.kvibews.operation_transformations.OperationTransformations
import org.slf4j.Logger
import org.springframework.stereotype.Component

@Component("char")
class CharOperationTransformations(val logger: Logger): OperationTransformations {

    override fun transform(op1: TextOperation, op2: TextOperation): List<TextOperation> {
        return if (op1.type == OperationType.INSERT && op2.type == OperationType.INSERT) {
            listOf(transformII(op1, op2))
        } else if (op1.type == OperationType.INSERT && op2.type == OperationType.DELETE) {
            listOf(transformID(op1, op2))
        } else if (op1.type == OperationType.DELETE && op2.type == (OperationType.INSERT)) {
            listOf(transformDI(op1, op2))
        } else if (op1.type == OperationType.DELETE && op2.type == OperationType.DELETE) {
            transformDD(op1, op2)?.let {
                return listOf(it)
            } ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun transformII(op1: TextOperation, op2: TextOperation): TextOperation {
        val newPos = if (op1.position < op2.position) op1.position else op1.position + 1
        return TextOperation(op1.type, op1.operand, newPos, op1.length)
    }

    fun transformID(op1: TextOperation, op2: TextOperation): TextOperation {
        val newPos = if (op1.position <= op2.position) op1.position else op1.position - 1
        return TextOperation(op1.type, op1.operand, newPos, op1.length)
    }

    fun transformDI(op1: TextOperation, op2: TextOperation): TextOperation {
        val newPos = if (op1.position < op2.position) op1.position else op1.position + 1
        return TextOperation(op1.type, op1.operand, newPos, op1.length)
    }

    fun transformDD(op1: TextOperation, op2: TextOperation): TextOperation? {
        val newPos = if (op1.position < op2.position) op1.position
        else if (op1.position > op2.position) op1.position - 1
        else return null
        return TextOperation(op1.type, op1.operand, newPos, op1.length)
    }
}

