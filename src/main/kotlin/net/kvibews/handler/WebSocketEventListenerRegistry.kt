package net.kvibews.handler

import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.listener.ConnectListener
import com.corundumstudio.socketio.listener.DataListener
import com.corundumstudio.socketio.listener.DisconnectListener
import com.fasterxml.jackson.databind.ObjectMapper
import net.kvibews.dto.OperationDTO
import net.kvibews.enum.OperationType
import net.kvibews.model.OperationWrapper
import net.kvibews.model.TextOperation
import net.kvibews.model.TextSelection
import net.kvibews.model.UserJoinedPayload
import net.kvibews.service.DocumentOperationHandlerService
import net.kvibews.service.EventDispatcherService
import org.springframework.stereotype.Component

object WsEventName {
    const val USER_JOINED_DOC = "user_joined_doc"
    const val USER_LEFT_DOC = "user_left_doc"
    const val OPERATION = "operation"
    const val SELECTION = "selection"
}

@Component
class WebSocketHandler(
    socketIOServer: SocketIOServer,
    val documentOperationHandlerService: DocumentOperationHandlerService,
    val eventDispatcherService: EventDispatcherService
) {
    init {
        socketIOServer.addConnectListener(onConnected())
        socketIOServer.addDisconnectListener(onDisconnected())
        socketIOServer.addEventListener(WsEventName.OPERATION, OperationWrapper::class.java, operationEvent())
        socketIOServer.addEventListener(WsEventName.SELECTION, TextSelection::class.java, selectionEvent())
        socketIOServer.addEventListener(WsEventName.USER_JOINED_DOC, String::class.java, userJoinedDocEvent())
    }

    private fun operationEvent(): DataListener<OperationWrapper> {
        return DataListener { socketIOClient, operationWrapper, ack ->
            val (revision, _) = documentOperationHandlerService.transformAndApply(operationWrapper, socketIOClient)
            ack.sendAckData(OperationDTO.AckMessage(revision))
        }
    }

    private fun selectionEvent(): DataListener<TextSelection> {
        return DataListener { socketIOClient, selection, _ ->

            val transformedSelection = documentOperationHandlerService.transformSelection(
                selection, socketIOClient
            )

            eventDispatcherService.dispatchToRoom(
                transformedSelection.docId,
                WsEventName.SELECTION, TextSelection(
                    transformedSelection.docId,
                    transformedSelection.revision,
                    transformedSelection.from,
                    transformedSelection.to,
                    selection.performedBy
                ), socketIOClient
            )
        }
    }


    private fun onConnected(): ConnectListener {
        return ConnectListener { client: SocketIOClient ->
            val docId = client.handshakeData.getSingleUrlParam("docId")
            docId?.let {
                documentOperationHandlerService.joinDocument(it, client.sessionId.toString())
                client.joinRoom(it)
            }
        }
    }

    private fun onDisconnected(): DisconnectListener {
        return DisconnectListener { client: SocketIOClient ->
            val docId = client.handshakeData.getSingleUrlParam("docId")
            docId?.let {
                documentOperationHandlerService.leaveDocument(docId, client.sessionId.toString())

                eventDispatcherService.dispatchToRoom(
                    it,
                    WsEventName.USER_LEFT_DOC,
                    client.sessionId.toString(),
                    client
                )
            }
        }
    }

    private fun userJoinedDocEvent(): DataListener<String> {
        return DataListener { client, username: String, _ ->
            val docId = client.handshakeData.getSingleUrlParam("docId")

            val payload = UserJoinedPayload(client.sessionId.toString(), username)

            println("${client.sessionId} $docId $payload")
            docId?.let {
                eventDispatcherService.dispatchToRoom(
                    it,
                    WsEventName.USER_JOINED_DOC,
                    payload,
                    client
                )
            }
        }
    }
}

