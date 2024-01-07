package net.kvibews.service

import com.corundumstudio.socketio.SocketIOServer
import com.fasterxml.jackson.databind.ObjectMapper
import net.kvibews.handler.EventName
import net.kvibews.handler.event.CursorPosition
import net.kvibews.model.OperationWrapper
import org.springframework.stereotype.Service

@Service
class EventRelayService(val socketIOServer: SocketIOServer, val objectMapper: ObjectMapper) {

    fun relay(operation: OperationWrapper) {
        socketIOServer.getRoomOperations(operation.docId).sendEvent(EventName.OPERATION, objectMapper.writeValueAsString(operation))
    }

    fun relay(cursorPosition: CursorPosition) {
        socketIOServer.getRoomOperations(cursorPosition.roomId).sendEvent(EventName.CURSOR_POSITION, objectMapper.writeValueAsString(cursorPosition))
    }
}