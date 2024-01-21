package net.kvibews.handler

import net.kvibews.model.OperationWrapper
import net.kvibews.model.TextSelection
import net.kvibews.service.EventDispatcherService
import org.redisson.api.RedissonClient
import org.redisson.api.listener.MessageListener
import org.springframework.stereotype.Component

object RedisTopicName {
    const val DOC_OPERATION_PROCESSED = "document:operation:processed"
    const val DOC_SELECTION = "document:selection"
    const val DOC_USER_LEFT = "document:user:left"
    const val DOC_USER_JOINED = "document:user:joined"
}

@Component
class RedisTopicListenerRegistry(
    redissonClient: RedissonClient,
    val eventDispatcherService: EventDispatcherService
) {

    init {
        redissonClient.getTopic(RedisTopicName.DOC_OPERATION_PROCESSED)
            .addListener(OperationWrapper::class.java, onOperation())
    }

    private fun onOperation(): MessageListener<OperationWrapper> {
        return MessageListener { _, operationWrapper ->
            eventDispatcherService.dispatchToWSRoom(operationWrapper)
        }
    }

    private fun onSelection(): MessageListener<TextSelection> {
        return MessageListener { _, selection ->
            eventDispatcherService.dispatchToWSRoom(selection)
        }
    }

}