package net.kvibews.model

import net.kvibews.model.enum.OperationType


class OperationWrapper(
    val docId: String,
    val revision: Int,
    val performedBy: String,
    val operation: TextOperation
) {
    constructor() : this("", 0, "", TextOperation(OperationType.INSERT, "", 0)) {
    }
}