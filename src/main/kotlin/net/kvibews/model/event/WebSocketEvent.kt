package net.kvibews.model.event

import net.kvibews.model.TextOperation

data class WebSocketEvent(val roomId: String, val payload: TextOperation)