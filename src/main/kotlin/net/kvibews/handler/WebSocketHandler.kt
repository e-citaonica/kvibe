package net.kvibews.handler

import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.listener.ConnectListener
import com.corundumstudio.socketio.listener.DataListener
import com.corundumstudio.socketio.listener.DisconnectListener
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.kvibews.dto.OperationAckMessage
import net.kvibews.handler.event.CursorPosition
import net.kvibews.model.OperationWrapper
import net.kvibews.service.DocumentService
import net.kvibews.service.EventRelayService
import org.springframework.stereotype.Component

object EventName {
    const val OPERATION = "operation"
    const val CURSOR_POSITION = "cursor_position"
}

@Component
class WebSocketHandler(
    socketIOServer: SocketIOServer,
    val documentService: DocumentService,
    val eventRelayService: EventRelayService,
    val objectMapper: ObjectMapper
) {

    init {
        socketIOServer.addConnectListener(onConnected())
        socketIOServer.addDisconnectListener(onDisconnected())
        socketIOServer.addEventListener(EventName.OPERATION, OperationWrapper::class.java, operationEvent())
        socketIOServer.addEventListener(EventName.CURSOR_POSITION, String::class.java, cursorPositionEvent())
    }

    private fun operationEvent(): DataListener<OperationWrapper> {
        return DataListener { _, data, ackData ->
            val performOperation = documentService.performOperation(data)
            ackData.sendAckData(OperationAckMessage(revision = performOperation))
        }
    }

    private fun cursorPositionEvent(): DataListener<String> {
        return DataListener { _, data, _ ->
            eventRelayService.relay(objectMapper.readValue<CursorPosition>(data))
        }
    }


    private fun onConnected(): ConnectListener {
        return ConnectListener { client: SocketIOClient ->
            val document = client.handshakeData.getSingleUrlParam("docId")
            document?.let {
                client.joinRoom(it)
            }
        }
    }

    private fun onDisconnected(): DisconnectListener? {
        return DisconnectListener { client: SocketIOClient ->
//            log.info(
//                "Client[{}] - Disconnected from socket",
//                client.sessionId.toString()
//            )
        }
    }

}

