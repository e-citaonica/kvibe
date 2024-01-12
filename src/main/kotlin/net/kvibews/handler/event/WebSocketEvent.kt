package net.kvibews.handler.event

data class WebSocketEvent<T>(val roomId: String, val payload: T)