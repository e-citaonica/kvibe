package net.kvibews.document

import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock
import net.kvibews.exception.InvalidOperationRevision
import net.kvibews.model.DocumentState
import net.kvibews.model.OperationWrapper
import net.kvibews.model.TextOperation
import net.kvibews.model.TextSelection
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

    fun transformAgainstRevisionLogs(operationWrapper: OperationWrapper): List<TextOperation> {

        if (operationWrapper.revision > state.revision) {
            throw InvalidOperationRevision(
                "Attempted to perform operation with greater revision (${operationWrapper.revision}) than current document revision ${state.revision}"
            )
        } else if (operationWrapper.revision == state.revision) {
            return listOf(operationWrapper.operation)
        }

        val transformedOperations: MutableList<TextOperation> = mutableListOf()
        val opQueue: Queue<Pair<TextOperation, Int>> = LinkedList()
        opQueue.add(Pair(operationWrapper.operation, operationWrapper.revision))

        while (!opQueue.isEmpty()) {
            val op = opQueue.poll()
            var transformedOperation: TextOperation? = operationWrapper.operation

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

    fun transformAgainstRevisionLogs(selection: TextSelection): TextSelection {
        if (selection.revision > state.revision) {
            throw InvalidOperationRevision(
                "Attempted to perform operation with greater revision (${selection.revision}) than current document revision ${state.revision}"
            )
        } else if (selection.revision == state.revision) {
            return selection
        }

        var transformedSelection = selection;
        val opQueue: Queue<Pair<TextSelection, Int>> = LinkedList()
        opQueue.add(Pair(selection, selection.revision))

        while (!opQueue.isEmpty()) {
            val op = opQueue.poll()
            transformedSelection = selection

            for (revision in op.second until state.operations.size) {
                transformedSelection = transformations.transform(transformedSelection, state.operations[revision])
                    opQueue.add(Pair(transformedSelection, revision + 1))
            }
        }

        return transformedSelection
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