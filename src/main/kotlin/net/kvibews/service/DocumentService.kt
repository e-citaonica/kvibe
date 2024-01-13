package net.kvibews.service

import com.corundumstudio.socketio.SocketIOClient
import net.kvibews.formatter.DocumentFormatter
import net.kvibews.model.Document
import net.kvibews.model.OperationWrapper
import net.kvibews.model.TextOperation
import net.kvibews.dto.DocumentCreationModel
import net.kvibews.exception.DocumentNotFoundException
import net.kvibews.operation_transformations.OperationTransformations
import net.kvibews.repository.DocumentRedisRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*


@Service
class DocumentService(
    val documentRedisRepository: DocumentRedisRepository,
    val eventRelayService: EventRelayService,
    @Qualifier("string") val operationTransformations: OperationTransformations
) {

    fun createDocument(documentCreationModel: DocumentCreationModel): Document {
        val randomUUID = UUID.randomUUID().toString()
        val document = Document(randomUUID, documentCreationModel.name, 0, "", emptyList(), emptyList())
        documentRedisRepository.setDocument(randomUUID, document)
        return document
    }

    fun getDocument(documentId: String): Document {
        val document = documentRedisRepository.getDocument(documentId)
        return document ?: throw DocumentNotFoundException(documentId)
    }

    @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    fun performOperation(operationWrapper: OperationWrapper, socketIOClient: SocketIOClient): Pair<Int, List<TextOperation>> {
        var document = requireNotNull(documentRedisRepository.getDocument(operationWrapper.docId))

        val currentRevision = document.revision
        val eventRevision = operationWrapper.revision

        var transformedOperations = listOf<TextOperation>();

        if (eventRevision < currentRevision) {
            transformedOperations = transformOperationAgainstRevisionLogs(document, operationWrapper)

            transformedOperations.forEach {
                eventRelayService.relay(
                    OperationWrapper(
                        operationWrapper.docId,
                        currentRevision + 1,
                        operationWrapper.performedBy,
                        it
                    ), socketIOClient
                )
                document = applyTransformation(document, it)
            }
        } else if (eventRevision == currentRevision) {
            eventRelayService.relay(
                OperationWrapper(
                    operationWrapper.docId,
                    currentRevision + 1,
                    operationWrapper.performedBy,
                    operationWrapper.operation
                ),
                socketIOClient
            )
            document = applyTransformation(document, operationWrapper.operation)
        }
        documentRedisRepository.setDocument(document.id, document)

        return Pair(document.revision, transformedOperations)
    }

    fun applyTransformation(doc: Document, operation: TextOperation): Document {
        val toMutableList = doc.operations.toMutableList()
        toMutableList.add(operation)
        val updateContent = updateContent(doc.content, operation)
        return Document(doc.id, doc.name, doc.revision + 1, updateContent, toMutableList, emptyList())
    }

    fun updateContent(content: String, operation: TextOperation): String {
        val documentFormatter = DocumentFormatter()
        documentFormatter.reset(content)
        documentFormatter.applyOperation(operation)

        return documentFormatter.getText()
    }

    fun transformOperationAgainstRevisionLogs(doc: Document, operation: OperationWrapper): List<TextOperation> {

        val transformedOperations: MutableList<TextOperation> = mutableListOf()
        val opQueue: Queue<Pair<TextOperation, Int>> = LinkedList()
        opQueue.add(Pair(operation.operation, operation.revision))

        while (!opQueue.isEmpty()) {
            val op = opQueue.poll()
            var transformedOperation: TextOperation? = operation.operation

            for (revision in op.second until doc.operations.size) {
                val operations = operationTransformations.transform(transformedOperation!!, doc.operations[revision])
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

}