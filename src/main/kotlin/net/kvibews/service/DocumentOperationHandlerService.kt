package net.kvibews.service

import com.corundumstudio.socketio.SocketIOClient
import net.kvibews.document.DocumentHolder
import net.kvibews.dto.DocumentDTO
import net.kvibews.exception.DocumentNotFoundException
import net.kvibews.model.DocumentState
import net.kvibews.model.OperationWrapper
import net.kvibews.model.TextOperation
import net.kvibews.operation_transformations.OperationTransformations
import net.kvibews.repository.DocumentRepository
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap


@Service
class DocumentOperationHandlerService(
    val documentRepo: DocumentRepository,
    val eventDispatcherService: EventDispatcherService,
    val logger: Logger,
    @Qualifier("string") val operationTransformations: OperationTransformations
) {
    var documents = ConcurrentHashMap<String, DocumentHolder>()

    fun joinDocument(documentId: String, user: String) {
        if (!documents.contains(documentId)) {
            documentRepo.getDocument(documentId)?.let {
                val docHolder = DocumentHolder.getInstance(it, operationTransformations)
                documents[documentId] = docHolder
                docHolder.lock.writeLock().lock()
                docHolder.addUser(user)
                docHolder.lock.writeLock().unlock()
            }
        }
    }

    fun leaveDocument(documentId: String, user: String) {
        val document = documents[documentId]!!

        document.lock.writeLock().lock()
        document.removeUser(user)
        val snapshot = document.getSnapshot()
        if (snapshot.activeUsers.isEmpty()) {
            documents.remove(documentId)
        }
        document.lock.writeLock().unlock()
    }

    fun createDocument(createDocument: DocumentDTO.Create): DocumentState {
        val documentId = UUID.randomUUID().toString()

        val document = DocumentState(documentId, createDocument.name, createDocument.language)
        documentRepo.setDocument(documentId, document)
        documents[documentId] = DocumentHolder.getInstance(document, operationTransformations)

        return document
    }

    fun getDocument(documentId: String): DocumentState {
        val document = documents[documentId]?.getSnapshot() ?: documentRepo.getDocument(documentId)
        return document ?: throw DocumentNotFoundException(documentId)
    }

    fun transformAndApply(
        operationWrapper: OperationWrapper,
        userSession: SocketIOClient
    ): Pair<Int, List<TextOperation>> {
        val document = documents[operationWrapper.docId]!!

        document.lock.updateLock().lock()

        //------
        document.lock.writeLock().lock()
        val transformedOperations = document.transformAgainstRevisionLogs(operationWrapper)

        transformedOperations.forEach {
            document.apply(it)
        }

        document.lock.writeLock().unlock()
        //------

        val snapshot = document.getSnapshot()
        documentRepo.setDocumentAsync(snapshot.id, snapshot)

        transformedOperations.forEach {
            eventDispatcherService.dispatch(
                OperationWrapper(
                    snapshot.id,
                    snapshot.revision,
                    operationWrapper.performedBy,
                    it
                ), userSession
            )
        }

        document.lock.updateLock().unlock()

        return Pair(snapshot.revision, transformedOperations)
    }

    fun isDocumentLocal(documentId: String): Boolean {
        return documents.contains(documentId)
    }

    fun getDocumentIfLocal(documentId: String): DocumentState? {
        return documents[documentId]?.getSnapshot()
    }

}