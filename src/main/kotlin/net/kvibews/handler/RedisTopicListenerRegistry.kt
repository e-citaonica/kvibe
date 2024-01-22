package net.kvibews.handler

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import net.kvibews.dto.DocumentDTO
import net.kvibews.model.OperationWrapper
import net.kvibews.model.TextSelection
import net.kvibews.provider.RedisOriginProvider
import net.kvibews.service.EventDispatcherService
import org.redisson.api.RedissonClient
import org.redisson.api.listener.MessageListener
import org.redisson.codec.TypedJsonJacksonCodec
import org.springframework.stereotype.Component

data class RedisMessage<T>(
        @JsonProperty("origin") val origin: String,
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE) @JsonProperty("payload") val payload: T
)

object RedisTopicName {
    const val DOC_OPERATION_PROCESSED = "document:operation:processed"
    const val DOC_SELECTION = "document:selection"
    const val DOC_USER_LEFT = "document:user:left"
    const val DOC_USER_JOINED = "document:user:joined"
}

@Component
class RedisTopicListenerRegistry(
        redissonClient: RedissonClient,
        origin: RedisOriginProvider,
        val mapper: ObjectMapper,
        val eventDispatcherService: EventDispatcherService,
) {

    init {
        redissonClient.addListener(RedisTopicName.DOC_OPERATION_PROCESSED, origin.value, onOperation())
        redissonClient.addListener(RedisTopicName.DOC_SELECTION, origin.value, onSelection())
        redissonClient.addListener(RedisTopicName.DOC_USER_JOINED, origin.value, onUserJoined())
        redissonClient.addListener(RedisTopicName.DOC_USER_LEFT, origin.value, onUserLeft())
    }

    private fun onOperation(): MessageListener<OperationWrapper> {
        return MessageListener { _, payload ->
            eventDispatcherService.dispatchToWSRoom(payload)
        }
    }

    private fun onSelection(): MessageListener<TextSelection> {
        return MessageListener { _, payload ->
            eventDispatcherService.dispatchToWsRoom(payload.docId, WsEventName.SELECTION, payload)
        }
    }

    private fun onUserJoined(): MessageListener<DocumentDTO.UserInfo> {
        return MessageListener { _, payload ->
            eventDispatcherService.dispatchToWsRoom(payload.docId, WsEventName.USER_JOINED_DOC, payload)
        }
    }

    private fun onUserLeft(): MessageListener<DocumentDTO.UserInfo> {
        return MessageListener { _, payload ->
            eventDispatcherService.dispatchToWsRoom(payload.docId, WsEventName.USER_LEFT_DOC, payload)
        }
    }

    private inline fun <reified T, reified M : RedisMessage<T>> RedissonClient.addListener(topicName: String, listener: MessageListener<M>) {
        this.getTopic(topicName, TypedJsonJacksonCodec(object : TypeReference<M>() {}, mapper)).addListener(M::class.java, listener)
    }

    private inline fun <reified T, reified M : RedisMessage<T>> RedissonClient.addListener(topicName: String, excludeOrigin: String, listener: MessageListener<T>) {
        this.getTopic(topicName, TypedJsonJacksonCodec(object : TypeReference<M>() {}, mapper)).addListener(M::class.java) { _, message ->
            val (origin, payload) = message
            if (origin != excludeOrigin) {
                listener.onMessage(topicName, payload)
            }
        }
    }

}
