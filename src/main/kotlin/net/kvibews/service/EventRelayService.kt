package net.kvibews.service

import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.fasterxml.jackson.databind.ObjectMapper
import net.kvibews.handler.EventName
import net.kvibews.handler.event.Selection
import net.kvibews.model.OperationWrapper
import org.springframework.stereotype.Service

@Service
class EventRelayService(val socketIOServer: SocketIOServer, val objectMapper: ObjectMapper) {

    fun relay(operation: OperationWrapper, socketIOClient: SocketIOClient) {
        socketIOServer.getRoomOperations(operation.docId)
            .sendEvent(EventName.OPERATION, socketIOClient, objectMapper.writeValueAsString(operation))
    }

    fun relay(cursorPosition: Selection, socketIOClient: SocketIOClient) {
        socketIOServer.getRoomOperations(cursorPosition.docId).sendEvent(EventName.SELECTION, objectMapper.writeValueAsString(cursorPosition))
    }
}