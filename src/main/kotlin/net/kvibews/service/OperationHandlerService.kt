package net.kvibews.service

import com.corundumstudio.socketio.SocketIOClient
import net.kvibews.config.ApplicationProperties
import net.kvibews.document.SubmitRequest
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


@Service
class OperationHandlerService(
    val documentRepo: DocumentRepository,
    val eventDispatcherService: EventDispatcherService,
    val props: ApplicationProperties,
    val logger: Logger,
    @Qualifier("string") val operationTransformations: OperationTransformations
) {

//    fun joinDocument(documentId: String, user: String) {
//        if (!documents.contains(documentId)) {
//            documentRepo.getDocument(documentId)?.let {
//                val docHolder = SubmitRequest.getInstance(it, operationTransformations)
//                documents[documentId] = docHolder
//                docHolder.lock.writeLock().lock()
//                docHolder.addUser(user)
//                docHolder.lock.writeLock().unlock()
//            }
//        }
//    }

//    fun leaveDocument(documentId: String, user: String) {
//        val document = documents[documentId]!!
//
//        document.lock.writeLock().lock()
//        document.removeUser(user)
//        val snapshot = document.getSnapshot()
//        if (snapshot.activeUsers.isEmpty()) {
//            documents.remove(documentId)
//        }
//        document.lock.writeLock().unlock()
//    }

    fun createDocument(createDocument: DocumentDTO.Create): DocumentState {
        val documentId = UUID.randomUUID().toString()

        val document = DocumentState(documentId, createDocument.name, createDocument.language)
        documentRepo.setDocument(documentId, document)

        return document
    }

    fun getDocument(documentId: String): DocumentState {
        return documentRepo.getDocument(documentId) ?: throw DocumentNotFoundException(documentId)
    }

    fun tryApply(operationWrapper: OperationWrapper, userSession: SocketIOClient): Pair<Int, List<TextOperation>>? {
        var retry = 0
        var request: SubmitRequest
        var success: Boolean

        do {
            retry++
            val snapshot = documentRepo.getDocument(operationWrapper.docId) ?: return null

            request = SubmitRequest(
                operation = operationWrapper,
                snapshot = snapshot,
                opTransformations = operationTransformations
            )

            success = tryApply(request)
        } while (retry < props.operation.maxNumberOfRetries && !success)

        if (success) {
            val ops = request.operations.subList(request.snapshot.revision, request.operations.size)
            ops.forEach {
                eventDispatcherService.dispatchToWSAndRedis(
                    OperationWrapper(
                        operation = it,
                        docId = request.snapshot.id,
                        revision = request.revision,
                        performedBy = operationWrapper.performedBy
                    ), userSession
                )
            }
            return Pair(request.revision, ops)
        }
        return null
    }

    fun tryApply(
        request: SubmitRequest
    ): Boolean {
        val ops = request.transform()

        ops.forEach {
            request.apply(it)
        }

        return documentRepo.compareAndSet(request.snapshot.id, request.snapshot.revision, request.getTransformed())
    }
}