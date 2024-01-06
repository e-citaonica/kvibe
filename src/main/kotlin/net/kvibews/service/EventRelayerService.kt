package net.kvibews.service

import com.corundumstudio.socketio.SocketIOServer
import com.fasterxml.jackson.databind.ObjectMapper
import net.kvibews.model.OperationWrapper
import org.springframework.stereotype.Service

@Service
class EventRelayerService(val socketIOServer: SocketIOServer, val objectMapper: ObjectMapper) {

    fun relay(operation: OperationWrapper) {
        socketIOServer.getRoomOperations(operation.docId).sendEvent("operation", objectMapper.writeValueAsString(operation))
    }
}