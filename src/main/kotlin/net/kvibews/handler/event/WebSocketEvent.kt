package net.kvibews.handler.event

data class CursorPosition(val roomId: String, val rowPos: Int, val linePos: Int, val userId: String)
data class WebSocketEvent<T>(val roomId: String, val payload: T)