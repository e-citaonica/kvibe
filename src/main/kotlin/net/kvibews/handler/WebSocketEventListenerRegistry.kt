package net.kvibews.handler

import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.listener.ConnectListener
import com.corundumstudio.socketio.listener.DataListener
import com.corundumstudio.socketio.listener.DisconnectListener
import net.kvibews.dto.DocumentDTO
import net.kvibews.dto.OperationDTO
import net.kvibews.model.OperationWrapper
import net.kvibews.model.TextSelection
import net.kvibews.service.DocumentService
import net.kvibews.service.EventDispatcherService
import org.slf4j.Logger
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
    val documentService: DocumentService,
    val eventDispatcherService: EventDispatcherService,
    val logger: Logger
) {
    companion object {
        const val Q_USERNAME = "username"
        const val Q_DOC_ID = "docId"
    }

    init {
        socketIOServer.addConnectListener(onConnected())
        socketIOServer.addDisconnectListener(onDisconnected())
        socketIOServer.addEventListener(WsEventName.OPERATION, OperationWrapper::class.java, operationEvent())
        socketIOServer.addEventListener(WsEventName.SELECTION, TextSelection::class.java, selectionEvent())
    }

    private fun operationEvent(): DataListener<OperationWrapper> {
        return DataListener { socketIOClient, operationWrapper, ack ->
            documentService.submit(operationWrapper, socketIOClient)?.let {
                ack.sendAckData(OperationDTO.AckMessage(it.first))
            }
        }
    }

    private fun selectionEvent(): DataListener<TextSelection> {
        return DataListener { socketIOClient, selection, _ ->
            val transformedSelection = documentService.transform(selection)
            transformedSelection?.let {
                eventDispatcherService.dispatch(
                    it, socketIOClient
                )
            }
        }
    }


    private fun onConnected(): ConnectListener {
        return ConnectListener { client: SocketIOClient ->
            val docId = client.handshakeData.getSingleUrlParam(Q_DOC_ID)
            val username = client.handshakeData.getSingleUrlParam(Q_USERNAME)
            logger.info("User {} ({}) joined the doc {}", client, username, docId)

            docId?.let {
                client.joinRoom(it)
                documentService.joinDocument(it, client.sessionId, username)
                eventDispatcherService.dispatch(
                    it,
                    WsEventName.USER_JOINED_DOC,
                    RedisTopicName.DOC_USER_JOINED,
                    DocumentDTO.UserInfo(docId, client.sessionId.toString(), username),
                    client
                )
            }
        }
    }

    private fun onDisconnected(): DisconnectListener {
        return DisconnectListener { client: SocketIOClient ->
            val docId = client.handshakeData.getSingleUrlParam(Q_DOC_ID)
            logger.info("User {} left the doc {}", client, docId)

            docId?.let {
                eventDispatcherService.dispatch(
                    it,
                    WsEventName.USER_LEFT_DOC,
                    RedisTopicName.DOC_USER_LEFT,
                    DocumentDTO.UserInfo(
                        docId,
                        client.sessionId.toString(),
                        documentService.getActiveUsername(docId, client.sessionId) ?: ""
                    ),
                    client
                )
                documentService.leaveDocument(docId, client.sessionId)
            }
        }
    }
}

