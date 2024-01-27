package net.kvibews.core.operation_transformations

import net.kvibews.model.TextOperation
import net.kvibews.model.TextSelection

interface OperationTransformations {
    fun transform(op1: TextOperation, op2: TextOperation): List<TextOperation>
    fun transform(op1: TextSelection, op2: TextOperation): TextSelection
}