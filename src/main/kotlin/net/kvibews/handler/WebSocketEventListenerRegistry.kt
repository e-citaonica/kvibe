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
import net.kvibews.service.OperationHandlerService
import net.kvibews.service.EventDispatcherService
import org.springframework.stereotype.Component

object WsEventName {
    const val OPERATION = "operation"
    const val SELECTION = "selection"
}

@Component
class WebSocketHandler(
    socketIOServer: SocketIOServer,
    val operationHandlerService: OperationHandlerService,
    val eventDispatcherService: EventDispatcherService,
    val objectMapper: ObjectMapper
) {

    init {
        socketIOServer.addConnectListener(onConnected())
        socketIOServer.addDisconnectListener(onDisconnected())
        socketIOServer.addEventListener(WsEventName.OPERATION, OperationWrapper::class.java, operationEvent())
//        socketIOServer.addEventListener(WsEventName.SELECTION, TextSelection::class.java, selectionEvent())
    }

    private fun operationEvent(): DataListener<OperationWrapper> {
        return DataListener { socketIOClient, operationWrapper, ack ->
            val (revision, _) = operationHandlerService.transformAndApply(operationWrapper, socketIOClient)
            ack.sendAckData(OperationDTO.AckMessage(revision))
        }
    }

    private fun selectionEvent(): DataListener<TextSelection> {
        return DataListener { socketIOClient, selection, _ ->

            val (_, transformedOps) = operationHandlerService.transformAndApply(
                OperationWrapper(
                    selection.docId, 0, selection.performedBy,
                    TextOperation(OperationType.DELETE, "", selection.from, selection.to - selection.from + 1)
                ), socketIOClient
            )

            eventDispatcherService.dispatch(
                TextSelection(
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
            val user = client.handshakeData.getSingleUrlParam("user")
            operationHandlerService.joinDocument(document, user, client)
            client.joinRoom(document)
        }
    }

    private fun onDisconnected(): DisconnectListener {
        return DisconnectListener { client: SocketIOClient ->
            val document = client.handshakeData.getSingleUrlParam("docId")
            val user = client.handshakeData.getSingleUrlParam("user")
            operationHandlerService.leaveDocument(document, user)
        }
    }

}

