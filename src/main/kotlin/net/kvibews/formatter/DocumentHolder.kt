package net.kvibews.formatter

import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock
import net.kvibews.exception.InvalidOperationRevision
import net.kvibews.model.DocumentState
import net.kvibews.model.OperationWrapper
import net.kvibews.model.TextOperation
import net.kvibews.operation_transformations.OperationTransformations
import java.util.*

class DocumentHolder private constructor(
    private val state: DocumentState,
    val lock: ReentrantReadWriteUpdateLock,
    private val formatter: DocumentFormatter,
    private val transformations: OperationTransformations
) {

    companion object Factory {
        fun getInstance(state: DocumentState, transformations: OperationTransformations): DocumentHolder {
            return DocumentHolder(
                state,
                ReentrantReadWriteUpdateLock(),
                DocumentFormatter(state.content),
                transformations
            )
        }
    }

    fun apply(operation: TextOperation) {
        state.operations.add(operation)
        state.revision += 1
        formatter.apply(operation)
    }

    fun addUser(user: String) {
        state.activeUsers.add(user)
    }

    fun removeUser(user: String) {
        state.activeUsers.removeIf { it == user }
    }

    fun transformAgainstRevisionLogs(operation: OperationWrapper): List<TextOperation> {

        if (operation.revision > state.revision) {
            throw InvalidOperationRevision(
                "Attempted to perform operation with greater revision (${operation.revision}) than current document revision ${state.revision}"
            )
        } else if (operation.revision == state.revision) {
            return listOf(operation.operation)
        }

        val transformedOperations: MutableList<TextOperation> = mutableListOf()
        val opQueue: Queue<Pair<TextOperation, Int>> = LinkedList()
        opQueue.add(Pair(operation.operation, operation.revision))

        while (!opQueue.isEmpty()) {
            val op = opQueue.poll()
            var transformedOperation: TextOperation? = operation.operation

            for (revision in op.second until state.operations.size) {
                val operations = transformations.transform(transformedOperation!!, state.operations[revision])
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
                transformedOperations.add(transformedOperation)
            }
        }

        return transformedOperations
    }

    fun getSnapshot(): DocumentState {
        return DocumentState(
            revision = state.revision,
            content = formatter.getValue(),
            activeUsers = state.activeUsers.toMutableList(),
            name = state.name,
            operations = state.operations.toMutableList(),
            id = state.id,
            language = state.language
        )
    }
}