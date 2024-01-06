package net.kvibews.handler

import com.corundumstudio.socketio.AckRequest
import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.listener.ConnectListener
import com.corundumstudio.socketio.listener.DataListener
import com.corundumstudio.socketio.listener.DisconnectListener
import com.fasterxml.jackson.databind.ObjectMapper
import net.kvibews.model.OperationWrapper
import net.kvibews.service.DocumentService
import org.slf4j.Logger
import org.springframework.stereotype.Component


@Component
class WebSocketHandler(
    socketIOServer: SocketIOServer,
    val documentService: DocumentService,
    val objectMapper: ObjectMapper,
    val logger: Logger
) {
    init {
        socketIOServer.addConnectListener(onConnected())
        socketIOServer.addDisconnectListener(onDisconnected())
        socketIOServer.addEventListener("operation", String::class.java, operationEvent())
        socketIOServer.addEventListener("cursor_position", String::class.java, cursorPositionEvent())
    }

    private fun operationEvent(): DataListener<String> {
        return DataListener { senderClient: SocketIOClient, data: String, _: AckRequest? ->
            logger.info("operation_performed")
            documentService.performOperation(objectMapper.readValue(data, OperationWrapper::class.java))
        }
    }

    private fun cursorPositionEvent(): DataListener<String> {
        return DataListener { senderClient: SocketIOClient?, data: String, a: AckRequest? ->
            logger.info("cursor_position")
//            senderClient.get
//            socketService.sendMessage(data.getRoom(), "get_message", senderClient, data.getMessage())
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