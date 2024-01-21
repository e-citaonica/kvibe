package net.kvibews.handler

import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.listener.ConnectListener
import com.corundumstudio.socketio.listener.DataListener
import com.corundumstudio.socketio.listener.DisconnectListener
import net.kvibews.dto.OperationDTO
import net.kvibews.model.OperationWrapper
import net.kvibews.model.TextSelection
import net.kvibews.model.UserJoinedPayload
import net.kvibews.service.OperationHandlerService
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
    val operationHandlerService: OperationHandlerService,
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
            operationHandlerService.tryApply(operationWrapper, socketIOClient)?.let {
                ack.sendAckData(OperationDTO.AckMessage(it.first))
            }
        }
    }

    private fun selectionEvent(): DataListener<TextSelection> {
        return DataListener { socketIOClient, selection, _ ->
            val transformedSelection = operationHandlerService.transform(selection)
            transformedSelection?.let {
                eventDispatcherService.dispatch(
                    TextSelection(
                        transformedSelection.docId,
                        transformedSelection.revision,
                        transformedSelection.from,
                        transformedSelection.to,
                        transformedSelection.performedBy
                    ), socketIOClient
                )
            }
        }
    }


    private fun onConnected(): ConnectListener {
        return ConnectListener { client: SocketIOClient ->
            val docId = client.handshakeData.getSingleUrlParam("docId")
            docId?.let {
//                operationHandlerService.joinDocument(it, client.sessionId.toString())
                client.joinRoom(it)
            }
        }
    }

    private fun onDisconnected(): DisconnectListener {
        return DisconnectListener { client: SocketIOClient ->
            val docId = client.handshakeData.getSingleUrlParam("docId")
            docId?.let {
//                operationHandlerService.leaveDocument(docId, client.sessionId.toString())
                eventDispatcherService.dispatch(
                    it,
                    WsEventName.USER_LEFT_DOC,
                    RedisTopicName.DOC_USER_LEFT,
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
                eventDispatcherService.dispatch(
                    it,
                    WsEventName.USER_JOINED_DOC,
                    RedisTopicName.DOC_USER_LEFT,
                    payload,
                    client
                )
            }
        }
    }
}

