package net.kvibews.service

import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.fasterxml.jackson.databind.ObjectMapper
import net.kvibews.handler.RedisTopicName
import net.kvibews.handler.WsEventName
import net.kvibews.model.OperationWrapper
import net.kvibews.model.TextSelection
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Service

@Service
class EventDispatcherService(
    val socketIOServer: SocketIOServer,
    val redissonClient: RedissonClient,
    val objectMapper: ObjectMapper
) {

    fun dispatchToWSAndRedis(operation: OperationWrapper, socketIOClient: SocketIOClient) {
        socketIOServer.getRoomOperations(operation.docId)
            .sendEvent(WsEventName.OPERATION, socketIOClient, objectMapper.writeValueAsString(operation))
        redissonClient.getTopic(RedisTopicName.DOC_OPERATION_PROCESSED).publish(operation)
    }

    fun dispatch(operation: OperationWrapper) {
        socketIOServer.getRoomOperations(operation.docId)
            .sendEvent(WsEventName.OPERATION, objectMapper.writeValueAsString(operation))
    }

    fun dispatchToWSAndRedis(operations: List<OperationWrapper>, socketIOClient: SocketIOClient) {
        operations.forEach {
            dispatchToWSAndRedis(it, socketIOClient)
        }
    }

    fun dispatchToWSAndRedis(cursorPosition: TextSelection, socketIOClient: SocketIOClient) {
        socketIOServer.getRoomOperations(cursorPosition.docId)
            .sendEvent(WsEventName.SELECTION, objectMapper.writeValueAsString(cursorPosition))
    }
}