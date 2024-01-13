package net.kvibews.handler

import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.listener.ConnectListener
import com.corundumstudio.socketio.listener.DataListener
import com.corundumstudio.socketio.listener.DisconnectListener
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.kvibews.dto.OperationAckMessage
import net.kvibews.enum.OperationType
import net.kvibews.handler.event.Selection
import net.kvibews.model.OperationWrapper
import net.kvibews.model.TextOperation
import net.kvibews.service.DocumentService
import net.kvibews.service.EventRelayService
import org.springframework.stereotype.Component

object EventName {
    const val OPERATION = "operation"
    const val SELECTION = "selection"
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
        socketIOServer.addEventListener(EventName.SELECTION, String::class.java, selectionEvent())
    }

    private fun operationEvent(): DataListener<OperationWrapper> {
        return DataListener { socketIOClient, operationWrapper, ack ->
            val (revision, transformedOps) = documentService.performOperation(operationWrapper, socketIOClient)
            ack.sendAckData(OperationAckMessage(revision))
        }
    }

    private fun selectionEvent(): DataListener<String> {
        return DataListener { socketIOClient, cursorPosition, _ ->
            val selection = objectMapper.readValue<Selection>(cursorPosition)

            val (revision, transformedOps) = documentService.performOperation(
                OperationWrapper(
                    selection.docId, 0, selection.performedBy,
                    TextOperation(OperationType.DELETE, "", selection.from, selection.to - selection.from + 1)
                ), socketIOClient
            )

            // TODO: return transformed cursor pos
            eventRelayService.relay(
                Selection(
                    selection.docId,
                    transformedOps[0].position,
                    transformedOps[0].position + transformedOps[0].length,
                    selection.performedBy
                ), socketIOClient
            )
        }
    }


    private fun onConnected(): ConnectListener {
        return ConnectListener { client: SocketIOClient ->
            val document = client.handshakeData.getSingleUrlParam("docId")
//            documentService.subscribeToDocument()
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

