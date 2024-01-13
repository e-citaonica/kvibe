package net.kvibews.model

data class Document (
    val id: String,
    val name: String,
    val revision: Int,
    val content: String,
    val operations: List<TextOperation>,
    val activeUsers: List<String>
)