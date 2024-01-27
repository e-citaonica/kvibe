package net.kvibews.core.document

import net.kvibews.core.operation_transformations.OperationTransformations
import net.kvibews.exception.InvalidOperationRevisionException
import net.kvibews.model.DocumentState
import net.kvibews.model.OperationWrapper
import net.kvibews.model.TextOperation
import java.time.ZonedDateTime
import java.util.*

class SubmitRequest(
    var operation: OperationWrapper,
    var snapshot: DocumentState,
    private val opTransformations: OperationTransformations,
) {
    private val formatter: DocumentFormatter = DocumentFormatter(snapshot.content)
    var revision: Int = snapshot.revision
        private set
    var operations: MutableList<TextOperation> = snapshot.operations.toMutableList()
        private set

    fun apply(operation: TextOperation) {
        operations.add(operation)
        formatter.apply(operation)
        revision += 1
    }

    fun transform(): List<TextOperation> {
        if (operation.revision > snapshot.revision) {
            throw InvalidOperationRevisionException(
                "Attempted to perform operation with greater revision (${operation.revision}) than current document revision ${snapshot.revision}"
            )
        }

        val opQueue: Queue<Pair<TextOperation, Int>> = LinkedList()
        val ops: MutableList<TextOperation> = mutableListOf()
        opQueue.add(Pair(operation.operation, operation.revision))

        while (!opQueue.isEmpty()) {
            val op = opQueue.poll()
            var transformedOperation: TextOperation? = operation.operation

            for (revision in op.second until snapshot.operations.size) {
                val operations = opTransformations.transform(transformedOperation!!, snapshot.operations[revision])
                if (operations.isEmpty()) {
                    transformedOperation = null
                    break
                }
                transformedOperation = operations[0]
                if (operations.size > 1) {
                    opQueue.add(Pair(operations[1], revision + 1))
                }
            }

            if (transformedOperation != null) {
                ops.add(transformedOperation)
            }
        }

        return ops
    }

    fun getTransformed() = DocumentState(
        revision = revision,
        operations = operations,
        id = snapshot.id,
        languageId = snapshot.languageId,
        name = snapshot.name,
        createdAt = snapshot.createdAt,
        updatedAt = ZonedDateTime.now(),
        content = formatter.getValue()
    )
}