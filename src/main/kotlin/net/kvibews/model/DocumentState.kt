package net.kvibews.model

import java.time.ZonedDateTime

class DocumentState(
    val id: String,
    val name: String,
    val languageId: Int,
    var revision: Int = 0,
    var content: String = "",
    val operations: List<TextOperation> = mutableListOf(),
    var createdAt: ZonedDateTime = ZonedDateTime.now(),
    var updatedAt: ZonedDateTime = ZonedDateTime.now()
)