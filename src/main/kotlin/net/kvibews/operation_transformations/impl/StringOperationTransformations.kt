package net.kvibews.operation_transformations.impl

import net.kvibews.model.TextOperation
import net.kvibews.operation_transformations.OperationTransformations
import org.springframework.stereotype.Component

@Component("string")
class StringOperationTransformations : OperationTransformations {

    override fun transform(op1: TextOperation, op2: TextOperation): List<TextOperation> {
        return if (op1.operationIsInsert() && op2.operationIsInsert())
            listOf(transformII(op1, op2))
        else if (op1.operationIsInsert() && op2.operationIsDelete())
            listOf(transformID(op1, op2))
        else if (op1.operationIsDelete() && op2.operationIsInsert())
            transformDI(op1, op2)
        else if (op1.operationIsDelete() && op2.operationIsDelete())
            transformDD(op1, op2)?.let {
                return listOf(it)
            } ?: emptyList()
        else emptyList()
    }

    private fun transformII(op1: TextOperation, op2: TextOperation): TextOperation {
        val newPos = if (op1.position < op2.position) {
            op1.position
        } else {
            op1.position + op2.operand.length
        }
        return TextOperation(op1.opType, op1.operand, newPos)
    }

    private fun transformID(op1: TextOperation, op2: TextOperation): TextOperation {
        val op2End = op2.position + op2.operand.length - 1
        return if (op1.position <= op2.position) {
            TextOperation(op1.opType, op1.operand, op1.position)
        } else if (op1.position <= op2End) {
            TextOperation(op1.opType, op1.operand, op2.position)
        } else {
            TextOperation(op1.opType, op1.operand, op1.position - op2.operand.length)
        }
    }

    private fun transformDI(op1: TextOperation, op2: TextOperation): List<TextOperation> {
        val op1End = op1.position + op1.operand.length - 1
        return if (op1.position < op2.position) {
            if (op1End < op2.position) {
                listOf(TextOperation(op1.opType, op1.operand, op1.position))
            } else {
                val left = op1.operand.substring(0, op2.position - op1.position)
                val right = op1.operand.substring(left.length)

                listOf(
                    TextOperation(op1.opType, left, op1.position),
                    TextOperation(op1.opType, right, op1.position + left.length + op2.operand.length)
                )
            }
        } else {
            listOf(TextOperation(op1.opType, op1.operand, op1.position + op2.operand.length))
        }
    }

    private fun transformDD(op1: TextOperation, op2: TextOperation): TextOperation? {
        val op1End = op1.position + op1.operand.length - 1
        val op2End = op2.position + op2.operand.length - 1

        return if (op1End < op2.position) {
            TextOperation(op1.opType, op1.operand, op1.position)
        } else if (op1.position > op2End) {
            TextOperation(op1.opType, op1.operand, op1.position - op2.operand.length)
        } else if (op1.position < op2.position) {
            val operand = op1.operand.substring(0, op2.position - op1.position)
            TextOperation(op1.opType, operand, op1.position)
        } else if (op1End > op2End) {
            val diff = op1.position + op1.operand.length - (op2.position + op2.operand.length)
            val operand = op1.operand.substring(op1.operand.length - diff)
            TextOperation(op1.opType, operand, op2.position)
        } else {
            null
        }
    }

}