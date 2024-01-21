package net.kvibews.service

import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import net.kvibews.handler.RedisTopicName
import net.kvibews.handler.WsEventName
import net.kvibews.model.OperationWrapper
import net.kvibews.model.TextSelection
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Service

@Service
class EventDispatcherService(
    val socketIOServer: SocketIOServer,
    val redissonClient: RedissonClient
) {
    fun <T> dispatch(roomId: String, wsEvent: String, redisTopicName: String, payload: T, exclude: SocketIOClient) {
        socketIOServer.getRoomOperations(roomId).sendEvent(wsEvent, exclude, payload)
        redissonClient.getTopic(redisTopicName).publishAsync(payload)
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
