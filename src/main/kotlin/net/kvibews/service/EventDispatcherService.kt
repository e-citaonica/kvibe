package net.kvibews.service

import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.fasterxml.jackson.databind.ObjectMapper
import net.kvibews.handler.RedisTopicName
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Service

@Service
class EventDispatcherService(
    val socketIOServer: SocketIOServer,
    val redissonClient: RedissonClient,
    val objectMapper: ObjectMapper
) {
    fun <T> dispatchToRoom(roomId: String, event: String, payload: T, socketIOClient: SocketIOClient) {
        socketIOServer.getRoomOperations(roomId)
            .sendEvent(event, socketIOClient, objectMapper.writeValueAsString(payload))
        redissonClient.getTopic(RedisTopicName.DOC_OPERATION_PROCESSED).publish(payload)
    }
}