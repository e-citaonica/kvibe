package net.kvibews.handler

import net.kvibews.model.OperationWrapper
import net.kvibews.service.EventDispatcherService
import net.kvibews.service.OperationHandlerService
import org.redisson.api.RedissonClient
import org.redisson.api.listener.MessageListener
import org.springframework.stereotype.Component

object RedisTopicName {
    const val DOC_OPERATION_REQUESTED = "document:operation:requested"
    const val DOC_OPERATION_PROCESSED = "document:operation:processed"
    const val DOC_USER_JOINED = "document:user:joined"
    const val DOC_USER_LEFT = "document:user:left"
}

@Component
class RedisTopicListenerRegistry(
    redissonClient: RedissonClient,
    val documentHandler: OperationHandlerService,
    val eventDispatcherService: EventDispatcherService
) {

    init {
        redissonClient.getTopic(RedisTopicName.DOC_OPERATION_REQUESTED)
            .addListener(OperationWrapper::class.java, operationRequested())
    }

    private fun operationRequested(): MessageListener<OperationWrapper> {
        return MessageListener { _, operation ->
            if (documentHandler.isDocumentLocal(operation.docId)) {
                documentHandler.transformAndApply(operation)
            }
        }
    }

//    private fun operationProcessed(): MessageListener<OperationWrapper> {
//        return MessageListener { _, operation ->
//            eventDispatcherService.notifyConnectedUsers(operation)
//        }
//    }
}