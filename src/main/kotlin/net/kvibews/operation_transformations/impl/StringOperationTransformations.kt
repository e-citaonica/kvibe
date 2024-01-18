package net.kvibews.operation_transformations.impl

import net.kvibews.enum.OperationType
import net.kvibews.model.TextOperation
import net.kvibews.model.TextSelection
import net.kvibews.operation_transformations.OperationTransformations
import org.slf4j.Logger
import org.springframework.stereotype.Component

@Component("string")
class StringOperationTransformations(val logger: Logger) : OperationTransformations {

    override fun transform(op1: TextOperation, op2: TextOperation): List<TextOperation> {
        return if (op1.type == OperationType.INSERT && op2.type == OperationType.INSERT)
            listOf(transformII(op1, op2))
        else if (op1.type == OperationType.INSERT && op2.type == OperationType.DELETE)
            listOf(transformID(op1, op2))
        else if (op1.type == OperationType.DELETE && op2.type == OperationType.INSERT)
            transformDI(op1, op2)
        else if (op1.type == OperationType.DELETE && op2.type == OperationType.DELETE)
            transformDD(op1, op2)?.let {
                return listOf(it)
            } ?: emptyList()
        else emptyList()
    }

    private fun transformII(op1: TextOperation, op2: TextOperation): TextOperation {
        logger.info("II op1:{} op2:{}", op1, op2)
        val newPos = if (op1.position < op2.position) {
            op1.position
        } else {
            op1.position + op2.length
        }
        return TextOperation(op1.type, op1.operand, newPos, op1.length)
    }

    private fun transformID(op1: TextOperation, op2: TextOperation): TextOperation {
        logger.info("ID op1:{} op2:{}", op1, op2)
        val op2End = op2.position + op2.length - 1
        return if (op1.position <= op2.position) {
            TextOperation(op1.type, op1.operand, op1.position, op1.length)
        } else if (op1.position <= op2End) {
            TextOperation(op1.type, op1.operand, op2.position, op1.length)
        } else {
            TextOperation(op1.type, op1.operand, op1.position - op2.length, op1.length)
        }
    }

    private fun transformDI(op1: TextOperation, op2: TextOperation): List<TextOperation> {
        logger.info("DI op1:{} op2:{}", op1, op2)
        val op1End = op1.position + op1.length - 1
        return if (op1.position < op2.position) {
            if (op1End < op2.position) {
                listOf(TextOperation(op1.type, op1.operand, op1.position, op1.length))
            } else {
                val leftLength = op2.position - op1.position
                val rightLength = op1.length - op2.position + op1.position
                val left = op1.operand?.substring(0, leftLength)
                val right = op1.operand?.substring(leftLength)

                listOf(
                    TextOperation(op1.type, left, op1.position, op2.position - op1.position),
                    TextOperation(op1.type, right, op1.position + leftLength + op2.length, rightLength)
                )
            }
        } else {
            listOf(TextOperation(op1.type, op1.operand, op1.position + op2.length, op1.length))
        }
    }

    private fun transformDD(op1: TextOperation, op2: TextOperation): TextOperation? {
        logger.info("DD op1:{} op2:{}", op1, op2)
        val op1End = op1.position + op1.length - 1
        val op2End = op2.position + op2.length - 1

        return if (op1End < op2.position) {
            TextOperation(op1.type, op1.operand, op1.position, op1.length)
        } else if (op1.position > op2End) {
            TextOperation(op1.type, op1.operand, op1.position - op2.length, op1.length)
        } else if (op1.position < op2.position) {
            val operand = op1.operand?.substring(0, op2.position - op1.position)
            TextOperation(op1.type, operand, op1.position, op2.position - op1.position)
        } else if (op1End > op2End) {
            val diff = op1.position + op1.length - (op2.position + op2.length)
            val operand = op1.operand?.substring(op1.length - diff)
            TextOperation(op1.type, operand, op2.position, diff)
        } else {
            null
        }
    }

    override fun transform(op1: TextSelection, op2: TextOperation): TextSelection {
        return if (op2.type == OperationType.INSERT)
            (transformSI(op1, op2))
        else
            (transformSD(op1, op2))
    }

    private fun transformSI(selection: TextSelection, op: TextOperation): TextSelection {
        // text inserted inside selection => extend selection by text length
        return if (op.position >= selection.from && op.position < selection.to) {
            TextSelection(
                selection.docId,
                selection.revision,
                selection.from,
                selection.to + op.length,
                selection.performedBy
            )
        }
        // selection left of insert or text inserted after selection
        // OR selection starts before insert and ends inside insert
        else selection
    }

    private fun transformSD(selection: TextSelection, op: TextOperation): TextSelection {
        val opEnd = op.position + op.length - 1

        // delete starts and ends before selection
        return if (opEnd <= selection.from) {
            TextSelection(
                selection.docId,
                selection.revision,
                selection.from - op.length,
                selection.to - op.length,
                selection.performedBy
            )
        }
        // delete starts before selection and ends in middle of selection
        else if (op.position < selection.from && opEnd > selection.from && opEnd < selection.to) {
            val overlapFromStartOfSelection = selection.from - op.position;
            TextSelection(
                selection.docId,
                selection.revision,
                selection.from - (op.length - overlapFromStartOfSelection),
                selection.to - overlapFromStartOfSelection,
                selection.performedBy
            )
        }
        // delete is inside of selection
        else if (op.position >= selection.from) {
            val overlapFromStartOfSelection = op.position - selection.from
            selection
        } else selection
    }
}