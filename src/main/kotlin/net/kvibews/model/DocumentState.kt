package net.kvibews.model

class DocumentState (
    val id: String,
    val name: String,
    val language: String,
    var revision: Int = 0,
    var content: String = "",
    val operations: List<TextOperation> = mutableListOf()
)