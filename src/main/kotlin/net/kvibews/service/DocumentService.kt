package net.kvibews.service

import net.kvibews.formatter.DocumentFormatter
import net.kvibews.model.Document
import net.kvibews.model.OperationWrapper
import net.kvibews.model.TextOperation
import net.kvibews.model.dto.DocumentCreationModel
import net.kvibews.operation_transformations.impl.CharSequenceOperationTransformations
import net.kvibews.repository.DocumentRedisRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*


@Service
class DocumentService(
    val documentRedisRepository: DocumentRedisRepository,
    val eventRelayerService: EventRelayerService,
    val charSequenceOperationTransformations: CharSequenceOperationTransformations
) {

    fun createDocument(documentCreationModel: DocumentCreationModel): Document {
        val randomUUID = UUID.randomUUID().toString()
        val document = Document(randomUUID, 0, "", emptyList())
        documentRedisRepository.setDocument(randomUUID, document)
        return document
    }

    fun getDocument(documentId: String): Document? {
        return documentRedisRepository.getDocument(documentId)
    }
    @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    fun performOperation(operationWrapper: OperationWrapper) {
        var document = requireNotNull(documentRedisRepository.getDocument(operationWrapper.docId))

        val currentRevision = document.revision
        val eventRevision = operationWrapper.revision

        if (eventRevision < currentRevision) {
            val transformedOperation = transformOperationAgainstRevisionLogs(document, operationWrapper)

            transformedOperation.forEach {
                eventRelayerService.relay(
                    OperationWrapper(operationWrapper.docId,
                        currentRevision + 1,
                        operationWrapper.performedBy,
                        it))
                document = applyTransformation(document, it)
            }
        } else if (eventRevision == currentRevision){
            eventRelayerService.relay(
                OperationWrapper(
                    operationWrapper.docId,
                    currentRevision + 1,
                    operationWrapper.performedBy,
                    operationWrapper.operation
                )
            )
            document = applyTransformation(document, operationWrapper.operation)
        }

        documentRedisRepository.setDocument(document.docId, document)
    }

    fun applyTransformation(doc: Document, operation: TextOperation): Document {
        val toMutableList = doc.operations.toMutableList()
        toMutableList.add(operation)
        val updateContent = updateContent(doc.content, operation)
        return Document(doc.docId, doc.revision + 1, updateContent, toMutableList)
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

            for (revision in op.second until doc.operations.size)  {
                val operations = charSequenceOperationTransformations.transform(transformedOperation!!, doc.operations[revision])
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