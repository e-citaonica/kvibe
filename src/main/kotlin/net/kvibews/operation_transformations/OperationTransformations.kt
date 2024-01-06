package net.kvibews.operation_transformations

import net.kvibews.model.TextOperation

interface OperationTransformations {
    fun transform(op1: TextOperation, op2: TextOperation): List<TextOperation>
}