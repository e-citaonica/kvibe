package net.kvibews.service

import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.fasterxml.jackson.databind.ObjectMapper
import net.kvibews.handler.RedisMessage
import net.kvibews.handler.RedisTopicName
import net.kvibews.handler.WsEventName
import net.kvibews.model.DocumentState
import net.kvibews.model.OperationWrapper
import net.kvibews.model.TextSelection
import net.kvibews.provider.RedisOriginProvider
import org.redisson.api.RTopic
import org.redisson.api.RedissonClient
import org.redisson.api.listener.MessageListener
import org.redisson.codec.JacksonCodec
import org.redisson.codec.JsonJacksonCodec
import org.redisson.codec.TypedJsonJacksonCodec
import org.springframework.stereotype.Service

@Service
class EventDispatcherService(
    val socketIOServer: SocketIOServer,
    val redissonClient: RedissonClient,
    val mapper: ObjectMapper,
    val origin: RedisOriginProvider
) {

    fun <T> dispatch(roomId: String, wsEvent: String, redisTopicName: String, payload: T, exclude: SocketIOClient) {
        socketIOServer.getRoomOperations(roomId).sendEvent(wsEvent, exclude, payload)
        redissonClient.getTopic(redisTopicName, JsonJacksonCodec(mapper))
            .publishAsync(RedisMessage(origin.value, payload))
    }

    fun dispatch(operation: OperationWrapper, exclude: SocketIOClient) {
        dispatch(operation.docId, WsEventName.OPERATION, RedisTopicName.DOC_OPERATION_PROCESSED, operation, exclude)
    }

    fun dispatch(textSelection: TextSelection, exclude: SocketIOClient) {
        dispatch(textSelection.docId, WsEventName.SELECTION, RedisTopicName.DOC_SELECTION, textSelection, exclude)
    }

    fun <T> dispatchToWsRoom(roomId: String, event: String, payload: T) {
        socketIOServer.getRoomOperations(roomId).sendEvent(event, payload)
    }

    fun dispatchToWSRoom(operation: OperationWrapper) {
        socketIOServer.getRoomOperations(operation.docId).sendEvent(WsEventName.OPERATION, operation)
    }

}
