package net.kvibews.handler.event

data class Selection(val docId: String, val from: Int, val to: Int, val performedBy: String)
data class WebSocketEvent<T>(val roomId: String, val payload: T)