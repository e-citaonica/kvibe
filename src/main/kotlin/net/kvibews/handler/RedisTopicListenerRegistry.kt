package net.kvibews.handler

import net.kvibews.model.OperationWrapper
import net.kvibews.service.EventDispatcherService
import net.kvibews.service.OperationHandlerService
import org.redisson.api.RedissonClient
import org.redisson.api.listener.MessageListener
import org.springframework.stereotype.Component

object RedisTopicName {
    const val DOC_OPERATION_PROCESSED = "document:operation:processed"
}

@Component
class RedisTopicListenerRegistry(
    redissonClient: RedissonClient,
    val eventDispatcherService: EventDispatcherService
) {

    init {
        redissonClient.getTopic(RedisTopicName.DOC_OPERATION_PROCESSED)
            .addListener(OperationWrapper::class.java, operationProcessed())
    }

    private fun operationProcessed(): MessageListener<OperationWrapper> {
        return MessageListener { _, operationWrapper ->
            eventDispatcherService.dispatch(operationWrapper)
        }
    }

}